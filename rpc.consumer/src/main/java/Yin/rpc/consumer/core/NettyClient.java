
package Yin.rpc.consumer.core;

import java.util.List;

import io.netty.channel.*;
import org.apache.curator.framework.CuratorFramework;
import com.alibaba.fastjson.JSONObject;

import Yin.rpc.consumer.constans.Constans;
import Yin.rpc.consumer.handler.SimpleClientHandler;
import Yin.rpc.consumer.param.ClientRequest;
import Yin.rpc.consumer.param.Response;
import Yin.rpc.consumer.zk.ServerWatcher;
import Yin.rpc.consumer.zk.ZooKeeperFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


/**
 * NettyClient is a client implementation that utilizes Netty for communication and integrates
 * with ZooKeeper for service discovery. It manages communication channels and dynamically connects
 * to server nodes from the ZooKeeper service registry.
 *
 * Features:
 * - Sets up a Netty-based client for asynchronous communication.
 * - Uses ZooKeeper to discover and manage server nodes dynamically.
 * - Sends requests to server nodes and handles responses.
 * - Provides a mechanism to maintain and clean up connections.
 */

public class NettyClient {
	private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

	public static final Bootstrap b = new Bootstrap();
	private final EventLoopGroup workerGroup;
	private final ChannelManager channelManager;
	private final CuratorFramework zkClient;
	private final ServerWatcher serverWatcher;

	private static final long DEFAULT_TIMEOUT_SECONDS = 600L;
	private static final String DELIMITER = "#";

	public NettyClient() {
		this.workerGroup = new NioEventLoopGroup();
		this.channelManager = new ChannelManager();
		this.zkClient = ZooKeeperFactory.getClient();
		this.serverWatcher = new ServerWatcher();
		initialize();
	}

	private void initialize() {
		try {
			// 配置Bootstrap
			b.group(workerGroup)
					.channel(NioSocketChannel.class)
					.option(ChannelOption.SO_KEEPALIVE, true)
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) {
							ch.pipeline()
									.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]))
									.addLast(new StringDecoder())
									.addLast(new StringEncoder())
									.addLast(new SimpleClientHandler());
						}
					});

			// 初始化Zookeeper连接和服务发现
			initServiceDiscovery();

		} catch (Exception e) {
			logger.error("NettyClient initialization failed", e);
			shutdown();
			throw new RuntimeException("NettyClient initialization failed", e);
		}
	}

	private void initServiceDiscovery() throws Exception {
		// 获取初始服务列表
		List<String> serverPaths = zkClient.getChildren()
				.usingWatcher(serverWatcher)
				.forPath(Constans.SERVER_PATH);
		logger.info("zookeeper {} 路径下服务列表: serverPaths: {}",Constans.SERVER_PATH, serverPaths);

		createConnections(serverPaths);
	}

	public void createConnections(List<String> serverPaths) {
		Set<String> newServers = new HashSet<>();

		for (String path : serverPaths) {
			try {
				String[] parts = path.split(DELIMITER);
				if (parts.length <2) {
					logger.warn("Invalid server path format: {}", path);
					continue;
				}

				String host = parts[0];
				int port = Integer.parseInt(parts[1]);
				String serverKey = host + DELIMITER + port;

				if (!newServers.contains(serverKey)) {
					ChannelFuture channelFuture = b.connect(host, port).syncUninterruptibly();
					ChannelManager.addChnannel(channelFuture);
					newServers.add(serverKey);
					ChannelManager.realServerPath.add(serverKey);
				}
			} catch (Exception e) {
				logger.error("Failed to connect to server: " + path, e);
			}
		}

	}

	public Response send(ClientRequest request) {
		return send(request, DEFAULT_TIMEOUT_SECONDS);
	}

	public Response send(ClientRequest request, long timeoutSeconds) {

		ChannelFuture f	= ChannelManager.get(ChannelManager.position);
		if (f == null) {
			logger.error("Failed to get a valid ChannelFuture. Cannot send the request.");
			return null;
		}
		// 检查 channel 是否为 null
		if (f.channel() == null) {
			logger.error("The channel of the ChannelFuture is null. Cannot send the request.");
			return null;
		}
		try {
			//将请求对象序列化成 JSON 字符串并通过 channel 发送到对端（服务端）
			String msg = JSONObject.toJSONString(request) + "\r\n";
			f.channel().writeAndFlush(msg);

		}catch (Exception e){
			logger.error("Failed to send the request: {}", e.getMessage(), e);
			return null;
		}

        ResultFuture future = new ResultFuture(request);
		return future.get(timeoutSeconds);
	}

	public void shutdown() {
		try {
			channelManager.closeAll();
			workerGroup.shutdownGracefully().sync();
			zkClient.close();
		} catch (Exception e) {
			logger.error("Error during shutdown", e);
		}
	}
}



//public class NettyClient {
//
//
//	public static final Bootstrap b = new Bootstrap(); //Netty客户端启动类，用于配置网络连接参数
//
//	private static ChannelFuture f = null; //当前活动的网络连接通道（静态变量存在线程安全问题，实际应避免这样使用）。
//
//	private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);
//
//	static{
//		String host = "localhost";
//		int port = 8080;
//
//		EventLoopGroup work = new NioEventLoopGroup();  //Netty的I/O事件处理线程组，负责处理连接、读写事件。
//		try {
//		/**配置Bootstrap*/
//		b.group(work) //将之前创建的 EventLoopGroup 对象 work 分配给 Bootstrap 对象 b。
//			.channel(NioSocketChannel.class) //指定使用 NioSocketChannel 作为客户端的通道类型，用于进行网络通信。
//			.option(ChannelOption.SO_KEEPALIVE, true) //表示启用 TCP 保持活动状态，即当连接空闲一段时间后，会自动发送探测包以检测连接是否仍然有效
//			.handler(new ChannelInitializer<SocketChannel>() {
//						@Override
//						protected void initChannel(SocketChannel ch) throws Exception {
//
//							ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]));
//							ch.pipeline().addLast(new StringDecoder());//字符串解码器
//							ch.pipeline().addLast(new StringEncoder());//字符串编码器
//							ch.pipeline().addLast(new SimpleClientHandler());//业务逻辑处理处
//						}
//			});
//
//		/**Zookeeper服务发现*/
//			CuratorFramework client = ZooKeeperFactory.getClient();   //通过 ZooKeeperFactory 工厂类获取一个 CuratorFramework 对象 client，用于与 ZooKeeper 服务器进行交互。
//
//			//从 ZooKeeper 服务器中获取指定路径下的所有子节点列表，这些子节点表示可用的服务器节点。
//			List<String> serverPath = client.getChildren().forPath(Constans.SERVER_PATH);
//
//			//创建一个ServerWatcher对象 watcher，用于监听 ZooKeeper 服务器节点的变化。
//			//通过ServerWatcher监听服务节点变化（动态扩缩容）
//			CuratorWatcher watcher = new ServerWatcher();
//			//监听 /SERVER_PATH 的子节点变化（服务节点增删），当子节点发生变化时，会触发 ServerWatcher 中的相应方法，
//			// 触发时通过 process() 方法动态调整 Netty 连接池。
//			client.getChildren().usingWatcher(watcher).forPath(Constans.SERVER_PATH);
//
//		/**建立初始连接池*/
//			for(String path :serverPath){
//				//解析服务地址：格式为host#port（如192.168.1.1#8080）
//				String[] str = path.split("#");
//				ChannelManager.realServerPath.add(str[0]+"#"+str[1]);
//				ChannelFuture channnelFuture = NettyClient.b.connect(str[0], Integer.valueOf(str[1]));
//				ChannelManager.addChnannel(channnelFuture);
//			}
//
//		/**设置默认连接*/
//			if(ChannelManager.realServerPath.size()>0){
//				String[] netMessageArray = ChannelManager.realServerPath.toArray()[0].toString().split("#");
//				host = netMessageArray[0];
//				port = Integer.valueOf(netMessageArray[1]);
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	/**一个静态方法 send，用于向服务器发送请求并返回响应。
//	 * 获取可用连接：从ChannelManager轮询获取（存在线程安全问题）。
//	 * 发送请求：将JSON序列化的请求写入Channel，追加\r\n以匹配行分隔符。
//	 * 异步等待响应：通过ResultFuture阻塞获取响应（超时60秒）。
//	 */
//	public static Response send(ClientRequest request){
//		f=ChannelManager.get(ChannelManager.position);
//		f.channel().writeAndFlush(JSONObject.toJSONString(request)+"\r\n");
////		f.channel().writeAndFlush("\r\n");
//		Long timeOut = 60L;
//		ResultFuture future = new ResultFuture(request);
//		return future.get(timeOut);
//
//	}
//}

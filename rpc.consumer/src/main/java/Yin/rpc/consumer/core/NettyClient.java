
package Yin.rpc.consumer.core;

import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import com.alibaba.fastjson.JSONObject;

import Yin.rpc.consumer.constans.Constans;
import Yin.rpc.consumer.handler.SimpleClientHandler;
import Yin.rpc.consumer.param.ClientRequest;
import Yin.rpc.consumer.param.Response;
import Yin.rpc.consumer.zk.ServerWatcher;
import Yin.rpc.consumer.zk.ZooKeeperFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


import io.netty.channel.Channel;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * NettyClient 类负责管理与 Netty 服务器集群的通信，
 * 通过 Zookeeper 实现动态服务发现 和连接管理，提供高可靠的异步客户端实现。
 *
 * 这个类使用合适的配置来初始化一个 Netty 客户端，比如：
 * - 使用必要的处理器和选项来配置 Netty 的 `Bootstrap` 对象。
 * - 设置事件循环组来处理网络事件。
 * - 利用 ZooKeeper 动态地发现和监控服务器实例。
 *
 * 关键功能：
 * - 动态服务器管理：
 * 基于 Zookeeper Watcher 机制自动发现和监控服务节点。实时调整服务器连接池（增/删节点时自动生效）
 * - 连接生命周期管理：
 * 配置并维护 Netty Bootstrap 连接池。通过 ChannelFuture 管理活跃连接状态
 * - 消息处理
 * 内置 StringEncoder/Decoder 实现消息编解码。通过自定义处理器实现业务逻辑
 *
 * 类的行为：
 * - 在初始化过程中，它会根据从 ZooKeeper 接收到的信息连接到可用的服务器。
 * - 添加诸如字符串编码器、解码器以及用于业务逻辑处理的自定义处理器。
 * - 通过 `ChannelManager` 维护活动的服务器连接。
 * - 监控服务器的变化（新增或移除），并动态调整可用服务器池。
 *
 * 关键组件：
 * - `Bootstrap`：已配置的用于建立连接的 Netty 客户端实例。
 * - `ChannelFuture`：表示 I/O 操作的结果，用于发送和接收消息。
 * - `ZooKeeper`：用于服务发现，并通过监听器接收有关服务器变化的更新。
 * - `ClientRequest` 和 `Response`：作为请求/响应负载的数据模型。
 *
 * 方法：
 * - `send(ClientRequest request)`：向服务器发送一个请求，等待并获取相应的响应。
 *
 * 注意：
 * 针对初始化和服务器通信实现了恰当的异常处理。
 * 该类在很大程度上依赖于像 `ChannelManager` 这样的外部组件来管理连接，以及依赖 ZooKeeper 来实现动态服务器发现。
 */

public class NettyClient {
	private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);


	public static final Bootstrap b = new Bootstrap();
	private final EventLoopGroup workerGroup;
	private final ChannelManager channelManager;
	private final CuratorFramework zkClient;
	private final ServerWatcher serverWatcher;

	private static final int DEFAULT_TIMEOUT_SECONDS = 60;
	private static final String DELIMITER = "#";

	public NettyClient(String zkAddress) {
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

		updateConnections(serverPaths);
	}

	public void updateConnections(List<String> serverPaths) {
		Set<String> newServers = new HashSet<>();

		for (String path : serverPaths) {
			try {
				String[] parts = path.split(DELIMITER);
				if (parts.length != 2) {
					logger.warn("Invalid server path format: {}", path);
					continue;
				}

				String host = parts[0];
				int port = Integer.parseInt(parts[1]);
				String serverKey = host + DELIMITER + port;

				if (!newServers.contains(serverKey)) {
					ChannelFuture channelFuture = b.connect(host, port).syncUninterruptibly();
					channelManager.addChnannel(channelFuture);
					newServers.add(serverKey);
					ChannelManager.realServerPath.add(serverKey);
				}
			} catch (Exception e) {
				logger.error("Failed to connect to server: " + path, e);
			}
		}

		// 移除不再存在的连接
		//channelManager.removeInactiveConnections(newServers);
	}

	public static Response send(ClientRequest request) {
		return send(request, DEFAULT_TIMEOUT_SECONDS);
	}

	public static Response send(ClientRequest request, long timeoutSeconds) {
		ChannelFuture f	=ChannelManager.get(ChannelManager.position);
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
			f.channel().writeAndFlush(JSONObject.toJSONString(request) + "\r\n");
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
//		Long timeOut = 60l;
//		ResultFuture future = new ResultFuture(request);
//		return future.get(timeOut);
//
//	}
//
//}

package bean;

import java.net.InetAddress;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import constants.Constans;
import factory.ZooKeeperFactory;
import handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * `NettyInitial` 类实现了 `ApplicationListener` 接口，用于监听 `ContextRefreshedEvent` 事件，以便在应用程序启动时引导并初始化一个 Netty 服务器。该类负责设置必要的 Netty 配置，如事件循环、服务器管道和处理器。它还会将服务器实例注册到 ZooKeeper 服务中，以实现服务发现。
 * 主要功能包括：
 * - 使用 `ServerBootstrap` 配置并启动 Netty 服务器。
 * - 为服务器端管道初始化处理器，用于执行特定任务，如解码、编码和业务逻辑处理。
 * - 将服务器实例信息注册到 ZooKeeper 节点。
 * - 在发生异常时，优雅地关闭服务器及相关资源。
 * 实现接口：
 * - `ApplicationListener<ContextRefreshedEvent>`：确保服务器作为 Spring 应用程序生命周期的一部分，在应用程序上下文刷新时启动。
 */
@Component
public class NettyInitial implements ApplicationListener<ContextRefreshedEvent> {
	
	public  void start() {		
		NioEventLoopGroup boss = new NioEventLoopGroup();
		NioEventLoopGroup work = new NioEventLoopGroup();
			
		try {//启动辅助
			ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(boss, work)
				   .option(ChannelOption.SO_BACKLOG, 128)//设置TCP队列大小:包含已连接+未连接
				   .option(ChannelOption.SO_KEEPALIVE, false)//不使用默认的心跳机制
				   .channel(NioServerSocketChannel.class)
				   .childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						// 设置\r\n为分隔符
						ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]));
						ch.pipeline().addLast(new StringDecoder());//字符串解码器
//						ch.pipeline().addLast(new IdleStateHandler(20, 15, 10, TimeUnit.SECONDS));
						ch.pipeline().addLast(new ServerHandler());//业务逻辑处理处
						ch.pipeline().addLast(new StringEncoder());//字符串编码器
					}
				   });
	
			int port = 8080;
			ChannelFuture f = serverBootstrap.bind(8080).sync();
		
			InetAddress address = InetAddress.getLocalHost();
			CuratorFramework client = ZooKeeperFactory.getClient();
			if(client != null){
				System.out.println(client);
				client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(Constans.SERVER_PATH+"/"+address.getHostAddress()+"#"+port+"#");
				System.out.println("成功");

			}
		
			f.channel().closeFuture().sync();
		
			System.out.println("Closed");
		} catch (Exception e) {
			e.printStackTrace();
			boss.shutdownGracefully();
			work.shutdownGracefully();
		}
	
	}

	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent arg0) {
		this.start();		
	}
	
	
	
}

package Yin.rpc.consumer.zk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;

import Yin.rpc.consumer.core.ChannelManager;
import Yin.rpc.consumer.core.NettyClient;
import io.netty.channel.ChannelFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * `ServerWatcher` 类实现了 `CuratorWatcher` 接口，用于监控 ZooKeeper 节点的变化。
 * 它会对特定的 ZooKeeper 事件做出反应，并相应地更新服务器的网络配置。
 * 这个类负责管理服务器路径、初始化新的服务器连接，以及更新通道管理。
 *
 */
public class ServerWatcher implements CuratorWatcher {
	private static final Logger logger = LoggerFactory.getLogger(ServerWatcher.class);

	@Override
	public void process(WatchedEvent event) throws Exception {
		//WatchedEvent event 参数包含了事件的详细信息，如事件类型、节点路径等。
		System.out.println("process------------------------");
		//logger.info("Processing ZooKeeper event: {}", event);
		CuratorFramework client = ZooKeeperFactory.getClient(); //获取 ZooKeeper 客户端实例
		String path = event.getPath();
		//重新为指定路径下的子节点设置监听器，usingWatcher(this) 表示使用当前的 ServerWatcher 实例来监听节点变化。这样，当该路径下的子节点再次发生变化时，process 方法会再次被调用。
		client.getChildren().usingWatcher(this).forPath(path);

		List<String> newServerPaths = client.getChildren().forPath(path);
		System.out.println("当前节点"+path+"下所有子节点: "+newServerPaths);

		//去重
		Set<String> activeServers = new HashSet<>();
		for(String realServer:newServerPaths){
			if(!activeServers.contains(realServer)){
				activeServers.add(realServer);
			}

		}

		//清空并更新服务器路径集合
		ChannelManager.realServerPath.clear();
		for(String p :activeServers){
			String[] str = p.split("#");
			ChannelManager.realServerPath.add(str[0]+"#"+str[1]);//去重
		}
		
		ChannelManager.clearChnannel();
		for(String realServer: ChannelManager.realServerPath){
			String[] str = realServer.split("#");
			ChannelFuture channnelFuture = NettyClient.b.connect(str[0], Integer.valueOf(str[1]));
			ChannelManager.addChnannel(channnelFuture);		
		}
	}
}

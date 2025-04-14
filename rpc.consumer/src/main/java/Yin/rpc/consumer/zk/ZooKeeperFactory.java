package Yin.rpc.consumer.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * ZooKeeperFactory 是一个工具类，用于提供 `CuratorFramework` ZooKeeper 客户端的单例实例。
 * 它能确保以线程安全的方式按需初始化该客户端，以便与 ZooKeeper 服务进行交互。
 *
 * 该类使用 {@link ExponentialBackoffRetry} 重试策略来处理连接 ZooKeeper 时的重试操作。
 * `CuratorFramework` 实例在创建时会自动启动，并在整个应用程序中共享。
 */

public class ZooKeeperFactory {
	public static CuratorFramework client;

	//CuratorFramework 是 Apache Curator 框架中用于与 ZooKeeper 服务进行交互的客户端接口
	//可以创建持久节点、临时节点、顺序节点等不同类型的节点。
	//可以删除指定路径的节点，并且可以选择递归删除节点及其子节点。
	//可以获取指定节点存储的数据。可以更新指定节点存储的数据。
	
	public static CuratorFramework getClient(){
		if(client == null){ //说明还没有创建 ZooKeeper 客户端实例，需要进行初始化。
			RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);//重试机制
			client = CuratorFrameworkFactory.newClient("localhost:2181", retryPolicy);
			client.start(); //启动 CuratorFramework 客户端实例，使其可以开始与 ZooKeeper 服务器进行通信。
		}
		
		return client;
	}
	
	public static void main(String[] args) {
		try {
			String s = "hello world";
			CuratorFramework client = ZooKeeperFactory.getClient();
			if(client != null){
				client.create().forPath("/netty1",s.getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
			}
		}
		
}

package Yin.rpc.consumer.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * ChannelManager 类提供了用于管理  Netty 的 ChannelFuture 对象集合并与之交互的方法。
 * 它维护一个线程安全的【通道列表】、一个【服务器路径列表】和一个用于管理通道使用情况的原子计数器。
 * 该类主要支持添加、删除、清除和检索 ChannelFuture 实例
 * ChannelFuture 代表一个异步操作的结果，通常用于异步网络通信。
 * 同时还支持使用 Position Counter 的简单循环机制。
 */
public class ChannelManager {

	private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

	//这是一个线程安全的列表，用于存储 ChannelFuture 对象。CopyOnWriteArrayList 是 Java 并发包中的一个类，
	//它的特点是在写入操作（如添加、删除元素）时会复制一份原数组，在新数组上进行操作，最后将引用指向新数组。
	//这样做的好处是读操作（如遍历）不需要加锁，不会被写入操作阻塞，保证了并发访问的安全性。该列表用于存储打开的通信通道。
	public static CopyOnWriteArrayList<ChannelFuture>  channelFutures = new CopyOnWriteArrayList<ChannelFuture>();

	// 同样是线程安全的字符串列表，可能用于存储服务器路径（如服务器地址或主机名）。
	// 这些服务器路径与 channelFutures 列表中的通道一一对应，通过它们可以知道每个通道对应的服务器信息。
	public static  CopyOnWriteArrayList<String> realServerPath=new CopyOnWriteArrayList<String>();

	// 这是一个原子计数器，用于确定选择 ChannelFuture 对象的当前位置。
	// AtomicInteger 提供了原子操作，如 getAndIncrement() 等，避免了手动同步的复杂性，支持轮询机制。
	public static AtomicInteger  position = new AtomicInteger(0);//先采用轮询的方式使用send

	// 该方法的作用是从 channelFutures 列表中移除指定的 ChannelFuture 对象。
	// 其目的是移除不活跃或有故障的连接，保证列表中存储的都是有效的通道。
	public static void removeChnannel(ChannelFuture channel){
		channelFutures.remove(channel);
	}

	// 此方法用于将给定的 ChannelFuture 对象添加到 channelFutures 列表中。目的是注册新的通信通道，当有新的服务器连接建立时，可以调用该方法将对应的通道添加到管理列表中。
	public static void addChnannel(ChannelFuture channel){
		channelFutures.add(channel);
	}

	// 该方法会完全清空 channelFutures 列表，移除所有的通道。其作用是重置存储的通道，例如在系统重新初始化或者需要清空通道列表时调用。
	public static void clearChnannel(){
		channelFutures.clear();
		closeAll();
		//logger.info("Channel list cleared");
	}

	//	该方法基于传入的原子整数 i 以轮询的方式获取一个 ChannelFuture 对象。
	public static ChannelFuture get(AtomicInteger index) {
		// 检查 channelFutures 列表是否为空
		if (channelFutures == null || channelFutures.isEmpty()) {
			return null;
		}
		//目前采用轮循机制
		ChannelFuture channelFuture = null;
		int size = channelFutures.size();

		// 如果当前位置超出列表大小，重置位置并获取第一个元素
//		if(i.get()>=size){
//			channelFuture = channelFutures.get(0);
//			ChannelManager.position= new AtomicInteger(1);
//		}else{
//			channelFuture = channelFutures.get(i.getAndIncrement());
//		}
//		return channelFuture;
		int currentIndex = index.getAndUpdate(i -> (i + 1) % size); // getAndUpdate 是 AtomicInteger 的原子操作方法，原子性地获取当前索引值，并更新为下一个轮询位置
		return channelFutures.get(currentIndex);
	}


	/**
	 * 优雅关闭所有连接
	 * @param timeoutMs 等待关闭的超时时间(毫秒)
	 */
	public static void closeAll(long timeoutMs) {
		if (channelFutures.isEmpty()) {
			return;
		}

		logger.info("Closing all {} active connections...", channelFutures.size());

		for (ChannelFuture channelFuture : channelFutures) {
			try {
				if (channelFuture != null && channelFuture.channel() != null) {
					Channel channel = channelFuture.channel();

					// 先尝试优雅关闭
					if (channel.isActive()) {
						channel.close().addListener(future -> {
							if (!future.isSuccess()) {
								logger.warn("Channel close failed: {}", future.cause().getMessage());
							}
						});
					}

					// 强制关闭未及时关闭的连接
					if (!channel.closeFuture().await(timeoutMs, TimeUnit.MILLISECONDS)) {
						channel.close().syncUninterruptibly();
						logger.warn("Force closed channel: {}", channel.id());
					}
				}
			} catch (Exception e) {
				logger.error("Error closing channel: {}", e.getMessage());
			} finally {
				channelFutures.remove(channelFuture);
			}
		}

		channelFutures.clear();
		position.set(0);
		logger.info("All connections closed");
	}

	/**
	 * 默认超时时间的关闭方法
	 */
	public static void closeAll() {
		closeAll(5000); // 默认5秒超时
	}
	
}

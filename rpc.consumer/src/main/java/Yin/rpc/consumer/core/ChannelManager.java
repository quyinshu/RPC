package Yin.rpc.consumer.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.ChannelFuture;

/**
 * ChannelManager 类提供了用于管理  Netty 的 ChannelFuture 对象集合并与之交互的方法。
 * 它维护一个线程安全的【通道列表】、一个【服务器路径列表】和一个用于管理通道使用情况的原子计数器。
 * 该类主要支持添加、删除、清除和检索 ChannelFuture 实例
 * 同时还支持使用 Position Counter 的简单循环机制。
 */
public class ChannelManager {
	public static CopyOnWriteArrayList<ChannelFuture>  channelFutures = new CopyOnWriteArrayList<ChannelFuture>();
	public static  CopyOnWriteArrayList<String> realServerPath=new CopyOnWriteArrayList<String>();
	public static AtomicInteger  position = new AtomicInteger(0);//先采用轮询的方式使用send

	public static void removeChnannel(ChannelFuture channel){
		channelFutures.remove(channel);
	}
	
	public static void addChnannel(ChannelFuture channel){
		channelFutures.add(channel);
	}
	public static void clearChnannel(){
		channelFutures.clear();
	}

	public static ChannelFuture get(AtomicInteger i) {
		
		//目前采用轮循机制
		ChannelFuture channelFuture = null;
		int size = channelFutures.size();
		if(i.get()>=size){
			channelFuture = channelFutures.get(0);
			ChannelManager.position= new AtomicInteger(1);
		}else{
			channelFuture = channelFutures.get(i.getAndIncrement());
		}
		return channelFuture;
	}
	
}

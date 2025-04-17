package Yin.provider.handler;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.alibaba.fastjson.JSONObject;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import Yin.provider.medium.Medium;
import Yin.provider.model.Response;
import Yin.provider.model.ServerRequest;

public class ServerHandler extends ChannelInboundHandlerAdapter  {
	private static final Executor exec = Executors.newFixedThreadPool(10);
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("服务器收到的原始请求为:"+msg.toString());
//		ServerRequest serverRequest = JSONObject.parseObject(msg.toString(), ServerRequest.class);
//		System.out.println(serverRequest.getCommand());
		exec.execute(new Runnable() {
			
			@Override
			public void run() {
				ServerRequest serverRequest = JSONObject.parseObject(msg.toString(), ServerRequest.class);
//				System.out.println("serverRequest的Command："+serverRequest.getCommand());
//				System.out.println("serverRequest的ID："+serverRequest.getId());

				Medium medium = Medium.newInstance();//生成中介者模式
				Response response = medium.process(serverRequest);   //根据serverRequest.getCommand() 获得对应的处理bean

				//向客户端发送Resonse

				String json = JSONObject.toJSONString(response) + "\r\n";
				System.out.println("💬 服务端发出的 JSON: " + json);
				ctx.channel().writeAndFlush(json);

			}
		});
//		Medium medium = Medium.newInstance();//生成中介者模式
//		
//		Response response = medium.process(serverRequest);
//		
//		//向客户端发送Resonse
//		ctx.channel().writeAndFlush(JSONObject.toJSONString(response)+"\r\n");
		
	}

//	@Override
//	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//		
//		if(evt instanceof IdleStateEvent){
//			IdleStateEvent event = (IdleStateEvent)evt;
//			
//			if(event.state().equals(IdleState.READER_IDLE)){
//				System.out.println("读空闲");
//			}
//			if(event.state().equals(IdleState.WRITER_IDLE)){
//				System.out.println("写空闲");
//			}
//			if(event.state().equals(IdleState.ALL_IDLE)){
//				System.out.println("读写空闲");
//				ctx.channel().writeAndFlush("ping\r\n");
//			}
//		}
//	}
	
	
	
}

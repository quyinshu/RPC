package Yin.rpc.consumer.handler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Yin.rpc.consumer.param.ClientRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import Yin.rpc.consumer.core.ResultFuture;
import Yin.rpc.consumer.param.Response;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * `SimpleClientHandler` 是 `ChannelInboundHandlerAdapter` 的一个自定义实现类，用于处理 Netty 通道中的入站消息。该类主要用于客户端需要处理从服务器接收的消息并处理异步操作的场景。
 *
 * 此类处理传入的消息，例如 “ping”，并回复 “pong” 消息以保持通信。此外，它将接收到的消息解析为 `Response` 对象，并使用线程池对其进行异步处理。
 *
 * 主要职责：
 * - 处理入站通道读取事件并处理消息。
 * - 响应特定的预定义消息，如 “ping”。
 * - 使用 `ExecutorService` 异步处理 `Response` 对象。
 * - 将已处理的 `Response` 对象委托给 `ResultFuture` 进行进一步处理和同步。
 */
public class SimpleClientHandler extends ChannelInboundHandlerAdapter {
	//ChannelInboundHandlerAdapter：Netty 提供的入站处理器适配器（简化事件处理）
	private static final Logger logger = LoggerFactory.getLogger(SimpleClientHandler.class);

	//创建固定大小为10的线程池,异步处理服务端响应，避免阻塞Netty的I/O线程
	private static final Executor exec = Executors.newFixedThreadPool(10);

	/**触发时机：当收到服务端消息时自动调用。
	 * 参数：
	 * ctx：处理器上下文（可操作Channel和Pipeline）
	 * msg：解码后的消息对象（前序处理器已处理）
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		//心跳检测处理，检测心跳消息 "ping"，立即回复 "pong\r\n"
		final Object message = msg;
		if(message.toString().equals("ping")){
			//System.out.println("收到读写空闲ping,向服务端发送pong");
			logger.info("收到读写空闲ping,向服务端发送pong");
			ctx.channel().writeAndFlush("pong\r\n"); //把pong传入管道给服务器端
		}
		String json = (String) message;
//		System.out.println("收到服务端返回的原始数据: " + message.toString());
//		ClientRequest request = JSONObject.parseObject(json, ClientRequest.class);
//		logger.info("请求命令: {}, 参数: {}", request.getCommand(), request.getContent());
		
		//设置response
		exec.execute(new Runnable() {
			public void run() {
				//ctx.channel().writeAndFlush("客户端已成功收到响应" + "\r\n");

				//把服务端返回的 msg（JSON字符串）转换成 Java 的 Response 对象。
				Response response = JSONObject.parseObject(message.toString(), Response.class);

				////通过response的ID可以在map中找到对应的Request,并为相应的request设置response,使得调用get()客户端得到结果
				ResultFuture.receive(response);

				logger.info("从服务器端收到的响应"+JSONObject.toJSONString(response));
			}
		});
	}
	
}

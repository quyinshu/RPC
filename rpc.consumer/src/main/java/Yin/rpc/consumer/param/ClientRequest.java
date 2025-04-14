package Yin.rpc.consumer.param;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 表示客户端的请求参数，该请求包含一个标识符、内容和一条命令。
 * 此类用于在客户端 - 服务器通信模型中创建、管理和传输请求。
 *
 * 该类的每个实例都会被分配一个使用原子长整型生成的唯一标识符。
 * 请求中携带实际的内容（参数）以及一个命令字符串，该命令字符串通常用作映射到特定操作或处理器的键。
 */
public class ClientRequest {
	private Long id ;
	private Object content;//方法参数
	private static AtomicLong realID = new AtomicLong(0);
	private String command;//media.map里的key
	
	
	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public ClientRequest(){
		id =  realID.incrementAndGet();
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}
	
	public Long getId() {
		return id;
	}
	
	
}

package Yin.rpc.consumer.param;

/**
 * 表示客户端 - 服务器通信中的标准化响应。
 *
 * 此类封装了响应的唯一标识符、结果数据、响应码以及可选的失败消息。响应码默认值为 "00000"，表示成功，其他值则表示失败。可选消息在失败时提供额外信息。
 */
public class Response {
	private Long id;
	private Object result;
	private String code = "00000";//00000表示成功，其他表示失败
	private String msg;//失败信息
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
	
	
}

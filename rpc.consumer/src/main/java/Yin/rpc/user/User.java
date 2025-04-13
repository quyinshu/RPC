package Yin.rpc.user;


/**
 * 表示一个包含 ID 和姓名的基本用户资料。
 * 该类在远程服务交互中充当数据传输对象。
 * 它为其字段提供了 getter 和 setter 方法，以便对其属性进行可控访问。
 */
public class User {
	private Integer id;
	private String name;
	
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}

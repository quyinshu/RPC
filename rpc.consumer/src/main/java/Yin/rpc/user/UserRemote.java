package Yin.rpc.user;

import java.util.List;

import Yin.rpc.consumer.param.Response;



/**
 * 表示一个用于管理用户实体的远程服务。
 * 提供了保存单个用户或用户列表的方法。
 */
public interface UserRemote {
	public Response saveUser(User user);
	public Response saveUsers(List<User> userlist);
}

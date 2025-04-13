package Yin.rpc.user;

import Yin.rpc.consumer.param.Response;


/**
 * 此接口提供了一个用于测试用户相关功能的远程服务方法。
 * 其目的是通过调用接口中定义的方法与远程服务进行交互。
 */
public interface TestRemote {
	public Response testUser(User user);
}

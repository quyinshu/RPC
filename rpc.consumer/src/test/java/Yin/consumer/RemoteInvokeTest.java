package Yin.consumer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import Yin.rpc.consumer.annotation.RemoteInvoke;
import Yin.rpc.user.TestRemote;
import Yin.rpc.user.User;

/**
 * `RemoteInvokeTest`类用作一个测试套件，用于使用`{@link RemoteInvoke}`注解来测试服务的远程调用。
 * 该类被配置为使用`SpringJUnit4ClassRunner`运行，并利用Spring应用上下文来进行依赖注入和管理。
 * 这个类主要专注于通过`{@link TestRemote}`接口，借助以`{@link User}`对象作为输入的方法来测试与远程服务的交互。
 * `{@link User}`的一个实例被静态初始化，以提供预定义的测试数据。
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=RemoteInvokeTest.class)
@ComponentScan("\\")
public class RemoteInvokeTest {
	public static List<User> list = new ArrayList<User>();
	@RemoteInvoke
	public static TestRemote userremote;
	public static User user;
	public static Long count = 0l;
	
	static{
		user = new User();
		user.setId(1000);
		user.setName("张三");
	}
	@Test
	public void testSaveUser(){
		User user = new User();
		user.setId(1000);
		user.setName("张三");
		userremote.testUser(user);
//		Long start = System.currentTimeMillis();
//		for(int i=1;i<10000;i++){
//			userremote.testUser(user);
//		}
//		Long end = System.currentTimeMillis();
//		Long count = end-start;
//		System.out.println("总计时:"+count/1000+"秒");
		
	}			


}

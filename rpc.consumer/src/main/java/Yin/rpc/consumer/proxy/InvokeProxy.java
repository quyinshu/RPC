package Yin.rpc.consumer.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.HashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import Yin.rpc.consumer.annotation.RemoteInvoke;
import Yin.rpc.consumer.core.NettyClient;
import Yin.rpc.consumer.param.ClientRequest;
import Yin.rpc.consumer.param.Response;

/**
 * 一个 Spring 组件，实现了 {@link BeanPostProcessor} 接口，用于处理应用上下文中的 Bean，
 * 并为使用 {@link RemoteInvoke} 注解标注的字段启用远程方法调用功能。
 *
 * 该类主要执行两项任务：
 * - 利用 {@code postProcessAfterInitialization} 方法在 Bean 初始化之后进行拦截，不过当前实现仅原样返回 Bean，未作进一步处理。
 * - 在 {@code postProcessBeforeInitialization} 方法中对 Bean 进行扫描和处理，为使用 {@link RemoteInvoke} 注解标注的字段动态创建代理对象。
 *
 * InvokeProxy 借助 {@link Enhancer} 类创建的动态代理，拦截被注解字段上的方法调用，
 * 构建一个 {@link ClientRequest} 对象，通过 {@link NettyClient} 发送请求，并返回获取到的响应。
 *
 * 这样做通过抽象底层的网络通信和方法调用过程，实现了与远程服务的透明交互。代理会动态地将远程方法调用绑定到被注解的字段上，简化了远程服务的集成，确保了客户端与服务器交互的一致性。
 */
@Component
public class InvokeProxy implements BeanPostProcessor {
	public static Enhancer enhancer = new Enhancer();

	public Object postProcessAfterInitialization(Object bean, String arg1) throws BeansException {
		return bean;
	}
	//对属性的所有方法和属性类型放入到HashMap中
	private void putMethodClass(HashMap<Method, Class> methodmap, Field field) {
		Method[] methods = field.getType().getDeclaredMethods();
		for(Method method : methods){
			methodmap.put(method, field.getType());
		}
		
	}

	public Object postProcessBeforeInitialization(Object bean, String arg1) throws BeansException {
//		System.out.println(bean.getClass().getName());
		Field[] fields = bean.getClass().getDeclaredFields();
		for(Field field : fields){
			if(field.isAnnotationPresent(RemoteInvoke.class)){
				field.setAccessible(true);
				
//				final HashMap<Method, Class> methodmap = new HashMap<Method, Class>();
//				putMethodClass(methodmap,field);
//				Enhancer enhancer = new Enhancer();
				enhancer.setInterfaces(new Class[]{field.getType()});
				enhancer.setCallback(new MethodInterceptor() {
					
					public Object intercept(Object instance, Method method, Object[] args, MethodProxy proxy) throws Throwable {
						ClientRequest clientRequest = new ClientRequest();
						clientRequest.setContent(args[0]);
//						String command= methodmap.get(method).getName()+"."+method.getName();
						String command = method.getName();//修改
//						System.out.println("InvokeProxy中的Command是:"+command);
						clientRequest.setCommand(command);
						
						Response response = NettyClient.send(clientRequest);
						return response;
					}
				});
				try {
					field.set(bean, enhancer.create());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return bean;
	}

}

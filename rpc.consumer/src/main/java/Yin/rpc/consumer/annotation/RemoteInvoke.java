package Yin.rpc.consumer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import Yin.rpc.consumer.core.NettyClient;
import Yin.rpc.consumer.proxy.InvokeProxy;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 此注解用于标记一个字段，该字段用于远程方法调用。
 * 它通常用于注入一个动态生成的代理对象，该代理对象允许在远程服务上调用方法。利用这个代理对象就能在远程服务上调用方法。
 * 被注解的字段应该是一个接口，代理将处理与远程服务的通信细节。
 *
 * 这个注解由特定的 {@link BeanPostProcessor}（如 {@link InvokeProxy}）进行处理，
 * 该处理器会扫描带有 {@code @RemoteInvoke} 注解的字段，生成代理实现，并将代理对象设置到该字段中。
 *
 * 注解使用细节：
 * - 被注解的字段必须在 Spring 管理的组件内声明。
 * - 字段的类型应该是一个接口，其方法要与远程服务的契约相匹配。
 * - 为该字段生成的代理会拦截方法调用，构建远程请求，通过网络客户端（如 {@link NettyClient}）发送请求，并处理响应。
 *
 * 一个示例用例可能涉及在 Spring 应用上下文中测试特定接口和输入数据对象的远程服务调用。此注解简化了【标记字段】和【将其与方法调用机制关联】的过程。
 *
 * 目标：只能应用于字段。
 * 保留策略：在运行时保留。
 * 文档化：包含在生成的 Javadoc 中。
 * 组件：表明它是一个 Spring 管理的组件。
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RemoteInvoke {
	
}
//代理的工作流程
//为字段生成的代理会拦截方法调用，然后执行以下步骤：
//构建远程请求：依据调用的方法和传入的参数，生成合适的远程请求。
//发送请求：借助网络客户端（例如 NettyClient）把请求发送到远程服务。
//处理响应：接收并处理远程服务返回的响应，最后把结果返回给调用者。
//在 Spring 管理的组件内部声明，是指在被 Spring 框架【识别和管理】的**类中定义成员（如字段、方法等）。
//当一个类被 @Component 注解标记后，Spring 会在应用程序启动时扫描并实例化这个类，将其实例纳入 Spring 的容器（ApplicationContext）进行管理
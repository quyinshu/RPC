package Yin.rpc.consumer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * `Remote` 注解用于在分布式应用程序中将某个类或方法标记为可远程访问的组件。
 * 它是一种特殊的原型注解，扩展了 Spring 的 `Component` 注解的功能。
 *
 * 此注解可应用于类或方法级别。当使用该注解时，意味着被注解的类或方法将被发现并可进行远程调用。
 *
 * 属性：
 * - value：一个可选属性，用于指定一个字符串值，可作为被注解元素的标识符或名称。
 *
 * 保留策略：
 * - 该注解的保留策略设置为运行时，这意味着在运行时可通过反射获取该注解。
 *
 * 目标：
 * - 此注解可应用于类型（类、接口或枚举声明）和方法。
 *
 * 预期用途：
 * - 该注解常用于分布式系统中不同组件之间的远程通信场景，可用于识别和注册远程服务。
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Remote {
	String value() default "";
}

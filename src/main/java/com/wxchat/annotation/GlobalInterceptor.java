package com.wxchat.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

/**
 * 自定义注解：用于登录拦截校验
 */
@Target({ElementType.METHOD, ElementType.TYPE}) //作用目标: 方法上
@Retention(RetentionPolicy.RUNTIME)             //运行时调用
@Documented
@Mapping
public @interface GlobalInterceptor {

    /**
     * 校验登录
     *
     * @return
     */
    boolean checkLogin() default true;

    /**
     * 校验管理员
     *
     * @return
     */
    boolean checkAdmin() default false;

}

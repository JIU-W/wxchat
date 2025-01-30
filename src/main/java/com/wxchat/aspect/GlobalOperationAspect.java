package com.wxchat.aspect;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.entity.enums.ResponseCodeEnum;
import com.wxchat.exception.BusinessException;
import com.wxchat.redis.RedisUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * @description 自定义AOP切面
 * @author JIU-W
 * @date 2025-01-28
 * @version 1.0
 */
@Component("operationAspect")
@Aspect
public class GlobalOperationAspect {

    @Resource
    private RedisUtils redisUtils;

    private static Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);


    @Before("@annotation(com.wxchat.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point) {
        try {
            //获取方法
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            //获取该方法的注解
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (null == interceptor) {
                return;
            }
            /**
             * 校验登录
             */
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
        } catch (BusinessException e) {
            logger.error("全局拦截器异常", e);
            throw e;
        } catch (Exception e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        } catch (Throwable e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    //校验登录、是否是管理员
    private void checkLogin(Boolean checkAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("token");
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
        //判断是否登录
        if (tokenUserInfoDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        //判断是否是管理员
        if (checkAdmin && !tokenUserInfoDto.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

}

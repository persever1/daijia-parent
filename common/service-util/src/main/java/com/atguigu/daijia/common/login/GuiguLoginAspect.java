package com.atguigu.daijia.common.login;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @FileName GuiguLoginAspect
 * @Description
 * @Author mark
 * @date 2024-07-29
 **/
@Slf4j
@Component
@Aspect
@Order(100)
public class GuiguLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 切面处理方法，用于在指定的控制器方法执行前进行登录验证。
     * 使用@Around注解表明这是一个环绕通知，可以在目标方法执行前后进行操作。
     *
     * @param joinPoint 切点对象，包含目标方法的信息。
     * @param guiguLogin 登录注解对象，用于标记需要登录的方法。
     * @return 目标方法的返回值。
     * @throws Throwable 如果目标方法执行过程中抛出异常，则会向上抛出。
     */
    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(guiguLogin)")
    public Object process(ProceedingJoinPoint joinPoint, GuiguLogin guiguLogin) throws Throwable {
        // 获取当前请求的属性
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        // 将RequestAttributes转换为ServletRequestAttributes，以便获取HttpServletRequest
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        // 从HttpServletRequest中获取token
        HttpServletRequest request = sra.getRequest();
        String token = request.getHeader("token");

        // 检查token是否存在，如果不存在则抛出登录认证异常
        if(!StringUtils.hasText(token)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        // 从Redis中根据token获取用户ID
        String userId = (String)redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX+token);
        // 如果用户ID存在，则设置到AuthContextHolder中，用于后续的权限检查
        if(StringUtils.hasText(userId)) {
            AuthContextHolder.setUserId(Long.parseLong(userId));
        }
        // 继续执行目标方法
        return joinPoint.proceed();
    }


}

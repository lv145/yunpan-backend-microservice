package com.lgg.yunpanbackendquestionservice.aop;

import com.lgg.yunpanbackendcommon.annotation.AuthCheck;
import com.lgg.yunpanbackendcommon.common.ErrorCode;
import com.lgg.yunpanbackendcommon.constant.UserConstant;
import com.lgg.yunpanbackendcommon.exception.BusinessException;
import com.lgg.yunpanbackendmodel.model.entity.User;
import com.lgg.yunpanbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验切面：拦截所有标注 @AuthCheck 的方法
 */
@Aspect
@Component
@Slf4j
public class AuthAspect {

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 拦截所有标注了 @AuthCheck 的方法
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 从当前线程上下文取到请求（切面方法参数里没有 HttpServletRequest）
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        String mustRole = authCheck.mustRole();

        // 2. 获取当前登录用户；未登录时 userService.getLoginUser 内部会抛 NOT_LOGIN_ERROR
        User loginUser = userFeignClient.getLoginUser(request);
        String userRole = loginUser.getUserRole();

        // 3. 角色校验
        if (mustRole != null && !mustRole.isEmpty()) {
            if (UserConstant.ADMIN_ROLE.equals(mustRole)
                    && !UserConstant.ADMIN_ROLE.equals(userRole)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            if (!mustRole.equals(userRole)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // 4. 校验通过，放行执行原方法
        return joinPoint.proceed();
    }
}

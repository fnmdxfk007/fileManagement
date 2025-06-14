package com.netdisk.aspect;

import com.netdisk.annotation.GlobalInterceptor;
import com.netdisk.annotation.VerifyParam;
import com.netdisk.entity.config.AppConfig;
import com.netdisk.entity.constants.Constants;
import com.netdisk.entity.dto.SessionWebUserDto;
import com.netdisk.entity.enums.ResponseCodeEnum;
import com.netdisk.entity.po.UserInfo;
import com.netdisk.entity.query.UserInfoQuery;
import com.netdisk.exception.BusinessException;
import com.netdisk.service.UserInfoService;
import com.netdisk.utils.StringTools;
import com.netdisk.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Component("operationAspect")
@Aspect
/**
 *  日志
 */
public class GlobalOperationAspect {

    //日志记录器
    private static Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);

    //定义常见类型的全限定名
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";

    //用户信息服务
    @Resource
    private UserInfoService userInfoService;

    //应用配置
    @Resource
    private AppConfig appConfig;

    //定义切点，拦截带有@GlobalInterceptor注解的方法
    @Pointcut("@annotation(com.netdisk.annotation.GlobalInterceptor)")
    private void requestInterceptor() {
    }

    //在目标方法执行前进行拦截处理
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) throws BusinessException {
        try {
            // 获取目标对象和方法信息
            Object target = point.getTarget();
            Object[] arguments = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName, parameterTypes);

            // 获取方法上的@GlobalInterceptor注解
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (null == interceptor) {
                return;
            }

            // 校验登录
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }

            // 校验参数
            if (interceptor.checkParams()) {
                validateParams(method, arguments);
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

    // 校验登录状态
    private void checkLogin(Boolean checkAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionUser = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);

        if (sessionUser == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                sessionUser = new SessionWebUserDto();
                sessionUser.setUserId(userInfo.getUserId());
                sessionUser.setNickName(userInfo.getNickName());
                sessionUser.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, sessionUser);
            }
        }

        //未登录抛出异常
        if (null == sessionUser) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        //需要管理员权限但当前用户不是管理员
        if (checkAdmin && !sessionUser.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

    //校验方法参数
    private void validateParams(Method m, Object[] arguments) throws BusinessException {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }

            // 基本数据类型校验
            if (TYPE_STRING.equals(parameter.getParameterizedType().getTypeName()) || 
                TYPE_LONG.equals(parameter.getParameterizedType().getTypeName()) || 
                TYPE_INTEGER.equals(parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            } else {
                // 对象类型校验
                checkObjValue(parameter, value);
            }
        }
    }

    // 校验对象类型的参数
    private void checkObjValue(Parameter parameter, Object value) {
        try {
            // 获取参数类型并反射获取字段
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();

            // 遍历字段进行校验
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }
        } catch (BusinessException e) {
            logger.error("校验参数失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    //校验单个值
    private void checkValue(Object value, VerifyParam verifyParam) throws BusinessException {
        //判断是否为空
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        // 获取值的长度
        Integer length = value == null ? 0 : value.toString().length();

        //校验必填
        if (isEmpty && verifyParam.required()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //校验长度
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || 
                         verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //校验正则表达式
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && 
            !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}
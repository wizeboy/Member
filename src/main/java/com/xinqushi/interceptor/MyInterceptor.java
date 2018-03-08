package com.xinqushi.interceptor;

import javax.servlet.http.HttpServletRequest;


import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.xinqushi.entity.LoginUserInfo;
import com.xinqushi.service.UserLoginService;
import com.xinqushi.utils.CookieUtils;

/**
 * 
 * @ClassName:  MyInterceptor   
 * @Description:拦截器   
 * @author: lijunda
 * @date:   2018年2月24日 下午7:16:46
 */
@Component
public class MyInterceptor implements HandlerInterceptor{

	@Autowired
	private UserLoginService userLoginService;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		//在Handler执行之前处理
		//判断用户是否登录
		//从cookie中取token
		String token = CookieUtils.getCookieValue(request, "MM_TOKEN");
		//根据token换取用户信息
		LoginUserInfo loginUser = userLoginService.getLoginUserByToken(token);
		//取不到用户信息
		if (null == loginUser) {
			response.sendRedirect(request.getContextPath() + "/member/login?redirect=" + request.getRequestURL());
			return false;
		}
		return true;// 只有返回true才会继续向下执行，返回false取消当前请求
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		//System.out.println(">>>MyInterceptor1>>>>>>>请求处理之后进行调用，但是在视图被渲染之前（Controller方法调用之后）");
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		//System.out.println(">>>MyInterceptor1>>>>>>>在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）");
	}

}

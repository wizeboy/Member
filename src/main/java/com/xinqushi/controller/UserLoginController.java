package com.xinqushi.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.xinqushi.service.UserLoginService;
import com.xinqushi.utils.CookieUtils;
import com.xinqushi.utils.MemberResult;
@Controller
public class UserLoginController {
    @Autowired
    private UserLoginService userLoginService;
    @Value("${COOKIE_TOKEN_KEY}")
    private String COOKIE_TOKEN_KEY;
    
    // 登录
    @RequestMapping(value="/user/login",method=RequestMethod.POST)
    @ResponseBody
    public MemberResult userLogin(@RequestParam String username,@RequestParam String password,
            HttpServletRequest request, HttpServletResponse response){
        MemberResult memberResult = userLoginService.login(username, password);
        if (memberResult.getData() != null) {
            //取出token
            String token = memberResult.getData().toString();
            //在返回结果前，将token写入cookie
            CookieUtils.setCookie(request, response, COOKIE_TOKEN_KEY, token);;
        }
        //返回结果
        return memberResult;
    }
    
}

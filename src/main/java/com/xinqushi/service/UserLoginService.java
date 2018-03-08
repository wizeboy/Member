package com.xinqushi.service;

import com.xinqushi.entity.LoginUserInfo;
import com.xinqushi.utils.MemberResult;

public interface UserLoginService {
	MemberResult login(String username,String password);
	LoginUserInfo getLoginUserByToken(String token);
}

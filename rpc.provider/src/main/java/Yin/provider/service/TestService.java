package Yin.provider.service;

import org.springframework.stereotype.Service;

import Yin.provider.model.User;

@Service
public class TestService {
	public void test(User user){
		System.out.println("调用了TestService.test");
		System.out.println("UserName:"+user.getName());
	}
}

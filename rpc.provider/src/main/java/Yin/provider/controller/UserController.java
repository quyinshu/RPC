package Yin.provider.controller;

import java.util.List;

//import javax.annotation.Resource;
import jakarta.annotation.Resource; // 替换javax为jakarta
import org.springframework.stereotype.Controller;

import Yin.provider.model.Response;
import Yin.provider.model.User;
import Yin.provider.service.UserService;
import Yin.provider.util.ResponseUtil;

@Controller
public class UserController {
	
	@Resource
	private UserService service;
	
	public Response saveUser(User user){
		service.saveUSer(user);
		Response response = ResponseUtil.createSuccessResponse(user);
		
		return response;
	}
	
	public Response saveUsers(List<User> userlist){
		service.saveUSerList(userlist);
		Response response = ResponseUtil.createSuccessResponse(userlist);
		
		return response;
	}
}

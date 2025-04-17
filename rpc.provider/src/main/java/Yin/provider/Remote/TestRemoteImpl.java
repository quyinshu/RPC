package Yin.provider.Remote;

import jakarta.annotation.Resource; // 替换javax为jakarta

import Yin.provider.annotation.Remote;
import Yin.provider.model.Response;
import Yin.provider.model.User;
import Yin.provider.service.TestService;
import Yin.provider.util.ResponseUtil;
import org.springframework.stereotype.Component;

@Component
@Remote
public class TestRemoteImpl implements TestRemote{
	
	@Resource
	private TestService service;
	
	public Response testUser(User user){
		//System.out.println("service 是否注入成功: " + service);
		if(service == null) {
			service = new TestService();
		}

		service.test(user);
		Response response = ResponseUtil.createSuccessResponse(user);
		
		return response;
	}

//	public Response saveUsers(List<User> userlist){
//		service.saveUSerList(userlist);
//		Response response = ResponseUtil.createSuccessResponse(userlist);
//		
//		return response;
//	}
}

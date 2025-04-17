package Yin.provider.Remote;

import java.util.List;

import Yin.provider.model.Response;
import Yin.provider.model.User;

public interface UserRemote {
	public Response saveUser(User user);
	public Response saveUsers(List<User> userlist);
}

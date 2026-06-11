package cn.stackssh.api;

import cn.stackssh.api.dto.LoginRequestDTO;
import cn.stackssh.api.dto.LoginResponseDTO;
import cn.stackssh.api.dto.RegisterRequestDTO;
import cn.stackssh.api.response.Response;

public interface IAuthService {
    Response<LoginResponseDTO> login(LoginRequestDTO request);
    Response<Void> register(RegisterRequestDTO request);
}

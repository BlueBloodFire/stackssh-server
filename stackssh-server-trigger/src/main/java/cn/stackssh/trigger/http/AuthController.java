package cn.stackssh.trigger.http;

import cn.stackssh.api.IAuthService;
import cn.stackssh.api.dto.LoginRequestDTO;
import cn.stackssh.api.dto.LoginResponseDTO;
import cn.stackssh.api.dto.RegisterRequestDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.auth.model.valobj.LoginResultVO;
import cn.stackssh.domain.auth.service.IAuthDomainService;
import cn.stackssh.types.enums.ResponseCode;
import cn.stackssh.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController implements IAuthService {

    @Resource
    private IAuthDomainService authDomainService;

    @PostMapping("/login")
    @Override
    public Response<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        try {
            if (request.getUsername() == null || request.getPassword() == null) {
                return Response.<LoginResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("用户名和密码不能为空")
                        .build();
            }
            LoginResultVO result = authDomainService.login(request.getUsername(), request.getPassword());
            LoginResponseDTO data = LoginResponseDTO.builder()
                    .token(result.getToken())
                    .userId(result.getUserId())
                    .username(result.getUsername())
                    .build();
            return Response.<LoginResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(data)
                    .build();
        } catch (AppException e) {
            return Response.<LoginResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("登录失败", e);
            return Response.<LoginResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("登录失败: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/register")
    @Override
    public Response<Void> register(@RequestBody RegisterRequestDTO request) {
        try {
            authDomainService.register(request.getUsername(), request.getPassword());
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("注册成功")
                    .build();
        } catch (AppException e) {
            return Response.<Void>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("注册失败", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("注册失败: " + e.getMessage())
                    .build();
        }
    }
}

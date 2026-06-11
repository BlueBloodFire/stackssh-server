package cn.stackssh.domain.auth.service;

import cn.stackssh.domain.auth.model.valobj.LoginResultVO;
import cn.stackssh.domain.auth.adapter.port.IPasswordHashPort;
import cn.stackssh.domain.auth.adapter.port.ITokenGeneratorPort;
import cn.stackssh.domain.auth.adapter.repository.IUserRepository;
import cn.stackssh.domain.auth.model.entity.UserEntity;
import cn.stackssh.types.enums.ResponseCode;
import cn.stackssh.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@Service
public class AuthDomainService implements IAuthDomainService {

    @Resource
    private IUserRepository userRepository;

    @Resource
    private IPasswordHashPort passwordHashPort;

    @Resource
    private ITokenGeneratorPort tokenGeneratorPort;

    @Override
    public LoginResultVO login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null || !passwordHashPort.matches(password, user.getPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "账号已被禁用");
        }
        String token = tokenGeneratorPort.generate(user.getUserId(), user.getUsername());
        log.info("用户登录成功 username={}", username);
        return LoginResultVO.builder()
                .token(token)
                .userId(user.getUserId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public void register(String username, String password) {
        if (username == null || username.trim().length() < 3) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名至少3个字符");
        }
        if (password == null || password.length() < 6) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码至少6个字符");
        }
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名已存在");
        }
        UserEntity user = UserEntity.builder()
                .userId(UUID.randomUUID().toString().replace("-", ""))
                .username(username.trim())
                .password(passwordHashPort.encode(password))
                .status(1)
                .build();
        userRepository.save(user);
        log.info("用户注册成功 username={}", username);
    }
}

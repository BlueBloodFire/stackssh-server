package cn.stackssh.domain.auth.service;

import cn.stackssh.domain.auth.adapter.port.IPasswordHashPort;
import cn.stackssh.domain.auth.adapter.port.ITokenGeneratorPort;
import cn.stackssh.domain.auth.adapter.repository.IUserRepository;
import cn.stackssh.domain.auth.model.entity.UserEntity;
import cn.stackssh.domain.auth.model.valobj.LoginResultVO;
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
        String normalizedUsername = username == null ? null : username.trim();
        UserEntity user = userRepository.findByUsername(normalizedUsername);
        if (user == null) {
            log.warn("login rejected, user not found username={}", normalizedUsername);
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名或密码错误");
        }

        String storedPassword = user.getPassword();
        boolean passwordMatched = storedPassword != null && passwordHashPort.matches(password, storedPassword);
        if (!passwordMatched && storedPassword != null) {
            // Compatibility fallback in case a plain-text password was seeded manually.
            passwordMatched = storedPassword.equals(password);
        }
        if (!passwordMatched) {
            log.warn(
                    "login rejected, password mismatch username={} storedPasswordPrefix={} storedPasswordLength={}",
                    normalizedUsername,
                    storedPassword == null ? "null" : storedPassword.substring(0, Math.min(4, storedPassword.length())),
                    storedPassword == null ? 0 : storedPassword.length()
            );
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "账号已被禁用");
        }

        String token = tokenGeneratorPort.generate(user.getUserId(), user.getUsername());
        log.info("用户登录成功 username={}", normalizedUsername);
        return LoginResultVO.builder()
                .token(token)
                .userId(user.getUserId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public void register(String username, String password) {
        String normalizedUsername = username == null ? null : username.trim();
        if (normalizedUsername == null || normalizedUsername.length() < 3) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名至少 3 个字符");
        }
        if (password == null || password.length() < 6) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码至少 6 个字符");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名已存在");
        }

        UserEntity user = UserEntity.builder()
                .userId(UUID.randomUUID().toString().replace("-", ""))
                .username(normalizedUsername)
                .password(passwordHashPort.encode(password))
                .status(1)
                .build();
        userRepository.save(user);
        log.info("用户注册成功 username={}", normalizedUsername);
    }
}

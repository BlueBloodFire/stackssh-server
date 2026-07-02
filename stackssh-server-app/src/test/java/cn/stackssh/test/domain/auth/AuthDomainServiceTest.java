package cn.stackssh.test.domain.auth;

import cn.stackssh.domain.auth.adapter.port.IPasswordHashPort;
import cn.stackssh.domain.auth.adapter.port.ITokenGeneratorPort;
import cn.stackssh.domain.auth.adapter.repository.IUserRepository;
import cn.stackssh.domain.auth.model.entity.UserEntity;
import cn.stackssh.domain.auth.model.valobj.LoginResultVO;
import cn.stackssh.domain.auth.service.AuthDomainService;
import cn.stackssh.infrastructure.adapter.port.PasswordHashAdapter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

public class AuthDomainServiceTest {

    @Test
    public void shouldRegisterThenLoginSuccessfully() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        IPasswordHashPort passwordHashPort = new PasswordHashAdapter();
        ITokenGeneratorPort tokenGeneratorPort = (userId, username) -> "token-" + userId + "-" + username;

        AuthDomainService authDomainService = new AuthDomainService();
        ReflectionTestUtils.setField(authDomainService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authDomainService, "passwordHashPort", passwordHashPort);
        ReflectionTestUtils.setField(authDomainService, "tokenGeneratorPort", tokenGeneratorPort);

        authDomainService.register("tester", "secret123");
        LoginResultVO result = authDomainService.login("tester", "secret123");

        Assert.assertNotNull(result);
        Assert.assertEquals("tester", result.getUsername());
        Assert.assertTrue(result.getToken().startsWith("token-"));
    }

    private static class InMemoryUserRepository implements IUserRepository {
        private final Map<String, UserEntity> users = new HashMap<>();

        @Override
        public void save(UserEntity user) {
            users.put(user.getUsername(), user);
        }

        @Override
        public UserEntity findByUsername(String username) {
            return users.get(username);
        }

        @Override
        public boolean existsByUsername(String username) {
            return users.containsKey(username);
        }
    }
}

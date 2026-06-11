package cn.stackssh.domain.auth.adapter.port;

public interface ITokenGeneratorPort {
    String generate(String userId, String username);
}

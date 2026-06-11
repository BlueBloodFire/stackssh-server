package cn.stackssh.domain.auth.adapter.port;

public interface IPasswordHashPort {
    String encode(String raw);
    boolean matches(String raw, String encoded);
}

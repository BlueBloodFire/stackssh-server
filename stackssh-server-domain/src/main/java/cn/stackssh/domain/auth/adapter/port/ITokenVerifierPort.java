package cn.stackssh.domain.auth.adapter.port;

public interface ITokenVerifierPort {
    /** 验证 token 是否合法，不合法抛异常 */
    void verify(String token);
    /** 验证并返回 true/false */
    boolean isValid(String token);
}

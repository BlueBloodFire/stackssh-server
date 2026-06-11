package cn.stackssh.domain.auth.service;

import cn.stackssh.domain.auth.model.valobj.LoginResultVO;

public interface IAuthDomainService {
    LoginResultVO login(String username, String password);
    void register(String username, String password);
}

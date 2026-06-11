package cn.stackssh.domain.auth.adapter.repository;

import cn.stackssh.domain.auth.model.entity.UserEntity;

public interface IUserRepository {
    void save(UserEntity user);
    UserEntity findByUsername(String username);
    boolean existsByUsername(String username);
}

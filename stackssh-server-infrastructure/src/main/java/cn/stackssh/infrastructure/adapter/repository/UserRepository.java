package cn.stackssh.infrastructure.adapter.repository;

import cn.stackssh.domain.auth.adapter.repository.IUserRepository;
import cn.stackssh.domain.auth.model.entity.UserEntity;
import cn.stackssh.infrastructure.dao.IUserDao;
import cn.stackssh.infrastructure.dao.po.UserPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class UserRepository implements IUserRepository {

    @Resource
    private IUserDao userDao;

    @Override
    public void save(UserEntity user) {
        UserPO po = new UserPO();
        po.setUserId(user.getUserId());
        po.setUsername(user.getUsername());
        po.setPassword(user.getPassword());
        po.setStatus(user.getStatus());
        userDao.insert(po);
    }

    @Override
    public UserEntity findByUsername(String username) {
        UserPO po = userDao.selectByUsername(username);
        if (po == null) return null;
        return UserEntity.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .username(po.getUsername())
                .password(po.getPassword())
                .status(po.getStatus())
                .build();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userDao.countByUsername(username) > 0;
    }
}

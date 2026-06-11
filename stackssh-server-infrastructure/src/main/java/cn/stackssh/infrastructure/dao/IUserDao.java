package cn.stackssh.infrastructure.dao;

import cn.stackssh.infrastructure.dao.po.UserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IUserDao {
    void insert(UserPO user);
    UserPO selectByUsername(@Param("username") String username);
    int countByUsername(@Param("username") String username);
}

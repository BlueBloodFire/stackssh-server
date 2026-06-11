package cn.stackssh.infrastructure.dao.po;

import lombok.Data;

import java.util.Date;

@Data
public class UserPO {
    private Long id;
    private String userId;
    private String username;
    private String password;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}

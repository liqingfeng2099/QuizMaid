package com.kanade.backend.model.dto;

import com.kanade.backend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserQueryDTO extends PageRequest implements Serializable {
    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 账号
     */
    private String username;

    /**
     * 用户角色：user/admin/ban
     */
    private String role;

    private static final long serialVersionUID = 1L;
}

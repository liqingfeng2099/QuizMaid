package com.kanade.backend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UserUpdateByAdminDTO {
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 角色
     */
    private String role;
    /**
     * 邮箱认证 0-未认证 1-已认证
     */

    private Integer emailVerified;

    /**
     * 账号状态 1-正常 0-禁用
     */
    private Integer status;

    /**
     * 总做题数
     */

    private Integer answerNum;

    /**
     * 总做对题数
     */
    private Integer correctNum;

}

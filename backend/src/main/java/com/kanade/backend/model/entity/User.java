package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户账号表 实体类。
 *
 * @author kanade
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("useraccount")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * BCrypt加密密码
     */
    private String password;

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
    @Column("emailVerified")
    private Integer emailVerified;

    /**
     * 第三方登录类型(gitee/github等)
     */
    @Column("oauthType")
    private String oauthType;

    /**
     * 第三方openid
     */
    @Column("oauthOpenid")
    private String oauthOpenid;

    /**
     * 账号状态 1-正常 0-禁用
     */
    private Integer status;

    /**
     * 总做题数
     */
    @Column("answerNum")
    private Integer answerNum;

    /**
     * 总做对题数
     */
    @Column("correctNum")
    private Integer correctNum;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;

}

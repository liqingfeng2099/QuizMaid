package com.kanade.backend.model.vo;

import com.mybatisflex.annotation.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
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

    private Integer emailVerified;

    /**
     * 第三方登录类型(gitee/github等)
     */

    private String oauthType;

    /**
     * 第三方openid
     */

    private String oauthOpenid;

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


    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

package com.kanade.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.UserLoginVO;
import com.kanade.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;

import static com.kanade.backend.common.Constant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/oauth")
@Slf4j
public class GithubOauthController {
    @Value("${github.client-id}")
    private String clientId;
    @Value("${github.client-secret}")
    private String clientSecret;
    @Value("${github.redirect-uri}")
    private String redirectUri;
    @Autowired
    UserService userService;

    // 1. 前端访问此接口：跳转到 GitHub 授权页 http://127.0.0.1:8080/api/oauth/github/login
    @GetMapping("/github/login")
    public BaseResponse<String> githubLogin() {
        String url = "https://github.com/login/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri ;
        return ResultUtils.success(url);
    }

    // 2. GitHub 授权回调地址（自动执行，无需手动访问）
    @GetMapping("/github/callback")
    public BaseResponse<UserLoginVO> callback(@RequestParam String code) {
        try {
            // ========== 1. 获取 AccessToken ==========
            HashMap<String, Object> param = new HashMap<>();
            param.put("client_id", clientId);
            param.put("client_secret", clientSecret);
            param.put("code", code);
            param.put("redirect_uri", redirectUri);

            String tokenResp = HttpUtil.post("https://github.com/login/oauth/access_token", param);
            String accessToken = null;
            for (String kv : tokenResp.split("&")) {
                if (kv.startsWith("access_token=")) {
                    accessToken = kv.split("=")[1];
                    break;
                }
            }
            if (StrUtil.isBlank(accessToken)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "GitHub授权失败");
            }

            // ========== 2. 获取用户信息 ==========
            String userJson = HttpUtil.createGet("https://api.github.com/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .execute().body();
            JSONObject obj = JSONUtil.parseObj(userJson);
            Long githubId = obj.getLong("id");
            String githubName = obj.getStr("login", "github_user");
            String avatar = obj.getStr("avatar_url", "");

            // ========== 3. 数据库操作：查询/注册用户（匹配你的userAccount表） ==========
            User user = userService.getByOauth("GITHUB", githubId.toString());
            if (user == null) {
                // 自动注册（严格按你的表结构）
                user = new User();
                user.setUsername("github_" + githubId); // 唯一用户名
                user.setPassword(""); // 第三方登录无密码
                user.setNickname(githubName);
                user.setOauthType("GITHUB");
                user.setOauthOpenid(githubId.toString());
                user.setRole("user");
                user.setStatus(1);
                userService.save(user);
            }

            // ========== 4. Sa-Token 登录 ==========
            StpUtil.login(user.getId());
            // fix 无法退出登录
            StpUtil.getSession().set(USER_LOGIN_STATE,user);
            String token = StpUtil.getTokenValue();

            // ========== 5. 组装返回值（无序列化报错） ==========
            UserLoginVO vo = new UserLoginVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            //vo.setAvatar(avatar);
            //vo.setToken(token);
            vo.setRole(user.getRole());

            return ResultUtils.success(vo);

        } catch (Exception e) {
            log.error("GitHub登录失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录失败");
        }
    }


}

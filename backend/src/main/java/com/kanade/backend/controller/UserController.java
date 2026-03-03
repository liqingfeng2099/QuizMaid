package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.email.IEmailService;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.exception.ThrowUtils;
import com.kanade.backend.model.dto.UserLoginByEmailDTO;
import com.kanade.backend.model.dto.UserLoginDTO;
import com.kanade.backend.model.dto.UserRegisterByEmailDTO;
import com.kanade.backend.model.dto.UserRegisterDTO;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.UserLoginVO;
import com.kanade.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.kanade.backend.common.Constant.EMAIL_VERIFY_CODE;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    @Autowired
    UserService userService;

    @Autowired
    IEmailService emailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok test  koko tttt check");

    }

    @PostMapping("/login")
    public BaseResponse<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request){
        UserLoginVO userLoginVO = userService.UserLogin(userLoginDTO, request);
        log.info(String.valueOf(userLoginVO));
        return ResultUtils.success(userLoginVO);
    }


    @GetMapping("/get/login")
    public BaseResponse<UserLoginVO> getLoginUserInfo(HttpServletRequest request){
        User user = userService.getUserLoginInfo(request);
        log.info(String.valueOf(user));
        return ResultUtils.success(userService.getLoginUserVO(user));
    }
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request){
        boolean result = userService.logout(request);
        return ResultUtils.success(result);
    }
    /**
     * user register
     */
    @PostMapping("/register")
    public BaseResponse<User> register(@RequestBody UserRegisterDTO userRegisterDTO){
        User register = userService.register(userRegisterDTO);
        return ResultUtils.success(register);
    }

    // todo oauth 登录注册

    // todo 邮箱注册登录
    @PostMapping("/send")
    public BaseResponse<String> sendEmail(String email){
        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> dataSourceConfig = dict.getByPath("spring.mail");
        String username = String.valueOf(dataSourceConfig.get("username"));

        String code = RandomUtil.randomString(6);
        String key = EMAIL_VERIFY_CODE + email;
        redisTemplate.opsForValue().set(key,code,300, TimeUnit.SECONDS);

        emailService.sendText(username,email,"验证码",code);
        return ResultUtils.success("*****");
    }

    @PostMapping("/email/registry")
    public BaseResponse<User> emailRegister(@RequestBody UserRegisterByEmailDTO user){
        String key = EMAIL_VERIFY_CODE + user.getEmail();
        String code = redisTemplate.opsForValue().get(key);
        if (!code.equals(user.getCode())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"验证码错误");
        }
        // todo 检查完成后删除验证码缓存
        User register = userService.registerByEmail(user);
        return ResultUtils.success(register);
    }

    @PostMapping("/email/login")
    public BaseResponse<UserLoginVO> emailLogin(@RequestBody UserLoginByEmailDTO user,HttpServletRequest request){
        ThrowUtils.throwIf(user.getEmail()==null,ErrorCode.PARAMS_ERROR);
        String key = EMAIL_VERIFY_CODE + user.getEmail();
        String code = redisTemplate.opsForValue().get(key);
        if (!code.equals(user.getCode())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"验证码错误");
        }

        UserLoginVO userLoginVO = userService.loginByEmail(user.getEmail(),request);
        return ResultUtils.success(userLoginVO);
    }


    // 根据id 获取用户
    @GetMapping("/get/user")
    @SaCheckRole("admin")
    public BaseResponse<User> getUserById(long id){
        if (id < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        if (user == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(user);
    }

    // 根据id 获取用户vo
    @GetMapping("/get/uservo")
    @SaCheckRole("admin")
    public BaseResponse<User> getUserVoById(long id){
        if (id < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        if (user == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        userService.getUserVOById(user);
        return ResultUtils.success(user);
    }

}


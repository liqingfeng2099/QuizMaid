package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.oauth2.SaOAuth2Manager;
import cn.dev33.satoken.oauth2.data.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.data.model.CodeModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.email.IEmailService;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.exception.ThrowUtils;
import com.kanade.backend.model.dto.*;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.UserHeatMapVO;
import com.kanade.backend.model.vo.UserLoginVO;
import com.kanade.backend.model.vo.UserVO;
import com.kanade.backend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
        return ResultUtils.success("ok test  koko tttt llll gg check");

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
        StpUtil.logout();
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

    // 邮箱注册登录
    @PostMapping("/send")
    public BaseResponse<String> sendEmail(String email){
        Dict dict = YamlUtil.loadByPath("application-local.yml");
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
        // 检查完成后删除验证码缓存
        redisTemplate.delete(key);
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

        redisTemplate.delete(key);
        UserLoginVO userLoginVO = userService.loginByEmail(user.getEmail(),request);
        return ResultUtils.success(userLoginVO);
    }
    // 个人信息编辑
    @PostMapping("/update")
    public BaseResponse<UserVO> updateSelf(@RequestBody UserUpdateDTO userUpdateDTO){

        // 1. 获取当前登录用户ID（强制修改自己，防越权）
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. DTO 转换为 实体类（MyBatis-Plus 要求）
        User user = new User();
        BeanUtils.copyProperties(userUpdateDTO, user);
        user.setId(userId); // 强制绑定当前登录用户ID

        // log.info(String.valueOf(user)+"\n"+ userUpdateDTO);

        // 3. 执行更新
        boolean success = userService.updateById(user);
        if (!success) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 4. 查询最新用户信息，返回给前端
        User newUser = userService.getById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(newUser, userVO);
        return ResultUtils.success(userVO);
    }

    // 添加用户
    @PostMapping("/add")
    @SaCheckRole("admin")
    public BaseResponse<Long> addUser(@RequestBody UserAddDTO userAddDTO){
        if(userAddDTO == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"request is null");
        }
        // copy
        User user = new User();
        BeanUtils.copyProperties(userAddDTO, user);
        // set password
        String password = DigestUtils.md5DigestAsHex("123456".getBytes());
        user.setPassword(password);

        log.info(userAddDTO.toString());
        log.info(user.toString());
        // save
        boolean save = userService.save(user);
        log.info(userAddDTO.getUsername() + "create successfully ");
        return ResultUtils.success(user.getId());
    }

    @PostMapping("del/user")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        if (deleteRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"request is null");
        }
        Long id = deleteRequest.getId();
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    // 修改admin
    @PostMapping("/admin/update")
    @SaCheckRole("admin")
    public BaseResponse<UserVO> updateUserByAdmin(@RequestBody UserUpdateByAdminDTO userUpdateDTO){

        Long id = userUpdateDTO.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        User user = new User();
        user.setId(id);

        // 昵称：只处理非空、非空白字符串
        String nickname = userUpdateDTO.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }

        // 邮箱：只处理非空、非空白字符串
        String email = userUpdateDTO.getEmail();
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }

        // 角色：只处理非空、非空白字符串
        String role = userUpdateDTO.getRole();
        if (role != null && !role.isBlank()) {
            user.setRole(role);
        }

        // 状态：只要不为null就更新
        Integer status = userUpdateDTO.getStatus();
        if (status != null) {
            user.setStatus(status);
        }

        // 2. 无修改内容，直接返回
        if (user.getNickname() == null && user.getEmail() == null
                && user.getRole() == null && user.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未修改任何内容");
        }
        // 3. 执行更新
        boolean success = userService.updateById(user);
        if (!success) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 4. 查询最新用户信息，返回给前端
        User newUser = userService.getById(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(newUser, userVO);
        return ResultUtils.success(userVO);
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
        userService.getUserVO(user);
        return ResultUtils.success(user);
    }
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserByPage(@RequestBody UserQueryDTO userQueryDTO){
        log.info(String.valueOf(userQueryDTO));
        if (userQueryDTO == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"userQueryDTO is null");
        }

        long pageNum = userQueryDTO.getPageNum();
        long pageSize = userQueryDTO.getPageSize();

        Page<User> page = userService.page(Page.of(pageNum, pageSize),userService.getQueryWrapper(userQueryDTO));
        Page<UserVO> userVOPage = new Page<>(pageNum,pageSize,page.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(page.getRecords());
        userVOPage.setRecords(userVOList);

        return ResultUtils.success(userVOPage);
    }

    // 密码重置
    @PostMapping("/resetpassword")
    public BaseResponse<String> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO ){
        // 检查是否绑定邮箱

        long loginId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(loginId);
        ThrowUtils.throwIf(user.getEmailVerified() == 0,ErrorCode.PARAMS_ERROR,"未绑定邮箱");

        // 验证码校验
        String code = redisTemplate.opsForValue().get(EMAIL_VERIFY_CODE + resetPasswordDTO.getEmail());
        ThrowUtils.throwIf(!code.equals(resetPasswordDTO.getCode()),ErrorCode.PARAMS_ERROR,"验证码错误");

        redisTemplate.delete(EMAIL_VERIFY_CODE + resetPasswordDTO.getEmail());
        user.setPassword(DigestUtils.md5DigestAsHex(resetPasswordDTO.getPassword().getBytes()));
        boolean updateById = userService.updateById(user);
        ThrowUtils.throwIf(!updateById,ErrorCode.PARAMS_ERROR);
        // 重新登录
        StpUtil.logout();

        return ResultUtils.success("重置成功,请重新登录");
    }

    // 绑定邮箱
    @PostMapping("/bindemail")
    public BaseResponse<UserVO> bindEmail(@RequestBody BindEmailDTO bindEmailDTO) {
        long loginId =  StpUtil.getLoginIdAsLong();
        User user = userService.getById(loginId);

        // 验证码校验
        String code = redisTemplate.opsForValue().get(EMAIL_VERIFY_CODE + bindEmailDTO.getEmail());
        ThrowUtils.throwIf(!code.equals(bindEmailDTO.getCode()),ErrorCode.PARAMS_ERROR,"验证码错误");
        redisTemplate.delete(EMAIL_VERIFY_CODE + bindEmailDTO.getEmail());
        // 设定邮箱
        user.setEmail(bindEmailDTO.getEmail());
        user.setEmailVerified(1);
        boolean updateById = userService.updateById(user);
        ThrowUtils.throwIf(!updateById,ErrorCode.PARAMS_ERROR);

        user = userService.getById(loginId);

        return ResultUtils.success(userService.getUserVO(user));
    }

    // 登录签到
    @PostMapping("/signin")
    public BaseResponse<Boolean> userSignIn(){
        long loginId = StpUtil.getLoginIdAsLong();
        boolean b = userService.userSignIn(loginId);
        return ResultUtils.success(b);
    }

    // 获取签到信息
    @PostMapping("/signdata")
    public BaseResponse<Map<LocalDate,Boolean>> getUserSignData(Integer year){
        long loginId = StpUtil.getLoginIdAsLong();
        List<Integer> userSignInData = userService.getUserSignInData(loginId, year);
        Map<LocalDate,Boolean> data = new LinkedHashMap<>();
        for (int i = 0; i < userSignInData.size(); i++) {
            LocalDate date = LocalDate.ofYearDay(year, userSignInData.get(i));
            data.put(date,true);
        }
        return ResultUtils.success(data);
    }

    // 连续签到天数
    @GetMapping("/continuous_sign_in_days")
    public BaseResponse<Integer> getSignInDays(){
        long loginId = StpUtil.getLoginIdAsLong();
        Integer days = userService.getUserSignDays(loginId);
        return ResultUtils.success(days);
    }

    // 做题热力图 分等级 0 - 4 别人可以查看
    @GetMapping("/heatmap/{userId}")
    public BaseResponse<List<UserHeatMapVO>> getSignInDays(@PathVariable long userId){

        List<UserHeatMapVO> userHeatMap = userService.getUserHeatMap(userId);
        return ResultUtils.success(userHeatMap);
    }
}


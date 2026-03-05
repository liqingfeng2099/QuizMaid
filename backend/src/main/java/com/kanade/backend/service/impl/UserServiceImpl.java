package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kanade.backend.common.Constant;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.exception.ThrowUtils;
import com.kanade.backend.mapper.UserMapper;
import com.kanade.backend.model.dto.UserLoginDTO;
import com.kanade.backend.model.dto.UserQueryDTO;
import com.kanade.backend.model.dto.UserRegisterByEmailDTO;
import com.kanade.backend.model.dto.UserRegisterDTO;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.UserLoginVO;
import com.kanade.backend.model.vo.UserVO;
import com.kanade.backend.service.UserService;
import com.kanade.backend.utils.DeviceUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import static com.kanade.backend.common.Constant.USER_LOGIN_STATE;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final RedissonClient redissonClient;

    public UserServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public User register(UserRegisterDTO userRegisterDTO) {
        if (StrUtil.isBlank(userRegisterDTO.getUserPassword())|| StrUtil.isBlank(userRegisterDTO.getUserName())||StrUtil.isBlank(userRegisterDTO.getCheckUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (userRegisterDTO.getUserName().length() < 3 || userRegisterDTO.getUserName().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度必须在 3-20 位之间");
        }

        if (userRegisterDTO.getUserPassword().length() < 6 || userRegisterDTO.getUserPassword().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在 6-20 位之间");
        }

        if (!userRegisterDTO.getUserPassword().equals(userRegisterDTO.getCheckUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("username",userRegisterDTO.getUserName());

        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"username 重复");
        }

        String md5DigestAsHex = DigestUtils.md5DigestAsHex(userRegisterDTO.getUserPassword().getBytes());

        User user = new User();
        user.setUsername(userRegisterDTO.getUserName());
        user.setPassword(md5DigestAsHex);
        user.setNickname(userRegisterDTO.getUserName());
        boolean saved = this.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "db fail to save");
        }
        log.info("user{} is saved in database",user.getUsername());
        return user;
    }

    @Override
    public User registerByEmail(UserRegisterByEmailDTO user) {
        if (StrUtil.isBlank(user.getUserPassword())|| StrUtil.isBlank(user.getUserName())||StrUtil.isBlank(user.getCheckUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (user.getUserName().length() < 3 || user.getUserName().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度必须在 3-20 位之间");
        }

        if (user.getUserPassword().length() < 6 || user.getUserPassword().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在 6-20 位之间");
        }

        if (!user.getUserPassword().equals(user.getCheckUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("username",user.getUserName());

        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"username 重复");
        }

        String md5DigestAsHex = DigestUtils.md5DigestAsHex(user.getUserPassword().getBytes());

        User user1 = new User();
        user1.setUsername(user.getUserName());
        user1.setPassword(md5DigestAsHex);
        user1.setNickname(user.getUserName());
        user1.setEmail(user.getEmail());
        user1.setEmailVerified(1);
        boolean saved = this.save(user1);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "db fail to save");
        }
        log.info("user{} is saved in database",user1.getUsername());
        return user1;
    }

    @Override
    public boolean userSignIn(long id) {
        LocalDate date = LocalDate.now();
        String userSignInRedisKey = Constant.getUserSignInRedisKey(date.getYear(), id);
        RBitSet bitSet = redissonClient.getBitSet(userSignInRedisKey);

        int dayOfYear = date.getDayOfYear();
        if(!bitSet.get(dayOfYear)){
            return bitSet.set(dayOfYear,true);
        }

        return true;
    }

    @Override
    public List<Integer> getUserSignInData(long loginId, Integer year) {
        if (year == null) {
            LocalDate date = LocalDate.now();
            year = date.getYear();
        }
        String key = Constant.getUserSignInRedisKey(year, loginId);
        RBitSet signInBitSet = redissonClient.getBitSet(key);
        // 加载 BitSet 到内存中，避免后续读取时发送多次请求
        BitSet bitSet = signInBitSet.asBitSet();
        // 统计签到的日期
        List<Integer> dayList = new ArrayList<>();
        // 从索引 0 开始查找下一个被设置为 1 的位
        int index = bitSet.nextSetBit(0);
        while (index >= 0) {
            dayList.add(index);
            // 查找下一个被设置为 1 的位
            index = bitSet.nextSetBit(index + 1);
        }
        return dayList;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryDTO userQueryDTO) {
        if (userQueryDTO == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"query is null");
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                //.eq("id",userQueryDTO.getId())
                .like("username",userQueryDTO.getUsername())
                .like("nickname",userQueryDTO.getNickname())
                ;

        // 只有当role不为空时才添加role查询条件
        if (StringUtils.isNotBlank(userQueryDTO.getRole())) {
            queryWrapper.eq("role",userQueryDTO.getRole());
        }

        // 只有当sortField不为空时才添加排序条件，并且需要转换字段名
        if (StringUtils.isNotBlank(userQueryDTO.getSortField())) {
            String sortField = userQueryDTO.getSortField();
            // 将驼峰命名转换为下划线命名，如createAt -> created_at
            if ("createAt".equals(sortField)) {
                sortField = "createTime";
            } else if ("updateAt".equals(sortField)) {
                sortField = "updateTime";
            }
            queryWrapper.orderBy(sortField,"ascend".equals(userQueryDTO.getSortOrder()));
        }

        return queryWrapper;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public UserLoginVO getLoginUserVO(User user) {
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(user,userLoginVO);

        return userLoginVO;
    }

    @Override
    public UserLoginVO UserLogin(UserLoginDTO userLoginDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(StrUtil.isBlank(userLoginDTO.getUserPassword()) ,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(userLoginDTO.getUsername()),ErrorCode.PARAMS_ERROR);

        String md5DigestAsHex = DigestUtils.md5DigestAsHex(userLoginDTO.getUserPassword().getBytes());

        /*QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("password",md5DigestAsHex);*/

        QueryWrapper queryWrapper1 = new QueryWrapper();
        queryWrapper1.eq("username",userLoginDTO.getUsername());

        User user = this.mapper.selectOneByQuery(queryWrapper1);
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"user is not exist or password is wrong");
        }
        if (!user.getPassword().equals(md5DigestAsHex)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"user is not exist or password is wrong");
        }
        StpUtil.login(user.getId(), DeviceUtil.getRequestDevice(request));
        StpUtil.getSession().set(USER_LOGIN_STATE,user);
        // request.getSession().setAttribute(USER_LOGIN_STATE,user);
        return this.getLoginUserVO(user);
    }

    @Override
    public UserLoginVO loginByEmail(String email,HttpServletRequest request) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("email",email);
        User user = this.mapper.selectOneByQuery(queryWrapper);

        if (user == null){
            throw new RuntimeException("该邮箱未认证");
        }
        StpUtil.login(user.getId(), DeviceUtil.getRequestDevice(request));
        StpUtil.getSession().set(USER_LOGIN_STATE,user);
        return this.getLoginUserVO(user);
    }



    @Override
    public User getUserLoginInfo(HttpServletRequest request) {
        Object loginUserId = StpUtil.getLoginIdDefaultNull();
        if (loginUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User current = this.getById((Serializable) loginUserId);

        if (current == null || current.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        return current;
    }

    @Override
    public boolean logout(HttpServletRequest request) {
        User user =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"not login");
        }
        StpUtil.logout();
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);

        return userVO;
    }


    public User getByOauth(String oauthType, String oauthOpenid) {

        QueryWrapper wrapper = new QueryWrapper();
        // 匹配你的表字段：oauthType / oauthOpenid / isDeleted
        wrapper.eq("oauthType", oauthType);
        wrapper.eq("oauthOpenid", oauthOpenid);
        wrapper.eq("isDeleted", 0);

        // MP 自带方法，直接查询
        return getOne(wrapper);

    }

}

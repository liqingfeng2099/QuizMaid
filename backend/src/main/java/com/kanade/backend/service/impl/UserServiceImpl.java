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
import com.kanade.backend.model.vo.UserHeatMapVO;
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
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.kanade.backend.common.Constant.USER_LOGIN_STATE;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final Set<Long> cleanedHeatmapUsers = new HashSet<>();

    public UserServiceImpl(RedissonClient redissonClient, StringRedisTemplate stringRedisTemplate) {
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = stringRedisTemplate;
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
        if(bitSet.get(dayOfYear)){
            // 今日已签到，无需重复签到
            return true;
        }

        String continuousKey = Constant.getUserContinuousSignKey(id);
        String lastDateKey = Constant.getUserLastSignDateKey(id);

        RBucket<String> lastDateBucket = redissonClient.getBucket(lastDateKey);
        RBucket<Integer> continuousBucket = redissonClient.getBucket(continuousKey);
        String lastSignDate = lastDateBucket.get();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (lastSignDate == null) {
            // 首次签到
            continuousBucket.set(1);
        } else {
            LocalDate lastDate = LocalDate.parse(lastSignDate);
            if (lastDate.equals(today)) {
                // 今日重复签到
            } else if (lastDate.equals(yesterday)) {
                // 连续签到（包含 12.31 → 01.01 跨年场景）
                continuousBucket.set(continuousBucket.get() + 1);
            } else {
                // 断签（无论是否跨年，直接重置）
                continuousBucket.set(1);
            }
        }
        // 标记今日已签到
        bitSet.set(dayOfYear, true);
        // 更新最后签到日期
        lastDateBucket.set(today.toString());
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
    public Integer getUserSignDays(long loginId) {
        String userContinuousSignKey = Constant.getUserContinuousSignKey(loginId);
        RBucket<Object> bucket = redissonClient.getBucket(userContinuousSignKey);
        return bucket.get() == null ? 0 : (Integer) bucket.get();
    }

    @Override
    public List<UserHeatMapVO> getUserHeatMap(long loginId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(364);

        String hashKey = Constant.getUserQuestionHashKey(loginId);

        Map<Object, Object> rawMap = stringRedisTemplate.opsForHash().entries(hashKey);

        List<UserHeatMapVO> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateStr = current.toString();
            UserHeatMapVO vo = new UserHeatMapVO();
            vo.setDate(LocalDate.parse(dateStr));

            Object val = rawMap.get(dateStr);
            int count = val != null ? Integer.parseInt(val.toString()) : 0;
            vo.setCount(count);
            vo.setLevel(calculateHeatLevel(count));

            result.add(vo);
            current = current.plusDays(1);
        }

        return result;
    }
    private Integer calculateHeatLevel(int count) {
        if (count == 0) return 0;
        if (count <= 5) return 1;
        if (count <= 15) return 2;
        if (count <= 30) return 3;
        return 4;
    }
    @Override
    public void addUserQuestionCount(long userId, int count) {
        if (count <= 0) return;
        String hashKey = Constant.getUserQuestionHashKey(userId);
        String date = LocalDate.now().toString();

        // 首次写入前清除旧Kryo序列化数据
        synchronized (cleanedHeatmapUsers) {
            if (!cleanedHeatmapUsers.contains(userId)) {
                stringRedisTemplate.delete(hashKey);
                cleanedHeatmapUsers.add(userId);
            }
        }

        stringRedisTemplate.opsForHash().increment(hashKey, date, count);
        stringRedisTemplate.expire(hashKey, 365, TimeUnit.DAYS);
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
        StpUtil.getSession().delete(USER_LOGIN_STATE);
        StpUtil.logout();
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

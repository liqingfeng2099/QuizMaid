package com.kanade.backend.service;

import com.kanade.backend.model.dto.UserLoginDTO;
import com.kanade.backend.model.dto.UserQueryDTO;
import com.kanade.backend.model.dto.UserRegisterByEmailDTO;
import com.kanade.backend.model.dto.UserRegisterDTO;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.UserLoginVO;
import com.kanade.backend.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

import java.util.List;
import java.util.Map;


public interface UserService extends IService<User> {

    User register(UserRegisterDTO userRegisterDTO);

    UserLoginVO getLoginUserVO(User user);

    UserLoginVO UserLogin(UserLoginDTO userLoginDTO, HttpServletRequest request);

    User getUserLoginInfo(HttpServletRequest request);

    boolean logout(HttpServletRequest request);

    UserVO getUserVO(User user);

    UserLoginVO loginByEmail(String email,HttpServletRequest request);

    User registerByEmail(UserRegisterByEmailDTO user);

    QueryWrapper getQueryWrapper(UserQueryDTO userQueryDTO);

    List<UserVO> getUserVOList(List<User> records);

    User getByOauth(String github, String string);

    boolean userSignIn(long id);

    List<Integer> getUserSignInData(long loginId, Integer year);
}

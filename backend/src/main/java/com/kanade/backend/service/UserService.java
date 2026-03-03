package com.kanade.backend.service;

import com.kanade.backend.model.dto.UserLoginDTO;
import com.kanade.backend.model.dto.UserRegisterByEmailDTO;
import com.kanade.backend.model.dto.UserRegisterDTO;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.UserLoginVO;
import com.kanade.backend.model.vo.UserVO;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;


public interface UserService extends IService<User> {

    User register(UserRegisterDTO userRegisterDTO);

    UserLoginVO getLoginUserVO(User user);

    UserLoginVO UserLogin(UserLoginDTO userLoginDTO, HttpServletRequest request);

    User getUserLoginInfo(HttpServletRequest request);

    boolean logout(HttpServletRequest request);

    UserVO getUserVOById(User user);

    UserLoginVO loginByEmail(String email,HttpServletRequest request);

    User registerByEmail(UserRegisterByEmailDTO user);
}

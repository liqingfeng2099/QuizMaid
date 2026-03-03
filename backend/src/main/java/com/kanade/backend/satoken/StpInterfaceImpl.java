package com.kanade.backend.satoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.model.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.kanade.backend.common.Constant.USER_LOGIN_STATE;
@Component
public class StpInterfaceImpl implements StpInterface {
    @Override
    public List<String> getPermissionList(Object o, String s) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object id, String s) {
        User user =(User) StpUtil.getSessionByLoginId(id).get(USER_LOGIN_STATE);
        return Collections.singletonList(user.getRole());
    }
}

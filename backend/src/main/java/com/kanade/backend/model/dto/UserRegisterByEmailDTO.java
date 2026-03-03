package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class UserRegisterByEmailDTO {
    private String userName;
    private String userPassword;
    private String checkUserPassword;
    private String email;
    private String code;
}

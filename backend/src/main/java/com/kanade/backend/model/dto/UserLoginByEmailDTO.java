package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class UserLoginByEmailDTO {
    String email;
    String code;
}

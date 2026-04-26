package com.researchi.admin.auth.web;

import jakarta.validation.constraints.NotBlank;

public class LoginForm {

    @NotBlank(message = "로그인 ID를 입력해 주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

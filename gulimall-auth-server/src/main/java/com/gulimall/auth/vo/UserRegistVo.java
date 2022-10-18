package com.gulimall.auth.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Data
public class UserRegistVo {

    @NotEmpty(message = "用户名必须提交")
    @Length(min = 6, max = 10, message = "用户名必须是6-10位汉字、字母、数字")
    private String userName;

    @NotEmpty(message = "密码必须提交")
    @Length(min = 6, max = 12, message = "密码必须是6-12位汉字、字母、数字")
    private String password;

    @NotEmpty(message = "手机号必须提交")
    @Pattern(regexp = "^[1]([3-9])[0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @NotEmpty(message = "验证码必须提交")
    @Pattern(regexp = "^[a-zA-Z0-9]{5}$", message = "验证码必须是5位字母、数字")
    private String code;
}

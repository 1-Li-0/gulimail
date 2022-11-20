package com.example.common.to;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *  社交登录所需要的封装对象
 */
@Data
public class MemberRespVo implements Serializable {
	//会员id
	private Long id;
	private Long levelId;
	private String username;
	private String password;
	private String nickname;
	private String mobile;
	private String email;
	private String header;
	private Integer gender;
	private Date birth;
	private String city;
	private String job;
	private String sign;
	private Integer sourceType;
	private Integer integration;
	private Integer growth;
	private Integer status;
	private Date createTime;

	//社交账号的uid
	private String uid;

}

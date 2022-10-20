package com.example.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.exception.PhoneExistException;
import com.example.gulimall.member.exception.UsernameExistException;
import com.example.gulimall.member.vo.MemberLoginVo;
import com.example.gulimall.member.vo.MemberRegistVo;
import com.example.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:38:01
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo) throws PhoneExistException, UsernameExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser vo) throws Exception;
}


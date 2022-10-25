package com.example.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.common.utils.HttpUtils;
import com.example.gulimall.member.dao.MemberLevelDao;
import com.example.gulimall.member.entity.MemberLevelEntity;
import com.example.gulimall.member.exception.PhoneExistException;
import com.example.gulimall.member.exception.UsernameExistException;
import com.example.gulimall.member.vo.MemberLoginVo;
import com.example.gulimall.member.vo.MemberRegistVo;
import com.example.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.member.dao.MemberDao;
import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) throws PhoneExistException, UsernameExistException {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setLevelId(getDefaultLevel());

        //保存基本信息时，验证数据库中是否有重复的用户名和手机号
        checkMobile(vo.getPhone());
        checkUsername(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setMobile(vo.getPhone());

        //密码的加密保存
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        this.baseMapper.insert(memberEntity);
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String userAccount = vo.getLoginAccount();
        String password = vo.getPassword();
        //根据账号查询数据库是否存在记录
        MemberEntity entity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", userAccount).or().eq("mobile", userAccount));
        if (entity == null) {
            return null;
        }
        //验证密码
        String entityPassword = entity.getPassword();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches(password, entityPassword);
        if (matches) {
            //匹配成功，返回实体类
            return entity;
        } else {
            return null;
        }
    }

    @Override
    public MemberEntity login(SocialUser vo) throws Exception {
        String uid = vo.getUid();
        //查询该uid是否注册过账号
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("uid", uid));
        if (memberEntity != null) {
            //直接使用此账号登录，读取信息
            return memberEntity;
        } else {
            //使用uid自动注册一个账号
            MemberEntity entity = new MemberEntity();
            entity.setLevelId(getDefaultLevel());
            entity.setUid(uid);
            try {
                //通过token查出社交信息，作为新建账号的信息
                Map<String, String> query = new HashMap<>();
                query.put("access_token", vo.getAccess_token());
                query.put("uid", uid);
                entity.setCreateTime(new Date());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    JSONObject object = JSON.parseObject(EntityUtils.toString(response.getEntity()));
                    entity.setNickname(object.getString("name"));
                    entity.setGender("m".equals(object.getString("gender")) ? 1 : 0);
                    entity.setCity(object.getString("location"));
                }
            } catch (Exception e) {}
            //保存数据
            this.baseMapper.insert(entity);
            return entity;
        }
    }

    private void checkUsername(String userName) throws UsernameExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0) {
            throw new UsernameExistException();
        }
    }

    private void checkMobile(String phone) throws PhoneExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneExistException();
        }
    }

//    @Cacheable(cacheNames = {"defaultMemberLevel"},key = "#root.methodName")
    public Long getDefaultLevel() {
        //查询默认会员等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.selectOne(new QueryWrapper<MemberLevelEntity>().eq("default_status", 1));
        return memberLevelEntity.getId();
    }
}
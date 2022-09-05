package com.example.gulimall.member.dao;

import com.example.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:38:01
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}

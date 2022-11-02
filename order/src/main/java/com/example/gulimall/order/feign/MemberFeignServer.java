package com.example.gulimall.order.feign;

import com.example.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("member")
public interface MemberFeignServer {
    @GetMapping("/{memberId}/getMemberAddress")
    List<MemberAddressVo> getMemberAddress(@PathVariable("memberId") Long memberId);
}

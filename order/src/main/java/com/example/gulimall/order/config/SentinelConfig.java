/*
package com.example.gulimall.order.config;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SentinelConfig {
    public SentinelConfig() {
        //1、加载流控策略
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new
                NacosDataSource<>("127.0.0.1:8848", "demo", "sentinel",
                source -> JSON.parseObject(source, new
                        TypeReference<List<FlowRule>>() {
                        }));
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        //2、加载降级策略
        ReadableDataSource<String, List<DegradeRule>> degradeRuleDataSource =
                new NacosDataSource<>("127.0.0.1:8848", "demo", "sentinel",
                        source -> JSON.parseObject(source, new
                                TypeReference<List<DegradeRule>>() {
                                }));
        DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());
        //3、加载系统规则
        ReadableDataSource<String, List<SystemRule>> systemRuleDataSource =
                new NacosDataSource<>("127.0.0.1:8848", "demo", "sentinel",
                        source -> JSON.parseObject(source, new
                                TypeReference<List<SystemRule>>() {
                                }));
        SystemRuleManager.register2Property(systemRuleDataSource.getProperty());
        //4、加载权限策略
        ReadableDataSource<String, List<AuthorityRule>>
                authorityRuleDataSource = new NacosDataSource<>("127.0.0.1:8848", "demo",
                "sentinel",
                source -> JSON.parseObject(source, new
                        TypeReference<List<AuthorityRule>>() {
                        }));
        AuthorityRuleManager.register2Property(authorityRuleDataSource.getProperty(
        ));
    }
}
*/

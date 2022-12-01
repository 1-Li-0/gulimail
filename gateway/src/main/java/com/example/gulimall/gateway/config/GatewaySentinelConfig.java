package com.example.gulimall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.example.common.exception.BizCodeEnum;
import com.example.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewaySentinelConfig {
    public GatewaySentinelConfig() {
        //网关限流时的回调函数
        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            /**
             * TODO 响应式编程
             * @param serverWebExchange 当前请求和响应的上下文
             * @return Mono/Flux都是响应式编程新特性，Mono返回0个或1个数据，Mono.just()返回一个数据；Flux返回0个或多个数据。
             */
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {
                String json = JSON.toJSONString(R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(), BizCodeEnum.TOO_MANY_REQUEST.getMsg()));
                Mono<ServerResponse> mono = ServerResponse.ok().body(Mono.just(json),String.class);
                return mono;
            }
        });
    }
}

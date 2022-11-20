package com.example.gulimall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.gulimall.order.config.AlipayTemplate;
import com.example.gulimall.order.service.OrderService;
import com.example.gulimall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AliPayListener {
    @Autowired
    OrderService orderService;
    @Autowired
    AlipayTemplate alipayTemplate;
    /**
     * 接收支付宝的异步通知（post请求）
     * nignx中需要修改配置文件，重新设置host指定到order.gulimall.com（申请的内网地址host不符）
     *
     * @return 必须返回success字符串，支付宝才会停止通知
     */
    @PostMapping("/pay/notify")
    public String handleAliPay(PayAsyncVo vo, HttpServletRequest request) throws AlipayApiException {
        /** 请求体中有签名，可以验证支付状态
         *  Map<String, String[]> map = request.getParameterMap();
         *  for (String key : map.keySet()) {
         *    String value = request.getParameter(key);
         *    System.out.println("参数名："+key+"==>参数值："+value);
         *  }
         */
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(),
                alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名
        if (signVerified){
            System.out.println("签名验证成功!");
            String result = orderService.handlePayResult(vo);
            return result;
        }else {
            System.out.println("签名验证失败!");
            return "error";
        }
    }
}

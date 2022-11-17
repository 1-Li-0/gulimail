package com.example.gulimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("mq_message")
public class MqMessageEntity {
    @TableId
    private String messageId;
    //消息对象转换成的JSON字符串
    private String content;
    private String toExchane;
    private String routingKey;
    //消息对象的类型
    private String classType;
    //0-新建；1-已发送；2-错误抵达；3-成功抵达
    private Integer messageStatus;
    private Date createTime;
    private Date updateTime;
}

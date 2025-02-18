package com.wxchat.websocket.netty;

import com.alibaba.fastjson.JSON;
import com.wxchat.entity.dto.MessageSendDto;
import com.wxchat.websocket.ChannelContextUtils;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @description 消息处理器
 * @author JIU-W
 * @date 2025-02-18
 * @version 1.0
 */
@Component("messageHandler")
public class MessageHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    //定义一个监听常量
    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtils channelContextUtils;

    /**
     * 监听消息
     */
    @PostConstruct
    public void lisMessage() {
        //获取监听对象
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        //监听消息
        rTopic.addListener(MessageSendDto.class, (MessageSendDto, sendDto) -> {
            logger.info("收到广播消息:{}", JSON.toJSONString(sendDto));
            //发送消息
            //channelContextUtils.sendMessage(sendDto);
        });
    }

    /**
     * 发送消息
     * @param sendDto
     */
    public void sendMessage(MessageSendDto sendDto) {
        //
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        //发送消息
        rTopic.publish(sendDto);
    }

}

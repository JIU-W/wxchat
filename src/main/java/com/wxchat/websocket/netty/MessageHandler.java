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

    //定义一个监听常量：消息主题
    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtils channelContextUtils;

    /**
     * 监听消息 (该方法的主要功能是初始化一个消息监听器，监听指定的 Redis主题(Topic)，并在收到消息时执行相应的逻辑。)
     */
    @PostConstruct  //表示在依赖注入完成后执行lisMessage方法
    public void lisMessage() {
        //根据主题名称获取消息主题  指定的 Redis 主题（Topic）
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        //监听消息
        rTopic.addListener(MessageSendDto.class, (MessageSendDto, sendDto) -> {
            logger.info("收到广播消息:{}", JSON.toJSONString(sendDto));
            //发送消息(拿到消息后去 )
            channelContextUtils.sendMessage(sendDto);
        });
    }

    /**
     * 发送消息
     * @param sendDto
     */
    public void sendMessage(MessageSendDto sendDto) {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(sendDto);
    }

}

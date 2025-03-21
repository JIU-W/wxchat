package com.wxchat.websocket;

import com.alibaba.fastjson.JSON;
import com.wxchat.entity.dto.MessageSendDto;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @description 消息处理器   通过这个处理器(借用redisson来管理服务器)从而"实现跨服务器的消息发送与接收"。
 *                                                                     (从而让服务器可以实现集群化部署)
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
    @PostConstruct  //表示在依赖注入完成后自动执行lisMessage方法
    public void lisMessage() {
        //根据主题名称获取消息主题
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        //监听消息
        rTopic.addListener(MessageSendDto.class, (MessageSendDto, sendDto) -> {
            logger.info("收到广播消息:{}", JSON.toJSONString(sendDto));
            //"实际消息的发送"，这里是真正的通过调用工具类的方法去实现单聊或者群聊的"消息发送"，
            //                          去实现发送消息到客户端。
            channelContextUtils.sendMessage(sendDto);
        });
    }

    /**
     * 发送消息  (将消息发布到Redis主题，触发所有订阅者的监听器。)
     * @param sendDto
     */
    public void sendMessage(MessageSendDto sendDto) {
        //根据主题名称获取消息主题
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        //发布消息，Redisson自动将对象序列化后广播
        rTopic.publish(sendDto);
    }

}

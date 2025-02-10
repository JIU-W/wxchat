package com.wxchat.websocket.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author JIU-W
 * @version 1.0
 * @description ws 心跳处理  超时处理器
 * @date 2025-02-09
 */
public class HandlerHeartBeat extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HandlerHeartBeat.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {//心跳超时
                //获取用户id
                Attribute<String> attribute = ctx.channel().attr(AttributeKey.valueOf(ctx.channel().id().toString()));
                //获取通道属性的值：userId
                String userId = attribute.get();
                logger.info("用户{}没有发送心跳断开连接", userId);
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush("heart");
            }
        }
    }

}

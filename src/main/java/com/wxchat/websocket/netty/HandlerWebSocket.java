package com.wxchat.websocket.netty;

import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.redis.RedisComponet;
import com.wxchat.utils.StringTools;
import com.wxchat.websocket.ChannelContextUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description ws 业务处理
 * @author JIU-W
 * @date 2025-02-09
 * @version 1.0
 */

/**
 * 设置通道共享
 */
@ChannelHandler.Sharable  //标记该handler可被多个channel安全共享，就是一个标记，没啥其他用处，但是Netty会检测
@Component("handlerWebSocket")
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(HandlerWebSocket.class);

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Resource
    private RedisComponet redisComponet;

    /**
     * 当通道就绪后会调用此方法，通常我们会在这里做一些初始化操作
     *            (用户登录上线的时候就会自动调用这个方法)
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        logger.info("有新的连接加入。。。");
    }

    /**
     * 当通道不再活跃时（连接关闭）会调用此方法，我们可以在这里做一些清理工作
     *             (用户退出登录的时候就会自动调用这个方法)
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("有连接已经断开。。。");
        //移除通道
        channelContextUtils.removeContext(ctx.channel());
    }

    /**
     * 读就绪事件 当有消息可读时会调用此方法，我们可以在这里读取消息并处理。  (用于接收心跳)
     * @param ctx
     * @param textWebSocketFrame
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        //获取通道
        Channel channel = ctx.channel();
        //获取通道属性
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        //获取通道属性的值：userId
        String userId = attribute.get();
        logger.info("用户{}发送了消息：{}", userId, textWebSocketFrame.text());
        //保存用户心跳到redis中
        redisComponet.saveUserHeartBeat(userId);
    }


    //用于处理用户自定义的事件  当有用户事件触发时会调用此方法，例如连接超时，异常等。
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete complete =
                    (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            //获取url
            String url = complete.requestUri();
            //从url中解析出token
            String token = getToken(url);
            if (token == null) {
                ctx.channel().close();//关闭连接
                return;
            }
            //从redis中获取token对应的用户信息
            TokenUserInfoDto tokenUserInfoDto = redisComponet.getTokenUserInfoDto(token);
            if (null == tokenUserInfoDto) {
                ctx.channel().close();//关闭连接
                return;
            }
            //走到这里就说明连接没有被断开

            /**
             * 用户加入 (用户在客户端登录账号的时候会走到这里)
             */
            channelContextUtils.addContext(tokenUserInfoDto.getUserId(), ctx.channel());
        }
    }

    //获取token
    private String getToken(String url) {
        if (StringTools.isEmpty(url) || url.indexOf("?") == -1) {
            return null;
        }
        String[] queryParams = url.split("\\?");
        if (queryParams.length < 2) {
            return null;
        }
        String[] params = queryParams[1].split("=");
        if (params.length != 2) {
            return null;
        }
        return params[1];
    }

}

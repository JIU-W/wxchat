package com.wxchat.websocket.netty;

import com.wxchat.entity.config.AppConfig;
import com.wxchat.utils.StringTools;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author JIU-W
 * @version 1.0
 * @description WebSocket服务端启动类
 * @date 2025-02-09
 */
@Component
public class NettyWebSocketStarter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketStarter.class);

    @Resource
    private AppConfig appConfig;

    @Resource
    private HandlerWebSocket handlerWebSocket;

    /**
     * boss线程组，用于处理连接
     */
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    /**
     * work线程组，用于处理消息
     */
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    /**
     * 资源关闭——在容器销毁时关闭
     */
    @PreDestroy
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void run() {
        try {
            //创建服务端启动助手
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            //设置几个重要的处理器

                            //1.对http协议的支持，使用http的编码器，解码器
                            pipeline.addLast(new HttpServerCodec());

                            //聚合解码 httpRequest/htppContent/lastHttpContent到fullHttpRequest
                            //2.保证接收的http请求的完整性
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));

                            //3.心跳规则
                            // 参数：long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit
                            // readerIdleTime  读超时时间(读空闲时间)
                            // writerIdleTime  写超时时间(写空闲时间)
                            // allIdleTime  所有类型的超时时间(读写空闲时间)
                            pipeline.addLast(new IdleStateHandler(6, 0,
                                    0, TimeUnit.SECONDS));
                            //4.心跳超时处理器
                            pipeline.addLast(new HandlerHeartBeat());

                            //5.将http协议升级为ws协议，对websocket支持
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws",
                                    null, true, 64 * 1024,
                                    true, true,
                                    10000L));
                            //6.自定义处理器
                            pipeline.addLast(handlerWebSocket);
                        }
                    });
            //以下对ws端口的处理是为了"模拟"在本机一个服务器的条件下"实现项目服务的集群"(多个server)
            Integer wsPort = appConfig.getWsPort();
            String wsPortStr = System.getProperty("ws.port");
            if(!StringTools.isEmpty(wsPortStr)){
                wsPort = Integer.parseInt(wsPortStr);
            }
            //启动
            ChannelFuture channelFuture = serverBootstrap.bind(wsPort).sync();
            logger.info("Netty服务端启动成功,端口:{}", appConfig.getWsPort());
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}

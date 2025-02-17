package com.wxchat.websocket;

import com.alibaba.fastjson.JSON;
import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.MessageSendDto;
import com.wxchat.entity.dto.WsInitData;
import com.wxchat.entity.enums.MessageTypeEnum;
import com.wxchat.entity.enums.UserContactApplyStatusEnum;
import com.wxchat.entity.enums.UserContactTypeEnum;
import com.wxchat.entity.po.*;
import com.wxchat.entity.query.*;
import com.wxchat.mappers.*;
import com.wxchat.redis.RedisComponet;
import com.wxchat.utils.JsonUtils;
import com.wxchat.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @description ws通道工具类
 * @author JIU-W
 * @date 2025-02-18
 * @version 1.0
 */
@Component("channelContextUtils")
public class ChannelContextUtils {

    private static final Logger logger = LoggerFactory.getLogger(ChannelContextUtils.class);

    @Resource
    private RedisComponet redisComponet;

    //用户通道  基于服务器内存存储，使用Map集合存储。  (Channel：用户通道)
    public static final ConcurrentMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap();

    //群组通道        (ChannelGroup：群组通道)
    public static final ConcurrentMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap();

    @Resource
    private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Resource
    private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

    /**
     * 用户加入通道 (用户登录上线)
     * @param userId
     * @param channel
     */
    public void addContext(String userId, Channel channel) {
        try {
            String channelId = channel.id().toString();
            AttributeKey attributeKey = null;
            if (!AttributeKey.exists(channelId)) {
                // 如果属性不存在，则创建一个属性
                attributeKey = AttributeKey.newInstance(channel.id().toString());
            } else {
                // 如果属性已经存在，则获取该属性
                attributeKey = AttributeKey.valueOf(channel.id().toString());
            }
            channel.attr(attributeKey).set(userId);

            //从redis里面获取用户的联系人信息(好友，群组)
            List<String> contactList = redisComponet.getUserContactList(userId);
            //遍历群组，添加到群组通道
            for (String groupId : contactList) {
                if (groupId.startsWith(UserContactTypeEnum.GROUP.getPrefix())) {
                    //添加到群组通道
                    add2Group(groupId, channel);
                }
            }

            //添加到用户通道
            USER_CONTEXT_MAP.put(userId, channel);
            //保存用户心跳到redis中
            redisComponet.saveUserHeartBeat(userId);

            //更新用户最后连接时间
            UserInfo updateInfo = new UserInfo();
            updateInfo.setLastLoginTime(new Date());
            userInfoMapper.updateByUserId(updateInfo, userId);

            //给用户发送一些消息

            //获取用户最后离线时间
            UserInfo userInfo = userInfoMapper.selectByUserId(userId);
            Long sourceLastOffTime = userInfo.getLastOffTime();
            //这里避免毫秒时间差，所以减去1秒的时间
            //如果时间太久，只取最近三天的消息数
            Long lastOffTime = sourceLastOffTime;
            if (sourceLastOffTime != null &&
                    System.currentTimeMillis() - Constants.MILLISECOND_3DAYS_AGO > sourceLastOffTime) {
                lastOffTime = Constants.MILLISECOND_3DAYS_AGO;
            }

            /**
             * 1、查询会话信息(查询用户所有会话，避免换设备会话不同步)
             */
            ChatSessionUserQuery sessionUserQuery = new ChatSessionUserQuery();
            sessionUserQuery.setUserId(userId);
            sessionUserQuery.setOrderBy("last_receive_time desc");
            List<ChatSessionUser> chatSessionList = chatSessionUserMapper.selectList(sessionUserQuery);
            WsInitData wsInitData = new WsInitData();
            //设置会话信息
            wsInitData.setChatSessionList(chatSessionList);

            /**
             * 2、查询聊天消息
             */
            //查询用户所有的联系人
            UserContactQuery contactQuery = new UserContactQuery();
            contactQuery.setContactType(UserContactTypeEnum.GROUP.getType());
            contactQuery.setUserId(userId);
            //查询该用户加入的所有群组
            List<UserContact> groupContactList = userContactMapper.selectList(contactQuery);
            //获取群组id集合
            List<String> groupIdList = groupContactList.stream()
                    .map(item -> item.getContactId()).collect(Collectors.toList());
            //将自己也加进去
            groupIdList.add(userId);

            //groupIdList("用户加入的所有群组id"和"自己的id")：
            //原因是为了查询用户离线消息(接收人就是"该用户加入的群组接受的消息"以及"该用户接受的消息")

            ChatMessageQuery messageQuery = new ChatMessageQuery();
            //设置(查询条件：接收联系人)为groupIdList(用户加入的所有群组id 和 自己的id)
            messageQuery.setContactIdList(groupIdList);
            //设置(查询条件：最后接收时间)为lastOffTime(小于等于三天)
            messageQuery.setLastReceiveTime(lastOffTime);
            //查询符合条件的"聊天消息集合"
            List<ChatMessage> chatMessageList = chatMessageMapper.selectList(messageQuery);
            //设置"离线消息列表"到wsInitData
            wsInitData.setChatMessageList(chatMessageList);

            /**
             * 3、查询好友申请数量
             */
            UserContactApplyQuery applyQuery = new UserContactApplyQuery();
            applyQuery.setReceiveUserId(userId);
            //设置为sourceLastOffTime(用户最后离线时间)
            applyQuery.setLastApplyTimestamp(sourceLastOffTime);
            applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
            Integer applyCount = userContactApplyMapper.selectCount(applyQuery);
            //设置"好友申请的消息数量"到wsInitData
            wsInitData.setApplyCount(applyCount);

            /**
             * 4、发送消息
             */
            MessageSendDto messageSendDto = new MessageSendDto();
            messageSendDto.setMessageType(MessageTypeEnum.INIT.getType());
            messageSendDto.setContactId(userId);
            messageSendDto.setExtendData(wsInitData);

            sendMsg(messageSendDto, userId);

        } catch (Exception e) {
            logger.error("初始化链接失败", e);
        }

    }

    /**
     * 删除通道连接异常
     * @param channel
     */
    public void removeContext(Channel channel) {
        //获取属性
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        //获取属性的值:userId
        String userId = attribute.get();
        if (!StringTools.isEmpty(userId)) {
            //从用户通道中删除
            USER_CONTEXT_MAP.remove(userId);
        }
        //删除redis里面用户心跳
        redisComponet.removeUserHeartBeat(userId);

        //更新用户最后断线时间
        UserInfo userInfo = new UserInfo();
        userInfo.setLastOffTime(System.currentTimeMillis());
        userInfoMapper.updateByUserId(userInfo, userId);
    }

    public void sendMessage(MessageSendDto messageSendDto) {
        //根据消息类型判断是发"单聊消息"还是"群聊消息"
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(messageSendDto.getContactId());
        switch (contactTypeEnum) {
            case USER: //发送消息给用户
                send2User(messageSendDto);
                break;
            case GROUP: //发送消息到群聊
                sendMsg2Group(messageSendDto);
        }
    }

    /**
     * 发送消息给用户
     */
    private void send2User(MessageSendDto messageSendDto) {
        //获取消息接收人ID
        String contactId = messageSendDto.getContactId();
        //发送消息
        sendMsg(messageSendDto, contactId);
        //强制下线
        if (MessageTypeEnum.FORCE_OFF_LINE.getType().equals(messageSendDto.getMessageType())) {
            //关闭通道
            closeContext(contactId);
        }
    }

    /**
     * 关闭通道连接
     * @param userId
     */
    public void closeContext(String userId) {
        if (StringTools.isEmpty(userId)) {
            return;
        }
        //清除用户token信息
        redisComponet.cleanUserTokenByUserId(userId);
        //从用户通道中获取channel
        Channel channel = USER_CONTEXT_MAP.get(userId);
        //从用户通道中删除
        USER_CONTEXT_MAP.remove(userId);
        if (channel != null) {
            channel.close();//关闭channel
        }
    }


    /**
     * 发送消息到群组
     */
    private void sendMsg2Group(MessageSendDto messageSendDto) {
        if (messageSendDto.getContactId() == null) {
            return;
        }
        ChannelGroup group = GROUP_CONTEXT_MAP.get(messageSendDto.getContactId());
        if (group == null) {
            return;
        }
        group.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(messageSendDto)));

        //移除群聊
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(messageSendDto.getMessageType());
        if (MessageTypeEnum.LEAVE_GROUP == messageTypeEnum || MessageTypeEnum.REMOVE_GROUP == messageTypeEnum) {
            String userId = (String) messageSendDto.getExtendData();
            redisComponet.removeUserContact(userId, messageSendDto.getContactId());
            Channel channel = USER_CONTEXT_MAP.get(userId);
            if (channel == null) {
                return;
            }
            group.remove(channel);
        }

        if (MessageTypeEnum.DISSOLUTION_GROUP == messageTypeEnum) {
            GROUP_CONTEXT_MAP.remove(messageSendDto.getContactId());
            group.close();
        }
    }


    private static void sendMsg(MessageSendDto messageSendDto, String reciveId) {
        if (reciveId == null) {
            return;
        }
        Channel sendChannel = USER_CONTEXT_MAP.get(reciveId);
        if (sendChannel == null) {
            return;
        }

        //相当于客户端而言，联系人就是发送人，所以这里转换一下再发送，好友打招呼信息发送给自己需要特殊处理
        if (MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDto.getMessageType())) {
            UserInfo userInfo = (UserInfo) messageSendDto.getExtendData();
            messageSendDto.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDto.setContactId(userInfo.getUserId());
            messageSendDto.setContactName(userInfo.getNickName());
            messageSendDto.setExtendData(null);
        } else {
            messageSendDto.setContactId(messageSendDto.getSendUserId());
            messageSendDto.setContactName(messageSendDto.getSendUserNickName());
        }
        //发送消息
        sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDto)));
    }

    //添加到群聊
    private void add2Group(String groupId, Channel context) {
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        //如果群聊不存在，则创建一个
        if (group == null) {
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONTEXT_MAP.put(groupId, group);
        }
        if (context == null) {
            return;
        }
        //添加到群聊
        group.add(context);
    }

    public void addUser2Group(String userId, String groupId) {
        Channel channel = USER_CONTEXT_MAP.get(userId);
        add2Group(groupId, channel);
    }

}

package com.wxchat.service.impl;

import com.wxchat.entity.dto.MessageSendDto;
import com.wxchat.entity.dto.SysSettingDto;
import com.wxchat.entity.dto.UserContactSearchResultDto;
import com.wxchat.entity.enums.*;
import com.wxchat.entity.po.*;
import com.wxchat.entity.query.*;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.mappers.*;
import com.wxchat.redis.RedisComponet;
import com.wxchat.service.UserContactService;
import com.wxchat.utils.CopyTools;
import com.wxchat.utils.StringTools;
import com.wxchat.websocket.ChannelContextUtils;
import com.wxchat.websocket.MessageHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 联系人 业务接口实现
 */
@Service("userContactService")
public class UserContactServiceImpl implements UserContactService {

    @Resource
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private RedisComponet redisComponet;

    @Resource
    private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

    @Resource
    private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private ChannelContextUtils channelContextUtils;


    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserContact> findListByParam(UserContactQuery param) {
        return this.userContactMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserContactQuery param) {
        return this.userContactMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserContact> findListByPage(UserContactQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserContact> list = this.findListByParam(param);
        PaginationResultVO<UserContact> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserContact bean) {
        return this.userContactMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserContact> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserContact> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(UserContact bean, UserContactQuery param) {
        StringTools.checkParam(param);
        return this.userContactMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(UserContactQuery param) {
        StringTools.checkParam(param);
        return this.userContactMapper.deleteByParam(param);
    }

    /**
     * 根据UserIdAndContactId获取对象
     */
    @Override
    public UserContact getUserContactByUserIdAndContactId(String userId, String contactId) {
        return this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
    }

    /**
     * 根据UserIdAndContactId修改
     */
    @Override
    public Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId, String contactId) {
        return this.userContactMapper.updateByUserIdAndContactId(bean, userId, contactId);
    }

    /**
     * 根据UserIdAndContactId删除
     */
    @Override
    public Integer deleteUserContactByUserIdAndContactId(String userId, String contactId) {
        return this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
    }

    public UserContactSearchResultDto searchContact(String userId, String contactId) {
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (typeEnum == null) {
            return null;
        }
        UserContactSearchResultDto resultDto = new UserContactSearchResultDto();
        switch (typeEnum) {
            case USER://搜索的联系人类型为"好友"
                UserInfo userInfo = userInfoMapper.selectByUserId(contactId);
                if (userInfo == null) {
                    return null;
                }
                //将用户信息拷贝到搜索结果中
                resultDto = CopyTools.copy(userInfo, UserContactSearchResultDto.class);
                break;
            case GROUP:
                GroupInfo groupInfo = groupInfoMapper.selectByGroupId(contactId);
                if (null == groupInfo) {
                    return null;
                }
                //将群信息(群组名)拷贝到搜索结果中
                resultDto.setNickName(groupInfo.getGroupName());
                break;
        }
        //将联系人类型及其ID拷贝到搜索结果中
        resultDto.setContactType(typeEnum.toString());
        resultDto.setContactId(contactId);

        //判断是否是自己
        if (userId.equals(contactId)) {
            //如果是自己，则直接返回好友状态
            resultDto.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            return resultDto;
        }
        //查询是否已经是好友
        //查询联系人状态status并放入resultDto中
        UserContact userContact = this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
        resultDto.setStatus(userContact == null ? null : userContact.getStatus());
        return resultDto;
    }

    /**
     * 添加联系人：添加好友或者群组
     */
    public void addContact(String applyUserId, String receiveUserId, String contactId, Integer contactType, String applyInfo) {
        //联系人类型为"群组"类型时的前置判断：群组的人数上限判断
        if (UserContactTypeEnum.GROUP.getType().equals(contactType)) {
            UserContactQuery contactQuery = new UserContactQuery();
            contactQuery.setContactId(contactId);
            contactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            Integer count = userContactMapper.selectCount(contactQuery);
            SysSettingDto sysSettingDto = redisComponet.getSysSetting();
            if (count >= sysSettingDto.getMaxGroupMemberCount()) {
                throw new BusinessException("成员已满，无法加入");
            }
        }

        //同意，双方添加为好友
        List<UserContact> contactList = new ArrayList<>();
        Date curDate = new Date();
        //因为加好友是互相添加的，所以要添加两条记录。
        //而进群组是只是申请人进，消息接收人(群组)本身就在群组里，所以只要添加一条记录。

        //申请人添加接收人：(注：不管是"好友"类型还是"群组"类型，申请人这边都要添加记录)
        UserContact userContact = new UserContact();
        userContact.setUserId(applyUserId);
        userContact.setContactId(contactId);
        userContact.setContactType(contactType);
        userContact.setCreateTime(curDate);
        userContact.setLastUpdateTime(curDate);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        contactList.add(userContact);
        //接收人添加申请人(前提：联系人类型是"好友"类型)(注："群组"类型的话，接收人即群主不用再次加入本群，也就不用添加记录)
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            userContact = new UserContact();
            userContact.setUserId(receiveUserId);
            userContact.setContactId(applyUserId);
            userContact.setContactType(contactType);
            userContact.setCreateTime(curDate);
            userContact.setLastUpdateTime(curDate);
            userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            contactList.add(userContact);
        }
        //批量加入
        userContactMapper.insertOrUpdateBatch(contactList);

        //添加缓存:
        //申请人添加联系人(好友或群组)
        redisComponet.addUserContact(applyUserId, contactId);

        //如果是好友申请，接收人也添加申请人为联系人(好友)
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            redisComponet.addUserContact(receiveUserId, applyUserId);
        }

        //创建会话信息
        String sessionId = null;
        if (UserContactTypeEnum.USER.getType().equals(contactType)) {
            //获取会话ID(单聊)
            sessionId = StringTools.getChatSessionId4User(new String[]{applyUserId, contactId});
        } else {
            //获取会话ID(群聊)
            sessionId = StringTools.getChatSessionId4Group(contactId);
        }

        //会话参与人
        List<ChatSessionUser> chatSessionUserList = new ArrayList<>();

        if (UserContactTypeEnum.USER.getType().equals(contactType)) {//单聊
            //创建会话
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionId(sessionId);
            chatSession.setLastMessage(applyInfo);
            chatSession.setLastReceiveTime(curDate.getTime());
            this.chatSessionMapper.insertOrUpdate(chatSession);

            //添加"会话用户"表记录(两条记录)
            //1.申请人session
            ChatSessionUser applySessionUser = new ChatSessionUser();
            applySessionUser.setUserId(applyUserId);
            applySessionUser.setContactId(contactId);
            applySessionUser.setSessionId(sessionId);
            //applySessionUser.setLastReceiveTime(curDate.getTime());
            //applySessionUser.setLastMessage(applyInfo);
            //查询接收人信息
            UserInfo contactUser = this.userInfoMapper.selectByUserId(contactId);
            applySessionUser.setContactName(contactUser.getNickName());
            chatSessionUserList.add(applySessionUser);

            //2.接收人session
            ChatSessionUser contactSessionUser = new ChatSessionUser();
            contactSessionUser.setUserId(contactId);
            contactSessionUser.setContactId(applyUserId);
            contactSessionUser.setSessionId(sessionId);
            //contactSessionUser.setLastReceiveTime(curDate.getTime());
            //contactSessionUser.setLastMessage(applyInfo);
            //查询申请人信息
            UserInfo applyUserInfo = this.userInfoMapper.selectByUserId(applyUserId);
            contactSessionUser.setContactName(applyUserInfo.getNickName());
            chatSessionUserList.add(contactSessionUser);

            //两条记录同时插入"会话用户"表
            this.chatSessionUserMapper.insertOrUpdateBatch(chatSessionUserList);

            //记录消息到"聊天消息表"
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());//"添加好友打招呼"类型的消息
            chatMessage.setMessageContent(applyInfo);
            chatMessage.setSendUserId(applyUserId);
            chatMessage.setSendUserNickName(applyUserInfo.getNickName());
            chatMessage.setSendTime(curDate.getTime());
            chatMessage.setContactId(contactId);
            chatMessage.setContactType(UserContactTypeEnum.USER.getType());//单聊
            chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
            chatMessageMapper.insert(chatMessage);

            //以上是对三张数据库表的操作

            //以下是发送消息部分

            //封装messageSendDto的数据
            MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
            /**
             * 发送给接受好友申请的人
             */
            messageHandler.sendMessage(messageSendDto);

            /**
             * 发送给申请人 (因为这里是添加好友成功后，成功后，打招呼信息不仅要发给"接收人"的客户端，
             * 同时也要发送给"申请人"的客户端，从而让"申请人的聊天会话框"也可以渲染这个打招呼信息)
             *
             * 1.给自己发送ws消息，把联系人(接收人)改成申请人从而找到channel。
             */
            //TODO 没完全理解
            messageSendDto.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());//
            messageSendDto.setContactId(applyUserId);//发送给申请人
            messageSendDto.setExtendData(contactUser);//扩展数据：扩展数据是"初始的接收人信息"。(这个数据后续有用)
            messageHandler.sendMessage(messageSendDto);

        } else {//群聊

            /**
            * 加入群组
             */

            //创建"会话用户"
            ChatSessionUser chatSessionUser = new ChatSessionUser();
            chatSessionUser.setUserId(applyUserId);
            chatSessionUser.setContactId(contactId);
            GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
            chatSessionUser.setContactName(groupInfo.getGroupName());
            chatSessionUser.setSessionId(sessionId);
            //为什么是insertOrUpdate，而不是insert，因为"用户加入的群组"可能是"之前加入过的又退出的群"，所以
            //这种情况会话是需要更新，而不是插入。
            this.chatSessionUserMapper.insertOrUpdate(chatSessionUser);


            UserInfo applyUserInfo = this.userInfoMapper.selectByUserId(applyUserId);
            //组装消息
            String sendMessage = String.format(MessageTypeEnum.ADD_GROUP.getInitMessage(),
                    applyUserInfo.getNickName());

            //创建会话信息(增加session信息)
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionId(sessionId);
            chatSession.setLastReceiveTime(curDate.getTime());
            chatSession.setLastMessage(sendMessage);
            this.chatSessionMapper.insertOrUpdate(chatSession);

            //增加聊天消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.ADD_GROUP.getType());
            chatMessage.setMessageContent(sendMessage);
            chatMessage.setSendUserId(null);
            chatMessage.setSendUserNickName(null);
            chatMessage.setSendTime(curDate.getTime());
            chatMessage.setContactId(contactId);
            chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
            chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
            chatMessageMapper.insert(chatMessage);


            //把"用户通道"加入到"群组通道"中，使其成为群组成员
            channelContextUtils.addUser2Group(applyUserId, groupInfo.getGroupId());


            //发送群消息
            //封装MessageSendDto数据
            MessageSendDto messageSend = CopyTools.copy(chatMessage, MessageSendDto.class);
            messageSend.setContactId(groupInfo.getGroupId());
            //获取群成员数量
            UserContactQuery userContactQuery = new UserContactQuery();
            userContactQuery.setContactId(contactId);
            userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            Integer memberCount = this.userContactMapper.selectCount(userContactQuery);
            messageSend.setMemberCount(memberCount);
            messageSend.setContactName(groupInfo.getGroupName());
            //发送群消息到redis主题
            messageHandler.sendMessage(messageSend);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum) {
        //移除好友
        UserContact userContact = new UserContact();
        userContact.setStatus(statusEnum.getStatus());
        userContactMapper.updateByUserIdAndContactId(userContact, userId, contactId);

        //好友中也移除自己
        UserContact friendContact = new UserContact();
        if (UserContactStatusEnum.DEL == statusEnum) {
            friendContact.setStatus(UserContactStatusEnum.DEL_BE.getStatus());
        } else if (UserContactStatusEnum.BLACKLIST == statusEnum) {
            friendContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
        }
        userContactMapper.updateByUserIdAndContactId(friendContact, contactId, userId);

        //缓存相关
        //将我从对方的好友缓存中删除
        redisComponet.removeUserContact(contactId, userId);
        //将对方从我的列表中删除
        redisComponet.removeUserContact(userId, contactId);
    }

    @Override
    public void removeGroupContact(String userId, String groupId, String contactId, UserContactStatusEnum statusEnum) {
        GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
        if (null == groupInfo || !groupInfo.getGroupOwnerId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserContact userContact = new UserContact();
        userContact.setStatus(statusEnum.getStatus());
        userContactMapper.updateByUserIdAndContactId(userContact, contactId, groupId);
        //将群组从群员列表中删除
        redisComponet.removeUserContact(contactId, groupId);
    }


    @Transactional(rollbackFor = Exception.class)
    public void addContact4Robot(String userId) {
        Date curDate = new Date();

        SysSettingDto sysSettingDto = redisComponet.getSysSetting();
        String contactId = sysSettingDto.getRobotUid();
        String contactName = sysSettingDto.getRobotNickName();
        String senMessage = sysSettingDto.getRobotWelcome();
        //清理html标签
        senMessage = StringTools.cleanHtmlTag(senMessage);

        //增加机器人好友
        UserContact userContact = new UserContact();
        userContact.setUserId(userId);
        userContact.setContactId(contactId);
        userContact.setContactType(UserContactTypeEnum.USER.getType());
        userContact.setCreateTime(curDate);
        userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        userContact.setLastUpdateTime(curDate);
        userContactMapper.insert(userContact);

        //增加会话信息
        String sessionId = StringTools.getChatSessionId4User(new String[]{userId, contactId});//获取会话id
        ChatSession chatSession = new ChatSession();
        chatSession.setLastMessage(senMessage);
        chatSession.setSessionId(sessionId);
        chatSession.setLastReceiveTime(curDate.getTime());
        this.chatSessionMapper.insert(chatSession);

        //增加会话人信息
        ChatSessionUser applySessionUser = new ChatSessionUser();
        applySessionUser.setUserId(userId);
        applySessionUser.setContactId(contactId);
        applySessionUser.setContactName(contactName);
        applySessionUser.setSessionId(sessionId);
        this.chatSessionUserMapper.insertOrUpdate(applySessionUser);

        //增加聊天消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
        chatMessage.setMessageContent(senMessage);
        chatMessage.setSendUserId(contactId);
        chatMessage.setSendUserNickName(contactName);
        chatMessage.setSendTime(curDate.getTime());
        chatMessage.setContactId(userId);
        chatMessage.setContactType(UserContactTypeEnum.USER.getType());
        chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
        chatMessageMapper.insert(chatMessage);
    }

}

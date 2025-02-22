package com.wxchat.service.impl;

import com.wxchat.entity.config.AppConfig;
import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.MessageSendDto;
import com.wxchat.entity.dto.SysSettingDto;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.entity.enums.*;
import com.wxchat.entity.po.ChatMessage;
import com.wxchat.entity.po.ChatSession;
import com.wxchat.entity.po.UserContact;
import com.wxchat.entity.query.ChatMessageQuery;
import com.wxchat.entity.query.ChatSessionQuery;
import com.wxchat.entity.query.SimplePage;
import com.wxchat.entity.query.UserContactQuery;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.mappers.ChatMessageMapper;
import com.wxchat.mappers.ChatSessionMapper;
import com.wxchat.mappers.UserContactMapper;
import com.wxchat.redis.RedisComponet;
import com.wxchat.service.ChatMessageService;
import com.wxchat.utils.CopyTools;
import com.wxchat.utils.DateUtil;
import com.wxchat.utils.StringTools;
import com.wxchat.websocket.MessageHandler;
import jodd.util.ArraysUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;


/**
 * 聊天消息表 业务接口实现
 */
@Service("chatMessageService")
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Resource
    private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private AppConfig appConfig;

    @Resource
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Resource
    private RedisComponet redisComponet;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ChatMessage> findListByParam(ChatMessageQuery param) {
        return this.chatMessageMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(ChatMessageQuery param) {
        return this.chatMessageMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<ChatMessage> list = this.findListByParam(param);
        PaginationResultVO<ChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ChatMessage bean) {
        return this.chatMessageMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ChatMessage> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.chatMessageMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ChatMessage> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.chatMessageMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(ChatMessage bean, ChatMessageQuery param) {
        StringTools.checkParam(param);
        return this.chatMessageMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(ChatMessageQuery param) {
        StringTools.checkParam(param);
        return this.chatMessageMapper.deleteByParam(param);
    }

    /**
     * 根据MessageId获取对象
     */
    @Override
    public ChatMessage getChatMessageByMessageId(Long messageId) {
        return this.chatMessageMapper.selectByMessageId(messageId);
    }

    /**
     * 根据MessageId修改
     */
    @Override
    public Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId) {
        return this.chatMessageMapper.updateByMessageId(bean, messageId);
    }

    /**
     * 根据MessageId删除
     */
    @Override
    public Integer deleteChatMessageByMessageId(Long messageId) {
        return this.chatMessageMapper.deleteByMessageId(messageId);
    }


    public MessageSendDto saveMessage(ChatMessage chatMessage, TokenUserInfoDto tokenUserInfoDto) {
        //不是"机器人回复用户的消息"的话，都要进行判断。(因为机器人回复我们的时候也会调这个方法)
        if (!Constants.ROBOT_UID.equals(tokenUserInfoDto.getUserId())) {
            //获取用户联系人(好友，加入的群组，机器人)列表
            List<String> contactList = redisComponet.getUserContactList(tokenUserInfoDto.getUserId());
            if (!contactList.contains(chatMessage.getContactId())) {
                UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
                if (UserContactTypeEnum.USER == userContactTypeEnum) {
                    //不是好友
                    throw new BusinessException(ResponseCodeEnum.CODE_902);
                } else {
                    //不在群组中
                    throw new BusinessException(ResponseCodeEnum.CODE_903);
                }
            }
        }
        String sessionId = null;
        String sendUserId = tokenUserInfoDto.getUserId();
        String contactId = chatMessage.getContactId();
        Long curTime = System.currentTimeMillis();//获取当前时间的时间戳(毫秒)
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(chatMessage.getMessageType());
        String lastMessage = chatMessage.getMessageContent();
        //把消息内容进行过滤(清除html标签：防止消息注入)
        String messageContent = StringTools.resetMessageContent(chatMessage.getMessageContent());
        chatMessage.setMessageContent(messageContent);
        //设置消息状态
        Integer status = MessageTypeEnum.MEDIA_CHAT == messageTypeEnum ?
                MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDED.getStatus();
        if (ArraysUtil.contains(new Integer[]{//"普通聊天消息"、"媒体文件"、"群组创建成功"、"添加好友打招呼消息"
                MessageTypeEnum.CHAT.getType(), MessageTypeEnum.GROUP_CREATE.getType(),
                MessageTypeEnum.ADD_FRIEND.getType(), MessageTypeEnum.MEDIA_CHAT.getType()
        }, messageTypeEnum.getType())) {
            if (UserContactTypeEnum.USER == contactTypeEnum) {
                sessionId = StringTools.getChatSessionId4User(new String[]{sendUserId, contactId});
            } else {
                sessionId = StringTools.getChatSessionId4Group(contactId);
            }

            //更新会话消息(ChatSession)
            ChatSession chatSession = new ChatSession();
            chatSession.setLastMessage(messageContent);
            //如果是在群组里发消息，则发消息的时候要带上用户自己的"群昵称"
            if (UserContactTypeEnum.GROUP == contactTypeEnum &&
                    !MessageTypeEnum.GROUP_CREATE.getType().equals(messageTypeEnum.getType())) {
                chatSession.setLastMessage(tokenUserInfoDto.getNickName() + "：" + messageContent);
            }
            lastMessage = chatSession.getLastMessage();
            //如果是媒体文件
            chatSession.setLastReceiveTime(curTime);
            chatSessionMapper.updateBySessionId(chatSession, sessionId);

            //记录消息表(ChatMessage)
            chatMessage.setSessionId(sessionId);
            chatMessage.setSendUserId(sendUserId);
            chatMessage.setSendUserNickName(tokenUserInfoDto.getNickName());
            chatMessage.setSendTime(curTime);
            chatMessage.setContactType(contactTypeEnum.getType());
            chatMessage.setStatus(status);
            chatMessageMapper.insert(chatMessage);
        }

        //发送消息
        MessageSendDto messageSend = CopyTools.copy(chatMessage, MessageSendDto.class);

        if (Constants.ROBOT_UID.equals(contactId)) {//发消息给机器人
            SysSettingDto sysSettingDto = redisComponet.getSysSetting();
            TokenUserInfoDto robot = new TokenUserInfoDto();
            robot.setUserId(sysSettingDto.getRobotUid());
            robot.setNickName(sysSettingDto.getRobotNickName());
            //封装机器人回复的消息到ChatMessage
            ChatMessage robotChatMessage = new ChatMessage();
            robotChatMessage.setContactId(sendUserId);
            //TODO 这里可以对接Ai 根据输入的信息做出回答
            robotChatMessage.setMessageContent("我只是一个机器人无法识别你的消息");
            robotChatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
            //设置机器人的回复消息
            saveMessage(robotChatMessage, robot);
        } else {
            //发送消息(和好友，在群组发消息)
            messageHandler.sendMessage(messageSend);
        }
        return messageSend;
    }

    public void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) {
        ChatMessage message = chatMessageMapper.selectByMessageId(messageId);
        if (null == message) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!message.getSendUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //获取系统设置
        SysSettingDto sysSettingDto = redisComponet.getSysSetting();
        //获取文件后缀
        String fileSuffix = StringTools.getFileSuffix(file.getOriginalFilename());
        if (!StringTools.isEmpty(fileSuffix) && ArraysUtil.contains(Constants.IMAGE_SUFFIX_LIST, fileSuffix.toLowerCase())
                && file.getSize() > Constants.FILE_SIZE_MB * sysSettingDto.getMaxImageSize()) {
            //图片过大
            return;
        } else if (!StringTools.isEmpty(fileSuffix) && ArraysUtil.contains(Constants.VIDEO_SUFFIX_LIST, fileSuffix.toLowerCase())
                && file.getSize() > Constants.FILE_SIZE_MB * sysSettingDto.getMaxVideoSize()) {
            //视频过大
            return;
        } else if (!StringTools.isEmpty(fileSuffix) &&
                !ArraysUtil.contains(Constants.VIDEO_SUFFIX_LIST, fileSuffix.toLowerCase()) &&
                !ArraysUtil.contains(Constants.IMAGE_SUFFIX_LIST, fileSuffix.toLowerCase()) &&
                file.getSize() > Constants.FILE_SIZE_MB * sysSettingDto.getMaxFileSize()) {
            //其它文件过大
            return;
        }

        //文件原始名
        String fileName = file.getOriginalFilename();
        //文件后缀名
        String fileExtName = StringTools.getFileSuffix(fileName);
        //新文件名：消息id + 文件后缀
        String fileRealName = messageId + fileExtName;
        //获取文件发送时间对应的"年月"，用来组成文件路径
        String month = DateUtil.format(new Date(message.getSendTime()), DateTimePatternEnum.YYYYMM.getPattern());
        File folder = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + month);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File uploadFile = new File(folder.getPath() + "/" + fileRealName);
        try {
            file.transferTo(uploadFile);
            //cover是文件对应的缩略图
            if (cover != null) {
                cover.transferTo(new File(uploadFile.getPath() + Constants.COVER_IMAGE_SUFFIX));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("上传文件失败", e);
            throw new BusinessException("文件上传失败");
        }
        //修改消息状态
        ChatMessage updateInfo = new ChatMessage();
        updateInfo.setStatus(MessageStatusEnum.SENDED.getStatus());
        ChatMessageQuery messageQuery = new ChatMessageQuery();
        messageQuery.setMessageId(messageId);
        chatMessageMapper.updateByParam(updateInfo, messageQuery);

        //发送消息(发送给"要接收这个图片文件的客户端")(这个消息不用入库)
        MessageSendDto messageSend = new MessageSendDto();
        messageSend.setStatus(MessageStatusEnum.SENDED.getStatus());
        messageSend.setMessageId(message.getMessageId());
        messageSend.setMessageType(MessageTypeEnum.FILE_UPLOAD.getType());
        messageSend.setContactId(message.getContactId());
        //发送消息到redis主题
        messageHandler.sendMessage(messageSend);
    }

    @Override
    public File downloadFile(TokenUserInfoDto userInfoDto, Long messageId, Boolean cover) {
        ChatMessage message = chatMessageMapper.selectByMessageId(messageId);
        String contactId = message.getContactId();
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (UserContactTypeEnum.USER.getType().equals(contactTypeEnum) && !userInfoDto.getUserId().equals(message.getContactId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (UserContactTypeEnum.GROUP.getType().equals(contactTypeEnum)) {
            UserContactQuery userContactQuery = new UserContactQuery();
            userContactQuery.setUserId(userInfoDto.getUserId());
            userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());
            userContactQuery.setContactId(contactId);
            userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            Integer contactCount = userContactMapper.selectCount(userContactQuery);
            if (contactCount == 0) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        String month = DateUtil.format(new Date(message.getSendTime()), DateTimePatternEnum.YYYYMM.getPattern());
        File folder = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + month);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String fileName = message.getFileName();
        String fileExtName = StringTools.getFileSuffix(fileName);
        String fileRealName = messageId + fileExtName;

        if (cover != null && cover) {
            fileRealName = fileRealName + Constants.COVER_IMAGE_SUFFIX;
        }
        File file = new File(folder.getPath() + "/" + fileRealName);
        if (!file.exists()) {
            logger.info("文件不存在");
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        return file;
    }

}

package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.config.AppConfig;
import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.MessageSendDto;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.entity.enums.MessageTypeEnum;
import com.wxchat.entity.enums.ResponseCodeEnum;
import com.wxchat.entity.po.ChatMessage;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.service.ChatMessageService;
import com.wxchat.service.ChatSessionUserService;
import com.wxchat.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @description 聊天控制器
 * @author JIU-W
 * @date 2025-02-21
 * @version 1.0
 */
@RestController
@RequestMapping("/chat")
public class ChatController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatSessionUserService chatSessionUserService;

    @Resource
    private AppConfig appConfig;

    /**
     * 发送消息
     */
    @RequestMapping("/sendMessage")
    @GlobalInterceptor
    public ResponseVO sendMessage(HttpServletRequest request, @NotEmpty String contactId,
                                  @NotEmpty @Max(500) String messageContent, @NotNull Integer messageType,
                                  Long fileSize, String fileName, Integer fileType) {
        //获取消息类型
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(messageType);
        if (null == messageTypeEnum || !ArrayUtils.contains(new Integer[]{MessageTypeEnum.CHAT.getType(),
                            MessageTypeEnum.MEDIA_CHAT.getType()}, messageType)) {
            //消息类型必须有且必须是"普通聊天消息"类型或者"媒体文件"类型
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //获取用户信息
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContactId(contactId);
        chatMessage.setMessageContent(messageContent);
        chatMessage.setFileSize(fileSize);
        chatMessage.setFileName(fileName);
        chatMessage.setFileType(fileType);
        chatMessage.setMessageType(messageType);
        //发送消息
        MessageSendDto messageSendDto = chatMessageService.saveMessage(chatMessage, tokenUserInfoDto);
        return getSuccessResponseVO(messageSendDto);
    }

    @RequestMapping("uploadFile")
    @GlobalInterceptor
    public ResponseVO uploadFile(HttpServletRequest request, @NotNull Long messageId,
                                 @NotNull MultipartFile file, @NotNull MultipartFile cover) {
        TokenUserInfoDto userInfoDto = getTokenUserInfo(request);
        chatMessageService.saveMessageFile(userInfoDto.getUserId(), messageId, file, cover);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("downloadFile")
    @GlobalInterceptor
    public void downloadFile(HttpServletRequest request, HttpServletResponse response,
                             @NotEmpty String fileId, @NotNull Boolean showCover) throws Exception {
        TokenUserInfoDto userInfoDto = getTokenUserInfo(request);
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = null;
            if (!StringTools.isNumber(fileId)) {
                String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
                String avatarPath = appConfig.getProjectFolder() + avatarFolderName + fileId + Constants.IMAGE_SUFFIX;
                if (showCover) {
                    avatarPath = avatarPath + Constants.COVER_IMAGE_SUFFIX;
                }
                file = new File(avatarPath);
                if (!file.exists()) {
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
            } else {
                file = chatMessageService.downloadFile(userInfoDto, Long.parseLong(fileId), showCover);
            }
            response.setContentType("application/x-msdownload; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;");
            response.setContentLengthLong(file.length());
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
        }

    }



}

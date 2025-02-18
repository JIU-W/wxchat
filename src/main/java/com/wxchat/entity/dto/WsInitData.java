package com.wxchat.entity.dto;


import com.wxchat.entity.po.ChatMessage;
import com.wxchat.entity.po.ChatSessionUser;

import java.util.List;
/**
 * @description websocket初始化数据
 * @author JIU-W
 * @date 2025-02-18
 * @version 1.0
 */
public class WsInitData {

    //会话列表
    private List<ChatSessionUser> chatSessionList;

    //离线消息列表
    private List<ChatMessage> chatMessageList;

    //申请消息数量
    private Integer applyCount;

    public List<ChatSessionUser> getChatSessionList() {
        return chatSessionList;
    }

    public void setChatSessionList(List<ChatSessionUser> chatSessionList) {
        this.chatSessionList = chatSessionList;
    }

    public List<ChatMessage> getChatMessageList() {
        return chatMessageList;
    }

    public void setChatMessageList(List<ChatMessage> chatMessageList) {
        this.chatMessageList = chatMessageList;
    }

    public Integer getApplyCount() {
        return applyCount;
    }

    public void setApplyCount(Integer applyCount) {
        this.applyCount = applyCount;
    }
}

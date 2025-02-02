package com.wxchat.entity.vo;

import com.wxchat.entity.po.GroupInfo;
import com.wxchat.entity.po.UserContact;

import java.util.List;

/**
 * @description GroupInfoVO
 * @author JIU-W
 * @date 2025-01-24
 * @version 1.0
 */
public class GroupInfoVO {

    //群聊详情信息
    private GroupInfo groupInfo;

    //联系人信息表的数据(该群的成员)及其"群成员用户信息"
    private List<UserContact> userContactList;

    public List<UserContact> getUserContactList() {
        return userContactList;
    }

    public void setUserContactList(List<UserContact> userContactList) {
        this.userContactList = userContactList;
    }

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }
}

package com.wxchat.entity.dto;

import com.wxchat.entity.enums.UserContactStatusEnum;

public class UserContactSearchResultDto {

    //用户的好友ID 或者 用户加入的群组ID
    private String contactId;

    //联系人类型：0-好友，1-群组
    private String contactType;

    //联系人名称 或者 群组名称
    private String nickName;

    //头像最后更新时间
    private Long avatarLastUpdate;

    //联系人状态 或者 群组状态
    private Integer status;

    //联系人状态名称 或者 群组状态名称
    private String statusName;

    //联系人性别
    private Integer sex;

    //联系人地区
    private String areaName;

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getContactType() {
        return contactType;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Long getAvatarLastUpdate() {
        return avatarLastUpdate;
    }

    public void setAvatarLastUpdate(Long avatarLastUpdate) {
        this.avatarLastUpdate = avatarLastUpdate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        UserContactStatusEnum statusEnum = UserContactStatusEnum.getByStatus(status);
        return statusEnum == null ? null : statusEnum.getDesc();
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }
}

package com.wxchat.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wxchat.entity.constants.Constants;

import java.io.Serializable;

/**
 * @description 系统设置
 * @author JIU-W
 * @date 2025-01-26
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDto implements Serializable {

    //最多可创建的群聊数量
    private Integer maxGroupCount = 5;

    //群聊最多人数
    private Integer maxGroupMemberCount = 500;

    //允许上传的图片的最大大小，单位MB
    private Integer maxImageSize = 2;

    //允许上传的视频的最大大小，单位MB
    private Integer maxVideoSize = 5;

    //其它文件上传的最大大小，单位MB
    private Integer maxFileSize = 5;

    //机器人的ID
    private String robotUid = Constants.ROBOT_UID;

    //机器人的昵称
    private String robotNickName = "EasyChat";

    //机器人的欢迎语
    private String robotWelcome = "欢迎使用EasyChat";

    public Integer getMaxGroupCount() {
        return maxGroupCount;
    }

    public void setMaxGroupCount(Integer maxGroupCount) {
        this.maxGroupCount = maxGroupCount;
    }

    public Integer getMaxGroupMemberCount() {
        return maxGroupMemberCount;
    }

    public void setMaxGroupMemberCount(Integer maxGroupMemberCount) {
        this.maxGroupMemberCount = maxGroupMemberCount;
    }

    public Integer getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(Integer maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public Integer getMaxVideoSize() {
        return maxVideoSize;
    }

    public void setMaxVideoSize(Integer maxVideoSize) {
        this.maxVideoSize = maxVideoSize;
    }

    public String getRobotNickName() {
        return robotNickName;
    }

    public void setRobotNickName(String robotNickName) {
        this.robotNickName = robotNickName;
    }

    public String getRobotWelcome() {
        return robotWelcome;
    }

    public void setRobotWelcome(String robotWelcome) {
        this.robotWelcome = robotWelcome;
    }

    public Integer getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Integer maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getRobotUid() {
        return robotUid;
    }
}

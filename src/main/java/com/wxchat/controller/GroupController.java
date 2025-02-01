package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.entity.enums.GroupStatusEnum;
import com.wxchat.entity.enums.MessageTypeEnum;
import com.wxchat.entity.enums.UserContactStatusEnum;
import com.wxchat.entity.po.GroupInfo;
import com.wxchat.entity.po.UserContact;
import com.wxchat.entity.query.GroupInfoQuery;
import com.wxchat.entity.query.UserContactQuery;
import com.wxchat.entity.vo.GroupInfoVO;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.service.GroupInfoService;
import com.wxchat.service.UserContactService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @description 群组管理
 * @author JIU-W
 * @date 2025-01-30
 * @version 1.0
 */
@RestController("groupController")
@RequestMapping("/group")
public class GroupController extends ABaseController {

    @Resource
    private GroupInfoService groupInfoService;

    @Resource
    private UserContactService userContactService;

    /**
     * 新增群组，修改群组信息
     * @param request
     * @param groupId
     * @param groupName
     * @param groupNotice
     * @param joinType
     * @param avatarFile 头像图片
     * @param avatarCover 客户端electron使用ffmpeg根据avatarFile生成的它对应的缩略图。
     * @return
     */
    @RequestMapping(value = "/saveGroup")
    @GlobalInterceptor
    public ResponseVO saveGroup(HttpServletRequest request, String groupId,
                                @NotEmpty String groupName, String groupNotice,
                                @NotNull Integer joinType,
                                MultipartFile avatarFile, MultipartFile avatarCover) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(groupId);
        groupInfo.setGroupOwnerId(tokenUserInfoDto.getUserId());
        groupInfo.setGroupName(groupName);
        groupInfo.setGroupNotice(groupNotice);
        groupInfo.setJoinType(joinType);
        this.groupInfoService.saveGroup(groupInfo, avatarFile, avatarCover);
        return getSuccessResponseVO(null);
    }

    /**
     * 加载我的群组
     * @param request
     * @return
     */
    @RequestMapping(value = "/loadMyGroup")
    @GlobalInterceptor
    public ResponseVO loadMyGroup(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        GroupInfoQuery infoQuery = new GroupInfoQuery();
        infoQuery.setGroupOwnerId(tokenUserInfoDto.getUserId());
        infoQuery.setOrderBy("create_time desc");
        List<GroupInfo> groupInfoList = this.groupInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(groupInfoList);
    }

    /**
     * 获取群信息
     * @param request
     * @param groupId
     * @return
     */
    @RequestMapping(value = "/getGroupInfo")
    //@GlobalInterceptor
    public ResponseVO getGroupInfo(HttpServletRequest request, @NotEmpty String groupId) {
        GroupInfo groupInfo = getGroupDetailCommon(request, groupId);
        //查询群成员数
        UserContactQuery userContactQuery = new UserContactQuery();
        userContactQuery.setContactId(groupId);
        Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
        groupInfo.setMemberCount(memberCount);
        return getSuccessResponseVO(groupInfo);
    }

    private GroupInfo getGroupDetailCommon(HttpServletRequest request, String groupId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), groupId);
        if (userContact == null || !UserContactStatusEnum.FRIEND.getStatus().equals(userContact.getStatus())) {
            throw new BusinessException("你不在群聊或者群聊不存在或已经解散");
        }
        GroupInfo groupInfo = this.groupInfoService.getGroupInfoByGroupId(groupId);
        if (groupInfo == null || !GroupStatusEnum.NORMAL.getStatus().equals(groupInfo.getStatus())) {
            throw new BusinessException("群聊不存在或已经解散");
        }
        return groupInfo;
    }

    @RequestMapping(value = "/getGroupInfo4Chat")
    //@GlobalInterceptor
    public ResponseVO getGroupInfo4Chat(HttpServletRequest request, @NotEmpty String groupId) {
        GroupInfo groupInfo = getGroupDetailCommon(request, groupId);
        UserContactQuery userContactQuery = new UserContactQuery();
        userContactQuery.setContactId(groupId);
        userContactQuery.setQueryUserInfo(true);
        userContactQuery.setOrderBy("create_time asc");
        userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        List<UserContact> userContactList = this.userContactService.findListByParam(userContactQuery);
        GroupInfoVO groupInfoVo = new GroupInfoVO();
        groupInfoVo.setGroupInfo(groupInfo);
        groupInfoVo.setUserContactList(userContactList);
        return getSuccessResponseVO(groupInfoVo);
    }


    /**
     * 退群
     *
     * @param request
     * @param groupId
     * @return
     */
    @RequestMapping(value = "/leaveGroup")
    //@GlobalInterceptor
    public ResponseVO leaveGroup(HttpServletRequest request, @NotEmpty String groupId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        groupInfoService.leaveGroup(tokenUserInfoDto.getUserId(), groupId, MessageTypeEnum.LEAVE_GROUP);
        return getSuccessResponseVO(null);
    }

    /**
     * 解散群
     *
     * @param request
     * @param groupId
     * @return
     */
    @RequestMapping(value = "/dissolutionGroup")
    //@GlobalInterceptor
    public ResponseVO dissolutionGroup(HttpServletRequest request, @NotEmpty String groupId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        groupInfoService.dissolutionGroup(tokenUserInfoDto.getUserId(), groupId);
        return getSuccessResponseVO(null);
    }


    /**
     * 添加或者移除人员
     *
     * @param request
     * @param groupId
     * @param selectContacts
     * @param opType
     * @return
     */
    @RequestMapping(value = "/addOrRemoveGroupUser")
    //@GlobalInterceptor
    public ResponseVO addOrRemoveGroupUser(HttpServletRequest request, @NotEmpty String groupId, @NotEmpty String selectContacts,
                                           @NotNull Integer opType) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        groupInfoService.addOrRemoveGroupUser(tokenUserInfoDto, groupId, selectContacts, opType);
        return getSuccessResponseVO(null);
    }

}

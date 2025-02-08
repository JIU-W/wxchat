package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.enums.ResponseCodeEnum;
import com.wxchat.entity.po.GroupInfo;
import com.wxchat.entity.query.GroupInfoQuery;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.service.GroupInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

@RestController("adminGroupController")
@RequestMapping("/admin")
public class AdminGroupController extends ABaseController {

    @Resource
    private GroupInfoService groupInfoService;

    /**
     * 加载群组列表
     * @param groupInfoQuery
     * @return
     */
    @RequestMapping("/loadGroup")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadGroup(GroupInfoQuery groupInfoQuery) {
        //设置要查询群主名称
        groupInfoQuery.setQueryGroupOwnerName(true);
        //设置要查询群组成员数量
        groupInfoQuery.setQueryMemberCount(true);
        PaginationResultVO resultVO = groupInfoService.findListByPage(groupInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 解散群组
     * @param groupId
     * @return
     */
    @RequestMapping("/dissolutionGroup")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO dissolutionGroup(@NotEmpty String groupId) {
        //查询群组信息
        GroupInfo groupInfo = groupInfoService.getGroupInfoByGroupId(groupId);
        if (null == groupInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //解散群组
        groupInfoService.dissolutionGroup(groupInfo.getGroupOwnerId(), groupId);
        return getSuccessResponseVO(null);
    }

}

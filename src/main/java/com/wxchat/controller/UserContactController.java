package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.entity.dto.UserContactSearchResultDto;
import com.wxchat.entity.enums.PageSize;
import com.wxchat.entity.enums.ResponseCodeEnum;
import com.wxchat.entity.enums.UserContactStatusEnum;
import com.wxchat.entity.enums.UserContactTypeEnum;
import com.wxchat.entity.po.UserContact;
import com.wxchat.entity.po.UserInfo;
import com.wxchat.entity.query.UserContactApplyQuery;
import com.wxchat.entity.query.UserContactQuery;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.entity.vo.UserInfoVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.redis.RedisUtils;
import com.wxchat.service.GroupInfoService;
import com.wxchat.service.UserContactApplyService;
import com.wxchat.service.UserContactService;
import com.wxchat.service.UserInfoService;
import com.wxchat.utils.CopyTools;
import jodd.util.ArraysUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/contact")
public class UserContactController extends ABaseController {
    private static final Logger logger = LoggerFactory.getLogger(UserContactController.class);

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private GroupInfoService groupInfoService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserContactService userContactService;

    @Resource
    private UserContactApplyService userContactApplyService;

    /**
     * 搜索联系人：根据contactId精确搜索"用户"或者"群组"
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/search")
    @GlobalInterceptor
    public ResponseVO search(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        //精确搜索联系人信息(好友或者群组)
        //这里搜索出来的联系人不一定就是好友，也有可能是非好友，也有可能被好友删除等等，要根据status去具体判断。
        UserContactSearchResultDto resultDto =
                userContactService.searchContact(tokenUserInfoDto.getUserId(), contactId);
        return getSuccessResponseVO(resultDto);
    }

    /**
     * 申请添加联系人(申请添加好友 或者 申请加入群组)
     * @param request
     * @param contactId 用户id或者群组id
     * @param contactType 联系人类型(好友 USER 或者群组 GROUP)
     * @param applyInfo 申请信息
     * @return
     */
    @RequestMapping("/applyAdd")
    @GlobalInterceptor
    public ResponseVO applyAdd(HttpServletRequest request, @NotEmpty String contactId,
                               @NotEmpty String contactType, String applyInfo) {
        //获取当前登录用户信息
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        //申请添加联系人
        Integer joinType = userContactApplyService.applyAdd(tokenUserInfoDto, contactId, contactType, applyInfo);
        return getSuccessResponseVO(joinType);
    }

    /**
     * 加载申请列表
     * @param request
     * @param pageNo
     * @return
     */
    @RequestMapping("/loadApply")
    @GlobalInterceptor
    public ResponseVO loadApply(HttpServletRequest request, Integer pageNo) {
        //获取当前登录用户信息
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactApplyQuery userContactApplyQuery = new UserContactApplyQuery();
        userContactApplyQuery.setOrderBy("last_apply_time desc");
        userContactApplyQuery.setReceiveUserId(tokenUserInfoDto.getUserId());
        //设置要去关联查询申请联系人的具体信息
        userContactApplyQuery.setQueryContactInfo(true);
        userContactApplyQuery.setPageNo(pageNo);
        userContactApplyQuery.setPageSize(PageSize.SIZE15.getSize());
        PaginationResultVO resultVO = userContactApplyService.findListByPage(userContactApplyQuery);
        return getSuccessResponseVO(resultVO);
    }




}

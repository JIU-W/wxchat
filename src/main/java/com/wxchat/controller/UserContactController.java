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
        //这里搜索出来的联系人不一定就是好友，也有可能是非好友，也有可能被好友删除，也有可能被拉黑了等等，
        //要根据status去具体判断。
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

    /**
     * 处理申请
     * @param request
     * @param applyId
     * @param status
     * @return
     */
    @RequestMapping("/dealWithApply")
    @GlobalInterceptor
    public ResponseVO dealWithApply(HttpServletRequest request, @NotNull Integer applyId, @NotNull Integer status) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        //处理申请
        userContactApplyService.dealWithApply(tokenUserInfoDto.getUserId(), applyId, status);
        return getSuccessResponseVO(null);
    }

    /**
     * 1.加载我加入的群组列表(不包括我创建的群组)、2.加载我的好友列表
     * @param request
     * @param contactType
     * @return
     */
    @RequestMapping("/loadContact")
    @GlobalInterceptor
    public ResponseVO loadContact(HttpServletRequest request, @NotEmpty String contactType) {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByName(contactType);
        if (null == contactTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //获取当前登录用户信息
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactQuery contactQuery = new UserContactQuery();
        contactQuery.setUserId(tokenUserInfoDto.getUserId());
        contactQuery.setContactType(contactTypeEnum.getType());
        if (UserContactTypeEnum.USER == contactTypeEnum) {//查询"好友"的额外查询条件
            //设置需要关联查询用户的信息(昵称、性别)
            contactQuery.setQueryContactUserInfo(true);
        } else if (UserContactTypeEnum.GROUP == contactTypeEnum) {//查询"群组"的额外查询条件
            //设置需要关联查询群组的信息(群组名称)
            contactQuery.setQueryGroupInfo(true);
            //设置查询条件，排除"我"创建的群组
            contactQuery.setExcludeMyGroup(true);
        }
        //设置查询条件，排除"被"删除或者"被"拉黑的联系人
        //注：这里的"首次被对方拉黑"状态的情况不能被查出来，因为"首次被对方拉黑"是在你初次申请的时候别人还没有同意申请就把你拉黑了，
        //你还没有进入对方的好友列表，所以不能被查出来。
        contactQuery.setStatusArray(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE.getStatus()});
        contactQuery.setOrderBy("last_update_time desc");
        //查询出我的联系人列表
        List<UserContact> contactList = userContactService.findListByParam(contactQuery);
        return getSuccessResponseVO(contactList);
    }

    /**
     * 获取联系人信息 1.从自己的 好友列表、群组列表 查看 2.从群组的成员列表查看
     * 因为有第二种情况，所以查出来的结果可能不是好友，也可能是好友。
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/getContactInfo")
    @GlobalInterceptor
    public ResponseVO getContactInfo(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        //封装userInfoVO
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        //设置默认值：不是好友
        userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus());
        //判断是否是好友
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), contactId);
        if (userContact != null) {//是好友
            userInfoVO.setContactStatus(userContact.getStatus());
        }
        return getSuccessResponseVO(userInfoVO);
    }

    /**
     * 获取
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/getContactUserInfo")
    @GlobalInterceptor
    public ResponseVO getContactUserInfo(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), contactId);
        if (null == userContact || !ArraysUtil.contains(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus()}, userContact.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        return getSuccessResponseVO(userInfoVO);
    }


}

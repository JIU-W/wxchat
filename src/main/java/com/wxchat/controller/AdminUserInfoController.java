package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.query.UserInfoQuery;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController("adminUserInfoController")
@RequestMapping("/admin")
public class AdminUserInfoController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    /**
     * 加载用户列表
     * @param userInfoQuery
     * @return
     */
    @RequestMapping("/loadUser")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("create_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 更新用户状态
     * @param status
     * @param userId
     * @return
     */
    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO updateUserStatus(@NotNull Integer status,
                                       @NotEmpty String userId) {
        userInfoService.updateUserStatus(status, userId);
        return getSuccessResponseVO(null);
    }

    /**
     * 强制下线
     * @param userId
     * @return
     */
    @RequestMapping("/forceOffLine")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO forceOffLine(@NotEmpty String userId) {
        userInfoService.forceOffLine(userId);
        return getSuccessResponseVO(null);
    }

}

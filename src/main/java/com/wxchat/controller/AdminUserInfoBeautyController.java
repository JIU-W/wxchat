package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.po.UserInfoBeauty;
import com.wxchat.entity.query.UserInfoBeautyQuery;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.service.UserInfoBeautyService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 靓号 Controller
 */
@RestController("userInfoBeautyController")
@RequestMapping("/admin")
@Validated
public class AdminUserInfoBeautyController extends ABaseController {

    @Resource
    private UserInfoBeautyService userInfoBeautyService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadBeautyAccountList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadBeautyAccountList(UserInfoBeautyQuery query) {
        return getSuccessResponseVO(userInfoBeautyService.findListByPage(query));
    }

    /**
     * 新增、修改靓号
     * @param beauty
     * @return
     */
    @RequestMapping("/saveBeautAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveBeautAccount(UserInfoBeauty beauty) {
        //保存靓号
        userInfoBeautyService.saveAccount(beauty);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除靓号
     * @param id
     * @return
     */
    @RequestMapping("/delBeautAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO delBeautAccount(@NotNull Integer id) {
        return getSuccessResponseVO(userInfoBeautyService.deleteUserInfoBeautyById(id));
    }


}

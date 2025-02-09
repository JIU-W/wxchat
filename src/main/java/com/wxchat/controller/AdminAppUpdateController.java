package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.po.AppUpdate;
import com.wxchat.entity.query.AppUpdateQuery;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.service.AppUpdateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * app发布
 */
@RestController("adminAppUpdateController")
@RequestMapping("/admin")
@Validated
public class AdminAppUpdateController extends ABaseController {

    @Resource
    private AppUpdateService appUpdateService;

    /**
     * 根据条件分页查询"APP版本更新"
     */
    @RequestMapping("/loadUpdateList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadUpdateList(AppUpdateQuery query) {
        query.setOrderBy("id desc");
        return getSuccessResponseVO(appUpdateService.findListByPage(query));
    }

    /**
     * 保存"APP版本更新"信息(新增或者修改)
     */
    @RequestMapping("/saveUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveUpdate(Integer id, @NotEmpty String version,
                                 @NotEmpty String updateDesc, @NotNull Integer fileType,
                                 String outerLink, MultipartFile file) throws IOException {
        AppUpdate appUpdate = new AppUpdate();
        appUpdate.setId(id);
        appUpdate.setVersion(version);
        appUpdate.setUpdateDesc(updateDesc);
        appUpdate.setFileType(fileType);
        appUpdate.setOuterLink(outerLink);
        //保存"APP版本更新"信息(新增或者修改)
        appUpdateService.saveUpdate(appUpdate, file);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除"APP版本更新"
     */
    @RequestMapping("/delUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO delUpdate(@NotNull Integer id) {
        appUpdateService.deleteAppUpdateById(id);
        return getSuccessResponseVO(null);
    }

    /**
     * 发布"APP版本更新"
     * @param id
     * @param status
     * @param grayscaleUid
     * @return
     */
    @RequestMapping("/postUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO postUpdate(@NotNull Integer id, @NotNull Integer status,
                                 String grayscaleUid) {
        //发布"app版本"
        appUpdateService.postUpdate(id, status, grayscaleUid);
        return getSuccessResponseVO(null);
    }

}

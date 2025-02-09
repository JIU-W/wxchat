package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.config.AppConfig;
import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.entity.po.AppUpdate;
import com.wxchat.entity.vo.AppUpdateVO;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.service.AppUpdateService;
import com.wxchat.utils.CopyTools;
import com.wxchat.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

@RestController("updateController")
@RequestMapping("/update")
@Validated
public class UpdateController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(UpdateController.class);

    @Resource
    private AppConfig appConfig;

    @Resource
    private AppUpdateService appUpdateService;

    /**
     * 版本检测更新
     * @param appVersion 用户的版本号
     * @param uid 用户的uid
     * @return
     */
    @RequestMapping("/checkVersion")
    @GlobalInterceptor
    public ResponseVO loadAllCategory(HttpServletRequest request, String appVersion, String uid) {
        if (StringTools.isEmpty(appVersion)) {
            return getSuccessResponseVO(null);
        }
        //TODO 修复bug
        //bug内容：在用户刚打开客户端时，会刷新调用这个接口，但是这个时候前端不会传递uid过来，所以这里需要做一下填补。
        if("".equals(uid)){
            TokenUserInfoDto tokenUserInfo = getTokenUserInfo(request);
            uid = tokenUserInfo.getUserId();
        }

        //查询相对于用户现在的版本来说是否有新版本，并查询出最新版本
        AppUpdate appUpdate = appUpdateService.getLatestUpdate(appVersion, uid);
        if (appUpdate == null) {
            return getSuccessResponseVO(null);
        }
        //封装成前端需要的数据
        AppUpdateVO updateVO = CopyTools.copy(appUpdate, AppUpdateVO.class);

        File file = new File(appConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER
                + appUpdate.getId() + Constants.APP_EXE_SUFFIX);
        updateVO.setSize(file.length());
        updateVO.setUpdateList(Arrays.asList(appUpdate.getUpdateDescArray()));
        String fileName = Constants.APP_NAME + appUpdate.getVersion() + Constants.APP_EXE_SUFFIX;
        updateVO.setFileName(fileName);
        return getSuccessResponseVO(updateVO);
    }

    /*@RequestMapping("/download")
    @GlobalInterceptor
    public void download(HttpServletResponse response, @NotNull Integer id) {
        OutputStream out = null;
        FileInputStream in = null;
        try {
            AppUpdate appUpdate = appUpdateService.getAppUpdateById(id);
            File file = new File(appConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER + appUpdate.getId() + Constants.APP_EXE_SUFFIX);
            if (!file.exists()) {
                return;
            }
            response.setContentType("application/x-msdownload; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;");
            response.setContentLengthLong(file.length());
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("读取文件异常", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
        }
    }*/

}

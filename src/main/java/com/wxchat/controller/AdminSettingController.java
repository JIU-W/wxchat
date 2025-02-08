package com.wxchat.controller;

import com.wxchat.annotation.GlobalInterceptor;
import com.wxchat.entity.config.AppConfig;
import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.SysSettingDto;
import com.wxchat.entity.vo.ResponseVO;
import com.wxchat.redis.RedisComponet;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@RestController("adminSettingController")
@RequestMapping("/admin")
public class AdminSettingController extends ABaseController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponet redisComponet;

    /**
     * 保存系统设置
     * @param sysSettingDto
     * @param robotFile
     * @param robotCover
     * @return
     * @throws IOException
     */
    @RequestMapping("/saveSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveSysSetting(SysSettingDto sysSettingDto,
                                     MultipartFile robotFile,
                                     MultipartFile robotCover) throws IOException {
        if (robotFile != null) {
            String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
            if (!targetFileFolder.exists()) {
                targetFileFolder.mkdirs();
            }
            String filePath = targetFileFolder.getPath() + "/" + Constants.ROBOT_UID + Constants.IMAGE_SUFFIX;
            robotFile.transferTo(new File(filePath));
            robotCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
        }
        redisComponet.saveSysSetting(sysSettingDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取系统设置
     * @return
     */
    @RequestMapping("/getSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO getSysSetting() {
        SysSettingDto sysSettingDto = redisComponet.getSysSetting();
        return getSuccessResponseVO(sysSettingDto);
    }

}

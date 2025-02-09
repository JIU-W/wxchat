package com.wxchat.service.impl;

import com.wxchat.entity.config.AppConfig;
import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.enums.AppUpdateFileTypeEnum;
import com.wxchat.entity.enums.AppUpdateSatusEnum;
import com.wxchat.entity.enums.PageSize;
import com.wxchat.entity.enums.ResponseCodeEnum;
import com.wxchat.entity.po.AppUpdate;
import com.wxchat.entity.query.AppUpdateQuery;
import com.wxchat.entity.query.SimplePage;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.mappers.AppUpdateMapper;
import com.wxchat.service.AppUpdateService;
import com.wxchat.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * app发布 业务接口实现
 */
@Service("appUpdateService")
public class AppUpdateServiceImpl implements AppUpdateService {


    @Resource
    private AppConfig appConfig;

    @Resource
    private AppUpdateMapper<AppUpdate, AppUpdateQuery> appUpdateMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<AppUpdate> findListByParam(AppUpdateQuery param) {
        return this.appUpdateMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(AppUpdateQuery param) {
        return this.appUpdateMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<AppUpdate> findListByPage(AppUpdateQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<AppUpdate> list = this.findListByParam(param);
        PaginationResultVO<AppUpdate> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(AppUpdate bean) {
        return this.appUpdateMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<AppUpdate> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.appUpdateMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<AppUpdate> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.appUpdateMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(AppUpdate bean, AppUpdateQuery param) {
        StringTools.checkParam(param);
        return this.appUpdateMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(AppUpdateQuery param) {
        StringTools.checkParam(param);
        return this.appUpdateMapper.deleteByParam(param);
    }

    /**
     * 根据Id获取对象
     */
    @Override
    public AppUpdate getAppUpdateById(Integer id) {
        return this.appUpdateMapper.selectById(id);
    }

    /**
     * 根据Id修改
     */
    @Override
    public Integer updateAppUpdateById(AppUpdate bean, Integer id) {
        return this.appUpdateMapper.updateById(bean, id);
    }

    /**
     * 根据Id删除
     */

    public Integer deleteAppUpdateById(Integer id) {
        AppUpdate dbInfo = this.getAppUpdateById(id);
        if (!AppUpdateSatusEnum.INIT.getStatus().equals(dbInfo.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        return this.appUpdateMapper.deleteById(id);
    }


    @Transactional(rollbackFor = Exception.class)
    public void saveUpdate(AppUpdate appUpdate, MultipartFile file) throws IOException {
        AppUpdateFileTypeEnum fileTypeEnum = AppUpdateFileTypeEnum.getByType(appUpdate.getFileType());
        if (null == fileTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //发布了的版本就不能再进行信息修改了
        if (appUpdate.getId() != null) {//修改的情况
            AppUpdate dbInfo = this.getAppUpdateById(appUpdate.getId());
            if (!AppUpdateSatusEnum.INIT.getStatus().equals(dbInfo.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        AppUpdateQuery updateQuery = new AppUpdateQuery();
        updateQuery.setOrderBy("id desc");//根据id降序排列
        updateQuery.setSimplePage(new SimplePage(0, 1));//只查一条数据
        //查询最新版本
        List<AppUpdate> appUpdateList = appUpdateMapper.selectList(updateQuery);
        if (!appUpdateList.isEmpty()) {
            //最新版本
            AppUpdate lastest = appUpdateList.get(0);
            //最新版本号
            Long dbVersion = Long.parseLong(lastest.getVersion().replace(".", ""));

            //currentVersion：前端传过来的版本号
            Long currentVersion = Long.parseLong(appUpdate.getVersion().replace(".", ""));

            //当前版本：数据库里最大版本号的版本
            //历史版本：版本号小于数据库里最大版本号的版本
            //新增的情况
            if (appUpdate.getId() == null && currentVersion <= dbVersion) {
                throw new BusinessException("当前版本必须大于历史版本");
            }
            //修改的情况
            if (appUpdate.getId() != null && currentVersion >= dbVersion
                    && !appUpdate.getId().equals(lastest.getId())) {
                throw new BusinessException("修改版本号时，历史版本必须小于当前版本");
            }

            //版本号不能重复
            AppUpdate versionDb = appUpdateMapper.selectByVersion(appUpdate.getVersion());
            if (appUpdate.getId() != null && versionDb != null && !versionDb.getId().equals(appUpdate.getId())) {
                throw new BusinessException("版本号已存在");
            }

        }

        if (appUpdate.getId() == null) {
            appUpdate.setCreateTime(new Date());
            appUpdate.setStatus(AppUpdateSatusEnum.INIT.getStatus());
            //新增"APP版本更新"
            appUpdateMapper.insert(appUpdate);
        } else {
            //修改"APP版本更新"
            appUpdateMapper.updateById(appUpdate, appUpdate.getId());
        }
        //文件上传
        if (file != null) {
            File folder = new File(appConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file.transferTo(new File(folder.getAbsolutePath() + "/" + appUpdate.getId() + Constants.APP_EXE_SUFFIX));
        }

    }


    public void postUpdate(Integer id, Integer status, String grayscaleUid) {
        AppUpdateSatusEnum satusEnum = AppUpdateSatusEnum.getByStatus(status);
        if (status == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (AppUpdateSatusEnum.GRAYSCALE == satusEnum && StringTools.isEmpty(grayscaleUid)) {
            //灰度发布时，必须指定灰度用户
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (AppUpdateSatusEnum.GRAYSCALE != satusEnum) {
            //不是灰度发布时，灰度用户必须置空
            grayscaleUid = "";
        }
        AppUpdate update = new AppUpdate();
        update.setStatus(status);
        update.setGrayscaleUid(grayscaleUid);
        appUpdateMapper.updateById(update, id);
    }

    @Override
    public AppUpdate getLatestUpdate(String appVersion, String uid) {
        return appUpdateMapper.selectLatestUpdate(appVersion, uid);
    }

}

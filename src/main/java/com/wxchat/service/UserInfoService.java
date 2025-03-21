package com.wxchat.service;

import com.wxchat.entity.po.UserInfo;
import com.wxchat.entity.query.UserInfoQuery;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.entity.vo.UserInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * 业务接口
 */
public interface UserInfoService {

    /**
     * 根据条件查询列表
     */
    List<UserInfo> findListByParam(UserInfoQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(UserInfoQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param);

    /**
     * 新增
     */
    Integer add(UserInfo bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<UserInfo> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<UserInfo> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(UserInfo bean, UserInfoQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(UserInfoQuery param);

    /**
     * 根据UserId查询对象
     */
    UserInfo getUserInfoByUserId(String userId);


    /**
     * 根据UserId修改
     */
    Integer updateUserInfoByUserId(UserInfo bean, String userId);


    /**
     * 根据UserId删除
     */
    Integer deleteUserInfoByUserId(String userId);


    /**
     * 根据Email查询对象
     */
    UserInfo getUserInfoByEmail(String email);


    /**
     * 根据Email修改
     */
    Integer updateUserInfoByEmail(UserInfo bean, String email);


    /**
     * 根据Email删除
     */
    Integer deleteUserInfoByEmail(String email);

    /**
     * 注册
     */
    void register(String email, String nickName, String password);

    /**
     * 登录
     */
    UserInfoVO login(String email, String password);

    /**
     * 修改用户信息
     */
    void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;

    /**
     * 修改用户状态
     */
    void updateUserStatus(Integer status, String userId);

    /**
     * 强制下线
     */
    void forceOffLine(String userId);

}

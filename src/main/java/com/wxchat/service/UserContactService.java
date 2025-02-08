package com.wxchat.service;


import com.wxchat.entity.dto.UserContactSearchResultDto;
import com.wxchat.entity.enums.UserContactStatusEnum;
import com.wxchat.entity.po.UserContact;
import com.wxchat.entity.query.UserContactQuery;
import com.wxchat.entity.vo.PaginationResultVO;

import java.util.List;


/**
 * 联系人 业务接口
 */
public interface UserContactService {

    /**
     * 根据条件查询列表
     */
    List<UserContact> findListByParam(UserContactQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(UserContactQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<UserContact> findListByPage(UserContactQuery param);

    /**
     * 新增
     */
    Integer add(UserContact bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<UserContact> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<UserContact> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(UserContact bean, UserContactQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(UserContactQuery param);

    /**
     * 根据UserIdAndContactId查询对象
     */
    UserContact getUserContactByUserIdAndContactId(String userId, String contactId);


    /**
     * 根据UserIdAndContactId修改
     */
    Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId, String contactId);


    /**
     * 根据UserIdAndContactId删除
     */
    Integer deleteUserContactByUserIdAndContactId(String userId, String contactId);

    /**
     * 精确搜索联系人信息(好友或者群组)
     * @param userId
     * @param contactId
     * @return
     */
    UserContactSearchResultDto searchContact(String userId, String contactId);

    /**
     * 添加联系人：添加好友或者群组
     */
    void addContact(String applyUserId, String receiveUserId, String contactId, Integer contactType, String applyInfo);

    /**
     * 删除，拉黑用户
     * @param userId
     * @param contactId
     * @param statusEnum
     */
    void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum);

    void removeGroupContact(String userId, String groupId, String contactId, UserContactStatusEnum statusEnum);

    void addContact4Robot(String userId);
}

package com.wxchat.service.impl;

import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.MessageSendDto;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.entity.enums.*;
import com.wxchat.entity.po.GroupInfo;
import com.wxchat.entity.po.UserContact;
import com.wxchat.entity.po.UserContactApply;
import com.wxchat.entity.po.UserInfo;
import com.wxchat.entity.query.*;
import com.wxchat.entity.vo.PaginationResultVO;
import com.wxchat.exception.BusinessException;
import com.wxchat.mappers.GroupInfoMapper;
import com.wxchat.mappers.UserContactApplyMapper;
import com.wxchat.mappers.UserContactMapper;
import com.wxchat.mappers.UserInfoMapper;
import com.wxchat.service.UserContactApplyService;
import com.wxchat.service.UserContactService;
import com.wxchat.utils.StringTools;
import jodd.util.ArraysUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


/**
 * 联系人申请 业务接口实现
 */
@Service("userContactApplyService")
public class UserContactApplyServiceImpl implements UserContactApplyService {

    @Resource
    private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

    @Resource
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    //@Resource
    //private MessageHandler messageHandler;

    @Resource
    private UserContactService userContactService;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserContactApply> findListByParam(UserContactApplyQuery param) {
        return this.userContactApplyMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserContactApplyQuery param) {
        return this.userContactApplyMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserContactApply> list = this.findListByParam(param);
        PaginationResultVO<UserContactApply> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserContactApply bean) {
        return this.userContactApplyMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserContactApply> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactApplyMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserContactApply> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactApplyMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(UserContactApply bean, UserContactApplyQuery param) {
        StringTools.checkParam(param);
        return this.userContactApplyMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(UserContactApplyQuery param) {
        StringTools.checkParam(param);
        return this.userContactApplyMapper.deleteByParam(param);
    }

    /**
     * 根据ApplyId获取对象
     */
    @Override
    public UserContactApply getUserContactApplyByApplyId(Integer applyId) {
        return this.userContactApplyMapper.selectByApplyId(applyId);
    }

    /**
     * 根据ApplyId修改
     */
    @Override
    public Integer updateUserContactApplyByApplyId(UserContactApply bean, Integer applyId) {
        return this.userContactApplyMapper.updateByApplyId(bean, applyId);
    }

    /**
     * 根据ApplyId删除
     */
    @Override
    public Integer deleteUserContactApplyByApplyId(Integer applyId) {
        return this.userContactApplyMapper.deleteByApplyId(applyId);
    }

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId获取对象
     */
    @Override
    public UserContactApply getUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
        return this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
    }

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId修改
     */
    @Override
    public Integer updateUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(UserContactApply bean, String applyUserId, String receiveUserId, String contactId) {
        return this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserIdAndContactId(bean, applyUserId, receiveUserId, contactId);
    }

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId删除
     */
    @Override
    public Integer deleteUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
        return this.userContactApplyMapper.deleteByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
    }



    @Transactional(rollbackFor = Exception.class)
    public Integer applyAdd(TokenUserInfoDto tokenUserInfoDto, String contactId, String contactType, String applyInfo) {
        UserContactTypeEnum typeEnum = UserContactTypeEnum.getByName(contactType);
        if (null == typeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        //申请人
        String applyUserId = tokenUserInfoDto.getUserId();
        //设置默认申请信息，如果前端传的申请信息为空则使用默认的信息。
        applyInfo = StringTools.isEmpty(applyInfo) ? String.format(Constants.APPLY_INFO_TEMPLATE, tokenUserInfoDto.getNickName()) : applyInfo;

        Long curDate = System.currentTimeMillis();
        Integer joinType = null;
        String receiveUserId = contactId;

        //查询对方好友是否已经添加，如果已经拉黑无法添加
        UserContact userContact = userContactMapper.selectByUserIdAndContactId(applyUserId, contactId);
        if (userContact != null &&
                ArraysUtil.contains(new Integer[]{
                        UserContactStatusEnum.BLACKLIST_BE.getStatus(), UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus()},
                        userContact.getStatus())) {
            throw new BusinessException("对方已经把你拉黑，无法添加");
        }

        if (UserContactTypeEnum.GROUP == typeEnum) {//申请加入群组
            GroupInfo groupInfo = groupInfoMapper.selectByGroupId(contactId);
            if (groupInfo == null || GroupStatusEnum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())) {
                throw new BusinessException("群聊不存在或已解散");
            }
            //设置"消息接收人"为"群主"
            receiveUserId = groupInfo.getGroupOwnerId();
            joinType = groupInfo.getJoinType();
        } else {//申请添加好友
            UserInfo userInfo = userInfoMapper.selectByUserId(contactId);
            if (userInfo == null) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            joinType = userInfo.getJoinType();
        }

        //加入方式为"直接加入"：不用记录申请记录
        if (JoinTypeEnum.JOIN.getType().equals(joinType)) {
            //添加联系人
            this.userContactService.addContact(applyUserId, receiveUserId, contactId, typeEnum.getType(), applyInfo);
            return joinType;
        }

        UserContactApply dbApply = this.userContactApplyMapper
                .selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
        if (dbApply == null) {//第一次申请，新增申请记录
            UserContactApply contactApply = new UserContactApply();
            contactApply.setApplyUserId(applyUserId);
            contactApply.setContactType(typeEnum.getType());
            contactApply.setReceiveUserId(receiveUserId);
            contactApply.setLastApplyTime(curDate);
            contactApply.setContactId(contactId);
            contactApply.setStatus(UserContactApplyStatusEnum.INIT.ordinal());//设置为待处理
            contactApply.setApplyInfo(applyInfo);
            this.userContactApplyMapper.insert(contactApply);
        } else {//不是第一次申请，修改申请记录的部分字段
            UserContactApply contactApply = new UserContactApply();
            contactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());//更新状态
            contactApply.setLastApplyTime(curDate);
            contactApply.setApplyInfo(applyInfo);
            this.userContactApplyMapper.updateByApplyId(contactApply, dbApply.getApplyId());
        }
        //发送ws消息
        if (dbApply == null || !UserContactApplyStatusEnum.INIT.getStatus().equals(dbApply.getStatus())) {
            //如果是待处理状态就不发消息，避免重复发送

            //发送ws消息
            MessageSendDto messageSend = new MessageSendDto();
            messageSend.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());//"好友申请"类型
            messageSend.setMessageContent(applyInfo);
            messageSend.setContactId(receiveUserId);
            //
            //messageHandler.sendMessage(messageSend);
        }
        return joinType;
    }


    @Transactional(rollbackFor = Exception.class)
    public void dealWithApply(String userId, Integer applyId, Integer status) {
        UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
        if (null == statusEnum || UserContactApplyStatusEnum.INIT == statusEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        UserContactApply applyInfo = this.userContactApplyMapper.selectByApplyId(applyId);
        if (applyInfo == null || !userId.equals(applyInfo.getReceiveUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        //更新申请信息 只能由"待处理"更新为"其他状态"
        UserContactApply updateInfo = new UserContactApply();
        updateInfo.setStatus(statusEnum.getStatus());
        updateInfo.setLastApplyTime(System.currentTimeMillis());

        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setApplyId(applyId);
        applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());//设置查询条件，只更新"待处理"的申请记录
        //更新申请记录的状态信息
        Integer count = userContactApplyMapper.updateByParam(updateInfo, applyQuery);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        //处理申请为"同意"的情况
        if (UserContactApplyStatusEnum.PASS.getStatus().equals(status)) {
            //添加联系人
            userContactService.addContact(applyInfo.getApplyUserId(), applyInfo.getReceiveUserId(), applyInfo.getContactId(), applyInfo.getContactType(), applyInfo.getApplyInfo());
            return;
        }

        //处理申请为"拉黑"的情况
        if (UserContactApplyStatusEnum.BLACKLIST == statusEnum) {
            //拉黑 将接收人添加到申请人的联系人中，标记申请人被拉黑
            Date curDate = new Date();
            UserContact userContact = new UserContact();
            userContact.setUserId(applyInfo.getApplyUserId());
            userContact.setContactId(applyInfo.getContactId());
            userContact.setContactType(applyInfo.getContactType());
            userContact.setCreateTime(curDate);
            userContact.setStatus(UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus());
            userContact.setLastUpdateTime(curDate);
            userContactMapper.insertOrUpdate(userContact);
            return;
        }

    }

}

package com.wxchat.redis;

import com.wxchat.entity.constants.Constants;
import com.wxchat.entity.dto.SysSettingDto;
import com.wxchat.entity.dto.TokenUserInfoDto;
import com.wxchat.utils.StringTools;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class RedisComponet {
    @Resource
    private RedisUtils redisUtils;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 获取token信息
     *
     * @param token
     * @return
     */
    public TokenUserInfoDto getTokenUserInfoDto(String token) {
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
        return tokenUserInfoDto;
    }

    public TokenUserInfoDto getTokenUserInfoDtoByUserId(String userId) {
        String token = (String) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN_USERID + userId);
        return getTokenUserInfoDto(token);
    }

    /**
     * 保存token信息
     * @param tokenUserInfoDto
     */
    public void saveTokenUserInfoDto(TokenUserInfoDto tokenUserInfoDto) {
        //以token为key，保存tokenUserInfoDto到redis中
        redisUtils.setex(Constants.REDIS_KEY_WS_TOKEN + tokenUserInfoDto.getToken(), tokenUserInfoDto,
                Constants.REDIS_KEY_EXPIRES_DAY * 2);
        //以userId为key，保存token到redis中
        //因为登录之后在传输过程中很多情况都拿不到token，而拿得到userId，
        //我们就可以先根据userId获取token，再根据token获取tokenUserInfoDto(用户信息)
        redisUtils.setex(Constants.REDIS_KEY_WS_TOKEN_USERID + tokenUserInfoDto.getUserId(),
                tokenUserInfoDto.getToken(), Constants.REDIS_KEY_EXPIRES_DAY * 2);
    }

    /**
     * 清除token信息
     * @param userId
     */
    public void cleanUserTokenByUserId(String userId) {
        String token = (String) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN_USERID + userId);
        if (!StringTools.isEmpty(token)) {
            redisUtils.delete(Constants.REDIS_KEY_WS_TOKEN + token);
        }
    }


    //保存最后心跳时间，过期时间为6秒
    public void saveUserHeartBeat(String userId) {
        redisUtils.setex(Constants.REDIS_KEY_WS_USER_HEART_BEAT + userId, System.currentTimeMillis(),
                Constants.REDIS_KEY_EXPIRES_HEART_BEAT);
    }

    //删除用户心跳
    public void removeUserHeartBeat(String userId) {
        redisUtils.delete(Constants.REDIS_KEY_WS_USER_HEART_BEAT + userId);
    }


    //获取用户心跳
    public Long getUserHeartBeat(String userId) {
        return (Long) redisUtils.get(Constants.REDIS_KEY_WS_USER_HEART_BEAT + userId);
    }

    //获取用户联系人
    public List<String> getUserContactList(String userId) {
        return redisUtils.getQueueList(Constants.REDIS_KEY_USER_CONTACT + userId);
    }

    //添加用户联系人
    public void addUserContact(String userId, String contactId) {
        List<String> contactList = redisUtils.getQueueList(Constants.REDIS_KEY_USER_CONTACT + userId);
        if (!contactList.contains(contactId)) {
            redisUtils.lpush(Constants.REDIS_KEY_USER_CONTACT + userId, contactId, Constants.REDIS_KEY_TOKEN_EXPIRES);
        }
    }

    //清空用户联系人
    public void cleanUserContact(String userId) {
        redisUtils.delete(Constants.REDIS_KEY_USER_CONTACT + userId);
    }

    //删除用户联系人
    public void removeUserContact(String userId, String contactId) {
        redisUtils.remove(Constants.REDIS_KEY_USER_CONTACT + userId, contactId);
    }

    //批量添加用户联系人
    public void addUserContactBatch(String userId, List<String> contactIdList) {
        redisUtils.lpushAll(Constants.REDIS_KEY_USER_CONTACT + userId, contactIdList,
                Constants.REDIS_KEY_TOKEN_EXPIRES);
    }

    //获取用户session列表
    public List<String> getUserSessionList(String userId) {
        return redisUtils.getQueueList(Constants.REDIS_KEY_USER_SESSION + userId);
    }

    //添加用户Session
    public void addUserSession(String userId, String sessionId) {
        List<String> sessionList = redisUtils.getQueueList(Constants.REDIS_KEY_USER_SESSION + userId);
        if (!sessionList.contains(sessionId)) {
            redisUtils.lpush(Constants.REDIS_KEY_USER_SESSION + userId, sessionId, Constants.REDIS_KEY_TOKEN_EXPIRES);
        }
    }

    //清空用户Session
    public void cleanUserSession(String userId) {
        redisUtils.delete(Constants.REDIS_KEY_USER_SESSION + userId);
    }

    public void saveSysSetting(SysSettingDto sysSettingDto) {
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
    }

    /**
     * 获取系统设置(管理员需要查询系统设置)
     * @return
     */
    public SysSettingDto getSysSetting() {
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        sysSettingDto = sysSettingDto == null ? new SysSettingDto() : sysSettingDto;
        return sysSettingDto;
    }
}

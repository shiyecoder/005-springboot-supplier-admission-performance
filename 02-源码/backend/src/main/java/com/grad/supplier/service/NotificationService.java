package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grad.supplier.entity.SysMessage;
import com.grad.supplier.entity.SysUser;
import com.grad.supplier.mapper.SysMessageMapper;
import com.grad.supplier.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内消息：待审批、即将超时、已退回、待验证等事件进入消息中心，
 * 消息带业务编号与详情跳转地址。供应商侧的消息同时推给该企业所有联系人。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;

    /** 推给指定用户 */
    public void pushToUser(Long userId, String title, String content, String bizNo, String bizType, String url) {
        if (userId == null) {
            return;
        }
        SysMessage m = new SysMessage();
        m.setUserId(userId);
        m.setTitle(title);
        m.setContent(content);
        m.setBizNo(bizNo);
        m.setBizType(bizType);
        m.setUrl(url);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
    }

    /** 推给某供应商的全部联系人 */
    public void pushToSupplier(Long supplierId, String title, String content, String bizNo, String bizType, String url) {
        List<SysUser> users = userMapper.selectList(new QueryWrapper<SysUser>()
                .eq("supplier_id", supplierId).eq("status", 1));
        for (SysUser u : users) {
            pushToUser(u.getId(), title, content, bizNo, bizType, url);
        }
    }

    public List<SysMessage> mine() {
        Long userId = com.grad.supplier.common.UserContext.userId();
        return messageMapper.selectList(new QueryWrapper<SysMessage>()
                .eq("user_id", userId).orderByDesc("create_time").last("limit 50"));
    }

    public long unread() {
        Long userId = com.grad.supplier.common.UserContext.userId();
        Long c = messageMapper.selectCount(new QueryWrapper<SysMessage>()
                .eq("user_id", userId).eq("read_flag", 0));
        return c == null ? 0 : c;
    }

    public void readAll() {
        Long userId = com.grad.supplier.common.UserContext.userId();
        SysMessage m = new SysMessage();
        m.setReadFlag(1);
        messageMapper.update(m, new QueryWrapper<SysMessage>().eq("user_id", userId));
    }
}

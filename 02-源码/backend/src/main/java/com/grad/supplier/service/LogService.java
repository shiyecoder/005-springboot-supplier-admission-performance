package com.grad.supplier.service;

import com.grad.supplier.common.UserContext;
import com.grad.supplier.entity.SysLog;
import com.grad.supplier.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志统一入口。状态流转、审批、退回、人工修正等关键动作必须调用，
 * 事件一经写入不允许普通用户修改。
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final SysLogMapper logMapper;

    public void record(String module, String action, String bizNo, String detail) {
        SysLog log = new SysLog();
        log.setUserId(UserContext.userId());
        log.setUsername(UserContext.get() == null ? "system" : UserContext.get().username());
        log.setModule(module);
        log.setAction(action);
        log.setBizNo(bizNo);
        log.setDetail(detail);
        log.setIp(currentIp());
        log.setCostMs(0);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "127.0.0.1";
            }
            String ip = attrs.getRequest().getHeader("X-Forwarded-For");
            return ip == null ? attrs.getRequest().getRemoteAddr() : ip.split(",")[0];
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}

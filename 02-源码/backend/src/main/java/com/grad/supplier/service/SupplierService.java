package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.common.BizException;
import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.UserContext;
import com.grad.supplier.dto.SupplierDtos;
import com.grad.supplier.entity.*;
import com.grad.supplier.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 供应商主档服务。所有写操作都做服务端校验：
 * 统一信用代码唯一、版本号并发控制、过期证照限制类目变更、被单据引用时禁止删除。
 */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final SupplierCategoryMapper categoryMapper;
    private final SupplierCertificateMapper certMapper;
    private final AdmissionApplicationMapper applyMapper;
    private final PerformanceTaskMapper taskMapper;
    private final SysUserMapper userMapper;
    private final LogService logService;

    public PageResult<Supplier> page(long page, long size, String kw, Long categoryId, String status,
                                     String level, String riskLevel, String certStart, String certEnd) {
        Page<Supplier> p = new Page<>(page, size);
        Long forceSupplierId = UserContext.isSupplierSide() ? UserContext.supplierId() : null;
        IPage<Supplier> result = supplierMapper.selectSupplierPage(p, kw, categoryId, status, level,
                riskLevel, certStart, certEnd, forceSupplierId);
        result.getRecords().forEach(s -> {
            if (s.getOwnerId() != null) {
                SysUser u = userMapper.selectById(s.getOwnerId());
                s.setOwnerName(u == null ? "" : u.getRealName());
            }
        });
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public List<Supplier> options() {
        if (UserContext.isSupplierSide()) {
            Supplier s = supplierMapper.selectById(UserContext.supplierId());
            return s == null ? List.of() : List.of(s);
        }
        return supplierMapper.selectOptions();
    }

    /** 详情：主档 + 证照 + 关联单据 + 操作时间轴，详情页不重复拼接列表字段 */
    public Map<String, Object> detail(Long id) {
        Supplier s = supplierMapper.selectDetail(id);
        if (s == null) {
            throw new BizException(404, "供应商不存在");
        }
        if (UserContext.isSupplierSide() && !Objects.equals(s.getId(), UserContext.supplierId())) {
            throw new BizException(403, "无权查看其他供应商数据");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("supplier", s);
        m.put("certificates", certMapper.selectBySupplier(id));
        List<AdmissionApplication> applies = applyMapper.selectList(new QueryWrapper<AdmissionApplication>()
                .eq("supplier_id", id).orderByDesc("update_time").last("limit 10"));
        List<PerformanceTask> tasks = taskMapper.selectList(new QueryWrapper<PerformanceTask>()
                .eq("supplier_id", id).orderByDesc("period").last("limit 10"));
        m.put("applications", applies);
        m.put("tasks", tasks);
        return m;
    }

    @Transactional
    public Long create(SupplierDtos.SupplierReq req) {
        String code = req.supplierCode().trim();
        if (supplierMapper.selectCreditCode(req.creditCode().trim()) != null) {
            throw new BizException("统一信用代码已存在，同一代码不允许重复准入");
        }
        Long dup = supplierMapper.selectCount(new QueryWrapper<Supplier>().eq("supplier_code", code));
        if (dup != null && dup > 0) {
            throw new BizException("供应商编码已存在");
        }
        Supplier s = new Supplier();
        applyReq(s, req);
        s.setVersion(1);
        s.setCreateTime(LocalDateTime.now());
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.insert(s);
        logService.record("供应商", "新建主档", s.getSupplierCode(), "创建 " + s.getSupplierName());
        return s.getId();
    }

    @Transactional
    public void update(Long id, SupplierDtos.SupplierReq req) {
        Supplier old = supplierMapper.selectById(id);
        if (old == null) {
            throw new BizException(404, "供应商不存在");
        }
        // 数据权限：外部供应商只能维护本企业资料
        if (UserContext.isSupplierSide() && !Objects.equals(id, UserContext.supplierId())) {
            throw new BizException(403, "只能维护本企业信息");
        }
        // 乐观锁：前端提交的 version 必须与库内一致，避免覆盖他人修改
        if (req.version() != null && old.getVersion() != null && !req.version().equals(old.getVersion())) {
            throw new BizException(409, "数据已被他人更新，请刷新后重试");
        }
        if (!old.getCreditCode().equals(req.creditCode().trim())) {
            if (supplierMapper.selectCreditCode(req.creditCode().trim()) != null) {
                throw new BizException("统一信用代码已存在");
            }
        }
        // 过期证照未换证时不得变更合作类目
        if (req.categoryId() != null && !Objects.equals(req.categoryId(), old.getCategoryId())) {
            long expired = certMapper.selectCount(new QueryWrapper<SupplierCertificate>()
                    .eq("supplier_id", id).eq("cert_status", "已过期"));
            if (expired > 0) {
                throw new BizException("该供应商存在过期证照，换证前不得变更合作类目");
            }
        }
        Supplier s = new Supplier();
        s.setId(id);
        applyReq(s, req);
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(s);
        logService.record("供应商", "编辑主档", old.getSupplierCode(), "更新基本信息");
    }

    private void applyReq(Supplier s, SupplierDtos.SupplierReq req) {
        s.setSupplierCode(req.supplierCode().trim());
        s.setSupplierName(req.supplierName().trim());
        s.setCreditCode(req.creditCode().trim());
        s.setCategoryId(req.categoryId());
        s.setContactName(req.contactName());
        s.setContactPhone(req.contactPhone());
        s.setEmail(req.email());
        s.setAddress(req.address());
        s.setSupplierStatus(req.supplierStatus() == null ? "潜在" : req.supplierStatus());
        s.setLevel(req.level() == null ? "C" : req.level());
        s.setRiskLevel(req.riskLevel() == null ? "低" : req.riskLevel());
        s.setAdmissionDate(req.admissionDate());
        s.setCertExpireDate(req.certExpireDate());
        s.setAnnualCapacity(req.annualCapacity());
        s.setMainCustomers(req.mainCustomers());
        s.setCapacityDesc(req.capacityDesc());
        if (req.coverImage() != null && !req.coverImage().isBlank()) {
            s.setCoverImage(req.coverImage());
        }
        s.setOwnerId(req.ownerId() == null ? UserContext.userId() : req.ownerId());
        s.setOrgId(UserContext.get() == null ? null : UserContext.get().orgId());
    }

    /** 状态变更：暂停、恢复、黑名单、退出。黑名单必须记录原因与期限，由前端传 remark */
    @Transactional
    public void changeStatus(Long id, String target, String reason) {
        Supplier old = supplierMapper.selectById(id);
        if (old == null) {
            throw new BizException(404, "供应商不存在");
        }
        if (List.of("暂停", "黑名单", "已退出").contains(target) && (reason == null || reason.trim().length() < 5)) {
            throw new BizException("变更原因不少于 5 个字");
        }
        if ("黑名单".equals(old.getSupplierStatus())) {
            throw new BizException("该供应商处于黑名单期间，不可直接恢复，需走解除审批");
        }
        Supplier s = new Supplier();
        s.setId(id);
        s.setSupplierStatus(target);
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(s);
        logService.record("供应商", "状态变更", old.getSupplierCode(),
                old.getSupplierStatus() + " → " + target + "，原因：" + reason);
    }

    @Transactional
    public void delete(Long id) {
        long refs = applyMapper.selectCount(new QueryWrapper<AdmissionApplication>().eq("supplier_id", id));
        if (refs > 0) {
            throw new BizException("该供应商已被准入/评价单据引用，不能删除，可改为已退出");
        }
        supplierMapper.deleteById(id);
        logService.record("供应商", "删除主档", String.valueOf(id), "未被单据引用，执行逻辑删除");
    }

    public List<SupplierCategory> categories() {
        return categoryMapper.selectList(new QueryWrapper<SupplierCategory>()
                .eq("status", 1).orderByAsc("sort"));
    }

    /** 按类目必填证照数，用于材料完整度展示 */
    public int requiredCertCount(Long categoryId) {
        if (categoryId == null) {
            return 0;
        }
        SupplierCategory c = categoryMapper.selectById(categoryId);
        if (c == null || c.getRequiredCert() == null || c.getRequiredCert().isBlank()) {
            return 0;
        }
        return c.getRequiredCert().split(",").length;
    }

    public List<Supplier> expiringSuppliers(int days) {
        return supplierMapper.selectList(new QueryWrapper<Supplier>()
                .le("cert_expire_date", LocalDate.now().plusDays(days))
                .orderByAsc("cert_expire_date"));
    }
}

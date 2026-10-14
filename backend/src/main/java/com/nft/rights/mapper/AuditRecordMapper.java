package com.nft.rights.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nft.rights.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审核记录 MyBatis-Plus Mapper
 */
@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {
}

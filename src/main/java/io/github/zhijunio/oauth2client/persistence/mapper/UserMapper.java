package io.github.zhijunio.oauth2client.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.zhijunio.oauth2client.persistence.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<UserEntity> {

    @Insert("""
            INSERT INTO users (
                id, tenant_id, customer_id, email, authority, first_name, last_name, additional_info,
                created_time, version
            ) VALUES (
                #{user.id}, #{user.tenantId}, #{user.customerId}, #{user.email}, #{user.authority},
                #{user.firstName}, #{user.lastName}, #{user.additionalInfo},
                #{user.createdTime}, #{user.version}
            ) ON CONFLICT (email) DO NOTHING
            """)
    int insertIfAbsent(@Param("user") UserEntity user);
}

package io.github.zhijunio.oauth2client.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("domain")
public class DomainEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private long createdTime;
    private String tenantId;
    private String name;
    private Boolean oauth2Enabled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getOauth2Enabled() {
        return oauth2Enabled;
    }

    public void setOauth2Enabled(Boolean oauth2Enabled) {
        this.oauth2Enabled = oauth2Enabled;
    }
}

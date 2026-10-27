package io.github.zhijunio.oauth2client.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("domain_oauth2_client")
public class DomainOauth2ClientEntity {

    private String domainId;
    private String oauth2ClientId;

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getOauth2ClientId() {
        return oauth2ClientId;
    }

    public void setOauth2ClientId(String oauth2ClientId) {
        this.oauth2ClientId = oauth2ClientId;
    }
}

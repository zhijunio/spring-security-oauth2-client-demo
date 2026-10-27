package io.github.zhijunio.oauth2client.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.zhijunio.oauth2client.persistence.entity.CustomerEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.CustomerMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

    private final CustomerMapper customers;
    private final Clock clock;

    public CustomerService(CustomerMapper customers, Clock clock) {
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional
    public synchronized CustomerEntity findOrCreate(String tenantId, String title) {
        CustomerEntity customer = customers.selectOne(Wrappers.<CustomerEntity>lambdaQuery()
                .eq(CustomerEntity::getTenantId, tenantId)
                .eq(CustomerEntity::getTitle, title));
        if (customer != null) return customer;
        customer = new CustomerEntity();
        customer.setId(UUID.randomUUID().toString());
        customer.setTenantId(tenantId);
        customer.setTitle(title);
        customer.setCreatedTime(clock.millis());
        customer.setVersion(1L);
        customers.insert(customer);
        return customer;
    }

    public CustomerEntity requireForTenant(String tenantId, String customerId) {
        CustomerEntity customer = customers.selectOne(Wrappers.<CustomerEntity>lambdaQuery()
                .eq(CustomerEntity::getId, customerId)
                .eq(CustomerEntity::getTenantId, tenantId));
        if (customer == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mapped customer does not belong to the resolved tenant");
        }
        return customer;
    }
}

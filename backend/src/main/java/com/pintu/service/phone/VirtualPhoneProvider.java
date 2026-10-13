package com.pintu.service.phone;

import com.pintu.entity.OrderRecord;

public interface VirtualPhoneProvider {
    VirtualPhoneProvision bind(OrderRecord order);

    void unbind(String providerBindingId);
}

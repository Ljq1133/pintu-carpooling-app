package com.pintu.service.phone;

import com.pintu.common.util.Hashing;
import com.pintu.entity.OrderRecord;
import org.springframework.stereotype.Component;

@Component
public class MockVirtualPhoneProvider implements VirtualPhoneProvider {

    @Override
    public VirtualPhoneProvision bind(OrderRecord order) {
        String hash = Hashing.sha256(order.getId());
        StringBuilder suffix = new StringBuilder();
        for (int index = 0; index < 8; index++) {
            suffix.append(Math.abs(hash.charAt(index)) % 10);
        }
        return new VirtualPhoneProvision("mock-vp-" + order.getId(), "95013" + suffix);
    }

    @Override
    public void unbind(String providerBindingId) {
        // Course/demo provider only records the unbind locally.
    }
}

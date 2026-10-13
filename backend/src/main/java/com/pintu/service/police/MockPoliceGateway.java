package com.pintu.service.police;

import com.pintu.common.util.IdGenerator;
import com.pintu.entity.SafetyAlert;
import org.springframework.stereotype.Component;

@Component
public class MockPoliceGateway implements PoliceGateway {

    @Override
    public PoliceReport report(SafetyAlert alert) {
        return new PoliceReport(
                IdGenerator.next("police"),
                "课程 Mock 报警已上报，不会真实联系警方"
        );
    }
}

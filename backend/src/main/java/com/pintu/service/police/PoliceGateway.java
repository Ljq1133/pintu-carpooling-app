package com.pintu.service.police;

import com.pintu.entity.SafetyAlert;

public interface PoliceGateway {

    PoliceReport report(SafetyAlert alert);
}

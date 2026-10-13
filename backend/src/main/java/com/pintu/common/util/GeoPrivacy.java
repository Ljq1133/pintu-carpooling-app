package com.pintu.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GeoPrivacy {
    private GeoPrivacy() {
    }

    /** Three decimal places are roughly 100 metres in Shanghai. */
    public static BigDecimal blur(BigDecimal coordinate) {
        return coordinate == null ? null : coordinate.setScale(3, RoundingMode.HALF_UP);
    }
}

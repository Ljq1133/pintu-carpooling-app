package com.pintu.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaskingTest {

    @Test
    void masksPhoneNameAndPlate() {
        assertEquals("138****5678", Masking.phone("13812345678"));
        assertEquals("张**", Masking.name("张三丰"));
        assertEquals("沪A***8", Masking.plate("沪A12348"));
    }

    @Test
    void keepsShortOrEmptyValuesReadable() {
        assertNull(Masking.phone(null));
        assertEquals("123456", Masking.phone("123456"));
        assertEquals("", Masking.name(""));
        assertEquals("沪A", Masking.plate("沪A"));
    }
}

package com.pintu.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdGeneratorTest {

    @Test
    void generatedIdsShouldStartWithPrefix() {
        String id = IdGenerator.next(IdGenerator.PREFIX_PASSENGER_ORDER);
        assertTrue(id.startsWith("po-"));
    }

    @Test
    void generatedIdsShouldBeUnique() {
        String a = IdGenerator.next(IdGenerator.PREFIX_ORDER);
        String b = IdGenerator.next(IdGenerator.PREFIX_ORDER);
        assertNotEquals(a, b);
    }
}

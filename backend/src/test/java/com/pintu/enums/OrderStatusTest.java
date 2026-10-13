package com.pintu.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单状态枚举单元测试。
 * 覆盖 {@link OrderStatus#fromValue(String)} 的全部分支，配合订单状态机集成测试形成路径覆盖。
 */
class OrderStatusTest {

    @Test
    void fromValueShouldReturnEnumForKnownValue() {
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.fromValue("已确认"));
        assertEquals(OrderStatus.ONGOING, OrderStatus.fromValue("履约中"));
        assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.fromValue("待付款"));
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromValue("已完成"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.fromValue("已取消"));
        assertEquals(OrderStatus.PENDING_CONFIRM, OrderStatus.fromValue("待确认"));
        assertEquals(OrderStatus.ARCHIVED, OrderStatus.fromValue("已归档"));
    }

    @Test
    void fromValueShouldThrowForUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromValue("foo"));
    }

    @Test
    void allEnumValuesShouldHaveChineseLabel() {
        for (OrderStatus status : OrderStatus.values()) {
            assertNotNull(status.getValue());
            assertEquals(status, OrderStatus.fromValue(status.getValue()));
        }
    }

    @Test
    void transitionsShouldFollowLifecycleContract() {
        assertEquals(true, OrderStatus.PENDING_CONFIRM.canTransitionTo(OrderStatus.CONFIRMED));
        assertEquals(true, OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.ONGOING));
        assertEquals(true, OrderStatus.ONGOING.canTransitionTo(OrderStatus.PENDING_PAYMENT));
        assertEquals(true, OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.COMPLETED));
        assertEquals(true, OrderStatus.COMPLETED.canTransitionTo(OrderStatus.ARCHIVED));
        assertEquals(false, OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.COMPLETED));
        assertEquals(false, OrderStatus.ARCHIVED.canTransitionTo(OrderStatus.CONFIRMED));
    }
}

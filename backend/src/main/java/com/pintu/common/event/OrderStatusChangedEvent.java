package com.pintu.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 订单状态发生变化时发布的领域事件。
 * <p>
 * 解耦订单核心业务与下游通知、信用、审计、统计等关注点。
 * 监听器应使用 {@link org.springframework.transaction.event.TransactionalEventListener}
 * 在事务提交后再执行，避免事务回滚后产生悬空副作用。
 * </p>
 */
@Getter
public class OrderStatusChangedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final String fromStatus;
    private final String toStatus;
    private final String operatorId;
    private final String reason;

    public OrderStatusChangedEvent(Object source,
                                    String orderId,
                                    String fromStatus,
                                    String toStatus,
                                    String operatorId,
                                    String reason) {
        super(source);
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operatorId = operatorId;
        this.reason = reason;
    }
}

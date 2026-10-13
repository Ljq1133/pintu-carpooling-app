package com.pintu.common.event;

import com.pintu.common.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单状态变化事件的默认审计监听器。
 * <p>
 * 仅记录结构化日志，不会影响主事务。
 * 后续接入通知、信用、统计模块时再追加各自的 {@link TransactionalEventListener}。
 * </p>
 */
@Slf4j
@Component
public class OrderStatusAuditListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        log.info("order_status_changed orderId={} from={} to={} operator={} reason={} traceId={}",
                event.getOrderId(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getOperatorId(),
                event.getReason(),
                TraceContext.currentTraceId());
    }

    @EventListener
    public void onAnyOrderStatusChange(OrderStatusChangedEvent event) {
        // 在事务尚未提交时也记录一条 debug 日志，便于排查事务回滚导致状态最终未变更的情况
        log.debug("order_status_pending_commit orderId={} from={} to={}",
                event.getOrderId(), event.getFromStatus(), event.getToStatus());
    }
}

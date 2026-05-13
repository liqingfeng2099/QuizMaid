package com.kanade.backend.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LargeAssemblyProducer {

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE_ASSEMBLY = "exchange.assembly.large";
    public static final String ROUTING_KEY_ASSEMBLY = "routing.key.assembly.large";
    public static final String QUEUE_ASSEMBLY = "queue.assembly.large";

    public void sendAssemblyTask(LargeAssemblyMessageDTO message) {
        rabbitTemplate.convertAndSend(EXCHANGE_ASSEMBLY, ROUTING_KEY_ASSEMBLY, message);
        log.info("[异步组卷] 发送大试卷组卷任务: taskId={}, userId={}, subject={}",
                message.getTaskId(), message.getUserId(), message.getSubject());
    }
}

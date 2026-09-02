package com.jpkocommunity.domain.notification.sse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SseEmitterRepository {

    private static final long TIMEOUT = 30L * 60 * 1000; // 30분
    private static final int SEND_THREADS = 16;
    // 전송 처리를 기다리는(드레인 대기) 연결의 최대 개수 (연결당 큐에는 최대 1개만 등록됨)
    private static final int SEND_QUEUE_CAPACITY = 1000;
    // 전송이 지연되는 동안 연결 하나가 보관할 수 있는 이벤트 최대 개수
    private static final int PER_EMITTER_CAPACITY = 100;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15;

    private final Map<Long, List<Channel>> channelsByUserId = new ConcurrentHashMap<>();
    private final AtomicInteger sendThreadSeq = new AtomicInteger(1);
    private final ThreadFactory sendThreadFactory = r -> {
        Thread t = new Thread(r, "sse-send-" + sendThreadSeq.getAndIncrement());
        t.setDaemon(true); // JVM 종료 막지 않도록
        return t;
    };

    private final ExecutorService sendExecutor = new ThreadPoolExecutor(
            SEND_THREADS, SEND_THREADS,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(SEND_QUEUE_CAPACITY),
            sendThreadFactory,
            new ThreadPoolExecutor.AbortPolicy() // 큐가 차면 조용히 버리지 않고 예외 발생
    );

    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public SseEmitter save(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        Channel channel = new Channel(userId, emitter);
        channelsByUserId.compute(userId, (id, channels) -> {
            List<Channel> list = (channels != null) ? channels : new CopyOnWriteArrayList<>();
            list.add(channel);
            return list;
        });

        emitter.onCompletion(() -> remove(channel));
        emitter.onTimeout(() -> remove(channel));
        emitter.onError(e -> remove(channel));

        enqueue(channel, "connect", "connected");

        return emitter;
    }

    public void sendTo(Long userId, String eventName, Object data) {
        List<Channel> channels = channelsByUserId.get(userId);
        if (channels == null || channels.isEmpty()) {
            return;
        }

        for (Channel channel : channels) {
            enqueue(channel, eventName, data);
        }
    }

    @PostConstruct
    public void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 죽은 연결 감지
     */
    private void sendHeartbeat() {
        try {
            for (List<Channel> channels : channelsByUserId.values()) {
                for (Channel channel : channels) {
                    enqueue(channel, "heartbeat", SseEmitter.event().comment("keepalive"));
                }
            }
        } catch (Exception e) {
            // scheduleAtFixedRate는 예외가 새어나가면 이후 실행이 영구 중단되므로 반드시 흡수
            log.warn("[SSE 하트비트 실패]", e);
        }
    }

    private void enqueue(Channel channel, String eventName, Object data) {
        enqueue(channel, eventName, SseEmitter.event().name(eventName).data(data));
    }

    /**
     * 이벤트를 해당 연결의 큐에 넣고 드레인을 예약함
     * emitter 하나당 실제 send를 수행하는 스레드가 항상 유지가 되도록
     */
    private void enqueue(Channel channel, String eventName, SseEmitter.SseEventBuilder builder) {
        PendingEvent event = new PendingEvent(eventName, builder);

        // 이 연결이 밀려있으면 이 연결만 끊음
        if (!channel.pending.offer(event)) {
            log.warn("[SSE 연결 종료] 수신자 큐 포화. userId={}, event={}", channel.userId, eventName);
            dropChannel(channel);
            return;
        }
        scheduleDrain(channel);
    }

    /**
     * 드레인 중복 실행을 방지하고, 한 연결당 드레인 태스크가 최대 1개만
     * Executor에 제출되도록함
     */
    private void scheduleDrain(Channel channel) {
        if (!channel.draining.compareAndSet(false, true)) {
            return; // 이미 드레인 중 -> 그 스레드가 방금 넣은 것까지 마저 보냄
        }
        try {
            sendExecutor.execute(() -> drain(channel));
        } catch (RejectedExecutionException e) {
            if (!sendExecutor.isShutdown()) { // 종료 중엔 정상 동작이므로 경고하지 않음
                log.warn("[SSE 연결 종료] 전송 큐 포화. userId={}", channel.userId);
            }

            // 큐 비우기는 드레인 권한을 쥐고 있는 지금 끝냄
            dropChannel(channel);

            // 플래그 해제는 반드시 마지막에
            channel.draining.set(false);
        }
    }

    /**
     * 이벤트를 버릴 수 밖에 없을 때 연결 자체를 정리함
     */
    private void dropChannel(Channel channel) {
        remove(channel);
        channel.pending.clear();
        try {
            channel.emitter.complete();
        } catch (Exception ignored) {
            // 이미 완료된 emitter일 수 있음 - 여긴 할일 없음
        }
    }

    private void drain(Channel channel) {
        boolean alive = true;
        try {
            PendingEvent event;
            while ((event = channel.pending.poll()) != null) {
                if (!doSend(channel, event)) {
                    alive = false;
                    channel.pending.clear();
                    break;
                }
            }
        } finally {
            channel.draining.set(false);
        }
        if (alive && !channel.pending.isEmpty()) {
            scheduleDrain(channel);
        }
    }

    private boolean doSend(Channel channel, PendingEvent event) {
        try {
            channel.emitter.send(event.builder());
            return true;
        } catch (IOException e) {
            // 클라이언트가 연결 끊은 경우
            log.debug("[SSE 전송 실패] 클라이언트 연결 종료 userId={}, event={}",
                    channel.userId, event.name());
            remove(channel);
            return false;
        } catch (Exception e) {
            // 완료/타임아웃 같은 실패는 IllegalStateException으로 감싸서 여기서 잡음
            log.warn("[SSE 전송 실패] userId={}, event={}", channel.userId, event.name, e);
            remove(channel);
            completeQuietly(channel.emitter, e);
            return false;
        }
    }

    private void completeQuietly(SseEmitter emitter, Exception cause) {
        try {
            emitter.completeWithError(cause);
        } catch (Exception ignored) {
            // 이미 완료된 emitter일 수 있음 - 여긴 할일 없음
        }
    }

    private void remove(Channel channel) {
        channelsByUserId.computeIfPresent(channel.userId, (id, channels) -> {
            channels.remove(channel);
            return channels.isEmpty() ? null : channels;
        });
    }

    /**
     * @PreDestroy *Spring Bean 소멸 될 때 호출되는 종료 처리 어노테이션
     * -애플리케이션 종료 시 sendExecutor를 graceful shutdown
     */
    @PreDestroy
    public void shutdown() {
        heartbeatScheduler.shutdownNow();
        sendExecutor.shutdown();
        try {
            if (!sendExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                sendExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sendExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private record PendingEvent(String name, SseEmitter.SseEventBuilder builder) {
    }

    private static final class Channel {
        private final Long userId;
        private final SseEmitter emitter;
        private final BlockingQueue<PendingEvent> pending = new LinkedBlockingQueue<>(PER_EMITTER_CAPACITY);
        private final AtomicBoolean draining = new AtomicBoolean(false);

        private Channel(Long userId, SseEmitter emitter) {
            this.userId = userId;
            this.emitter = emitter;
        }
    }
}

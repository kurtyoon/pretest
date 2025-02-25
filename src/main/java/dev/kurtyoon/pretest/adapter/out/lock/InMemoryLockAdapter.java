package dev.kurtyoon.pretest.adapter.out.lock;

import dev.kurtyoon.pretest.application.port.out.LockPort;
import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Adapter
public class InMemoryLockAdapter implements LockPort {

    private static final Logger log = LoggerUtils.getLogger(InMemoryLockAdapter.class);

    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public void lock(String key) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
    }

    @Override
    public void unlock(String key) {
        ReentrantLock lock = lockMap.get(key);

        if (lock != null && lock.tryLock()) {
            try {
                lock.unlock();
            } finally {
                lockMap.remove(key);
            }
        } else {
            log.warn("Failed to Unlock : {}", key);
        }
    }
}

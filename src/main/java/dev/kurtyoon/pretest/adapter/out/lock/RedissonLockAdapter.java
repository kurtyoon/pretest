package dev.kurtyoon.pretest.adapter.out.lock;

import dev.kurtyoon.pretest.application.port.out.LockPort;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

@Adapter
public class RedissonLockAdapter implements LockPort {

    private final RedissonClient redissonClient;

    public RedissonLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void lock(String key) {
        RLock lock = redissonClient.getLock(key);

        try {
            boolean acquired = lock.tryLock(10, 60, TimeUnit.SECONDS);

            if (!acquired) {
                throw new CommonException(ErrorCode.LOCK_ACQUIRE_FAILED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

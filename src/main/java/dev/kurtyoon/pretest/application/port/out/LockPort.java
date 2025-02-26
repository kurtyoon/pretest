package dev.kurtyoon.pretest.application.port.out;

public interface LockPort {

    /**
     * Lock을 획득합니다.
     * @param key
     */
    void lock(String key);

    /**
     * Lock을 해제합니다.
     * @param key
     */
    void unlock(String key);
}

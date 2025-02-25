package dev.kurtyoon.pretest.application.port.out;

public interface LockPort {

    void lock(String key);

    void unlock(String key);
}

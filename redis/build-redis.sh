#!/bin/bash

BASE_DIR=$(pwd)

docker run -d -p 6379:6379 -v "${BASE_DIR}/redis/redis.conf:/usr/local/etc/redis/redis.conf" -v "${BASE_DIR}/redis/data:/data" --name pretest-redis redis redis-server /usr/local/etc/redis/redis.conf
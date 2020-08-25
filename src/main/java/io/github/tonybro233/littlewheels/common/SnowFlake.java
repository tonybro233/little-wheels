package io.github.tonybro233.littlewheels.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Twitter开源分布式id生成器
 */
public final class SnowFlake {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowFlake.class);

    public static final SnowFlake DEFAULT = new SnowFlake(0, 0, 0);

    public static SnowFlake newInstance(long workerId, long datacenterId, long sequence) {
        return new SnowFlake(workerId, datacenterId, sequence);
    }

    private SnowFlake(long workerId, long datacenterId, long sequence) {
        // sanity check for workerId
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        LOGGER.info("Snowflake worker starting. timestamp left shift {}, datacenter id bits {}, worker id bits {}, "
                        + "sequence bits {}, workerid {}",
                timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, workerId);

        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = sequence;
    }

    /**
     * 机器号
     */
    private long workerId;

    /**
     * 数据中心号
     */
    private long datacenterId;

    /**
     * 同毫秒内自增序列号
     */
    private long sequence;

    /**
     * 程序序列号 第一次生成时间 可以自己配置
     */
    private long twepoch = 1038834974657L;

    /**
     * 机器号 5位
     */
    private long workerIdBits = 5;

    /**
     * 数据中心号 5位
     */
    private long datacenterIdBits = 5;

    /**
     * 最大机器号
     */
    private long maxWorkerId = -1 ^ (-1 << workerIdBits);

    /**
     * 最大数据中心号
     */
    private long maxDatacenterId = -1 ^ (-1 << datacenterIdBits);

    /**
     * 同毫秒内 自增序列位数
     */
    private long sequenceBits = 12;

    /**
     * 机器号左移位数
     */
    private long workerIdShift = sequenceBits;

    /**
     * 数据中心号左移位数
     */
    private long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间戳差值左移位数
     */
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 同毫秒内自增序列号最大值   防止溢出 影响机器号的值
     */
    private long sequenceMask = -1 ^ (-1 << sequenceBits);

    /**
     * 上次生成序列号的时间
     */
    private long lastTimestamp = -1;

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    public synchronized long nextId() {
        // 获取当前时间
        long timestamp = timeGen();
        // 检查时间是否倒退
        if (timestamp < lastTimestamp) {
            LOGGER.error("clock is moving backwards.  Rejecting requests until {}.", lastTimestamp);
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds",
                    lastTimestamp - timestamp));
        }
        //如果本次生成时间跟上次时间相同 那么自增序列增加，如果溢出那么就等下个时间，主要是防止重复
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                //获取下个时间
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        // 更新上次生成时间
        lastTimestamp = timestamp;
        // 将4部分合在一起
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

}

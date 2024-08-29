package site.guohw;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class shortFlake {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final long maxSequencePerDay = (1L << 14) - 1; // 14位二进制，用于表示一天内的秒数
    private final long machineId;
    private long lastSecondOfTheDay = -1L;
    private long sequence = 0L;

    public shortFlake() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        byte[] ipAddressBytes = localHost.getAddress();
        machineId = getMachineId(ipAddressBytes);
    }

    private long getMachineId(byte[] ipAddressBytes) {
        // 将IP地址的字节转换为一个长整型值
        long ipAsLong = 0;
        for (byte b : ipAddressBytes) {
            ipAsLong = (ipAsLong << 8) | (b & 0xFF);
        }
        // 取最后9位作为机器ID
        return ipAsLong & ((1L << 12) - 1);
    }

    public synchronized String generateId() {
        long currentTime = System.currentTimeMillis();
        long currentSecond = currentTime / 1000; // 当前时间的秒数
        long secondOfTheDay = currentSecond % 86400; // 当天的秒数

        if (secondOfTheDay < lastSecondOfTheDay) {
            // 如果秒数小于上次记录的秒数，说明时钟回拨
            throw new RuntimeException("Clock moved backwards, refusing to generate id");
        }

        if (secondOfTheDay == lastSecondOfTheDay) {
            // 如果是同一秒钟，增加序列号
            sequence = (sequence + 1) & ((1 << 6) - 1); // 6位最大63
            if (sequence == 0) {
                // 如果序列号达到最大值，则等待下一秒
                while (secondOfTheDay == lastSecondOfTheDay) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 恢复中断状态
                        throw new RuntimeException("Thread interrupted while waiting for the next second", e);
                    }
                    currentTime = System.currentTimeMillis();
                    secondOfTheDay = (currentTime / 1000) % 86400;
                }
            }
        } else {
            // 如果是新的一秒，重置序列号
            sequence = 0;
            lastSecondOfTheDay = secondOfTheDay;
        }

        // 构建ID：日期 + 序列号（36进制）
        long idPart = (secondOfTheDay << 6) | sequence;
        String idStr = DATE_FORMATTER.format(LocalDate.now()) + Long.toString(idPart, 36).toUpperCase();

        return idStr;
    }
    public static void main(String[] args) throws UnknownHostException {
        shortFlake idGenerator = new shortFlake();
        for (int i = 0; i < 1000; i++) {
            String id = idGenerator.generateId();
            System.out.println(id);
        }
    }
}
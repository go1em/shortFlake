/* guohw 2024-5-21 */
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
public class CustomSnowflakeIdGenerator {
	private static final long twepoch = LocalDateTime.of(2024, 5, 1, 0, 0, 0)
		.toEpochSecond(ZoneOffset.UTC);
    private final long timestampBits = 30L;
    private final long machineIdBits = 9L;
    private final long sequenceBits = 6L;

    private final long maxTimestamp = (1L << timestampBits) - 1;
    private final long maxMachineId = (1L << machineIdBits) - 1;
    private final long maxSequence = (1L << sequenceBits) - 1;

    private final long machineId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public CustomSnowflakeIdGenerator() throws UnknownHostException {
        byte[] ipAddress = InetAddress.getLocalHost().getAddress();
        long machineId = getMachineId(ipAddress);
        this.machineId = machineId;
    }

    private long getMachineId(byte[] ipAddress) {
        long ipAsLong = ((ipAddress[ipAddress.length - 1] & 0xFFL) % (maxMachineId + 1));
        return ipAsLong;
    }

    public synchronized String generateId() {
        long timestamp = System.currentTimeMillis() / 1000;

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards!");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                timestamp = nextSeconds(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        long id = ((timestamp - twepoch) << (machineIdBits + sequenceBits))
                | (machineId << sequenceBits)
                | sequence;

        String idStr = Long.toString(id, 36).toUpperCase();
        return idStr;
    }

    private long nextSeconds(long currentTimestamp) {
        long timestamp = System.currentTimeMillis() / 1000;
        while (timestamp <= currentTimestamp) {
            timestamp = System.currentTimeMillis() / 1000;
        }
        return timestamp;
    }

    public static void main(String[] args) throws UnknownHostException {
        CustomSnowflakeIdGenerator idGenerator = new CustomSnowflakeIdGenerator();
        for (int i = 0; i < 10; i++) {
            String id = idGenerator.generateId();
            System.out.println(id);
        }
    }
}
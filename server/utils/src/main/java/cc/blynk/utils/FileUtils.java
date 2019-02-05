package cc.blynk.utils;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public final class FileUtils {

    private static final String BLYNK_FOLDER = "blynk";
    public static final String CSV_DIR = Paths.get(
            System.getProperty("java.io.tmpdir"), BLYNK_FOLDER)
            .toString();

    private FileUtils() {
    }

    private static final String[] POSSIBLE_LOCAL_PATHS = new String[] {
            "./server/http-dashboard/target/classes",
            "./server/http-api/target/classes",
            "./server/http-admin/target/classes",
            "./server/http-core/target/classes",
            "./server/core/target",
            "../server/http-api/target/classes/static/reset",
            "../server/http-admin/target/classes",
            "../server/http-dashboard/target/classes",
            "../server/http-core/target/classes",
            "../server/core/target",
            "./server/utils/target",
            "../server/utils/target",
            Paths.get(System.getProperty("java.io.tmpdir"), BLYNK_FOLDER).toString()
    };

    //reporting entry is long value (8 bytes) + timestamp (8 bytes)
    public static final int SIZE_OF_REPORT_ENTRY = 16;

    public static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            //ignore
        }
    }

    public static void move(Path source, Path target) throws IOException {
        Path targetFile = Paths.get(target.toString(), source.getFileName().toString());
        Files.move(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Simply writes single reporting entry to disk (16 bytes).
     * Reporting entry is value (double) and timestamp (long)
     *
     * @param reportingPath - path to user specific reporting file
     * @param value         - sensor data
     * @param ts            - time when entry was created
     */
    public static void write(Path reportingPath, double value, long ts) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                Files.newOutputStream(reportingPath, CREATE, APPEND))) {
            dos.writeDouble(value);
            dos.writeLong(ts);
            dos.flush();
        }
    }

    /**
     * Read bunch of last records from file.
     *
     * @param userDataFile - file to read
     * @param count = number of records to read
     *
     * @return - byte buffer with data
     */
    public static ByteBuffer read(Path userDataFile, int count) throws IOException {
        return read(userDataFile, count, 0);
    }

    /**
     * Read bunch of last records from file.
     *
     * @param userDataFile - file to read
     * @param count        - number of records to read
     * @param skip         - number of entries to skip from the end
     * @return - byte buffer with data
     */
    public static ByteBuffer read(Path userDataFile, int count, int skip) throws IOException {
        int size = (int) Files.size(userDataFile);
        int expectedMinimumLength = (count + skip) * SIZE_OF_REPORT_ENTRY;
        int diff = size - expectedMinimumLength;
        int startReadIndex = Math.max(0, diff);
        int bufferSize = diff < 0 ? count * SIZE_OF_REPORT_ENTRY + diff : count * SIZE_OF_REPORT_ENTRY;
        if (bufferSize <= 0) {
            return null;
        }

        ByteBuffer buf = ByteBuffer.allocate(bufferSize);

        try (SeekableByteChannel channel = Files.newByteChannel(userDataFile, EnumSet.of(READ))) {
            channel.position(startReadIndex)
                    .read(buf);
            ((Buffer) buf).flip();
            return buf;
        }
    }

    //todo format data in select query
    public static boolean writeBufToCsvFilterAndFormat(BufferedWriter writer, ByteBuffer onePinData,
                                                       String pin, String deviceName,
                                                       DateTimeFormatter formatter) throws IOException {
        if (onePinData.remaining() <= 0) {
            return false;
        }
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            String formattedTs = formatTS(formatter, ts);
            writer.write(formattedTs + ',' + pin + ',' + deviceName + ',' + value + '\n');
        }
        writer.flush();
        return true;
    }

    //todo format data in select query
    public static boolean writeBufToCsvFilterAndFormat(BufferedWriter writer, ByteBuffer onePinData, String pin,
                                                       DateTimeFormatter formatter) throws IOException {
        if (onePinData.remaining() <= 0) {
            return false;
        }
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            String formattedTs = formatTS(formatter, ts);
            writer.write(formattedTs + ',' + pin + ',' + value + '\n');
        }

        writer.flush();
        return true;
    }

    //todo format data in select query
    public static String writeBufToCsvFilterAndFormat(ByteBuffer onePinData, DateTimeFormatter formatter) {
        StringBuilder sb = new StringBuilder(onePinData.capacity() * 3);
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            String formattedTs = formatTS(formatter, ts);
            sb.append(formattedTs).append(',')
                    .append(value).append('\n');
        }
        return sb.toString();
    }

    //todo format data in select query
    private static String formatTS(DateTimeFormatter formatter, long ts) {
        if (formatter == null) {
            return String.valueOf(ts);
        }
        return formatter.format(Instant.ofEpochMilli(ts));
    }

    public static Path getUserReportDir(String email, int orgId, int reportId, String date) {
        return Paths.get(FileUtils.CSV_DIR, email + "_" + orgId + "_" + reportId + "_" + date);
    }

    public static final String BUILD = "build";
    public static final String FIRMWARE_VERSION = "fw";
    public static final String BOARD_TYPE = "dev";
    public static final String MD5 = "md5";

    private static final String[] OTA_FIELDS = new String[] {
            BUILD, FIRMWARE_VERSION, BOARD_TYPE
    };

    public static Map<String, String> getPatternFromString(Path path) {
        try {
            byte[] data = Files.readAllBytes(path);
            Map<String, String> map = new HashMap<>();
            for (String key : OTA_FIELDS) {
                String value = findValueForPattern(data, key);
                if (value != null) {
                    map.put(key, value);
                }
            }
            byte[] md5Hash = MessageDigest.getInstance("MD5").digest(data);
            String md5StringHex = String.format("%032X", new BigInteger(1, md5Hash));
            map.put(MD5, md5StringHex);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Unable to read build number fro firmware.");
        }
    }

    private static String findValueForPattern(byte[] data, String pattern) {
        pattern = "\0" + pattern + "\0";
        int index = KMPMatch.indexOf(data, pattern.getBytes());

        if (index != -1) {
            int start = index + pattern.length();
            int end = 0;
            byte b = -1;

            while (b != '\0') {
                end++;
                b = data[start + end];
            }

            byte[] copy = Arrays.copyOfRange(data, start, start + end);
            return new String(copy);
        }
        return null;
    }

    public static Path getPathForLocalRun(String uri) {
        for (String possiblePath : POSSIBLE_LOCAL_PATHS) {
            Path path = Paths.get(possiblePath, uri);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

}

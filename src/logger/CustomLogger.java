package logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CustomLogger {
    private static String currentPeerID;
    private static Logger logger = Logger.getLogger(CustomLogger.class.getName());
    private FileHandler fileHandler;

    public static void log(String message) {
        logger.info(currentPeerID + ": " + message);
        System.out.println(LogFormat.getMessage(message));
    }

    public void initialize(String peerID) {
        try {
            fileHandler = new FileHandler("logRecords" + peerID + ".log");
            fileHandler.setFormatter(new LogFormat());
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
            currentPeerID = peerID;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class LogFormat extends Formatter {
        private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

        static String getMessage(String message) {
            return dateTimeFormatter.format(LocalDateTime.now()) + " PEER " + message + "\n";
        }

        @Override
        public String format(LogRecord record) {
            return getMessage(record.getMessage());
        }
    }
}

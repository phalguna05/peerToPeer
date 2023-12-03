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
        logger.info(message);
    }
    public static void tcpRequest(int peerID1,int peerID2)
    {
        logger.info(peerID1+" makes connection with "+peerID2+".");
    }
    public static void tcpDone(int peerID1,int peerID2)
    {
        logger.info(peerID1+" is connected from Peer "+peerID2);
    }
    public static void changeOfPreferredNeighbors(int peerID1,int[] peers)
    {
        String p="";
        for (int i : peers) {
            p+=i+", ";
        }
        logger.info(peerID1+" has the preferred neighbors "+p.substring(0,p.length()-2)+".");
    }
    public static void changeOfOptimisticallyUnchokedNeighbor(int peerID1,int peerID2)
    {
        logger.info(peerID1+" has the optimistically unchoked neighbor "+peerID2+".");
    }
    public static void unchoking(int peerID1,int peerID2)
    {
        logger.info(peerID1+" is unchoked by "+peerID2+".");
    }
    public static void choking(int peerID1,int peerID2)
    {
        logger.info(peerID1+" is choked by "+peerID2+".");
    }
    public static void have(int peerID1,int peerID2, int index)
    {
        logger.info(peerID1+" received the ‘have’ message from "+peerID2+" for the piece "+index+".");
    }
    public static void interested(int peerID1,int peerID2)
    {
        logger.info(peerID1+" received the ‘interested’ message from "+peerID2+".");
    }
    public static void notInterested(int peerID1,int peerID2)
    {
        logger.info(peerID1+" received the ‘not interested’ message from "+peerID2+".");
    }
    public static void downloading(int peerID1,int peerID2, int index, int numberOfPieces)
    {
        logger.info(peerID1+" has downloaded the piece "+index+" from "+peerID2+". Now the number of pieces it has is "+numberOfPieces+".");
    }
    public static void downloaded(int peerID1)
    {
        logger.info(peerID1+" has downloaded the complete file.");
    }

    public void initialize(String peerID) {
        try {
            fileHandler = new FileHandler("log_peer_" + peerID + ".log");
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
            return dateTimeFormatter.format(LocalDateTime.now()) +" "+ message + "\n";
        }

        @Override
        public String format(LogRecord record) {
            return getMessage(record.getMessage());
        }
    }
}
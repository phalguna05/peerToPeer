package message;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Piece {
    byte[] content;
    int pieceIndex;

    public Piece() {
        Properties properties = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("Common.cfg");
            properties.load(fileInputStream);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int pieceSize = (int) Double.parseDouble(properties.getProperty("PieceSize"));

        this.setContent(new byte[pieceSize]);
        this.setPieceIndex(-1);
    }


    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }
}

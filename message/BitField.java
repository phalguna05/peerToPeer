package message;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BitField {
    private int numberOfPieces;
    private Piece[] pieces;
    public BitField() {
        Properties properties = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("Common.cfg");
            properties.load(fileInputStream);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Double fileSize = Double.parseDouble(String.valueOf(properties.getProperty("FileSize")));
        Double pieceSize = Double.parseDouble(String.valueOf(properties.getProperty("PieceSize")));
        numberOfPieces = (int) Math.ceil(fileSize / pieceSize);
        pieces = new Piece[numberOfPieces];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new Piece();
            System.out.println(pieces[i].getContent());
            System.out.println(pieces[i].getPieceIndex());

        }
}
public static void main(String[] args) {
    BitField bit = new BitField();
    System.out.println(bit.numberOfPieces);
}

}

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
            //reading the Common Configuration file
            FileInputStream fileInputStream = new FileInputStream("Common.cfg");
            properties.load(fileInputStream);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //reading the FileSize from the Common Configuration file
        Double fileSize = Double.parseDouble(String.valueOf(properties.getProperty("FileSize")));
        //reading the PieceSize from the Common Configuration file
        Double pieceSize = Double.parseDouble(String.valueOf(properties.getProperty("PieceSize")));
        //Finding the Number of pieces in which the file will be sent
        numberOfPieces = (int) Math.ceil(fileSize / pieceSize);
        //Creating piece objects
        pieces = new Piece[numberOfPieces];
        //initializing each Piece object with content and piece index
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new Piece();
        }
}
/*public static void main(String[] args) {
    BitField bit = new BitField();
    System.out.println(bit.numberOfPieces);
}
*/
}

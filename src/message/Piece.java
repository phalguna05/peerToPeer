package message;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

// Class representing a piece of a file in a peer-to-peer file sharing network
public class Piece {
    byte[] content;// Content of the piece
    int pieceIndex;// Index of the piece in the file
    Boolean isPresent;// Flag to indicate if the piece is currently held

    // Constructor for creating a piece with a specific index
    public Piece(int index) {
        Properties properties = new Properties();
        try {
            // Load configuration properties from 'Common.cfg'
            FileInputStream fileInputStream = new FileInputStream("Common.cfg");
            properties.load(fileInputStream);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Retrieve the size for each piece from the configuration
        int pieceSize = (int) Double.parseDouble(properties.getProperty("PieceSize"));

        // Initialize the piece content and its index
        this.setContent(new byte[pieceSize]);
        this.setPieceIndex(index);
        this.setIsPresent(false);
    }

    // Getter for piece content
    public byte[] getContent() {
        return content;
    }

    // Setter for piece content
    public void setContent(byte[] content) {
        this.content = content;
    }

    // Getter for piece index
    public int getPieceIndex() {
        return pieceIndex;
    }

    // Setter for piece presence flag
    public void setIsPresent(Boolean flag){
        isPresent = flag;
    }

    // Getter for piece presence flag
    public Boolean getIsPresent(){
        return isPresent;
    }

    // Setter for piece index
    public void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }
}

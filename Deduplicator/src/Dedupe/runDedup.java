package Dedupe;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class runDedup {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        HashTheChunks h = new HashTheChunks();
        String filePath = "/Users/jasonxubw/desktop/random.txt";
        File file = new File(filePath);

        // calls getFileSize to get the file size
        int fileSize = h.getFileSize(file);
        System.out.println("File Size : " + fileSize);

        // calls getFileExtension to get the file type
        String extension = h.getFileExtension(file);
        System.out.println("File Extension : " + extension);

        int chunkSize = h.getChunksize(extension, fileSize);

        String hashOutput = h.fixedSizeChunk(filePath, chunkSize);

    }
}


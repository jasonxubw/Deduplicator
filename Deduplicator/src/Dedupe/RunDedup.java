package Dedupe;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class RunDedup {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        HashTheChunks h = new HashTheChunks();
        String filePath1 = "/Users/jasonxubw/desktop/random.txt";

        MyLocker locker = new MyLocker("locker");   //initialize MyLocker


        File file = new File(filePath1);
        String fileName = file.getName();

        // calls getFileSize to get the file size
        int fileSize = h.getFileSize(file);
        System.out.println("File Size : " + fileSize);

        // calls getFileExtension to get the file type
        String extension = h.getFileExtension(file);
        System.out.println("File Extension : " + extension);

        int chunkSize = h.getChunksize(extension, fileSize);



        h.fixedSizeChunk(filePath1, chunkSize, locker);
//        locker.retrieveFileFromMyLocker("random.txt");


        String filePath2 = "/Users/jasonxubw/desktop/random2.txt";
        File file2 = new File(filePath2);
        String fileName2 = file2.getName();
        if(locker.sameFileNameExists(fileName2)){
            MyFile sameFileNameFromLocker = locker.getSameFileNameFromLocker(fileName2);
            Path p = FileSystems.getDefault().getPath(filePath2);
            byte[] fileBytes = Files.readAllBytes(p);
            String hashedNewFile = h.hashContent(fileBytes);
            if (locker.exactSameFile(hashedNewFile, sameFileNameFromLocker)){
                System.out.println("This file already exists.");
            }
        }
        else{
            System.out.println("This is a new file. Going to chunk this new file.");
            h.fixedSizeChunk(filePath2, chunkSize, locker);
        }

    }
}


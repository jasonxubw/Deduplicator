package Dedupe;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class HashTheChunks {
    // this function returns a file type as a string
    public String getFileExtension(File file) {
        String extension = "";
        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                extension = name.substring(name.lastIndexOf("."));
            }
        } catch (Exception e) {
            extension = "";
        }
        return extension;
    }

    // read in files through FileInputStream
    public int getFileSize(File file) {
        FileInputStream fis = null;
        int size = 0;
        try {
            fis = new FileInputStream(file);
            // get bytes count
            size = fis.available();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return size;
    }

    public int getChunksize(String extension, int fileSize) {
        int chunkSize = 128;
        return chunkSize;
    }

    public String fixedSizeChunk(String filePath, int chunkSize, MyLocker locker) throws NoSuchAlgorithmException, IOException {
        FileInputStream fis = null;
        ArrayList<String> fileRetriever = new ArrayList<>();

        File file = new File(filePath);
        String fileName = file.getName();
        Path p = FileSystems.getDefault().getPath(filePath);
        byte[] fileBytes = Files.readAllBytes(p);

        String hashOfFile = hashContent(fileBytes);

        int currChunkPosition = 0;
        byte[] chunk;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while(currChunkPosition < fileBytes.length) {
            //take the fileBytes array and starting from the current chunk position, take a length of chunk size
            baos.write(fileBytes, currChunkPosition, chunkSize); // baos.write(byte[] chunk, offset, len)
            chunk = baos.toByteArray();
            // hash the chunks

            String hashOutput = hashContent(chunk);
            currChunkPosition += chunkSize;
            locker.storeChunktoChunkLocker(hashOutput, chunk); //stores to chunkLocker the hash and its chunk
            fileRetriever.add(hashOutput);



            System.out.println("Each chunk's length: " + chunk.length + "  &&   Our chunk's hash output: " + hashOutput);
            baos.reset();

            // If there is enough unprocessed data left in file to fill a full chunk
            if (!(currChunkPosition < fileBytes.length - chunkSize)){
                chunkSize = fileBytes.length - currChunkPosition;
            }
        }
        locker.storeFileToMyLocker(fileName, fileRetriever, hashOfFile);
        System.out.println("Size of locker: " + locker.getSizeOfLocker());
        System.out.println("Size of chunk locker: " + locker.getSizeOfChunkLocker());
        System.out.println("Name of my locker: " + locker.getNameOfLocker());

        return "";
    }

    public String hashContent(byte[] content) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(content);
        byte[] hashedFile = md.digest(content);
        StringBuffer sb = new StringBuffer();
        for(Byte SHAhash : hashedFile){
            sb.append(Integer.toString((SHAhash & 0xff) + 0x100, 16).substring(1));
        }
        String hashedFileString = sb.toString();
        return hashedFileString;
    }
}

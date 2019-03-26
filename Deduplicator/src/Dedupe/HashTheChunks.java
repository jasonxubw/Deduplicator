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

    public String fixedSizeChunk(String filePath, int chunkSize) throws NoSuchAlgorithmException, IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] chunk;
        Path p = FileSystems.getDefault().getPath(filePath);
        byte[] fileBytes = Files.readAllBytes(p);
        System.out.println(fileBytes.length);
        int currChunkPosition = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while(currChunkPosition < fileBytes.length) {
            //take the fileBytes array and starting from the current chunk position, take a length of chunk size
            baos.write(fileBytes, currChunkPosition, chunkSize); // baos.write(byte[] chunk, offset, len)
            chunk = baos.toByteArray();
            // hash the chunks
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(chunk);
            byte[] hashedChunk = md.digest(chunk);

            // turn hashed chunks to string representation
            StringBuffer sb = new StringBuffer();
            for (Byte SHAhash : hashedChunk){
                sb.append(Integer.toString((SHAhash & 0xff) + 0x100, 16).substring(1));
            }
            String hashOutput = sb.toString();
            currChunkPosition += chunkSize;
            System.out.println("Each chunk's length: " + chunk.length + "  &&   Our chunk's hash output: " + hashOutput);
            baos.reset();

            // If there is enough unprocessed data left in file to fill a full chunk
            if (!(currChunkPosition < fileBytes.length - chunkSize)){
                chunkSize = fileBytes.length - currChunkPosition;
            }
        }
    return "";
    }
}

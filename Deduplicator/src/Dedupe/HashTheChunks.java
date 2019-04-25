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
import java.util.LinkedHashSet;

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
        int chunkSize = 2048;
        if(isVideo(extension) || isPDF(extension)){
            if (fileSize > 50000000){  //50mb
                chunkSize = 8192;
            }
            else if(fileSize > 10000000){ //10mb
                chunkSize = 4096;
            }
            else if(fileSize > 1000000){ //1mb
                chunkSize = 2048;
            }
            else if(fileSize > 500000){ //0.5mb
                chunkSize = 512;
            }
            else{
                chunkSize = 64;
            }
        }
        if((isImage(extension) || isTextFile(extension))){
            if (fileSize > 10000000){ //10mb
                chunkSize = 2048;
            }
            else if(fileSize > 1000000){ //1mb
                chunkSize = 1024;
            }
            else if(fileSize > 500000){ //0.5mb
                chunkSize = 512;
            }
            else{
                chunkSize = 64;
            }
        }
        return chunkSize;
    }

    public boolean isVideo(String extension){
        if(extension.equalsIgnoreCase(".wav") ||
           extension.equalsIgnoreCase(".mov") ||
           extension.equalsIgnoreCase(".mp4") ||
           extension.equalsIgnoreCase(".flv") ||
           extension.equalsIgnoreCase(".avi") ) {
            return true;
        }
        return false;
    }

    public boolean isImage(String extension){
        if(extension.equalsIgnoreCase(".png") ||
           extension.equalsIgnoreCase(".jpeg") ||
           extension.equalsIgnoreCase(".gif") ||
           extension.equalsIgnoreCase(".jpg")){
            return true;
        }
        return false;
    }

    public boolean isTextFile(String extension){
        if(extension.equalsIgnoreCase(".txt") ||
           extension.equalsIgnoreCase(".docx") ||
           extension.equalsIgnoreCase(".rtf")) {
            return true;
        }
        return false;
    }

    public boolean isPDF(String extension){
        if(extension.equalsIgnoreCase(".pdf")){
            return true;
        }
        return false;
    }

    public void fixedSizeChunk(String filePath, int chunkSize, MyLocker locker) throws NoSuchAlgorithmException, IOException {
        FileInputStream fis = null;
        ArrayList<String> fileRetriever = new ArrayList<>();

        File file = new File(filePath);
        String fileName = file.getName();
        Path p = FileSystems.getDefault().getPath(filePath);
        byte[] fileBytes = Files.readAllBytes(p);
        LinkedHashSet<String> hs = new LinkedHashSet<>();
        long sizeInsertedInLocker = 0L;
        long originalFileSize = fileBytes.length;
        long actualStoredSize = 0;
        locker.addToOriginalFileSize(originalFileSize);

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

            if(!locker.containsHashInChunkLocker(hashOutput)){
                locker.storeChunktoChunkLocker(hashOutput, chunk); //stores to chunkLocker the hash and its chunk
                sizeInsertedInLocker += chunk.length;
                actualStoredSize += chunk.length;
            }

            fileRetriever.add(hashOutput);
            hs.add(hashOutput);

            baos.reset();

            // If there is enough unprocessed data left in file to fill a full chunk
            if (!(currChunkPosition < fileBytes.length - chunkSize)){
                chunkSize = fileBytes.length - currChunkPosition;
            }
        }
        locker.storeFileToMyLocker(fileName, fileRetriever, hashOfFile, hs, originalFileSize, actualStoredSize);
        locker.addSizeToChunkLocker(sizeInsertedInLocker);
//        System.out.println("Size of file locker: " + locker.getSizeOfFileLocker());
//        System.out.println("Size of chunk locker: " + locker.getNumOfChunksInChunkLocker());
//        System.out.println("Name of my locker: " + locker.getNameOfLocker());
//        System.out.println("Size of entire locker: " + (locker.getSizeOfChunkLocker() / 1000) + "kb");

        return;
    }

    public void dynamicSizeChunk(String filePath, int chunkSize, MyLocker locker) throws IOException, NoSuchAlgorithmException{
        File file = new File(filePath);
        String fileName = file.getName();
        Path p = FileSystems.getDefault().getPath(filePath);
        byte[] fileBytes = Files.readAllBytes(p);
        ArrayList<String> fileRetriever = new ArrayList<>(); //stores hashes of chunks so file could be retrieved later on
        LinkedHashSet<String> hashSet = new LinkedHashSet<>();
        long originalFileSize = fileBytes.length;
        long sizeInsertedInLocker = 0;
        long actualStoredSize = 0;
        locker.addToOriginalFileSize(originalFileSize);

        String hashOfFile = hashContent(fileBytes);

        int largePrimeNumber = 71257;
        int fileBytesLength = fileBytes.length; // length of the file bytes array
        int rkWindowIndex = 0; //keep track of the rabin karp window's index
        int currRKWindowStartIndex = 0;
        int currRKWindowSize = chunkSize;
        byte[] rkWindow = new byte[currRKWindowSize];
        int rkWindowSize = rkWindow.length;
        int polynomial = 1; //polynomial multiplier
        int fileHashIndex = 0; //keep track of the file bytes array index as we iterate through
        int currHashVal = 0; //the hash of the chunked bytes ; will update as rabin karp window shifts
        int fileBytesCounter = 0; //keep track of how many bytes we've iterated through in file bytes array
        boolean initialChunk = true;

        int mask = decideMask(chunkSize);

        byte[] chunk;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        //Initialize the rabin karp hash window with the first chunk of bytes
        for(int i = 0; i < chunkSize; i++){
            byte currFileHashByte = fileBytes[fileHashIndex];
            fileHashIndex++;
            if(!initialChunk){
                currHashVal *= largePrimeNumber;
                currHashVal += currFileHashByte; //add next hash
                polynomial *= largePrimeNumber;

            }
            else{
                currHashVal += currFileHashByte;
                initialChunk = false;
            }
            rkWindow[rkWindowIndex] = currFileHashByte;
            rkWindowIndex++;
            rkWindowIndex = rkWindowIndex % rkWindowSize;
            baos.write(currFileHashByte);
            fileBytesCounter++;
        }

        while(currRKWindowStartIndex + currRKWindowSize < fileBytesLength){

            if(fileBytesCounter <= fileBytesLength){
                byte currFileHashByte = fileBytes[fileHashIndex];
                fileHashIndex++;
                int temp = polynomial * rkWindow[rkWindowIndex];
                currHashVal = currHashVal - temp; //remove the value of hash at beginning of window
                currHashVal *= largePrimeNumber;
                currHashVal += currFileHashByte;
                rkWindow[rkWindowIndex] = currFileHashByte;
                rkWindowIndex++;
                rkWindowIndex = rkWindowIndex % rkWindowSize;
                baos.write(currFileHashByte);
                fileBytesCounter++;
            }

            currRKWindowSize++;

            if((currHashVal & mask) == 0 || fileBytesCounter == fileBytesLength){
                chunk = baos.toByteArray();
                baos.reset();
                String hashOutput = hashContent(chunk);

                currRKWindowStartIndex += currRKWindowSize;
                currRKWindowSize = 0;

                if(!locker.containsHashInChunkLocker(hashOutput)) {
                    locker.storeChunktoChunkLocker(hashOutput, chunk);
                    sizeInsertedInLocker += chunk.length;
                    actualStoredSize += chunk.length;
                }
                fileRetriever.add(hashOutput);
                hashSet.add(hashOutput);
                }

        }
        locker.storeFileToMyLocker(fileName,fileRetriever, hashOfFile, hashSet, originalFileSize, actualStoredSize);
        locker.addSizeToChunkLocker(sizeInsertedInLocker);
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

    public int decideMask(int chunkSize){
        int mask;
        if(chunkSize == 512){
            mask = 1 << 9;
        }
        else if(chunkSize == 2048){
            mask = 1 << 11;
        }
        else if(chunkSize == 4096){
            mask = 1 << 12;
        }
        else if(chunkSize == 8192){
            mask = 1 << 13;
        }
        else {
            mask = 1 << 12;
        }
        mask -= 1;
        return mask;
    }
}

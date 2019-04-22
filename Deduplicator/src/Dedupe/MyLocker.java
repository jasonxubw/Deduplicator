package Dedupe;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;


public class MyLocker {
    private final String myLockerName;
    private final ArrayList<MyFile> fileLocker;
    private final HashMap<String, byte[]> chunkLocker;
    private long lockerSize;
    private long originalFileSize;

    public MyLocker(String lockerName){
        this.myLockerName = lockerName;
        this.fileLocker = new ArrayList<>();
        this.chunkLocker = new HashMap<>();
        this.lockerSize = 0;
        this.originalFileSize = 0;
    }

    protected void storeFileToMyLocker(String fileName, ArrayList<String> fileRetriever, String hashOfFile, LinkedHashSet<String> hs, long originalFileSize, long actualStored){
        this.fileLocker.add(new MyFile(fileName, fileRetriever, hashOfFile, hs, originalFileSize, actualStored));
    }

    protected MyFile getFile(String fileName){
        MyFile file = null;
        for(MyFile f : this.fileLocker){
            if(fileName.equalsIgnoreCase(f.getNameOfMyFile())) {
                file = f;
            }
        }
        return file;
    }

    protected boolean retrieveFileFromMyLocker(String fileName, String retrievalPath) throws IOException {
        MyFile file = getFile(fileName);
        File retrievedFile = new File(retrievalPath + "/retrieved_"+fileName);
        ArrayList<String> retrievalArray = file.getFileRetriever();
        FileOutputStream fos = new FileOutputStream(retrievedFile);
        for(int i = 0; i < retrievalArray.size(); i++){
            fos.write(this.chunkLocker.get(retrievalArray.get(i)));
        }
        fos.close();
        return true;
    }

    protected boolean sameFileNameExists(String fileName){
        for(MyFile f : this.fileLocker){
            if(fileName.equalsIgnoreCase(f.getNameOfMyFile())){
                return true;
            }
        }
        return false;
    }

    protected MyFile getSameFileNameFromLocker(String fileName){
        for(MyFile f : this.fileLocker){
            if(fileName.equalsIgnoreCase(f.getNameOfMyFile())){
                return f;
            }
        }
        return null;
    }

    protected boolean exactSameFile(String hashedNewFile, MyFile fileFromLocker){
        if(hashedNewFile.equals(fileFromLocker.getHashOfFile())){
            return true;
        }
        return false;
    }

    protected boolean deleteFile(String fileName){
        MyFile f = getFile(fileName);
        if(f == null){
            System.out.println("File not found.");
            return false;
        }

        for(String reconstructorString : f.getFileRetriever()){
            for(MyFile ff : this.fileLocker){
                if(ff != f){
                    if(!(ff.getHashSet().contains(reconstructorString))) {
                        this.chunkLocker.remove(reconstructorString);
                    }
                }
            }
        }

        this.fileLocker.remove(f);
        /* implement: subtract file size from total locker size */
        return true;
    }


    protected boolean isFileLockerEmpty(){
        return this.fileLocker.isEmpty();
    }

    protected void storeChunktoChunkLocker(String hash, byte[] chunk){
        this.chunkLocker.put(hash, chunk);
    }

    protected boolean containsHashInChunkLocker(String hash){
        return this.chunkLocker.containsKey(hash);
    }

    protected long getSizeOfFileLocker(){
        return this.fileLocker.size();
    }

    protected long getNumOfChunksInChunkLocker(){
        return this.chunkLocker.size();
    }

    protected long getSizeOfChunkLocker(){
        return this.lockerSize;
    }

    protected void addSizeToChunkLocker(long size){
        this.lockerSize += size;
    }

    protected HashMap<String, byte[]> getChunkLocker(){
        return this.chunkLocker;
    }

    protected ArrayList<MyFile> getFileLocker(){
        return this.fileLocker;
    }

    protected String getNameOfLocker(){
        return this.myLockerName;
    }

    protected void showFilesInFileLocker(){
        System.out.println("====== Current files in your file locker ======");
        for(MyFile file : this.fileLocker){
            System.out.println("---    " + file.getNameOfMyFile() + "   (original size: " + (file.getOriginalFileSize()/1000) + "kb)" + "   (deduped size: " + (file.getActualStoredSize()/1000) + "kb)" + "    ---");
        }
    }

    protected boolean findFileWithSameHash(String hashOfFile){
        for(MyFile file : this.fileLocker){
            String hof = file.getHashOfFile();
            if(hof.equals(hashOfFile)){
                return true;
            }
        }
        return false;
    }

    protected void addToOriginalFileSize(long size){
        this.originalFileSize += size;
    }

    protected long getOriginalFileSize(){
        return this.originalFileSize;
    }

    protected float getDedupeRatio(){
        float f = (float) getSizeOfChunkLocker() / (float) getOriginalFileSize();
        float ratio = 100 - (f * 100);
        return ratio;
    }

    protected void printMyLockerStats(){
        int filecounter = 1;
        System.out.println("\n");
        System.out.println("====== Locker Name ======");
        System.out.println("         " + this.getNameOfLocker() + "\n");
        System.out.println("==== Size of Locker ====");
        System.out.println("Original files size: " + (this.getOriginalFileSize() / 1000) + "kb");
        System.out.println("Deduped size: " + (this.getSizeOfChunkLocker() / 1000) +"kb" + "\n");
        System.out.println("==== Files ====");
        System.out.println("Number of files: " + fileLocker.size() + "\n");
        for(MyFile file : this.fileLocker){
            System.out.println(filecounter + ".  " + file.getNameOfMyFile());
            System.out.println("Original Size: " + (file.getOriginalFileSize() / 1000) + "kb");
            System.out.println("Deduped Size: " + (file.getActualStoredSize() / 1000) + "kb");
            System.out.println();
            filecounter++;
        }
        System.out.println("=== Deduplication Ratio === ");
        System.out.println(getDedupeRatio() + "%");
    }
}


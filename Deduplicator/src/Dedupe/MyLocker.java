package Dedupe;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;


public class MyLocker {
    private final String myLockerName;
    private final ArrayList<MyFile> fileLocker;
    private final HashMap<String, byte[]> chunkLocker;

    public MyLocker(String lockerName){
        this.myLockerName = lockerName;
        this.fileLocker = new ArrayList<>();
        this.chunkLocker = new HashMap<>();
    }

    protected void storeFileToMyLocker(String fileName, ArrayList<String> fileRetriever, String hashOfFile){
        this.fileLocker.add(new MyFile(fileName, fileRetriever, hashOfFile));
    }

    protected boolean retrieveFileFromMyLocker(String fileName) throws IOException {
        MyFile file = null;
        for(MyFile f : this.fileLocker){
            if(fileName.equalsIgnoreCase(f.getNameOfMyFile())) {
                file = f;
            }
        }
        File retrievedFile = new File("/Users/jasonxubw/desktop/retrievedFile.txt");
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


    protected void storeChunktoChunkLocker(String hash, byte[] chunk){
        this.chunkLocker.put(hash, chunk);
    }

    protected long getSizeOfLocker(){
        return this.fileLocker.size();
    }

    protected long getSizeOfChunkLocker(){
        return this.chunkLocker.size();
    }

    protected String getNameOfLocker(){
        return this.myLockerName;
    }
}


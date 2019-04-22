package Dedupe;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class MyFile{

    private String fileName;
    private ArrayList<String> fileRetriever;
    private String hashOfFile;
    private final LinkedHashSet<String> hs;
    private final long originalFileSize;
    private long actualStoredSize;
//    private long size;

    public MyFile(String fileName, ArrayList<String> fileRetriever, String hashOfFile, LinkedHashSet<String> hs, long originalFileSize, long actualStoredSize) {
        this.fileName = fileName;
        this.fileRetriever = fileRetriever;
        this.hashOfFile = hashOfFile;
        this.hs = hs;
        this.originalFileSize = originalFileSize;
        this.actualStoredSize = actualStoredSize;
//        this.size = size;
    }

    protected String getNameOfMyFile(){
        return this.fileName;
    }

    protected ArrayList<String> getFileRetriever(){
        return this.fileRetriever;
    }

    protected String getHashOfFile(){
        return this.hashOfFile;
    }

    protected LinkedHashSet<String> getHashSet(){
        return this.hs;
    }

    protected long getOriginalFileSize(){
        return this.originalFileSize;
    }

    protected long getActualStoredSize(){
        return this.actualStoredSize;
    }
}

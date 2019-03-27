package Dedupe;

import java.util.ArrayList;

public class MyFile{

    private String fileName;
    private ArrayList<String> fileRetriever;
    private String hashOfFile;
//    private long size;

    public MyFile(String fileName, ArrayList<String> fileRetriever, String hashOfFile) {
        this.fileName = fileName;
        this.fileRetriever = fileRetriever;
        this.hashOfFile = hashOfFile;
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
}

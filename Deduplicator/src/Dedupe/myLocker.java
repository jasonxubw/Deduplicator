package Dedupe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;



public class myLocker {
    private final String myLockerName;
    private final ArrayList<File> fileLocker;

    public myLocker(String lockerName){
        this.myLockerName = lockerName;
        this.fileLocker = new ArrayList<>();

    }
}


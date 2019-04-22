package Dedupe;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class RunDedup {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

        Scanner user_input = new Scanner(System.in).useDelimiter("\n");
        HashTheChunks h = new HashTheChunks();
        String input;
        do {
            System.out.println("Creating new locker? [Y/N]");
            input = user_input.next();
        }
        // error handle for wrong insertions, if empty? not Y or not N
        while (input.isEmpty() && !input.equalsIgnoreCase("Y") && !input.equalsIgnoreCase("N"));

        MyLocker locker;   //initialize MyLocker
        if (input.equalsIgnoreCase("Y")) {
            System.out.println("Please enter a name for your new Locker: ");
            String lockerName = user_input.next();
            locker = new MyLocker(lockerName);
            System.out.println("Locker " + lockerName + " is created!");
        } else if (input.equalsIgnoreCase("N")) {
            System.out.println("Please enter the name of the existed locker to load: ");
            String lockerName = user_input.next();
            return;
            // locker = loadDB(lockerName);
            // to be continued, figure out how to loadDB first
        } else {
            System.out.println("Unexpected problem detected.");
            user_input.close();
            return;
        }

        while (!user_input.equals("done")) {
            // choose operations to operate locker
            do {
                System.out.println("Please enter store, retrieve, delete, check files, or finish: ");
                input = user_input.next();
                if (locker.isFileLockerEmpty()) {
                    if (input.equalsIgnoreCase("retrieve") || input.equalsIgnoreCase("delete") || input.equalsIgnoreCase("check files")) {
                        System.out.println("Locker is currently empty! Please store a file first");
                        input = "";
                    }
                }
            } while (input.isEmpty()
                    && !input.equalsIgnoreCase("store")
                    && !input.equalsIgnoreCase("retrieve")
                    && !input.equalsIgnoreCase("delete")
                    && !input.equalsIgnoreCase("check files")
                    && !input.equalsIgnoreCase("finish"));

            if (input.equalsIgnoreCase("store")) {
                do {
                    System.out.println("Are you storing multiple files in a directory or individual file? [multiple/single]");
                    input = user_input.next();
                    // for doing single or directory
                } while (!input.equalsIgnoreCase("multiple") && !input.equalsIgnoreCase("single"));

                //SINGLE FILE STORAGE
                if (input.equalsIgnoreCase("single")) {
                    System.out.println("Please enter path of the file");
                    String filePath = user_input.next();
                    File file = new File(filePath);
                    String filename = file.getName();
                    MyFile fileExisted = locker.getSameFileNameFromLocker(filename);

                    // calls getFileSize to get the file size
                    int fileSize = h.getFileSize(file);
                    System.out.println("File Size : " + (fileSize / 1000) + "kb");

                    // calls getFileExtension to get the file type
                    String extension = h.getFileExtension(file);

                    //SINGLE FILE: Fix sized chunking
                    long timerStart;
                    long timerEnd;
                    if (extension != null && (h.isVideo(extension) || h.isImage(extension) || h.isPDF(extension))){
                        if (fileExisted != null) {
                            System.out.println("File name: " + filename + " exists in locker. Would you like to replace the file? (Y/N)");
                            input = user_input.next();
                            if(input.equalsIgnoreCase("Y")){
                                Path p = FileSystems.getDefault().getPath(filePath);
                                byte[] bytesOfNewFile = Files.readAllBytes(p);
                                String hashOfNewFile = h.hashContent(bytesOfNewFile);
                                String hashOfExistedFile = fileExisted.getHashOfFile();
                                long sizeOfNewFile = bytesOfNewFile.length;

                                if(!(hashOfNewFile.equals(hashOfExistedFile) && (sizeOfNewFile == fileExisted.getOriginalFileSize()))){
                                    timerStart = System.currentTimeMillis();
                                    locker.deleteFile(filename);
                                    manageFixSizeChunking(locker, filename, filePath, extension, fileSize, h);
                                    timerEnd = System.currentTimeMillis();
                                    System.out.println("Time used to store file <<" + filename + ">> via Fix Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                                    System.out.println("-------------------------------------" + "\n");
                                }
                                else{
                                    System.out.println("Exact same file exists. Nothing is done.");
                                }
                            }
                        } else {
                            timerStart = System.currentTimeMillis();

                            manageFixSizeChunking(locker, filename, filePath, extension, fileSize, h);
                            timerEnd = System.currentTimeMillis();
                            System.out.println("Time used to store file <<" + filename + ">> via Fix Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                            System.out.println("-------------------------------------" + "\n");
                        }
                    }
                    //SINGLE FILE: Dynamic sized chunking
                    else if(extension != null){
                        if (fileExisted != null) {
                            System.out.println("File name: " + filename + " exists in locker. Would you like to replace the file? (Y/N)");
                            input = user_input.next();
                            if(input.equalsIgnoreCase("Y")){
                                Path p = FileSystems.getDefault().getPath(filePath);
                                byte[] bytesOfNewFile = Files.readAllBytes(p);
                                String hashOfNewFile = h.hashContent(bytesOfNewFile);
                                String hashOfExistedFile = fileExisted.getHashOfFile();
                                long sizeOfNewFile = bytesOfNewFile.length;

                                if(!(hashOfNewFile.equals(hashOfExistedFile) && (sizeOfNewFile == fileExisted.getOriginalFileSize()))){

                                    timerStart = System.currentTimeMillis();

                                    locker.deleteFile(filename);
                                    manageDynamicSizeChunking(locker, filename, filePath, extension, fileSize, h);

                                    timerEnd = System.currentTimeMillis();
                                    System.out.println("Time used to store file <<" + filename + ">> via Dynamic Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                                    System.out.println("-------------------------------------" + "\n");
                                }
                                else{
                                    System.out.println("Exact same file exists. Nothing is done.");
                                }
                            }
                        } else {
                            timerStart = System.currentTimeMillis();
                            manageDynamicSizeChunking(locker, filename, filePath, extension, fileSize, h);
                            timerEnd = System.currentTimeMillis();
                            System.out.println("Time used to store file <<" + filename + ">> via Dynamic Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                            System.out.println("-------------------------------------" + "\n");
                        }
                    }
                }
                //MULTIPLE FILE STORAGE
                else if (input.equalsIgnoreCase("multiple")) {
                    Queue<File> storeQueue = new LinkedList<>();
                    System.out.println("Please enter path of the directory");
                    String directoryPath = user_input.next();
                    File directoryFile = new File(directoryPath);
                    if (!directoryFile.isDirectory()) {
                        System.out.println("This is not a directory");
                    } else {
                        for (File file : directoryFile.listFiles()) {
                            storeQueue.offer(file);
                        }
                        while (!storeQueue.isEmpty()) {
                            File currFile = storeQueue.poll();
                            String filename = currFile.getName();
                            String currFilepath = currFile.getAbsolutePath();
                            MyFile fileExisted = locker.getSameFileNameFromLocker(filename);

                            int fileSize = h.getFileSize(currFile);
                            System.out.println("File Size : " + (fileSize / 1000) + "kb");

                            String extension = h.getFileExtension(currFile);

                            long timerStart;
                            long timerEnd;
                            //MULTIPLE FILE: Fixed size chunking
                            if (extension != null && (h.isVideo(extension) || h.isPDF(extension)) || h.isImage(extension)) {
                                if (fileExisted != null) {
                                    System.out.println("File name: " + filename + " exists in locker. Would you like to replace the file? (Y/N)");
                                    input = user_input.next();
                                    if(input.equalsIgnoreCase("Y")){
                                        Path p = FileSystems.getDefault().getPath(currFilepath);
                                        byte[] bytesOfNewFile = Files.readAllBytes(p);
                                        String hashOfNewFile = h.hashContent(bytesOfNewFile);
                                        String hashOfExistedFile = fileExisted.getHashOfFile();
                                        long sizeOfNewFile = bytesOfNewFile.length;

                                        if(!(hashOfNewFile.equals(hashOfExistedFile) && (sizeOfNewFile == fileExisted.getOriginalFileSize()))){
                                            timerStart = System.currentTimeMillis();

                                            locker.deleteFile(filename);
                                            manageFixSizeChunking(locker, filename, currFilepath, extension, fileSize, h);

                                            timerEnd = System.currentTimeMillis();
                                            System.out.println("Time used to store file <<" + filename + ">> via Fix Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                                            System.out.println("-------------------------------------" + "\n");
                                        }
                                        else{
                                            System.out.println("Exact same file exists. Nothing is done.");
                                        }


                                    }
                                } else {
                                    timerStart = System.currentTimeMillis();
                                    manageFixSizeChunking(locker, filename, currFilepath, extension, fileSize, h);
                                    timerEnd = System.currentTimeMillis();
                                    System.out.println("Time used to store file <<" + filename + ">> via Fix Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                                    System.out.println("-------------------------------------" + "\n");
                                }
                            //MULTIPLE FILE: Dynamic size chunking
                            } else if (extension != null){
                                if (fileExisted != null) {
                                    System.out.println("File name: " + filename + " exists in locker. Would you like to replace the file? (Y/N)");
                                    input = user_input.next();
                                    if(input.equalsIgnoreCase("Y")){
                                        Path p = FileSystems.getDefault().getPath(currFilepath);
                                        byte[] bytesOfNewFile = Files.readAllBytes(p);
                                        String hashOfNewFile = h.hashContent(bytesOfNewFile);
                                        String hashOfExistedFile = fileExisted.getHashOfFile();
                                        long sizeOfNewFile = bytesOfNewFile.length;

                                        if(!(hashOfNewFile.equals(hashOfExistedFile) && (sizeOfNewFile == fileExisted.getOriginalFileSize()))){

                                            timerStart = System.currentTimeMillis();

                                            locker.deleteFile(filename);
                                            manageDynamicSizeChunking(locker, filename, currFilepath, extension, fileSize, h);

                                            timerEnd = System.currentTimeMillis();
                                            System.out.println("Time used to store file <<" + filename + ">> via Dynamic Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                                            System.out.println("-------------------------------------" + "\n");
                                        }
                                        else{
                                            System.out.println("Exact same file exists. Nothing is done.");
                                        }
                                    }
                                } else {
                                    timerStart = System.currentTimeMillis();
                                    manageDynamicSizeChunking(locker, filename, currFilepath, extension, fileSize, h);
                                    timerEnd = System.currentTimeMillis();
                                    System.out.println("Time used to store file <<" + filename + ">> via Dynamic Sized Chunking is " + (double) (timerEnd - timerStart)/1000 + " seconds." );
                                    System.out.println("-------------------------------------" + "\n");
                                }
                            }
                        }
                    }
                }

            } else if (input.equalsIgnoreCase("retrieve")) {
                long timerStart;
                long timerEnd;

                System.out.println("Enter the path in which you want to store the retrieved file. ");
                String retrievalPath = user_input.next();
                File rPath = new File(retrievalPath);
                while(!rPath.isDirectory()){
                    System.out.println("The path you entered is invalid. Please try again. ");
                    retrievalPath = user_input.next();
                    rPath = new File(retrievalPath);
                }

                System.out.println("Enter name of the file you want to retrieve.");
                String retrievalFileName;
                do {
                    retrievalFileName = user_input.next();
                    if (locker.sameFileNameExists(retrievalFileName)) {
                        timerStart = System.currentTimeMillis();

                        locker.retrieveFileFromMyLocker(retrievalFileName, retrievalPath);
                        timerEnd = System.currentTimeMillis();
                        System.out.println(retrievalFileName + " file has been retrieved successfully");
                        System.out.println("Time used to retrieve file <<" + retrievalFileName + ">> is " + (double) (timerEnd - timerStart)/1000 + " seconds.");
                        System.out.println("-------------------------------------" + "\n");

                    } else {
                        System.out.println("Could not find file you want to retrieve.");
                    }
                }
                while (!locker.sameFileNameExists(retrievalFileName));
            } else if (input.equalsIgnoreCase("delete")) {
                long timerStart;
                long timerEnd;

                System.out.println("Enter name of the file you want to delete. Please try again. ");
                String deletionFileName;
                do {
                    deletionFileName = user_input.next();
                    if (locker.sameFileNameExists(deletionFileName)) {
                        timerStart = System.currentTimeMillis();

                        locker.deleteFile(deletionFileName);
                        timerEnd = System.currentTimeMillis();

                        System.out.println(deletionFileName + " file deleted from file locker.");
                        System.out.println("Time used to delete file <<" + deletionFileName + ">> is " + (double) (timerEnd - timerStart)/1000 + " seconds.");
                        System.out.println("-------------------------------------" + "\n");
                    } else {
                        System.out.println("Could not find file you want to delete.");
                    }
                }
                while (deletionFileName.equals("finish"));
            } else if (input.equalsIgnoreCase("check files")){
                locker.showFilesInFileLocker();
            }else if (input.equalsIgnoreCase("finish")) {
                locker.printMyLockerStats();
                user_input.close();
                break;
            }
        }
        user_input.close();
    }

    public static void manageFixSizeChunking(MyLocker locker, String filename, String filePath, String extension, int fileSize, HashTheChunks h) throws IOException, NoSuchAlgorithmException{
        System.out.println("Storing " + filename + " using fixed size chunking");
        int chunkSize = h.getChunksize(extension, fileSize);
        h.fixedSizeChunk(filePath, chunkSize, locker);
        System.out.println(filename + " is added successfully");
    }

    public static void manageDynamicSizeChunking(MyLocker locker, String filename, String filePath, String extension, int fileSize, HashTheChunks h) throws IOException, NoSuchAlgorithmException{
        System.out.println("Storing " + filename + " using dynamic size chunking");
        int chunkSize = h.getChunksize(extension, fileSize);
        h.dynamicSizeChunk(filePath, chunkSize, locker);
        System.out.println(filename + " is added successfully");
    }
}

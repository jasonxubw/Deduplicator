package Dedupe;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.Scanner;
import java.util.Queue;
import java.util.LinkedList;

public class RunDedup {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

//        Connection connection = null;
//
//        try{
//            connection = Database.getConnection();
//            if(connection != null){
//                System.out.println("Connection successful.");
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }finally {
//            if (connection != null){
//                try {
//                    connection.close();
//                }
//                catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }

        Scanner user_input = new Scanner(System.in);
        HashTheChunks h = new HashTheChunks();
        String input;
        do {
            System.out.println("Creating new locker? [Y/N]");
            input = user_input.next();
        }
        // error handle for wrong insertions, if empty? not Y or not N
        while (input.isEmpty() && !input.equalsIgnoreCase("Y") && !input.equalsIgnoreCase("N"));

        String filePath1 = "/Users/jasonxubw/desktop/random.txt";

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
                System.out.println("Please enter store, retrieve, delete or finish: ");
                input = user_input.next();
                if (locker.isFileLockerEmpty()) {
                    if (input.equalsIgnoreCase("retrieve") || input.equalsIgnoreCase("delete")) {
                        System.out.println("Locker is currently empty! Please store a file first");
                        input = "";
                    }
                }
            } while (input.isEmpty()
                    && !input.equalsIgnoreCase("store")
                    && !input.equalsIgnoreCase("retrieve")
                    && !input.equalsIgnoreCase("delete")
                    && !input.equalsIgnoreCase("finish"));

            if (input.equalsIgnoreCase("store")) {
                do {
                    System.out.println("Are you storing multiple files in a directory or individual file? [multiple/single]");
                    input = user_input.next();
                    // for doing single or directory
                } while (!input.equalsIgnoreCase("multiple") && !input.equalsIgnoreCase("single"));
                if (input.equalsIgnoreCase("single")) {
                    System.out.println("Please enter path of the file");
                    String filePath = user_input.next();
                    File file = new File(filePath);
                    String filename = file.getName();
                    MyFile fileExisted = locker.getSameFileNameFromLocker(filename);

                    // calls getFileSize to get the file size
                    int fileSize = h.getFileSize(file);
                    System.out.println("File Size : " + fileSize);

                    // calls getFileExtension to get the file type
                    String extension = h.getFileExtension(file);
                    System.out.println("File Extension : " + extension);

                    if (extension != null && extension.equals(".txt")) {
                        if (fileExisted != null) {
                            System.out.println(filename + " is already in locker." + " start deduplication");
                            // start referencing the chunks
                        } else {
                            System.out.println("Storing " + filename + " using fixed size chunking");
                            int chunkSize = h.getChunksize(extension, fileSize);
                            h.fixedSizeChunk(filePath, chunkSize, locker);
                            System.out.println(filename + " is added successfully");
                        }
                    } else {
                        if (fileExisted != null) {
                            System.out.println(filename + " is already in locker." + " start deduplication");
                            // start referencing the chunks
                        } else {
                            System.out.println("Storing " + filename + " using dynamic size chunking");
                            int chunkSize = h.getChunksize(extension, fileSize);
                            h.dynamicSizeChunk(filePath, chunkSize, locker);
                            System.out.println(filename + " is added successfully");
                        }
                    }
                } else if (input.equalsIgnoreCase("multiple")) {
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
                            System.out.println("File Size : " + fileSize);

                            String extension = h.getFileExtension(currFile);
                            System.out.println("File Extension : " + extension);

                            if (extension != null && extension.equals(".txt")) {
                                if (fileExisted != null) {
                                    System.out.println(filename + " is already in locker." + " start deduplication");
                                    // start referencing the chunks
                                } else {
                                    System.out.println("Storing " + filename + " using fixed size chunking");
                                    int chunkSize = h.getChunksize(extension, fileSize);
                                    h.fixedSizeChunk(currFilepath, chunkSize, locker);
                                    System.out.println(filename + " is added successfully");
                                }
                            } else {
                                if (fileExisted != null) {
                                    System.out.println(filename + " is already in locker." + " start deduplication");
                                    // start referencing the chunks
                                } else {
                                    System.out.println("Storing " + filename + " using dynamic size chunking");
                                    int chunkSize = h.getChunksize(extension, fileSize);
                                    h.dynamicSizeChunk(currFilepath, chunkSize, locker);
                                    System.out.println(filename + " is added successfully");
                                }
                            }
                        }
                    }
                }

            } else if (input.equalsIgnoreCase("retrieve")) {
                System.out.println("Enter name of the file you want to retrieve.");
                String retrievalFileName;
                do {
                    retrievalFileName = user_input.next();
                    if (locker.sameFileNameExists(retrievalFileName)) {
                        locker.retrieveFileFromMyLocker(retrievalFileName);
                        System.out.println(retrievalFileName + " file has been retrieved successfully");
                    } else {
                        System.out.println("Could not find file you want to retrieve.");
                    }
                }
                while (!locker.sameFileNameExists(retrievalFileName));
            } else if (input.equalsIgnoreCase("delete")) {
                System.out.println("Enter name of the file you want to delete.");
                String deletionFileName;
                do {
                    deletionFileName = user_input.next();
                    if (locker.sameFileNameExists(deletionFileName)) {
                        locker.deleteFile(deletionFileName);
                        System.out.println(deletionFileName + " file deleted from file locker.");
                    } else {
                        System.out.println("Could not find file you want to delete.");
                    }
                }
                while (deletionFileName.equals("finish"));
            } else if (input.equalsIgnoreCase("finish")) {
                user_input.close();
                break;
            }
        }
        user_input.close();
    }
}

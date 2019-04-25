package Dedupe;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
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
        System.out.println("Create new locker [create] || Load existing locker [load] || Terminate [end]");
        input = user_input.next();
        while (!(input.equalsIgnoreCase("create")) && !(input.equalsIgnoreCase("load")) && !(input.equalsIgnoreCase("end"))){
            System.out.println("Invalid input. Please try again. \nCreate new locker [create] || Load existing locker [load] || Terminate [end]");
            input = user_input.next();
        }

        MyLocker locker = null;   //initialize MyLocker
        String lockerName = "";
        if (input.equalsIgnoreCase("create")) {
            System.out.println("Please enter a name for your new Locker: ");
            input = user_input.next();
            lockerName = input;
            locker = new MyLocker(lockerName);
            System.out.println("Locker " + lockerName + " is created!");
        } else if (input.equalsIgnoreCase("load")) {
            System.out.println("Loading Locker from default path [load] || Import Locker from specified path [import]");
            input = user_input.next();

            while(!input.equalsIgnoreCase("load") && !input.equalsIgnoreCase("import")){
                System.out.println("Invalid input. Please try again. \n" + "Loading Locker from default path [load] || Import Locker from specified path [import]");
                input = user_input.next();
            }

            if (input.equalsIgnoreCase("load")) {
                boolean validImportLockerName = false;
                while (validImportLockerName == false) {
                    System.out.println("Please enter the name of the existing locker: ");
                    input = user_input.next();
                    lockerName = input;
                    try {
                        locker = importSerializedLocker(lockerName);
                        validImportLockerName = true;
                        System.out.println(lockerName + " loaded successfully!");
                    } catch (Exception e) {
                        System.out.println("Locker " + lockerName + " does not exist. Please try again.");
                    }
                }
            }
            else if(input.equalsIgnoreCase("import")){
                boolean validImportLocker = false;
                System.out.println("Please enter valid path of the Locker you are importing: ");
                while(validImportLocker == false) {
                    input = user_input.next();
                    File importPath = new File(input);
                    while(importPath.isDirectory()){
                        System.out.println("Import path is invalid. Please try again. \n" + "Please enter valid path of the Locker you are importing:");
                        input = user_input.next();
                        importPath = new File(input);
                    }
                    lockerName = input.substring(input.lastIndexOf(File.separator) + 1);
                    try {
                        locker = importLocker(input);
                        validImportLocker = true;
                        System.out.println(lockerName + " imported successfully!");
                    } catch (Exception e) {
                        System.out.println("Locker " + lockerName + " does not exist. Please try again. \n" + "Please enter valid path of the Locker you are importing:");
                    }
                }
            }
        } else if(input.equalsIgnoreCase("end")){
            return;
        }

        while (!user_input.equals("finish")) {
            // choose operations to operate locker
            do {
                System.out.println("Please enter [store], [retrieve], [delete], [check files], or [finish]: ");
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
                    while(file.isDirectory() || !file.exists()){
                        System.out.println("Invalid file path. Please try again. \n" + "Please enter path of the file");
                        filePath = user_input.next();
                        file = new File(filePath);
                    }
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
                    if (extension != null && !filename.equalsIgnoreCase(".DS_STORE") && (h.isVideo(extension) || h.isImage(extension) || h.isPDF(extension))){
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
                                    System.out.println("Exact same file exists. Nothing is done. \n");
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
                    else if(extension != null && !filename.equalsIgnoreCase(".DS_STORE")){
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
                            if (extension != null && !filename.equalsIgnoreCase(".DS_STORE") && (h.isVideo(extension) || h.isPDF(extension)) || h.isImage(extension) || h.isTextFile(extension)) {
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
                            } else if (extension != null && !filename.equalsIgnoreCase(".DS_STORE")){
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

                System.out.println("Enter name of the file you want to delete. ");
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
                        System.out.println("Could not find file you want to delete. Please try again.");
                    }
                }
                while (deletionFileName.equals("finish"));
            } else if (input.equalsIgnoreCase("check files")){
                locker.showFilesInFileLocker();
            }else if (input.equalsIgnoreCase("finish")) {
                System.out.println("Would you like to export your current locker <" + lockerName + ">? [Y/N]");
                input = user_input.next();
                while(!input.equalsIgnoreCase("Y") && !input.equalsIgnoreCase("N")){
                    System.out.println("Input invalid. Please try again. \n" + "Would you like to export your current locker <" + lockerName + ">? [Y/N]");
                    input = user_input.next();
                }
                if(input.equalsIgnoreCase("Y")){
                    System.out.println("Please enter a valid path to store the exported locker.");
                    input = user_input.next();
                    String exportPath = input;
                    File exportedPath = new File(exportPath);
                    while(!exportedPath.isDirectory()){
                        System.out.println("Invalid path. Please try again.");
                        exportPath = user_input.next();
                        exportedPath = new File(exportPath);
                    }
                    exportLocker(locker, exportPath);
                    System.out.println("Locker has been successfully exported into the path: " + exportPath);
                }
                exportSerializedLocker(locker);
                locker.printMyLockerStats();
                try{
                    printAllLockers();
                } catch (Exception e){
                    e.printStackTrace();
                }
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

    protected static void exportSerializedLocker(MyLocker locker) throws IOException{
        File f = new File("." + File.separator + "serialized" + File.separator);
        if (!f.exists()){
            f.mkdir();
        }
        if(f.isDirectory()){
            System.out.println("IS DIRECTORY");
        }
        FileOutputStream fos = new FileOutputStream("." + File.separator + "serialized"+ File.separator + locker.getNameOfLocker());
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(locker);
    }

    protected static MyLocker importSerializedLocker(String lockerName) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("serialized"+ File.separator + lockerName));
        MyLocker loadedLocker = (MyLocker) ois.readObject();
        return loadedLocker;
    }

    protected static void exportLocker(MyLocker locker, String exportPath) throws IOException{
        FileOutputStream fos = new FileOutputStream(exportPath + File.separator + locker.getNameOfLocker());
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(locker);
    }

    protected static MyLocker importLocker(String importPath) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(importPath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        MyLocker loadedLocker = (MyLocker) ois.readObject();
        return loadedLocker;
    }

    protected static void printAllLockers() throws IOException, ClassNotFoundException{
        File folder = new File("serialized");
        File[] listOfFiles = folder.listFiles();
//        boolean cont = true;
//        ArrayList<MyLocker> lockers = new ArrayList<>();
//        FileInputStream fis = new FileInputStream("serialized");
//        ObjectInputStream ois = new ObjectInputStream(fis);
//        while(cont){
//            MyLocker ml = (MyLocker) ois.readObject();
//            if(ml != null)
//                lockers.add(ml);
//            else
//                cont = false;
//        }
//        for(MyLocker ml : lockers){
//            System.out.println(ml.getNameOfLocker());
//        }
        for (int i = 0; i < listOfFiles.length; i++){
            if(listOfFiles[i].isFile()){
                System.out.println(listOfFiles[i].getName());
            }
        }
    }
}

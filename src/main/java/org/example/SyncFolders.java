package org.example;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncFolders {

    private static final String LOCAL_FOLDER_PATH = "C://Local";
    private static final String REMOTE_FOLDER_PATH = "C://servidor//grupo";
    private static final String FTP_HOST = "127.0.0.1";
    private static final String FTP_USERNAME = "paula";
    private static final String FTP_PASSWORD = "paula2003";

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        while (true) {
            syncFolders(executorService);
            sleep(5000);
        }

    }

    private static void syncFolders(ExecutorService executorService) {

        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(FTP_HOST);
            ftpClient.login(FTP_USERNAME, FTP_PASSWORD);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            if (ftpClient.changeWorkingDirectory(REMOTE_FOLDER_PATH)) {
                try {
                    Files.walkFileTree(Paths.get(LOCAL_FOLDER_PATH), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path localFile, BasicFileAttributes attrs) throws IOException {
                            executorService.submit(() -> {
                                try {
                                    String fileName = localFile.getFileName().toString();
                                    String remoteFilePath = REMOTE_FOLDER_PATH + "/" + fileName;

                                    long localLastModifiedTime = attrs.lastModifiedTime().toMillis();
                                    long remoteLastModifiedTime = ftpClient.mdtm(remoteFilePath);

                                    if (localLastModifiedTime > remoteLastModifiedTime) {
                                        uploadFile(ftpClient, localFile.toFile(), fileName);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });

                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                System.err.println("No se pudo cambiar al directorio remoto.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void uploadFile(FTPClient ftpClient, File localFile, String fileName) throws IOException {
        try {
            if (ftpClient.storeFile(fileName, new FileInputStream(localFile))) {
                System.out.println("Se cargó el archivo: " + fileName);
            } else {
                System.err.println("Error al cargar el archivo: " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

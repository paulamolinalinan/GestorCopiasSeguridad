package org.example;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GestorFTP {

    public static void main(String[] args) {
        try {
            // Solicitar el nombre de la carpeta al usuario
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Ingrese el nombre de la carpeta a comprimir y subir: ");
            String folderName = reader.readLine();

            // Comprimir la carpeta en formato ZIP
            String compressedFileName = compressFolder(folderName);

            // Subir el archivo comprimido al servidor FTP
            uploadToFTP(compressedFileName);

            System.out.println("Proceso completado con éxito.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Método para comprimir la carpeta en formato ZIP
     * @param folderName, nombre de la carpeta que quiero comprimir
     * @return carpeta comprimida
     * @throws IOException
     */
    private static String compressFolder(String folderName) throws IOException {
        Path folderPath = Paths.get(folderName);
        String compressedFileName = getTimestampedFileName(folderPath.getFileName().toString(), "zip");

        try (
                FileOutputStream fileOutputStream = new FileOutputStream(compressedFileName);
                ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)
        ) {
            // Llamar al método para agregar archivos y directorios al ZIP
            addFilesToZip(zipOutputStream, folderPath.toFile(), "");
        }

        return compressedFileName;
    }

    /**
     * Método para agregar archivos y directorios al ZIP
     * @param zipOutputStream
     * @param file
     * @param parent
     * @throws IOException
     */
    private static void addFilesToZip(ZipOutputStream zipOutputStream, File file, String parent) throws IOException {
        // Crear el nombre de entrada para el archivo o directorio dentro del ZIP
        String entryName = parent + file.getName();
        ZipEntry zipEntry = new ZipEntry(entryName);
        zipOutputStream.putNextEntry(zipEntry);

        // Verificar si es un archivo y agregar su contenido al ZIP
        if (file.isFile()) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                // Leer y escribir el contenido del archivo en el ZIP
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    zipOutputStream.write(buffer, 0, bytesRead);
                }
            }
            // Si es un directorio, recorrer sus archivos y directorios internos
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    // Llamar recursivamente al método para cada archivo/directorio interno
                    addFilesToZip(zipOutputStream, child, entryName + "/");
                }
            }
        }
        // Cerrar el ZIP actual
        zipOutputStream.closeEntry();
    }

    /**
     * Método para subir el archivo al servidor FTP
     * @param fileName
     * @throws IOException
     */
    private static void uploadToFTP(String fileName) throws IOException {
        FTPClient ftpClient = new FTPClient();
        FileInputStream fileInputStream = null;

        try {
            // Conectarse al servidor FTP
            ftpClient.connect("127.0.0.1", 21);
            ftpClient.login("paula", "paula2003");
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Crear un flujo de entrada para el archivo a subir
            fileInputStream = new FileInputStream(fileName);

            // Subir el archivo al servidor FTP
            boolean uploaded = ftpClient.storeFile(fileName, fileInputStream);

            if (uploaded) {
                System.out.println("Archivo subido exitosamente al servidor FTP.");
            } else {
                System.out.println("Error al subir el archivo al servidor FTP.");
            }
        } finally {
            // Cerrar el flujo de entrada y desconectarse del servidor FTP
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }

    /**
     * Método para obtener un nombre de archivo con una marca de tiempo
     * @param baseName
     * @param extension
     * @return
     */
    private static String getTimestampedFileName(String baseName, String extension) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        return baseName + "_" + timestamp + "." + extension;
    }
}

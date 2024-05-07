package edu.wpi.first.gradlerio.deploy;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.OutputFile;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;

public class ZipSourcesTask extends DefaultTask {
    private String m_buildDirectory;
    private String m_projectDirectory;
    private FileOutputStream m_outFOS;

    public void setBuildDirectory(String buildDirectory) {
        m_buildDirectory = buildDirectory;
    }

    public void setProjectDirectory(String projectDirectory) {
        m_projectDirectory = projectDirectory;
    }

    @TaskAction
    public void execute() throws IOException {
        m_outFOS = new FileOutputStream(m_buildDirectory + "/sources.zip");
        ZipOutputStream zipOut = new ZipOutputStream(m_outFOS);

        File fileToZip = new File(m_projectDirectory);
        zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        m_outFOS.close();
    }

    @OutputFile
    public FileOutputStream getSourcesZip() {
        return m_outFOS;
    };

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                if (children.toString() != "build") {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
}

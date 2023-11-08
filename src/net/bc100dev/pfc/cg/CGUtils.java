package net.bc100dev.pfc.cg;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;

class CGUtils {

    public static String readFile(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("Cannot read a file due to the File object being null");

        if (!file.exists())
            throw new FileNotFoundException(String.format("File at \"%s\" does not exist", file.getAbsolutePath()));

        if (!file.canRead())
            throw new AccessDeniedException(String.format("Cannot read a file at \"%s\" (Permission denied)", file.getAbsolutePath()));

        FileInputStream fis = new FileInputStream(file);
        byte[] buff = fis.readAllBytes();
        fis.close();

        return new String(buff);
    }

    public static void writeFile(File file, String data) throws IOException {
        if (file == null)
            throw new NullPointerException("Cannot read a file due to the File object being null");

        if (!file.exists())
            throw new FileNotFoundException(String.format("File at \"%s\" does not exist", file.getAbsolutePath()));

        if (!file.canRead())
            throw new AccessDeniedException(String.format("Cannot write to a file at \"%s\" (Permission denied)", file.getAbsolutePath()));

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data.getBytes());
        fos.close();
    }

    public static void appendWriteFile(File file, String data) throws IOException {
        if (file == null)
            throw new NullPointerException("Cannot read a file due to the File object being null");

        if (!file.exists())
            throw new FileNotFoundException(String.format("File at \"%s\" does not exist", file.getAbsolutePath()));

        if (!file.canRead())
            throw new AccessDeniedException(String.format("Cannot write to a file at \"%s\" (Permission denied)", file.getAbsolutePath()));

        FileOutputStream fos = new FileOutputStream(file, true);
        fos.write(data.getBytes());
        fos.close();
    }

    private static String[] readMountsFile() throws IOException {
        return readFile(new File("/proc/mounts")).split("\n");
    }

    public static Path findCgMountPath() throws IOException {
        String[] lines = readMountsFile();
        for (String line : lines) {
            String[] splits = line.split(" ", 6);
            String device = splits[0];
            Path path = new File(splits[1]).toPath();
            String fs = splits[2];

            if (fs.contains("cgroup") || device.contains("cgroup"))
                return path;
        }

        return null;
    }

    public static Path findCgMountPath(boolean v2) throws IOException {
        String[] lines = readMountsFile();
        for (String line : lines) {
            String[] splits = line.split(" ", 6);
            String device = splits[0];
            Path path = new File(splits[1]).toPath();
            String fs = splits[2];

            if (fs.contains("cgroup") || device.contains("cgroup")) {
                if (v2) {
                    if (fs.contains("cgroup2") || device.contains("cgroup2"))
                        return path;
                } else
                    return path;
            }
        }

        return null;
    }

}

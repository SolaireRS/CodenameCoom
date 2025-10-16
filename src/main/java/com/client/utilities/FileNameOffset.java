package com.client.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameOffset {

    public static void main(String[] args) {
        // Input directory path here
        String directoryPath = "D:\\Server Tools\\suics editor\\savedmodels\\";
        int offset = 20000; // Offset value

        try {
            File dir = new File(directoryPath);
            if (!dir.exists() || !dir.isDirectory()) {
                System.out.println("Invalid directory path.");
                return;
            }

            // Process files in the directory
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    String newName = offsetFileName(file.getName(), offset);
                    if (newName != null) {
                        Path source = file.toPath();
                        Path target = new File(dir, newName).toPath();
                        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Renamed: " + file.getName() + " -> " + newName);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error while processing files: " + e.getMessage());
        }
    }

    private static String offsetFileName(String fileName, int offset) {
        // Regex to match numeric parts of the file name
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(fileName);
        StringBuilder newFileName = new StringBuilder();

        int lastMatchEnd = 0;
        boolean modified = false;

        while (matcher.find()) {
            // Append non-matching part
            newFileName.append(fileName, lastMatchEnd, matcher.start());

            // Offset the numeric value
            int number = Integer.parseInt(matcher.group());
            newFileName.append(number + offset);
            lastMatchEnd = matcher.end();
            modified = true;
        }

        // Append the remaining part of the file name
        newFileName.append(fileName.substring(lastMatchEnd));

        return modified ? newFileName.toString() : null;
    }
}
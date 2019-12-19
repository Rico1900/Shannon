package edu.nju.seg.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

/**
 * golden hammer
 */
public class $ {

    /**
     * read content as string from the file
     * @param f the target file
     * @return maybe the content
     */
    public static Optional<String> readContent(File f) {
        try {
            String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            return Optional.of(content);
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        return Optional.empty();
    }

}

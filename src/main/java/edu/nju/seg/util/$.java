package edu.nju.seg.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     * check if the string is blank
     * @param str the string
     * @return if the string is blank
     */
    public static boolean isBlank(String str) {
        return str == null || str.equals("");
    }

    /**
     * filter blank string
     * @param list the string list
     * @return the list without blank string
     */
    public static List<String> filterStrList(List<String> list) {
        return list.stream()
                .filter(s -> !isBlank(s))
                .collect(Collectors.toList());
    }

}

package edu.nju.seg;


import edu.nju.seg.data.ConfigReader;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.Parser;
import edu.nju.seg.parser.ParserDispather;
import edu.nju.seg.parser.UMLetParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Lab {

    public static void main(String[] args) {
        String configPath = System.getProperty("EXPERIMENTAL_CONFIG");
        if (configPath == null) {
            System.err.println("no experimental protocol");
            return;
        }
        ConfigReader reader = new ConfigReader(configPath);
        Optional<ExperimentConfig> maybe = reader.getConfig();
        maybe.ifPresent(Lab::runExperiment);
    }

    /**
     * parse uxf files and run the experiments
     * @param config experiment configuration
     */
    private static void runExperiment(ExperimentConfig config) {
        String inputPath = config.getInputFolder();
        File inputFolder = new File(inputPath);
        if (inputFolder.isDirectory()) {
            List<Diagram> diagrams = new ArrayList<>();
            for (File f : Objects.requireNonNull(inputFolder.listFiles())) {
                UMLetParser.parseElement(f)
                        .map(contents -> Lab.parseDiagram(f.getName(), contents))
                        .ifPresent(diagrams::add);
            }
            System.out.println(diagrams);
        } else {
            System.err.println("the input path is not a directory");
        }
    }

    /**
     * parse the diagram according to the given parser
     * @param fileName the uxf file name
     * @param content the element list
     * @return the structure diagram
     */
    private static Diagram parseDiagram(String fileName, List<Element> content) {
        Parser p = ParserDispather.dispatch(fileName, content);
        return p.parse();
    }

}

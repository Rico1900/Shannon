package edu.nju.seg;


import edu.nju.seg.data.ConfigReader;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.SequenceParser;
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

    private static void runExperiment(ExperimentConfig config) {
        String inputPath = config.getInputFolder();
        File inputFolder = new File(inputPath);
        List<SequenceDiagram> sd = new ArrayList<>();
        if (inputFolder.isDirectory()) {
            for (File f : Objects.requireNonNull(inputFolder.listFiles())) {
                UMLetParser.parseElement(f)
                        .ifPresent(contents -> Lab.parserDispatch(contents, sd));
            }
        } else {
            System.err.println("the input path is not a directory");
        }
    }

    /**
     * dispatch the contents to the appropriate parser
     * @param contents the UMLet model language
     * @param sequenceDiagrams the sequence diagram list
     */
    private static void parserDispatch(List<ElementContent> contents,
                                       List<SequenceDiagram> sequenceDiagrams) {
        if (isSequenceDiagram(contents)) {
            SequenceParser parser = new SequenceParser(contents.get(0), contents.get(1));
            sequenceDiagrams.add(parser.parseSequenceDiagram());
        } else {

        }
    }

    /**
     * check if contents belong to a sequence diagram; this method has effects
     * @param contents the contents list parsed from XML file
     * @return if contents belong to a sequence diagram
     */
    private static boolean isSequenceDiagram(List<ElementContent> contents) {
        if (contents.size() == 2) {
            if (contents.get(0).getType() == UMLType.UMLNote
                    && contents.get(1).getType() == UMLType.UMLSequenceAllInOne) {
                return true;
            } else if (contents.get(0).getType() == UMLType.UMLSequenceAllInOne
                    && contents.get(1).getType() == UMLType.UMLNote) {
                ElementContent temp = contents.get(0);
                contents.set(0, contents.get(1));
                contents.set(1, temp);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}

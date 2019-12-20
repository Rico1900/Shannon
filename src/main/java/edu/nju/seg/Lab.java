package edu.nju.seg;


import edu.nju.seg.data.ConfigReader;
import edu.nju.seg.model.ExperimentConfig;
import edu.nju.seg.parser.UMLetParser;

import java.io.File;
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
        if (inputFolder.isDirectory()) {
            for (File f : Objects.requireNonNull(inputFolder.listFiles())) {
                UMLetParser.parseElement(f);
            }

        } else {
            System.err.println("the input path is not a directory");
        }
    }

}

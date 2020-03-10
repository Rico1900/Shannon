package edu.nju.seg;


import edu.nju.seg.data.ConfigReader;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.Parser;
import edu.nju.seg.parser.ParserDispatcher;
import edu.nju.seg.parser.UMLetParser;
import edu.nju.seg.solver.AutomatonEncoder;
import edu.nju.seg.solver.SequenceEncoder;
import edu.nju.seg.solver.SolverManager;

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
        maybe.ifPresent(Lab::run);
    }

    private static void run(ExperimentConfig config) {
        prepareExperiment(config);
    }

    /**
     * parse uxf files
     * @param config experiment configuration
     */
    private static void prepareExperiment(ExperimentConfig config) {
        String inputPath = config.getInputFolder();
        File inputFolder = new File(inputPath);
        if (inputFolder.isDirectory()) {
            List<Diagram> diagrams = new ArrayList<>();
            for (File f : Objects.requireNonNull(inputFolder.listFiles())) {
                UMLetParser.parseElement(f)
                        .map(contents -> Lab.parseDiagram(f.getName(), contents))
                        .ifPresent(diagrams::add);
            }
            runExperiment(diagrams, config);
        } else {
            System.err.println("the input path is not a directory");
        }
    }

    /**
     * run the experiment
     * @param diagrams parsed diagrams
     */
    private static void runExperiment(List<Diagram> diagrams, ExperimentConfig config) {
        SolverManager manager = new SolverManager();
        for (Diagram d: diagrams) {
//            if (d instanceof AutomatonDiagram && d.getTitle().equals("Task1")) {
//                AutomatonEncoder encoder = new AutomatonEncoder((AutomatonDiagram) d,
//                        manager, config.getBound());
//                encoder.encode();
//            }
            if (d instanceof SequenceDiagram) {
                SequenceEncoder encoder = new SequenceEncoder((SequenceDiagram) d,
                        manager, config.getBound());
                encoder.encode();
            }
        }
        System.out.println(manager.check());
        System.out.println();
//        System.out.println(manager.getModel());
        System.out.println(manager.getEventTrace(true));
        System.out.println();
    }

    /**
     * parse the diagram according to the given parser
     * @param fileName the uxf file name
     * @param content the element list
     * @return the structure diagram
     */
    private static Diagram parseDiagram(String fileName, List<Element> content) {
        Parser p = ParserDispatcher.dispatch(fileName, content);
        return p.parse();
    }

}

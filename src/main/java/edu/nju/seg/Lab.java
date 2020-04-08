package edu.nju.seg;


import edu.nju.seg.config.ExperimentConfig;
import edu.nju.seg.data.ConfigReader;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.metric.SimpleTimer;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.Parser;
import edu.nju.seg.parser.ParserDispatcher;
import edu.nju.seg.parser.UMLetParser;
import edu.nju.seg.solver.AutomatonEncoder;
import edu.nju.seg.solver.SequenceEncoder;
import edu.nju.seg.solver.SolverManager;
import edu.nju.seg.solver.TASSATEncoder;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;

public class Lab {

    public static void main(String[] args)
    {
        String configPath = System.getProperty("EXPERIMENTAL_CONFIG");
        if (configPath == null) {
            System.err.println("no experimental protocol");
            return;
        }
        ConfigReader reader = new ConfigReader(configPath);
        Optional<ExperimentConfig> maybe = reader.getConfig();
        maybe.ifPresent(Lab::run);
    }

    private static void run(ExperimentConfig config)
    {
        prepareExperiment(config);
    }

    /**
     * parse uxf files
     * @param config experiment configuration
     */
    private static void prepareExperiment(ExperimentConfig config)
    {
        String inputPath = config.getInputFolder();
        File inputFolder = new File(inputPath);
        if (inputFolder.isDirectory()) {
            List<Diagram> diagrams = new ArrayList<>();
            for (File f : Objects.requireNonNull(inputFolder.listFiles())) {
                UMLetParser.parseElement(f)
                        .map(contents -> Lab.parseDiagram(f.getName(), contents))
                        .ifPresent(diagrams::add);
            }
            switch (config.getType()) {
                case ISD_SMT:
                    runISDSMT(diagrams, config);
                    break;
                case TASSAT_SMT:
                    runTASSAT(diagrams, config);
                    break;
                case SD_AUTOMATA:
                    runSD_Automata(diagrams, config);
                    break;
            }
        } else {
            System.err.println("the input path is not a directory");
        }
    }

    /**
     * run the experiment
     * @param diagrams parsed diagrams
     */
    private static void runISDSMT(List<Diagram> diagrams,
                                  ExperimentConfig config)
    {
        try {
            SolverManager manager = new SolverManager();
            for (Diagram d: diagrams) {
                if (d instanceof SequenceDiagram) {
                    SequenceEncoder encoder = new SequenceEncoder((SequenceDiagram) d,
                            manager, config.getBound());
                    encoder.encode();
                }
            }
            System.out.println(manager.check());
            System.out.println();
            System.out.println(manager.getEventTrace(false));
            System.out.println();
        } catch (Z3Exception e) {
            System.out.println(e.toString());
        }
    }

    private static void runSD_Automata(List<Diagram> diagrams,
                                       ExperimentConfig config)
    {
        try {
            SolverManager manager = new SolverManager();
            // TODO:
            for (Diagram d: diagrams) {
                if (d instanceof AutomatonDiagram && d.getTitle().equals("Task1")) {
                    AutomatonEncoder encoder = new AutomatonEncoder((AutomatonDiagram) d,
                            manager, config.getBound());
                    encoder.encode();
                }
            }
            System.out.println(manager.check());
            System.out.println();
        } catch (Z3Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * run TASSAT SMT verification
     * @param diagrams diagrams
     * @param config the experimental config
     */
    private static void runTASSAT(List<Diagram> diagrams,
                                  ExperimentConfig config)
    {
        try {
            SolverManager manager = new SolverManager();
            TASSATEncoder encoder = new TASSATEncoder(diagrams, config.getBound(),
                    config.getTargets(), manager);
            encoder.encode();
            System.out.println("============================");
            System.out.println("start TASSAT SMT check");
            System.out.println("bound: " + config.getBound());
            SimpleTimer t = new SimpleTimer();
            System.out.println(manager.check());
            System.out.println("verification costs: " + t.pastSeconds() + " s");
//            System.out.println(manager.getProof());
//            System.out.println(manager.getModel());
            System.out.println("============================");
        } catch (Z3Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * parse the diagram according to the given parser
     * @param fileName the uxf file name
     * @param content the element list
     * @return the structure diagram
     */
    private static Diagram parseDiagram(String fileName, List<Element> content)
    {
        Parser p = ParserDispatcher.dispatch(fileName, content);
        return p.parse();
    }

}

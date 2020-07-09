package edu.nju.seg;


import com.microsoft.z3.Status;
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
import edu.nju.seg.solver.tassat.TASSATEncoder;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.SimpleLog;

import java.io.File;
import java.util.*;

public class Lab {

    public static void main(String[] args)
    {
        String configPath = System.getProperty("EXPERIMENTAL_CONFIG");
        if (configPath == null) {
            SimpleLog.error("no experimental protocol");
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
                case AUTOMATON_SMT:
                    runAutomatonSMT(diagrams, config);
                    break;
                case ISD_AUTOMATA_OPT:
                    runScenarioOptimization(diagrams, config);
                    break;
            }
        } else {
            SimpleLog.error("the input path is not a directory");
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
            if (diagrams.size() == 1) {
                Diagram d = diagrams.get(0);
                if (d instanceof SequenceDiagram) {
                    SequenceEncoder encoder = new SequenceEncoder((SequenceDiagram) d, manager,
                            config.getType(), config.getBound());
                    encoder.encode();
                } else {
                    SimpleLog.error("wrong diagram type for ISD SMT verification");
                }
            } else {
                SimpleLog.error("ISD SMT verification only supports single diagram");
            }
            System.out.println(manager.check());
            System.out.println();
            System.out.println(manager.getEventTrace(false));
            System.out.println();
        } catch (Z3Exception e) {
            logZ3Exception(e);
        }
    }

    private static void runAutomatonSMT(List<Diagram> diagrams,
                                        ExperimentConfig config)
    {
        try {
            SolverManager manager = new SolverManager();
            if (diagrams.size() == 1) {
                Diagram d = diagrams.get(0);
                if (d instanceof AutomatonDiagram) {
                    AutomatonEncoder encoder = new AutomatonEncoder((AutomatonDiagram) d,
                            manager, config.getBound());
                    encoder.encode();
                }
            } else {
                SimpleLog.error("Automaton SMT verification only support single diagram");
            }
            System.out.println(manager.check());
            System.out.println();
        } catch (Z3Exception e) {
            logZ3Exception(e);
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
            logZ3Exception(e);
        }
    }

    /**
     * scenario-based optimization,
     * a canonical scenario including an ISD and several automaton
     * @param diagrams diagrams
     * @param config the experimental config
     */
    private static void runScenarioOptimization(List<Diagram> diagrams,
                                                ExperimentConfig config)
    {
        try {
            SolverManager manager = new SolverManager();
            Pair<SequenceDiagram, List<AutomatonDiagram>> p = partition(diagrams);
            SequenceEncoder encoder = new SequenceEncoder(p.getLeft(), manager,
                    config.getType(), config.getBound());
            encoder.encode();
            encoder.encodeAutomata(p.getRight());
            encoder.delegateToManager();
            Status result = manager.check();
            handleResult(result, manager);
        } catch (Z3Exception e) {
           logZ3Exception(e);
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

    private static Pair<SequenceDiagram, List<AutomatonDiagram>> partition(List<Diagram> diagrams)
    {
        Pair<SequenceDiagram, List<AutomatonDiagram>> p = new Pair<>();
        List<AutomatonDiagram> list = new ArrayList<>();
        for (Diagram d: diagrams) {
            if (d instanceof SequenceDiagram) {
                p.setLeft((SequenceDiagram) d);
            } else if (d instanceof AutomatonDiagram) {
                list.add((AutomatonDiagram) d);
            } else {
                SimpleLog.error("wrong diagram type for partition");
            }
        }
        p.setRight(list);
        return p;
    }

    /**
     * handle z2 exception log
     * @param e exception
     */
    private static void logZ3Exception(Z3Exception e)
    {
        SimpleLog.error(e.toString());
    }

    /**
     * handle z3 result
     * @param result z3 status
     * @param manager z3 manager
     */
    private static void handleResult(Status result, SolverManager manager)
    {
        System.out.println(result);
        if (result.toInt() == 0) {
            System.out.println(manager.getProof());
        } else {
            System.out.println(manager.getEventTrace(true));
        }
    }

}

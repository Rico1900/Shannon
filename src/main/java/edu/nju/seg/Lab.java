package edu.nju.seg;


import com.microsoft.z3.Status;
import edu.nju.seg.config.ExperimentConfig;
import edu.nju.seg.data.ConfigReader;
import edu.nju.seg.encoder.verification.VerificationEncoder;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.metric.ExperimentalData;
import edu.nju.seg.metric.SimpleTimer;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.DiagramParser;
import edu.nju.seg.parser.ParserDispatcher;
import edu.nju.seg.parser.UMLetTokenizer;
import edu.nju.seg.encoder.*;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.SimpleLog;

import java.io.File;
import java.util.*;

public class Lab {

    private final ExperimentConfig config;

    public Lab(ExperimentConfig config) {
        this.config = config;
    }

    /**
     * start the laboratory
     */
    private void run()
    {
        List<Diagram> diagrams = prepare_experiment();
        dispatch_experiment(diagrams);
    }

    /**
     * prepare the experimental data
     */
    private List<Diagram> prepare_experiment()
    {
        String inputPath = config.get_input_folder();
        File inputFolder = new File(inputPath);
        if (inputFolder.isDirectory()) {
            List<Diagram> diagrams = new ArrayList<>();
            for (File f : Objects.requireNonNull(inputFolder.listFiles())) {
                UMLetTokenizer.tokenize_elements(f)
                        .map(contents -> parse_diagram(f.getName(), contents))
                        .ifPresent(diagrams::add);
            }
            return diagrams;
        } else {
            SimpleLog.error("the input path is not a directory");
            return new ArrayList<>(0);
        }
    }

    /**
     * dispatch the experiment according to the experimental type
     * @param diagrams the diagram list
     */
    private void dispatch_experiment(List<Diagram> diagrams)
    {
        switch (config.get_type()) {
            case ISD_AUTOMATA_VERIFICATION:
                run_scenario_verification(diagrams);
                break;
            default:
                throw new EncodeException("");
        }
    }



    /**
     * scenario-based optimization,
     * a canonical scenario including an ISD and several automaton
     * @param diagrams diagrams
     */
    private void run_scenario_verification(List<Diagram> diagrams)
    {
        try {
            SolverManager manager = new SolverManager();
            Pair<SequenceDiagram, List<AutomatonDiagram>> p = partition(diagrams);
            VerificationEncoder ve = new VerificationEncoder(
                    p.get_left(), p.get_right(), config.get_bound(), manager);
            manager.add_clause(ve.encode());
            ExperimentalData data = new ExperimentalData(config.get_bound(), manager.get_clause_num());
            SimpleTimer timer = new SimpleTimer();
            Status result = manager.check();
            data.set_running_time(timer.past_seconds());
            System.out.println(data.toString());
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
    private Diagram parse_diagram(String fileName, List<Element> content)
    {
        DiagramParser p = ParserDispatcher.dispatch_uxf(fileName, content);
        return p.parse();
    }

    /**
     * partition a group of diagrams
     * @param diagrams the diagram list
     * @return a tuple consisted of diagrams
     */
    private Pair<SequenceDiagram, List<AutomatonDiagram>> partition(List<Diagram> diagrams)
    {
        SequenceDiagram sd = null;
        List<AutomatonDiagram> list = new ArrayList<>();
        for (Diagram d: diagrams) {
            if (d instanceof SequenceDiagram) {
                sd = (SequenceDiagram) d;
            } else if (d instanceof AutomatonDiagram) {
                list.add((AutomatonDiagram) d);
            } else {
                SimpleLog.error("wrong diagram type for partition");
            }
        }
        return new Pair<>(sd, list);
    }

    /**
     * handle z3 exception log
     * @param e exception
     */
    private void logZ3Exception(Z3Exception e)
    {
        SimpleLog.error(e.toString());
    }

    /**
     * handle z3 result
     * @param result z3 status
     * @param manager z3 manager
     */
    private void handleResult(Status result, SolverManager manager)
    {
        System.out.println(result);
        if (result == Status.SATISFIABLE) {
            manager.print_automata_trace();
//            manager.print_variables();
        } else if (result == Status.UNSATISFIABLE) {
            manager.print_proof();
        } else {
            System.out.println("Solver result Unknown");
        }
    }

    public static void main(String[] args)
    {
        String configPath = System.getProperty("EXPERIMENTAL_CONFIG");
        if (configPath == null) {
            SimpleLog.error("no experimental protocol");
            return;
        }
        ConfigReader reader = new ConfigReader(configPath);
        Optional<ExperimentConfig> maybe = reader.getConfig();
        maybe.ifPresent(c -> {
            Lab l = new Lab(c);
            l.run();
        });
    }

}

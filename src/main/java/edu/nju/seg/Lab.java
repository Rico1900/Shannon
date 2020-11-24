package edu.nju.seg;


import com.microsoft.z3.Status;
import edu.nju.seg.config.ExCase;
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

    private final boolean debug;

    private final ExperimentConfig config;

    public Lab(ExperimentConfig config) {
        this.config = config;
        debug = config.is_debug();
    }

    /**
     * start the laboratory
     */
    private void run()
    {
        List<Pair<ExCase, List<Diagram>>> cases = prepare_experiment();
        dispatch_experiment(cases);
    }

    /**
     * prepare the experimental data
     */
    private List<Pair<ExCase, List<Diagram>>> prepare_experiment()
    {
        List<Pair<ExCase, List<Diagram>>> cases = new ArrayList<>();
        for (ExCase ec: config.get_cases()) {
            if (ec.is_runable()) {
                String input_path = config.get_base_path() + ec.get_input_folder();
                File input_folder = new File(input_path);
                if (input_folder.isDirectory()) {
                    List<Diagram> diagrams = new ArrayList<>();
                    for (File f : Objects.requireNonNull(input_folder.listFiles())) {
                        if (f.getName().endsWith(".uxf")) {
                            UMLetTokenizer.tokenize_elements(f)
                                    .map(contents -> parse_diagram(f.getName(), contents))
                                    .ifPresent(diagrams::add);
                        }
                    }
                    cases.add(new Pair<>(ec, diagrams));
                } else {
                    SimpleLog.error("the input path is not a directory: " + input_path);
                }
            }
        }
        return cases;
    }

    /**
     * dispatch the experiment according to the experimental type
     * @param cases the case list
     */
    private void dispatch_experiment(List<Pair<ExCase, List<Diagram>>> cases)
    {
        switch (config.get_type()) {
            case ISD_AUTOMATA_VERIFICATION:
                run_scenario_verification(cases);
                break;
            default:
                throw new EncodeException("");
        }
    }



    /**
     * scenario-based optimization,
     * a canonical scenario including an ISD and several automaton
     * @param cases diagrams
     */
    private void run_scenario_verification(List<Pair<ExCase, List<Diagram>>> cases)
    {
        for (Pair<ExCase, List<Diagram>> c: cases) {
            try {
                SolverManager manager = new SolverManager();
                Pair<SequenceDiagram, List<AutomatonDiagram>> p = partition(c.get_right());
                VerificationEncoder ve = new VerificationEncoder(
                        p.get_left(), p.get_right(), config.get_bound(), manager);
                SimpleTimer encoding_timer = new SimpleTimer();
                manager.add_clause(ve.encode());
                double encoding_time = encoding_timer.past_seconds();
                ExperimentalData data = new ExperimentalData(
                        c.get_left().get_input_folder(),
                        config.get_bound(),
                        manager.get_clause_num());
                data.set_encoding_time(encoding_time);
                SimpleTimer running_timer = new SimpleTimer();
                Status result = manager.check();
                data.set_running_time(running_timer.past_seconds());
                handle_result(result, data, manager);
            } catch (Z3Exception e) {
                logZ3Exception(e);
            }
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
    private void handle_result(Status result, ExperimentalData data, SolverManager manager)
    {
        System.out.println(data.toString());
        System.out.println(result);
        if (result == Status.SATISFIABLE) {
            if (debug) {
                manager.print_automata_trace();
                manager.print_variables();
            }
        } else if (result == Status.UNSATISFIABLE) {
            if (debug) {
                manager.print_proof();
            }
        } else {
            System.out.println("Solver result Unknown");
        }
        System.out.println();
        System.out.println();
        System.out.println();
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

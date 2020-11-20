package edu.nju.seg.encoder.verification;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.RealExpr;
import edu.nju.seg.encoder.ExpressionEncoder;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.expression.*;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.EquationParser;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.Z3Wrapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * the loc_i represents the diagram location before the ith action happens;
 * loc_num = action_num + 1
 */
public class LocalAutomatonEncoder {

    private final AutomatonDiagram diagram;

    private final List<Message> timeline;

    private final Map<Set<String>, Judgement> const_dict;

    private final Map<Set<String>, Judgement> prop_dict;

    private final int bound;

    private final int seq_index;

    private final Z3Wrapper w;

    private final ExpressionEncoder ee;

    private final Map<String, Relation> edge_map;

    public LocalAutomatonEncoder(AutomatonDiagram d,
                                 List<Message> timeline,
                                 Map<Set<String>, Judgement> const_dict,
                                 Map<Set<String>, Judgement> prop_dict,
                                 Z3Wrapper w,
                                 int bound,
                                 int seq_index)
    {
        this.diagram = d;
        this.w = w;
        this.ee = new ExpressionEncoder(w);
        this.bound = bound;
        this.seq_index = seq_index;
        this.timeline = timeline;
        this.const_dict = const_dict;
        this.prop_dict = prop_dict;
        this.edge_map = diagram.get_relations().stream()
                .collect(Collectors.toMap(Relation::get_name, Function.identity()));
    }

    public Pair<Optional<BoolExpr>, Optional<BoolExpr>> encode()
    {
        if ($.isBlankList(timeline)) {
            return new Pair<>(Optional.empty(), Optional.empty());
        }
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(encode_init());
        // encode constraints
        for (Judgement j: const_dict.values()) {
            exprs.add(encode_property(j));
        }
        // encode init to the first edge segment
        List<BoolExpr> init_edge = new ArrayList<>();
        for (Relation ie: diagram.get_initial().getOuters()) {
            List<BoolExpr> subs = new ArrayList<>();
            subs.add(encode_single_jump(0, ie));
            for (int i = 1; i < 1 + bound; i++) {
                for (State s: diagram.get_states_exclude_initial()) {
                    exprs.add(encode_transit(i, s));
                }
            }
            init_edge.add(w.mk_and_not_empty(subs));
        }
        exprs.add(w.mk_or_not_empty(init_edge));
        for (int i = 0; i < timeline.size() - 1; i++) {
            Message start = timeline.get(i);
            Message end = timeline.get(i + 1);
            exprs.add(encode_segment(start, end, i + 1));
        }
        exprs.add(encode_message(timeline.get(timeline.size() - 1), calculate_offset(timeline.size())));
        return new Pair<>(Optional.of(w.mk_and_not_empty(exprs)), encode_properties());
    }

    private Optional<BoolExpr> encode_properties()
    {
        List<BoolExpr> exprs = new ArrayList<>();
        List<Judgement> properties = diagram.get_properties();
        if (properties.size() == 0 && prop_dict.size() == 0) {
            return Optional.empty();
        }
        for (Judgement j: properties) {
            exprs.add(encode_property(j.mark_seq_index(seq_index)));
        }
        for (Judgement j: prop_dict.values()) {
            exprs.add(encode_property(j.mark_seq_index(seq_index)));
        }
        return Optional.of(w.mk_and_not_empty(exprs));
    }

    private BoolExpr encode_property(Judgement j)
    {
        if (j instanceof AdJudgement) {
            AdJudgement aj = (AdJudgement) j;
            if (aj.get_qualifier() == UnaryOp.FORALL) {
                List<BoolExpr> subs = new ArrayList<>();
                for (int i = 0; i < calculate_total_index(); i++) {
                    subs.add(ee.encode_judgement(aj.attach_bound(i)));
                }
                return w.mk_and_not_empty(subs);
            } else if (aj.get_qualifier() == UnaryOp.EXISTS) {
                List<BoolExpr> subs = new ArrayList<>();
                for (int i = 0; i < calculate_total_index(); i++) {
                    subs.add(ee.encode_judgement(aj.attach_bound(i)));
                }
                return w.mk_or_not_empty(subs);
            } else {
                throw new EncodeException("Wrong AdJudgement: " + aj.toString());
            }
        } else {
            return ee.encode_judgement(j);
        }
    }

    /**
     * encode synchronized message,
     * assignments of the jumping edge will be rewritten according to the synchronized message
     * @param m the synchronized message
     * @param index the
     * @return the boolean expression
     */
    private BoolExpr encode_message(Message m, int index)
    {
        Relation r = edge_map.get(m.get_name());
        // if the automaton is the target of the synchronous message
        if (m.get_target().get_name().equals(diagram.get_title())) {
            State source = r.get_source();
            State target = r.get_target();
            List<BoolExpr> exprs = new ArrayList<>();
            exprs.add(encode_time_unchanged(index));
            exprs.add(encode_loc_info(index, source));
            exprs.add(encode_loc_info(index + 1, target));
            exprs.add(encode_time_of_message_until_now(m, index));
            // encode jump condition
            r.get_guards().stream()
                    .map(g -> ee.encode_judgement_with_index(g.mark_seq_index(seq_index), index))
                    .forEach(exprs::add);
            Set<String> vars = diagram.get_var_str();
            List<Assignment> assignments = exclude_synchronized_assignments(r.get_assignments(), m.get_assignments());
            Set<String> changed_vars = new HashSet<>();
            if ($.isNotBlankList(assignments)) {
                // encode jump assignment
                for (Assignment a: assignments) {
                    changed_vars.add(a.get_left().getName());
                    exprs.add(ee.encode_assignment_with_index(a.mark_seq_index(seq_index), index + 1));
                }
                // encode synchronized assignment
                for (Assignment a: m.get_assignments()) {
                    changed_vars.add(a.get_left().getName());
                    exprs.add(ee.encode_assignment_with_double_index(
                            a.mark_seq_index(seq_index),
                            calculate_offset(m.get_target_index() + 1) + 1,
                            calculate_offset(m.get_source_index() + 1)));
                }
            }
            // encode unchanged variables
            vars.removeAll(changed_vars);
            encode_variable_unchanged(index, vars).ifPresent(exprs::add);
            return w.mk_and_not_empty(exprs);
        // if the automaton is the source of the synchronous message
        } else {
            return w.get_ctx().mkAnd(encode_loc_info(index, r.get_source()), encode_single_jump(index, r));
        }
    }

    private List<Assignment> exclude_synchronized_assignments(List<Assignment> origin,
                                                              List<Assignment> exclude)
    {
        return origin.stream()
                .filter(a -> !is_excluded(a, exclude))
                .collect(Collectors.toList());
    }

    private boolean is_excluded(Assignment a, List<Assignment> exclude)
    {
        for (Assignment e: exclude) {
            if (e.get_left().equals(a.get_left())) {
                return true;
            }
        }
        return false;
    }

    private BoolExpr encode_init()
    {
        State initial = diagram.get_initial();
        List<Relation> outers = initial.getOuters();
        if (outers != null) {
            List<BoolExpr> inits = new ArrayList<>();
            for (Relation r : outers) {
                List<BoolExpr> subs = new ArrayList<>();
                // initial edges only contain assignments, no guards
                r.get_assignments().stream()
                        .map(a -> ee.encode_assignment_with_index(a.mark_seq_index(seq_index), 0))
                        .forEach(subs::add);
                inits.add(w.mk_and_not_empty(subs));
            }
            return w.get_ctx().mkAnd(w.mk_or_not_empty(inits), encode_loc_info(0, initial));
        } else {
            throw new EncodeException("wrong initial assignment");
        }
    }

    /**
     * encode the time of synchronous message
     * @param m the message
     * @param index the index
     * @return the boolean expression when the synchronous message happens
     */
    private BoolExpr encode_time_of_message_until_now(Message m,
                                                      int index)
    {
        List<RealExpr> reals = new ArrayList<>();
        for (int i = 0; i <= index; i++) {
            reals.add(mk_time_var(i));
        }
        return w.mk_eq(mk_synchronous_message_var(m, index), w.sum_reals(reals));
    }

    private BoolExpr encode_single_jump(int index,
                                        Relation r)
    {
        State target = r.get_target();
        BoolExpr time = encode_time_unchanged(index);
        BoolExpr loc = encode_loc_info(index + 1, target);
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(w.get_ctx().mkAnd(time, loc));
        // encode jump condition
        r.get_guards().stream()
                .map(g -> ee.encode_judgement_with_index(g.mark_seq_index(seq_index), index))
                .forEach(exprs::add);
        Set<String> vars = diagram.get_var_str();
        Set<String> changed_vars = new HashSet<>();
        List<Assignment> assignments = r.get_assignments();
        if ($.isNotBlankList(assignments)) {
            // encode jump assignments
            for (Assignment a: assignments) {
                changed_vars.add(a.get_left().getName());
                exprs.add(ee.encode_assignment_with_index(a.mark_seq_index(seq_index), index + 1));
            }
        }
        vars.removeAll(changed_vars);
        // variables remain unchanged
        for (String v: vars) {
            exprs.add(w.mk_eq(mk_var_var(v, index), mk_var_var(v, index + 1)));
        }
        encode_invariant(index + 1, r.get_target()).ifPresent(exprs::add);
        return w.mk_and_not_empty(exprs);
    }

    /**
     * encode the segment according to the bound b,
     * a segment is a transition path, like "edge a -> ... -> edge b"
     * @param start the start state
     * @param end the end state
     * @param seg_index the index of the segment
     * @return the bool expression
     */
    private BoolExpr encode_segment(Message start,
                                    Message end,
                                    int seg_index)
    {
        int offset = calculate_offset(seg_index);
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(encode_message(start, offset));
        List<RealExpr> times = new ArrayList<>();
        for (int i = offset + 1; i < offset + 1 + bound; i++) {
            times.add(mk_time_var(i));
            for (State s: diagram.get_states_exclude_initial()) {
                exprs.add(encode_transit(i, s));
            }
        }
        exprs.add(w.mk_eq(w.sum_reals(times), cal_segment_gap(start, end)));
        return w.mk_and_not_empty(exprs);
    }

    private BoolExpr encode_transit(int i, State s)
    {
        BoolExpr current = encode_loc_info(i, s);
        List<BoolExpr> trans = new ArrayList<>();
        encode_jump(i, s).ifPresent(trans::add);
        trans.add(encode_stutter(i));
        trans.add(encode_timed(i, s));
        return w.get_ctx().mkImplies(current, w.mk_or_not_empty(trans));
    }

    private Optional<BoolExpr> encode_jump(int index,
                                           State s)
    {
        List<BoolExpr> next = new ArrayList<>();
        for (Relation r: s.getOuters()) {
            next.add(encode_single_jump(index, r));
        }
        return w.mk_or(next);
    }

    // We can infer that the stutter transition will not break invariant constraints.
    private BoolExpr encode_stutter(int index)
    {
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(encode_time_unchanged(index));
        exprs.add(w.mk_eq(mk_loc_var(index + 1), mk_loc_var(index)));
        encode_all_variable_unchanged(index). ifPresent(exprs::add);
        return w.mk_and_not_empty(exprs);
    }

    private BoolExpr encode_timed(int index,
                                  State s)
    {
        RealExpr delta = mk_time_var(index);
        List<BoolExpr> exprs = new ArrayList<>();
        // delta time > 0
        exprs.add(w.mk_gt(delta, w.mk_real(0)));
        // the location will not change.
        exprs.add(w.mk_eq(mk_loc_var(index + 1), mk_loc_var(index)));
        Set<String> changed = EquationParser.parse_variables(s.getDeEquations());
        for (DeEquation e: s.getDeEquations()) {
            // add differential equation to the result, for example
            // f'(x) = 1, make sure that x' - x = 1 * delta
            exprs.add(ee.encode_deequation_with_index(e.mark_seq_index(seq_index), index, delta));
        }
        // rest variables remain unchanged
        Set<String> vars = diagram.get_var_str();
        vars.removeAll(changed);
        encode_variable_unchanged(index, vars);
        encode_invariant(index + 1, s).ifPresent(exprs::add);
        return w.mk_and_not_empty(exprs);
    }

    /**
     * encode invariant condition
     * @param index the bound index
     * @param current the current state
     * @return the bool expression that represent the invariant conditions
     */
    private Optional<BoolExpr> encode_invariant(int index, State current)
    {
        if ($.isBlankList(current.getConstraints())) {
            return Optional.empty();
        }
        BoolExpr current_expr = encode_loc_info(index, current);
        List<BoolExpr> invars = current.getConstraints()
                .stream()
                .map(j -> j.mark_seq_index(seq_index))
                .map(j -> j.attach_bound(index))
                .map(ee::encode_judgement)
                .collect(Collectors.toList());
        return Optional.of(w.get_ctx().mkImplies(current_expr, w.mk_and_not_empty(invars)));
    }

    /**
     * encode current location information
     * @param k the index
     * @param current the current state
     * @return current location bool expression
     */
    private BoolExpr encode_loc_info(int k, State current)
    {
        if (current.getType() == StateType.INITIAL) {
            return w.mk_eq(mk_loc_var(k), w.mk_string(diagram.get_title() + "_" + current.getStateName()));
        }
        return w.mk_eq(mk_loc_var(k), w.mk_string(current.getStateName()));
    }

    /**
     * make location symbol
     * @param k the index
     * @return the location symbol
     */
    private Expr mk_loc_var(int k)
    {
        return w.mk_string_var(seq_index + "_" + diagram.get_title() + "_" + "loc_" + k);
    }

    private RealExpr mk_time_var(int k)
    {
        return w.mk_real_var(seq_index + "_" + diagram.get_title() + "_" + "delta_" + k);
    }

    private RealExpr mk_synchronous_message_var(Message m, int k)
    {
        return w.mk_real_var(seq_index + "_" + diagram.get_title() + "_" + m.get_name() + "_" + k);
    }

    /**
     * make variable symbol
     * @param var variable name
     * @param k bound
     * @return the variable symbol
     */
    private RealExpr mk_var_var(String var, int k)
    {
        return w.mk_real_var(seq_index + "_" + var + "_" + k);
    }

    /**
     * calculate total offset for the segment encoding
     * @param seg_index the segment index, start from 0
     * @return the total offset
     */
    private int calculate_offset(int seg_index)
    {
        return seg_index * (bound + 1);
    }

    private int calculate_total_index()
    {
        return timeline.size() * (bound + 1) + 2;
    }

    private BoolExpr encode_time_unchanged(int index)
    {
        return w.mk_eq(mk_time_var(index), w.mk_real(0));
    }

    /**
     * make sure the variables remain unchanged during the specific transition
     * @param index the transition index
     * @return the boolean expression
     */
    private Optional<BoolExpr> encode_all_variable_unchanged(int index)
    {
        Set<String> vars = diagram.get_var_str();
        return encode_variable_unchanged(index, vars);
    }

    private Optional<BoolExpr> encode_variable_unchanged(int index, Set<String> vars)
    {
        if (vars == null) {
            return Optional.empty();
        }
        List<BoolExpr> exprs = new ArrayList<>();
        for (String v: vars) {
            exprs.add(w.mk_eq(mk_var_var(v, index + 1), mk_var_var(v, index)));
        }
        return w.mk_and(exprs);
    }

    private ArithExpr cal_segment_gap(Message s, Message e)
    {
        return w.mk_sub(w.mk_real_var(seq_index + "_" + e.get_name()),
                w.mk_real_var(seq_index + "_" + s.get_name()));
    }

}

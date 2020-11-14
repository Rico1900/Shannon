package edu.nju.seg.encoder.verification;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.SeqExpr;
import edu.nju.seg.encoder.ExpressionEncoder;
import edu.nju.seg.encoder.SolverManager;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.expression.*;
import edu.nju.seg.expression.Number;
import edu.nju.seg.model.*;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.Z3Wrapper;

import java.util.*;
import java.util.stream.Collectors;

public class VerificationEncoder {

    private final SequenceDiagram sd;

    private final List<AutomatonDiagram> ad_list;

    private final int bound;

    private final Z3Wrapper w;

    private final ExpressionEncoder ee;

    private Fragment clean;

    private List<IntFragment> flow;

    private int seq_index;

    private final Map<Set<String>, Judgement> const_dict = new HashMap<>();

    private final Map<Set<String>, Judgement> prop_dict = new HashMap<>();

    private final Map<Set<String>, Judgement> task_const_dict = new HashMap<>();

    private final Map<Set<String>, Judgement> task_prop_dict = new HashMap<>();

    private final SortedMap<Integer, List<IntFragment>> priority_map = new TreeMap<>();

    private final Map<String, IntFragment> mask_map = new HashMap<>();

    private final List<Pair<IntFragment, Integer>> unfold_ints = new ArrayList<>();

    private final List<BoolExpr> property_expr = new ArrayList<>();

    private final List<BoolExpr> synchronous_time_expr = new ArrayList<>();

    private final Set<Seq> seq_set = new HashSet<>();

    private final List<Set<Seq>> int_seq_set_list = new ArrayList<>();

    private final Map<String, AutomatonDiagram> auto_map = new HashMap<>();

    public VerificationEncoder(SequenceDiagram sd,
                               List<AutomatonDiagram> ad_list,
                               int bound,
                               SolverManager manager)
    {
        this.sd = sd;
        this.ad_list = ad_list;
        this.bound = bound;
        this.w = new Z3Wrapper(manager.get_context());
        this.ee = new ExpressionEncoder(w);
        init();
    }

    private void init()
    {
        this.clean = sd.get_clean();
        this.flow = sd.get_flow();
        cal_cons_and_prop();
        cal_priority_map();
        cal_mask_map();
        unfold_int_frags();
        cal_automata_map();
        this.seq_index = 0;
    }

    private void cal_cons_and_prop()
    {
        for (Judgement c: sd.getConstraints()) {
            if (c.is_task_judgement()) {
                task_const_dict.put(c.extract_variables(), c);
            } else {
                const_dict.put(c.extract_variables(), c);
            }
        }
        for (Judgement p: sd.get_properties()) {
            if (p.is_task_judgement()) {
                task_prop_dict.put(p.extract_variables(), p);
            } else {
                prop_dict.put(p.extract_variables(), p);
            }
        }
    }

    private void cal_priority_map()
    {
        for (IntFragment inf: flow) {
            if (priority_map.containsKey(inf.get_priority())) {
                priority_map.get(inf.get_priority()).add(inf);
            } else {
                List<IntFragment> value = new ArrayList<>();
                value.add(inf);
                priority_map.put(inf.get_priority(), value);
            }
        }
    }

    private void cal_mask_map()
    {
        for (IntFragment f: flow) {
            if (f.get_mask_var() != null) {
                mask_map.put(f.get_mask_var(), f);
            }
        }
    }

    private void unfold_int_frags()
    {
        for (IntFragment f: flow) {
            for (int i = 0; i < f.get_max(); i++) {
                unfold_ints.add(new Pair<>(f, i));
            }
        }
    }

    private void cal_automata_map()
    {
        for (AutomatonDiagram ad: ad_list) {
            auto_map.put(ad.get_title(), ad);
        }
    }

    private Map<String, List<Message>> cal_mask_instruction_map(List<SDComponent> children)
    {
        Map<String, List<Message>> map = new HashMap<>();
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                if (m.get_mask_instruction() != null) {
                    Assignment a = m.get_mask_instruction();
                    String v = a.getLeft().getName();
                    if (!map.containsKey(v)) {
                        map.put(v, new ArrayList<>());
                    }
                    map.get(v).add(m);
                }
            }
        }
        return map;
    }

    public BoolExpr encode() throws Z3Exception
    {
        Seq s = new Seq(clean.get_covered());
        List<BoolExpr> exprs = new ArrayList<>();
        BoolExpr clean_expr = encode_clean_frag(clean, new ArrayList<>(), s, true, seq_set);
        exprs.add(clean_expr);
//        encode_int_frags().ifPresent(exprs::add);
        // We add the negation form of property expression to the final SMT expression,
        // so if the final SMT expression is unsatisfied, then verification succeed,
        // otherwise, the SMT solver produces the counter example.
        encode_networks().ifPresent(exprs::add);
        w.mk_and(property_expr).ifPresent(e -> exprs.add(w.mk_not(e)));
        return w.mk_and_not_empty(exprs);
    }

    private Optional<BoolExpr> encode_networks() throws Z3Exception
    {
        List<BoolExpr> exprs = new ArrayList<>();
        // clean traces
        encode_all_seq_on_automata(seq_set).ifPresent(exprs::add);
        // interruption traces
        for (Set<Seq> seqs: int_seq_set_list) {
            encode_all_seq_on_automata(seqs).ifPresent(exprs::add);
        }
        return w.mk_and(exprs);
    }

    private Optional<BoolExpr> encode_all_seq_on_automata(Set<Seq> seqs) throws Z3Exception
    {
        List<BoolExpr> exprs = new ArrayList<>();
        for (Seq s: seqs) {
            encode_synchronous_message(s);
            encode_seq_on_automata(s).ifPresent(exprs::add);
            seq_index += 1;
        }
        return w.mk_or(exprs);
    }

    private Optional<BoolExpr> encode_seq_on_automata(Seq seq) throws Z3Exception
    {
        Map<Instance, List<Message>> traces = seq.get_seq();
        List<BoolExpr> exprs = new ArrayList<>();
        for (Instance key: traces.keySet()) {
            if (auto_map.containsKey(key.get_name())) {
                AutomatonDiagram ad = auto_map.get(key.get_name());
                encode_trace_on_automaton(traces.get(key), ad).ifPresent(exprs::add);
            } else {
                throw new EncodeException("Unmatched instance: " + key.get_name());
            }
        }
        return w.mk_and(exprs);
    }

    private void encode_synchronous_message(Seq seq)
    {
        Set<Message> set = new HashSet<>();
        seq.get_seq().values().forEach(set::addAll);
        for (Message m: set) {
            int from = seq.get_seq().get(m.get_source()).indexOf(m);
            int to = seq.get_seq().get(m.get_target()).indexOf(m);
            synchronous_time_expr.add(w.mk_eq(
                    mk_sync_var(m.get_source(), m, from),
                    mk_sync_var(m.get_target(), m, to)
            ));
        }
    }

    private Optional<BoolExpr> encode_trace_on_automaton(List<Message> trace,
                                                         AutomatonDiagram ad) throws Z3Exception
    {
        Pair<Optional<BoolExpr>, Optional<BoolExpr>> p = new LocalAutomatonEncoder(ad, trace, w, bound).encode();
        p.get_right().ifPresent(property_expr::add);
        return p.get_left();
    }

    private BoolExpr encode_clean_frag(Fragment f,
                                       List<Integer> loop_queue,
                                       Seq current,
                                       boolean is_outer,
                                       Set<Seq> collector) throws Z3Exception
    {
        if (f instanceof LoopFragment) {
            LoopFragment lf = (LoopFragment) f;
            return encode_loop_fragment(lf, loop_queue, current, is_outer, collector);
        } else if (f instanceof AltFragment) {
            AltFragment af = (AltFragment) f;
            return encode_alt_fragment(af, loop_queue, current, is_outer, collector);
        } else if (f instanceof OptFragment) {
            OptFragment of = (OptFragment) f;
            return encode_opt_fragment(of, loop_queue, current, is_outer, collector);
        } else {
            return encode_container(f, loop_queue, current, is_outer, collector);
        }
    }

    private Optional<BoolExpr> encode_int_frags() throws Z3Exception
    {
        if ($.isBlankList(flow)) {
            return Optional.empty();
        }
        List<BoolExpr> exprs = new ArrayList<>();
        for (IntFragment f: flow) {
            exprs.add(encode_single_frag(f));
        }
        return w.mk_and(exprs);
    }

    private BoolExpr encode_single_frag(IntFragment f) throws Z3Exception
    {
        List<BoolExpr> all_exprs = new ArrayList<>();
        for (int i = 0; i < f.get_max(); i++) {
            List<Integer> loop_queue = $.addToList(new ArrayList<>(), i);
            Seq s = new Seq(f.get_covered());
            Set<Seq> collector = new HashSet<>();
            int_seq_set_list.add(collector);
            all_exprs.add(w.get_ctx().mkAnd(
                    encode_clean_frag(f, loop_queue, s, true, collector),
                    encode_uninterrupted(
                            f.get_virtual_head().attach_loop_queue(loop_queue),
                            f.get_virtual_tail().attach_loop_queue(loop_queue),
                            f
                    )
            ));
        }
        List<BoolExpr> exprs = new ArrayList<>();
        for (int times = f.get_min(); times <= f.get_max(); times++) {
            exprs.add(w.mk_and_not_empty(all_exprs.subList(0, times)));
        }
        return w.mk_or_not_empty(exprs);
    }

    // We don't care how int fragments interrupt sequential execution order;
    // however we ensure that the int fragments cannot be interrupted by lower priority int fragments.
    private BoolExpr encode_uninterrupted(VirtualNode head,
                                          VirtualNode tail,
                                          IntFragment f)
    {
        Set<SDComponent> nodes = get_lower_priority_nodes(f);
        List<BoolExpr> subs = new ArrayList<>();
        for (SDComponent n: nodes) {
            subs.add(w.get_ctx().mkNot(w.get_ctx().mkAnd(
                    encode_strict_pre_and_succ(head, n),
                    encode_strict_pre_and_succ(n, tail)
            )));
        }
        return w.mk_or_not_empty(subs);
    }

    private Set<SDComponent> get_lower_priority_nodes(IntFragment f)
    {
        Set<SDComponent> result = new HashSet<>();
        Set<IntFragment> frags = get_le_priority_int_frag(f);
        for (IntFragment le: frags) {
            result.addAll(le.get_inside_nodes(new ArrayList<>()));
        }
        result.addAll(clean.get_inside_nodes(new ArrayList<>()));
        return result;
    }

    private Set<IntFragment> get_le_priority_int_frag(IntFragment inf)
    {
        Set<IntFragment> result = new HashSet<>();
        for (List<IntFragment> list: priority_map.headMap(inf.get_priority()).values()) {
            result.addAll(list);
        }
        for (IntFragment peer: priority_map.get(inf.get_priority())) {
            if (peer != inf) {
                result.add(peer);
            }
        }
        return result;
    }

    private BoolExpr encode_container(Fragment f,
                                      List<Integer> loop_queue,
                                      Seq current,
                                      boolean is_outer,
                                      Set<Seq> collector) throws Z3Exception
    {
        encode_properties(f, loop_queue);
        return w.get_ctx().mkAnd(
                encode_base_fragment(
                        f,
                        f.get_children(),
                        f.get_covered(),
                        f.get_virtual_head(),
                        f.get_virtual_tail(),
                        loop_queue,
                        current,
                        is_outer,
                        collector),
                encode_ge_zero(f.get_virtual_head().attach_loop_queue(loop_queue)));
    }

    private BoolExpr encode_opt_fragment(OptFragment of,
                                         List<Integer> loop_queue,
                                         Seq current,
                                         boolean is_outer,
                                         Set<Seq> collector) throws Z3Exception
    {
        encode_properties(of, loop_queue);
        return w.get_ctx().mkAnd(
                encode_base_fragment(
                        of,
                        of.get_children(),
                        of.get_covered(),
                        of.get_virtual_head(),
                        of.get_virtual_tail(),
                        loop_queue,
                        current,
                        is_outer,
                        collector),
                encode_ge_zero(of.get_virtual_head().attach_loop_queue(loop_queue)),
                ee.encode_judgement_with_loop_queue(of.get_condition(), loop_queue));
    }

    private BoolExpr encode_alt_fragment(AltFragment af,
                                         List<Integer> loop_queue,
                                         Seq current,
                                         boolean is_outer,
                                         Set<Seq> collector) throws Z3Exception
    {
        encode_properties(af, loop_queue);
        List<BoolExpr> expers = new ArrayList<>();
        expers.add(encode_ge_zero(af.get_virtual_head().attach_loop_queue(loop_queue)));
        expers.add(w.get_ctx().mkAnd(
                encode_base_fragment(
                        af,
                        af.get_children(),
                        af.get_covered(),
                        af.get_virtual_head(),
                        af.get_virtual_tail(),
                        loop_queue,
                        Seq.clone(current),
                        is_outer,
                        collector),
                ee.encode_judgement_with_loop_queue(af.get_condition(), loop_queue)
        ));
        expers.add(w.get_ctx().mkAnd(
                encode_base_fragment(
                        af,
                        af.get_else_children(),
                        af.get_covered(),
                        af.get_virtual_head(),
                        af.get_virtual_tail(),
                        loop_queue,
                        Seq.clone(current),
                        is_outer,
                        collector),
                ee.encode_judgement_with_loop_queue(af.get_else_condition(), loop_queue)
        ));
        return w.mk_or_not_empty(expers);
    }

    private BoolExpr encode_loop_fragment(LoopFragment lf,
                                          List<Integer> loop_queue,
                                          Seq current,
                                          boolean is_outer,
                                          Set<Seq> collector) throws Z3Exception
    {
        List<BoolExpr> all_expers = new ArrayList<>();
        for (int i = 0; i < lf.get_max(); i++) {
            List<Integer> next_queue = $.addToList(loop_queue, i);
            encode_properties(lf, next_queue);
            List<BoolExpr> single_loop = new ArrayList<>();
            encode_constraints(lf.get_children(), next_queue).ifPresent(single_loop::add);
            encode_mask(lf.get_children(), next_queue).ifPresent(single_loop::add);
            if (loop_queue.size() != 0) {
                all_expers.add(w.mk_and_not_empty(single_loop));
            }
        }
        List<BoolExpr> expers = new ArrayList<>();
        for (int loop_times = lf.get_min(); loop_times <= lf.get_max(); loop_times++) {
            List<BoolExpr> subs = new ArrayList<>();
            Map<Instance, List<SDComponent>> orders = cal_life_span_inside_loop_frag(
                    lf.get_covered(),
                    lf.get_children(),
                    loop_queue,
                    loop_times
            );
            BoolExpr relations = encode_relations_inside_fragment(
                    lf.get_virtual_head().attach_loop_queue(loop_queue),
                    lf.get_virtual_tail().attach_loop_queue(loop_queue),
                    orders
            );
            subs.add(relations);
            encode_loop_children(lf, lf.get_children(), loop_queue, loop_times, Seq.clone(current), is_outer, collector).ifPresent(subs::add);
            if (all_expers.size() > 0) {
                subs.add(w.mk_and_not_empty(all_expers.subList(0, loop_times)));
            }
            expers.add(w.mk_and_not_empty(subs));
        }
        return w.mk_or_not_empty(expers);
    }



    private BoolExpr encode_base_fragment(Fragment f,
                                          List<SDComponent> children,
                                          List<Instance> covered,
                                          VirtualNode head,
                                          VirtualNode tail,
                                          List<Integer> loop_queue,
                                          Seq current,
                                          boolean is_outer,
                                          Set<Seq> collector) throws Z3Exception
    {
        Map<Instance, List<SDComponent>> orders = cal_life_span_with_loop(covered, children, loop_queue);
        List<BoolExpr> exprs = new ArrayList<>();
        encode_constraints(children, loop_queue).ifPresent(exprs::add);
        exprs.add(encode_relations_inside_fragment(
                head.attach_loop_queue(loop_queue),
                tail.attach_loop_queue(loop_queue),
                orders
        ));
        encode_mask(children, loop_queue).ifPresent(exprs::add);
        encode_children(f, children, loop_queue, current, is_outer, collector).ifPresent(exprs::add);
        return w.mk_and_not_empty(exprs);
    }

    private Optional<BoolExpr> encode_children(Fragment f,
                                               List<SDComponent> children,
                                               List<Integer> loop_queue,
                                               Seq current,
                                               boolean is_outer,
                                               Set<Seq> collector) throws Z3Exception {
        List<BoolExpr> subs = new ArrayList<>();
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                List<Message> source_list = current.get_seq().get(m.get_source());
                List<Message> target_list = current.get_seq().get(m.get_target());
                source_list.add(m);
                target_list.add(m);
                m.set_source_index(source_list.size() - 1);
                m.set_target_index(target_list.size() - 1);
            } else if (c instanceof VirtualNode) {
                // do nothing
            } else {
                subs.add(encode_clean_frag((Fragment) c, loop_queue, current, false, collector));
            }
        }
        if (is_outer) {
            String path_tag = Integer.toString(current.hashCode());
            current.set_label(path_tag);
            subs.add(encode_path_label(f, path_tag));
            collector.add(current);
        }
        return w.mk_and(subs);
    }

    private Optional<BoolExpr> encode_loop_children(LoopFragment lf,
                                                    List<SDComponent> children,
                                                    List<Integer> loop_queue,
                                                    int loop_times,
                                                    Seq current,
                                                    boolean is_outer,
                                                    Set<Seq> collector) throws Z3Exception
    {
        List<BoolExpr> subs = new ArrayList<>();
        for (int i = 0; i < loop_times; i++) {
            List<Integer> next_queue = $.addToList(loop_queue, i);
            for (SDComponent c: children) {
                if (c instanceof Message) {
                    Message m = (Message) c;
                    List<Message> source_list = current.get_seq().get(m.get_source());
                    List<Message> target_list = current.get_seq().get(m.get_target());
                    source_list.add(m);
                    target_list.add(m);
                    m.set_source_index(source_list.size() - 1);
                    m.set_target_index(target_list.size() - 1);
                } else if (c instanceof VirtualNode) {
                    // do nothing
                } else {
                    subs.add(encode_clean_frag((Fragment) c, next_queue, current, false, collector));
                }
            }
        }
        if (is_outer) {
            String path_tag = Integer.toString(current.hashCode());
            current.set_label(path_tag);
            subs.add(encode_path_label(lf, path_tag));
            collector.add(current);
        }
        return w.mk_and(subs);
    }

    private BoolExpr encode_relations_inside_fragment(VirtualNode head,
                                                      VirtualNode tail,
                                                      Map<Instance, List<SDComponent>> orders)
    {
        List<BoolExpr> subs = new ArrayList<>();
        Set<Pair<SDComponent, SDComponent>> relations = flat_orders(orders);
        for (List<SDComponent> order: orders.values()) {
            if (order.size() > 0) {
                relations.add(new Pair<>(head, order.get(0)));
                relations.add(new Pair<>(order.get(order.size() - 1), tail));
            }
        }
        encode_relations(relations).ifPresent(subs::add);
        if ($.isBlankList(subs)) {
            subs.add(encode_pre_and_succ(head, tail));
        }
        return w.mk_and_not_empty(subs);
    }

    private Optional<BoolExpr> encode_relations(Set<Pair<SDComponent, SDComponent>> orders)
    {
        List<BoolExpr> subs = new ArrayList<>();
        for (Pair<SDComponent, SDComponent> p: orders) {
            subs.add(encode_pre_and_succ(p.get_left(), p.get_right()));
        }
        return w.mk_and(subs);
    }

    private Set<Pair<SDComponent, SDComponent>> flat_orders(Map<Instance, List<SDComponent>> orders)
    {
        Set<Pair<SDComponent, SDComponent>> result = new HashSet<>();
        for (List<SDComponent> order: orders.values()) {
            for (int i = 0; i < order.size() - 1; i++) {
                result.add(new Pair<>(order.get(i), order.get(i + 1)));
            }
        }
        return result;
    }

    private Map<Instance, List<SDComponent>> cal_life_span_with_loop(List<Instance> covered,
                                                                     List<SDComponent> children,
                                                                     List<Integer> loop_queue)
    {
        Map<Instance, List<SDComponent>> map = new HashMap<>();
        for (Instance i: covered) {
            map.put(i, new ArrayList<>());
        }
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                Message attached = m.attach_loop_queue(loop_queue);
                map.get(attached.get_source()).add(attached);
                map.get(attached.get_target()).add(attached);
            } else {
                for (List<SDComponent> queue: map.values()) {
                    Fragment block = (Fragment) c;
                    queue.add(block.get_virtual_head().attach_loop_queue(loop_queue));
                    queue.add(block.get_virtual_tail().attach_loop_queue(loop_queue));
                }
            }
        }
        return map;
    }

    private Map<Instance, List<SDComponent>> cal_life_span_inside_loop_frag(List<Instance> covered,
                                                                            List<SDComponent> children,
                                                                            List<Integer> loop_queue,
                                                                            int loop_times)
    {
        Map<Instance, List<SDComponent>> result = new HashMap<>();
        for (int i = 0; i < loop_times; i++) {
            Map<Instance, List<SDComponent>> map = cal_life_span_with_loop(covered, children, $.addToList(loop_queue, i));
            for (Instance key: map.keySet()) {
                if (result.containsKey(key)) {
                    result.get(key).addAll(map.get(key));
                } else {
                    result.put(key, map.get(key));
                }
            }

        }
        return result;
    }

    //I assume that the mask instructions will cross different fragments
    private Optional<BoolExpr> encode_mask(List<SDComponent> children,
                                           List<Integer> loop_queue)
    {
        Map<String, List<Message>> map = cal_mask_instruction_map(children);
        List<BoolExpr> subs = new ArrayList<>();
        for (String mask: map.keySet()) {
            List<Message> l = map.get(mask);
            List<Pair<VirtualNode, VirtualNode>> hts = get_int_frag_heads_and_tails(mask_map.get(mask));
            if ($.is_even(l.size())) {
                for (int i = 0; i < l.size(); i += 2) {
                    Message close = l.get(i);
                    Message open = l.get(i + 1);
                    verify_mask_pair(close, open);
                    for (Pair<VirtualNode, VirtualNode> ht: hts) {
                        subs.add(w.get_ctx().mkOr(
                            encode_pre_and_succ(ht.get_right(), close.attach_loop_queue(loop_queue)),
                            encode_pre_and_succ(open.attach_loop_queue(loop_queue), ht.get_left())
                        ));
                    }
                }
            } else {
                throw new EncodeException("unmatched mask");
            }

        }
        return w.mk_and(subs);
    }

    private void verify_mask_pair(Message close, Message open)
    {
        Assignment turn_off = close.get_mask_instruction();
        Assignment turn_on = open.get_mask_instruction();
        if (!(turn_off.getLeft().equals(turn_on.getLeft())
                && turn_off.getRight() instanceof Number
                && turn_on.getRight() instanceof Number)) {
            Number off = (Number) turn_off.getRight();
            Number on = (Number) turn_on.getRight();
            if (!(off.getValue() == 0.0 && on.getValue() == 0.0)) {
                throw new EncodeException("Mask instruction unmatched.");
            }
        }
    }

    private List<Pair<VirtualNode, VirtualNode>> get_int_frag_heads_and_tails(IntFragment f)
    {
        List<Pair<VirtualNode, VirtualNode>> result = new ArrayList<>();
        for (int i = 0; i < f.get_max(); i++) {
            List<Integer> loop_queue = new ArrayList<>();
            result.add(new Pair<>(f.get_virtual_head().attach_loop_queue(loop_queue),
                    f.get_virtual_tail().attach_loop_queue(loop_queue)));
        }
        return result;
    }

    private void encode_properties(Fragment f,
                                   List<Integer> loop_queue) throws Z3Exception
    {
        Set<String> variables = f.extract_variables();
        encode_constraint_or_property(prop_dict, task_prop_dict, variables, loop_queue)
                .ifPresent(property_expr::add);
    }

    private Optional<BoolExpr> encode_constraints(List<SDComponent> children,
                                                  List<Integer> loop_queue) throws Z3Exception
    {
        Set<String> variables = extract_variables(children);
        return encode_constraint_or_property(const_dict, task_const_dict, variables, loop_queue);
    }

    private Optional<BoolExpr> encode_constraint_or_property(Map<Set<String>, Judgement> dict,
                                                             Map<Set<String>, Judgement> task_dict,
                                                             Set<String> variables,
                                                             List<Integer> loop_queue) throws Z3Exception
    {
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.addAll(encode_single_normal_cp(dict, variables, loop_queue));
        exprs.addAll(encode_single_task_cp(task_dict, variables, loop_queue));
        return w.mk_and(exprs);
    }

    private List<BoolExpr> encode_single_normal_cp(Map<Set<String>, Judgement> map,
                                                   Set<String> variables,
                                                   List<Integer> loop_queue)
    {
        List<BoolExpr> result = new ArrayList<>();
        for (Set<String> key: map.keySet()) {
            if (variables.containsAll(key)) {
                result.add(ee.encode_judgement_with_loop_queue(map.get(key), loop_queue));
            }
        }
        return result;
    }

    private List<BoolExpr> encode_single_task_cp(Map<Set<String>, Judgement> task_map,
                                                 Set<String> variables,
                                                 List<Integer> loop_queue)
    {
        List<BoolExpr> result = new ArrayList<>();
        for (Set<String> key: task_map.keySet()) {
            if (variables.containsAll(key)) {
                Judgement j = task_map.get(key);
                List<BoolExpr> ors = new ArrayList<>();
                for (int i = 0; i <= unfold_ints.size(); i++) {
                    if (i ==0) { // no interruption happens
                        ors.add(ee.encode_judgement_with_loop_queue(j, loop_queue));
                    } else {
                        List<Set<Pair<IntFragment, Integer>>> collector = permutation(i);
                        // the group of the interruptions happen inside the task
                        for (Set<Pair<IntFragment, Integer>> permutation: collector) {
                            // if the interruptions happen inside the task,
                            // make sure that they don't happen in the other task.
                            BoolExpr identity = encode_int_frag_exclusive(permutation, variables, loop_queue);
                            ArithExpr base = ee.encode_expression_with_loop_queue(j.getLeft(), loop_queue);
                            for (Pair<IntFragment, Integer> p: permutation) {
                                base = w.get_ctx().mkSub(base, cal_int_frag_duration(p));
                            }
                            ors.add(w.get_ctx().mkAnd(
                                    ee.encode_binary_judgement(
                                            j.getOp(),
                                            base,
                                            ee.encode_expression_with_loop_queue(j.getRight(), loop_queue)
                                    ),
                                    identity
                            ));
                        }
                    }
                }
                w.mk_or(ors).ifPresent(result::add);
            }
        }
        return result;
    }

    private BoolExpr encode_pre_and_succ(SDComponent pre, SDComponent succ)
    {
        RealExpr start = w.mk_real_var(pre.get_name());
        RealExpr end = w.mk_real_var(succ.get_name());
        return w.mk_ge(end, start);
    }

    private BoolExpr encode_strict_pre_and_succ(SDComponent pre, SDComponent succ)
    {
        RealExpr start = w.mk_real_var(pre.get_name());
        RealExpr end = w.mk_real_var(succ.get_name());
        return w.mk_gt(end, start);
    }

    private BoolExpr encode_ge_zero(VirtualNode head)
    {
        return w.mk_ge(w.mk_real_var(head.get_name()), w.mk_real(0));
    }

    /**
     * make sure that the interruption don't happen in two tasks
     * @param permutation the interruption which happens
     * @param variables the variables which constitute a task
     * @param loop_queue the loop queue
     * @return the boolean expression
     */
    private BoolExpr encode_int_frag_exclusive(Set<Pair<IntFragment, Integer>> permutation,
                                               Set<String> variables,
                                               List<Integer> loop_queue)
    {
        List<BoolExpr> exprs = new ArrayList<>();
        SeqExpr task_mark = mk_task_mark(variables, loop_queue);
        for (Pair<IntFragment, Integer> p: permutation) {
            exprs.add(w.mk_eq(mk_int_frag_var(p), task_mark));
        }
        return w.mk_and_not_empty(exprs);
    }

    private List<Set<Pair<IntFragment, Integer>>> permutation(int n)
    {
        List<Set<Pair<IntFragment, Integer>>> collector = new ArrayList<>();
        permutation_helper(unfold_ints, n, new HashSet<>(), collector);
        return collector;
    }

    private void permutation_helper(List<Pair<IntFragment, Integer>> list,
                                    int n,
                                    Set<Pair<IntFragment, Integer>> current,
                                    List<Set<Pair<IntFragment, Integer>>> collector)
    {
        if (n == 0) {
            collector.add(current);
            return;
        }
        for (Pair<IntFragment, Integer> p: list) {
            Set<Pair<IntFragment, Integer>> next = new HashSet<>(current);
            next.add(p);
            permutation_helper($.listSubEle(list, p), n - 1, next, collector);
        }
    }

    private ArithExpr cal_int_frag_duration(Pair<IntFragment, Integer> p)
    {
        IntFragment f = p.get_left();
        List<Integer> queue = new ArrayList<>();
        queue.add(p.get_right());
        return w.mk_sub(f.get_virtual_tail().attach_loop_queue(queue).get_name(),
                f.get_virtual_head().attach_loop_queue(queue).get_name());
    }

    private SeqExpr mk_task_mark(Set<String> variables,
                                 List<Integer> loop_queue)
    {
        String prefix = $.loop_queue_prefix(loop_queue);
        return w.mk_string(
                variables.stream()
                        .map(v -> prefix + "-" + v)
                        .collect(Collectors.joining("-"))
        );

    }

    private Expr mk_int_frag_var(Pair<IntFragment, Integer> p)
    {
        return w.mk_string(p.get_left().yield_raw_name() + "_" + p.get_right());
    }

    private BoolExpr encode_path_label(Fragment f, String label)
    {
        return w.mk_eq(w.mk_string_var(f.yield_raw_name() + "_path_label"), w.mk_string(label));
    }

    private Set<String> extract_variables(List<SDComponent> children)
    {
        Set<String> set = new HashSet<>();
        for (SDComponent c: children) {
            set.addAll(c.extract_variables());
        }
        return set;
    }

    private int calculate_offset(int index)
    {
        return index * (bound + 1);
    }

    protected RealExpr mk_sync_var(Instance ins, Message m, int index)
    {
        return w.mk_real_var(ins.get_name() + "_" + m.get_name() + "_" + calculate_offset(index));
    }

}

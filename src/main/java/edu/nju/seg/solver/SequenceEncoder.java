package edu.nju.seg.solver;

import com.microsoft.z3.*;
import edu.nju.seg.config.ExperimentalType;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.solver.model.Node;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.Z3Wrapper;

import java.util.*;

import static edu.nju.seg.config.Constants.ACTUAL_MARKER;

public class SequenceEncoder {

    private SequenceDiagram diagram;

    private SolverManager manager;

    private Context ctx;

    private ExprParser p;

    private Z3Wrapper w;

    private ExperimentalType type;

    private List<IntFragment> flow = new ArrayList<>();

    /** the container fragment without int fragment **/
    private Fragment clean;

    private List<BoolExpr> propertyExpr = new ArrayList<>();

    private Map<Pair<String, String>, String> consMap = new HashMap<>();

    private Map<Pair<String, String>, String> propMap = new HashMap<>();

    private Map<Pair<String, String>, String> actualConsMap = new HashMap<>();

    private Map<Pair<String, String>, String> actualPropMap = new HashMap<>();

    private SortedMap<Integer, List<IntFragment>> priorityMap = new TreeMap<>();

    private Set<Event> cleanEvents = new HashSet<>();

    private Map<String, IntFragment> maskMap = new HashMap<>();

    private List<Pair<IntFragment, Integer>> unfoldInts = new ArrayList<>();

    private Set<Map<Instance, List<Node>>> sequelSet;

    private int bound;

    public SequenceEncoder(SequenceDiagram diagram,
                           SolverManager manager,
                           ExperimentalType type,
                           int bound)
    {
        this.diagram = diagram;
        this.manager = manager;
        this.type = type;
        this.ctx = manager.getContext();
        this.p = new ExprParser(ctx);
        this.w = new Z3Wrapper(ctx);
        this.clean = extractIntFrag(diagram.getContainer());
        calConsAndProp();
        calPriorityMap();
        calCleanEvents();
        calMaskMap();
        unfoldIntFrag();
        if (type == ExperimentalType.ISD_AUTOMATA_OPT) {
            this.sequelSet = new HashSet<>();
            this.bound = bound;
        }
    }

    public void encode() throws Z3Exception
    {
        BoolExpr cleanExpr = encodeCleanFrag(clean, new ArrayList<>());
        List<BoolExpr> finalExpr = new ArrayList<>();
        finalExpr.add(cleanExpr);
        encodeIntFrag().ifPresent(finalExpr::add);
        w.mkAnd(propertyExpr).ifPresent(e -> finalExpr.add(ctx.mkNot(e)));
        manager.addClause(w.mkAndNotEmpty(finalExpr));
    }

    public void encodeAutomata(List<AutomatonDiagram> diagrams)
    {
        
    }

    /**
     * extract interrupt fragment from the original diagram
     * @param container the container fragment
     */
    private Fragment extractIntFrag(Fragment container)
    {
        if (container instanceof IntFragment) {
            flow.add((IntFragment) container);
            return null;
        } else {
            List<SDComponent> children = filterChildren(container.getChildren());
            if (container instanceof LoopFragment) {
                LoopFragment origin = (LoopFragment) container;
                LoopFragment result = new LoopFragment(origin.getMin(), origin.getMax(),
                        children, origin.getRaw());
                result.setCovered(container.getCovered());
                return result;
            } else if (container instanceof AltFragment) {
                AltFragment origin = (AltFragment) container;
                List<SDComponent> elseChildren = filterChildren(origin.getElseChildren());
                AltFragment result = new AltFragment(origin.getCondition(), origin.getAltCondition(),
                        children, elseChildren, origin.getRaw());
                result.setCovered(container.getCovered());
                return result;
            } else if (container instanceof OptFragment) {
                OptFragment origin = (OptFragment) container;
                OptFragment result = new OptFragment(children, origin.getCondition(), origin.getRaw());
                result.setCovered(container.getCovered());
                return result;
            } else {
                return new Fragment(children, container.getCovered(), "container");
            }
        }
    }

    /**
     * filter component list, extract all the int fragments
     * @param origin the original list
     * @return the list without int fragments
     */
    private List<SDComponent> filterChildren(List<SDComponent> origin)
    {
        List<SDComponent> children = new ArrayList<>();
        for (SDComponent child: origin) {
            if (child instanceof Message) {
                children.add(child);
            } else {
                Fragment sub = extractIntFrag((Fragment) child);
                if (sub != null) {
                    children.add(sub);
                }
            }
        }
        return children;
    }

    /**
     * construct constraint map
     */
    private void calConsAndProp()
    {
        List<String> conStr = diagram.getConstraints();
        for (String c: conStr) {
            if (c.contains(ACTUAL_MARKER)) {
                String simple = getSimpleExpr(c);
                actualConsMap.put(p.getVarFromCP(simple), simple);
            } else {
                consMap.put(p.getVarFromCP(c), c);
            }
        }
        List<String> propStr = diagram.getProperties();
        if (propStr != null) {
            for (String c: propStr) {
                if (c.contains(ACTUAL_MARKER)) {
                    String simple = getSimpleExpr(c);
                    actualPropMap.put(p.getVarFromCP(simple), simple);
                } else {
                    propMap.put(p.getVarFromCP(c), c);
                }
            }
        }
    }

    /**
     * calculate priority map for the interruption fragments
     */
    private void calPriorityMap()
    {
        for (IntFragment inf: flow) {
            if (priorityMap.containsKey(inf.getPriority())) {
                priorityMap.get(inf.getPriority()).add(inf);
            } else {
                List<IntFragment> value = new ArrayList<>();
                value.add(inf);
                priorityMap.put(inf.getPriority(), value);
            }
        }
    }

    /**
     * calculate all the events which are not belong to interruption fragments
     */
    private void calCleanEvents()
    {
        this.cleanEvents = getAllEventsInFrag(clean, new ArrayList<>());
    }

    /**
     * construct mask instruction map
     * mask variable -> int fragment
     */
    private void calMaskMap()
    {
        for (IntFragment f: flow) {
            if (f.getMaskVar() != null) {
                maskMap.put(f.getMaskVar(), f);
            }
        }
    }

    /**
     * unfold int fragment according to the maximal occurrence time
     */
    private void unfoldIntFrag()
    {
        for (IntFragment f: flow) {
            for (int i = 0; i < f.getMax(); i++) {
                unfoldInts.add(new Pair<>(f, i));
            }
        }
    }

    private List<Pair<Event, Event>> getIntVirtualHTBatch(IntFragment f)
    {
        List<Pair<Event, Event>> result = new ArrayList<>();
        for (int i = 0; i < f.getMax(); i++) {
            List<Integer> loopQueue = new ArrayList<>();
            loopQueue.add(i);
            result.add(new Pair<>(copyEvent(f.getVirtualHead(), loopQueue),
                    copyEvent(f.getVirtualTail(), loopQueue)));
        }
        return result;
    }

    private Set<Event> getAllEventsInFrag(Fragment f,
                                          List<Integer> loopQueue)
    {
        Set<Event> result = new HashSet<>();
        result.add(copyEvent(f.getVirtualHead(), loopQueue));
        result.add(copyEvent(f.getVirtualTail(), loopQueue));
        for (SDComponent c: f.getChildren()) {
            if (c instanceof AltFragment) {
                AltFragment af = (AltFragment) c;
                result.addAll(getAllEventsInFrag(af, loopQueue));
                Fragment fake = new Fragment();
                fake.setChildren(af.getElseChildren());
                fake.setVirtualHead(af.getVirtualHead());
                fake.setVirtualTail(af.getVirtualTail());
                result.addAll(getAllEventsInFrag(fake, loopQueue));
            } else if (c instanceof LoopFragment) {
                LoopFragment lf = (LoopFragment) c;
                for (int i = 0; i < lf.getMax(); i++) {
                    result.addAll(getAllEventsInFrag(lf, $.addToList(loopQueue, i)));
                }
            } else if (c instanceof OptFragment) {
                result.addAll(getAllEventsInFrag((OptFragment) c, loopQueue));
            } else if (c instanceof Fragment) {
                result.addAll(getAllEventsInFrag((Fragment) c, loopQueue));
            } else {
                Message m = (Message) c;
                result.add(copyEvent(m.getFrom(), loopQueue));
                result.add(copyEvent(m.getTo(), loopQueue));
            }
        }
        return result;
    }

    /**
     * get lower or equal priority int fragment according to the given int fragment
     * @param inf the given int fragment
     * @return lower or equal priority int fragment
     */
    private Set<IntFragment> getLeIntFrag(IntFragment inf)
    {
        Set<IntFragment> result = new HashSet<>();
        for (List<IntFragment> list: priorityMap.headMap(inf.getPriority()).values()) {
            result.addAll(list);
        }
        for (IntFragment peer: priorityMap.get(inf.getPriority())) {
            if (peer != inf) {
                result.add(peer);
            }
        }
        return result;
    }

    private Set<Event> getLowerIntEvents(IntFragment inf)
    {
        Set<Event> result = new HashSet<>();
        Set<IntFragment> frags = getLeIntFrag(inf);
        for (IntFragment frag: frags) {
            for (int i = 0; i < frag.getMax(); i++) {
                result.addAll(getAllEventsInFrag(frag, $.addToList(new ArrayList<>(), i)));
            }
        }
        return result;
    }

    /**
     * construct the event order of each instance inside a fragment
     * @param covered the instances covered by the fragment
     * @param children the elements in the fragment
     * @return the map of the order of the instances: {instance: order list}
     */
    private Map<Instance, List<Event>> consLifeSpanMapWithLoop(List<Instance> covered,
                                                               List<SDComponent> children,
                                                               List<Integer> loopQueue)
    {
        Map<Instance, List<Event>> result = new HashMap<>();
        for (Instance i: covered) {
            result.put(i, new ArrayList<>());
        }
        // TODO: we assume that the fragment cover all the instances
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                Event from = copyEvent(m.getFrom(), loopQueue);
                result.get(from.getBelongTo()).add(from);
                Event to = copyEvent(m.getTo(), loopQueue);
                result.get(to.getBelongTo()).add(to);
            } else {
                for (List<Event> queue: result.values()) {
                    Fragment block = (Fragment) c;
                    queue.add(copyEvent(block.getVirtualHead(), loopQueue));
                    queue.add(copyEvent(block.getVirtualTail(), loopQueue));
                }
            }
        }
        return result;
    }

    private Map<Instance, List<Event>> consLifeSpanMap(List<Instance> covered,
                                                       List<SDComponent> children)
    {
        Map<Instance, List<Event>> result = new HashMap<>();
        for (Instance i: covered) {
            result.put(i, new ArrayList<>());
        }
        // TODO: we assume that the fragment cover all the instances
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                Event from = m.getFrom();
                result.get(from.getBelongTo()).add(from);
                Event to = m.getTo();
                result.get(to.getBelongTo()).add(to);
            } else {
                for (List<Event> queue: result.values()) {
                    Fragment block = (Fragment) c;
                    queue.add(block.getVirtualHead());
                    queue.add(block.getVirtualTail());
                }
            }
        }
        return result;
    }

    private Map<Instance, List<Event>> sumUpLifeSpanMap(List<Map<Instance, List<Event>>> list)
    {
        Map<Instance, List<Event>> result = new HashMap<>();
        for (Map<Instance, List<Event>> map: list) {
            for (Map.Entry<Instance, List<Event>> entry: map.entrySet()) {
                Instance key = entry.getKey();
                if (result.containsKey(key)) {
                    result.get(key).addAll(entry.getValue());
                } else {
                    result.put(key, entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * unfold loop fragment
     * @param covered the covered instance
     * @param children
     * @param loopQueue the loop queue which comes from outer fragment
     * @param loop the loop times of the loop fragment
     * @return the unfolded loop block
     */
    private Map<Instance, List<Event>> consLoopBlockMap(List<Instance> covered,
                                                        List<SDComponent> children,
                                                        List<Integer> loopQueue,
                                                        int loop)
    {
        List<Map<Instance, List<Event>>> list = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            list.add(consLifeSpanMapWithLoop(covered, children, $.addToList(loopQueue, i)));
        }
        return sumUpLifeSpanMap(list);
    }

    /**
     * construct the order relation according to the life span
     * @return the set of order relationship
     */
    private Set<Pair<Event, Event>> consRelation(Map<Instance, List<Event>> map)
    {
        Set<Pair<Event, Event>> result = new HashSet<>();
        for (List<Event> order: map.values()) {
            for (int i = 0; i < order.size() - 1; i++) {
                result.add(new Pair<>(order.get(i), order.get(i + 1)));
            }
        }
        return result;
    }

    private Set<Event> getHeadsOf(Map<Instance, List<Event>> map)
    {
        Set<Event> heads = new HashSet<>();
        for (List<Event> order: map.values()) {
            if ($.isNotBlankList(order)) {
                heads.add(order.get(0));
            }
        }
        return heads;
    }

    private Set<Event> getTailsOf(Map<Instance, List<Event>> map)
    {
        Set<Event> tails = new HashSet<>();
        for (List<Event> order: map.values()) {
            if ($.isNotBlankList(order)) {
                tails.add(order.get(order.size() - 1));
            }
        }
        return tails;
    }

    private Set<Pair<String, String>> getCPCandidate(List<SDComponent> list,
                                                     List<Instance> covered)
    {
        Set<Pair<String, String>> result = new HashSet<>();
        for (SDComponent c: list) {
            if (c instanceof Message) {
                Message m = (Message) c;
                result.add(new Pair<>(m.getTo().getName(), m.getFrom().getName()));
            }
        }
        Map<Instance, List<Event>> map = consLifeSpanMap(covered, list);
        for (List<Event> line: map.values()) {
            for (int i = 0; i < line.size() - 1; i++) {
                Event current = line.get(i);
                Event next = line.get(i + 1);
                if (!current.isVirtual() && !next.isVirtual()) {
                    result.add(new Pair<>(next.getName(), current.getName()));
                }
            }
        }
        return result;
    }

    /**
     * encode event relations
     * @param set relation set
     * @return events relation boolean expression
     */
    private Optional<BoolExpr> encodeRelation(Set<Pair<Event, Event>> set)
    {
        List<BoolExpr> subs = new ArrayList<>();
        for (Pair<Event, Event> p: set) {
            subs.add(encodeTwoEvents(p.getLeft(), p.getRight()));
        }
        return w.mkAnd(subs);
    }

    /**
     * encode sequential relation between events inside a fragment
     * @param virtualHead the virtual head of the fragment
     * @param virtualTail the virtual tail of the fragment
     * @param orders the event order of each instance
     * @return the bool expression which represents the relation inside the fragment
     */
    private BoolExpr encodeWholeRelation(Event virtualHead,
                                         Event virtualTail,
                                         Map<Instance, List<Event>> orders)
    {
        Set<Pair<Event, Event>> relation = consRelation(orders);
        Optional<BoolExpr> relationExpr = encodeRelation(relation);
        Set<Event> heads = getHeadsOf(orders);
        Set<Event> tails = getTailsOf(orders);
        List<BoolExpr> ht = new ArrayList<>();
        for (Event h: heads) {
            ht.add(encodeTwoEvents(virtualHead, h));
        }
        for (Event t: tails) {
            ht.add(encodeTwoEvents(t, virtualTail));
        }
        if ($.isBlankList(ht)) {
            ht.add(encodeTwoEvents(virtualHead, virtualTail));
        }
        relationExpr.ifPresent(ht::add);
        return w.mkAndNotEmpty(ht);
    }

    /**
     * encode fragment without interrupt fragments inside the given fragment,
     * but the f itself maybe an interrupt fragment
     * @param f the given fragment
     * @param loopQueue the loop queue
     * @return bool expression
     * @throws Z3Exception exception
     */
    private BoolExpr encodeCleanFrag(Fragment f,
                                     List<Integer> loopQueue) throws Z3Exception
    {
        if (f instanceof LoopFragment) {
            LoopFragment lf = (LoopFragment) f;
            return encodeLoopFragment(lf, loopQueue);
        } else if (f instanceof AltFragment) {
            AltFragment af = (AltFragment) f;
            return encodeAltFragment(af, loopQueue);
        } else if (f instanceof OptFragment) {
            OptFragment of = (OptFragment) f;
            return encodeOptFragment(of, loopQueue);
        } else {
            return encodeContainer(f, loopQueue);
        }
    }

    private Optional<BoolExpr> encodeIntFrag() throws Z3Exception
    {
        if ($.isBlankList(flow)) {
            return Optional.empty();
        }
        List<BoolExpr> subs = new ArrayList<>();
        for (IntFragment fragment : flow) {
            subs.add(encodeSingleIntFrag(fragment));
        }
        return w.mkAnd(subs);
    }

    private BoolExpr encodeSingleIntFrag(IntFragment f) throws Z3Exception
    {
        BoolExpr[] maxExpr = new BoolExpr[f.getMax()];
        for (int i = 0; i < f.getMax(); i++) {
            List<Integer> loopQueue = new ArrayList<>();
            loopQueue.add(i);
            maxExpr[i] = ctx.mkAnd(encodeCleanFrag(f, loopQueue),
                    encodeUninterrupted(copyEvent(f.getVirtualHead(), loopQueue),
                            copyEvent(f.getVirtualTail(), loopQueue), f));
        }
        BoolExpr[] timesExpr = new BoolExpr[f.getMax() - f.getMin() + 1];
        int index = 0;
        for (int times = f.getMin(); times <= f.getMax(); times++) {
            timesExpr[index] = ctx.mkAnd(Arrays.copyOfRange(maxExpr, 0, times));
            index++;
        }
        return ctx.mkOr(timesExpr);
    }

    private BoolExpr encodeUninterrupted(Event head, Event tail, IntFragment inf)
    {
        Set<Event> events = getLowerIntEvents(inf);
        events.addAll(cleanEvents);
        List<BoolExpr> subs = new ArrayList<>();
        for (Event ev: events) {
            subs.add(ctx.mkNot(ctx.mkAnd(encodeTwoEvents(head, ev),
                    encodeTwoEvents(ev, tail))));
        }
        return w.mkAndNotEmpty(subs);
    }

    private Optional<BoolExpr> scanMask(List<SDComponent> children, List<Integer> loopQueue)
    {
        Map<String, List<Pair<Event, Event>>> map = new HashMap<>();
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                Event from = m.getFrom();
                Event to = m.getTo();
                appendInstruction(map, from);
                appendInstruction(map, to);
            }
        }
        List<BoolExpr> subs = new ArrayList<>();
        for (String maskVar: map.keySet()) {
            List<Pair<Event, Event>> list = map.get(maskVar);
            for (Pair<Event, Event> p: list) {
                List<Pair<Event, Event>> hts = getIntVirtualHTBatch(maskMap.get(maskVar));
                for (Pair<Event, Event> ht: hts) {
                    subs.add(ctx.mkOr(encodeTwoEvents(ht.getRight(), copyEvent(p.getLeft(), loopQueue)),
                            encodeTwoEvents(copyEvent(p.getRight(), loopQueue), ht.getLeft())));
                }
            }
        }
        return w.mkAnd(subs);
    }

    private void appendInstruction(Map<String, List<Pair<Event, Event>>> map,
                                   Event e)
    {
        if (e.getInstruction() != null) {
            Pair<String, Integer> ins = e.parseInstruction();
            if (ins.getRight() == 0) {
                if (!map.containsKey(ins.getLeft())) {
                    map.put(ins.getLeft(), new ArrayList<>());
                }
                Pair<Event, Event> p = new Pair<>();
                p.setLeft(e);
                map.get(ins.getLeft()).add(p);
            } else if (ins.getRight() == 1) {
                int size = map.get(ins.getLeft()).size();
                map.get(ins.getLeft()).get(size - 1).setRight(e);
            }
        }
    }

    private void encodeProperties(Fragment f, List<Integer> loopQueue) throws Z3Exception
    {
        Set<Pair<String, String>> candidates = new HashSet<>();
        if (f instanceof AltFragment) {
            AltFragment af = (AltFragment) f;
            candidates.addAll(getCPCandidate(af.getChildren(), af.getCovered()));
            candidates.addAll(getCPCandidate(af.getElseChildren(), af.getCovered()));
        } else {
            candidates.addAll(getCPCandidate(f.getChildren(), f.getCovered()));
        }
        encodeCP(propMap, actualPropMap, candidates, loopQueue).ifPresent(propertyExpr::add);
    }

    private Optional<BoolExpr> encodeConstraints(List<SDComponent> children,
                                                 List<Instance> covered,
                                                 List<Integer> loopQueue) throws Z3Exception
    {
        Set<Pair<String, String>> candidates = new HashSet<>(getCPCandidate(children, covered));
        return encodeCP(consMap, actualConsMap, candidates, loopQueue);
    }

    private Optional<BoolExpr> encodeCP(Map<Pair<String, String>, String> map,
                                        Map<Pair<String, String>, String> actualMap,
                                        Set<Pair<String, String>> candidates,
                                        List<Integer> loopQueue) throws Z3Exception
    {
        String prefix = loopPrefix(loopQueue);
        List<BoolExpr> exprs = new ArrayList<>();
        for (Pair<String, String> can: candidates) {
            if (map.containsKey(can)) {
                String conStr = map.get(can)
                        .replaceAll(can.getLeft(), prefix + can.getLeft())
                        .replaceAll(can.getRight(), prefix + can.getRight());
                exprs.add(p.convert(conStr));
            }
            if (actualMap.containsKey(can)) {
                List<BoolExpr> num = new ArrayList<>();
                for (int i = 0; i <= unfoldInts.size(); i++) {
                    if (i == 0) {
                        String conStr = actualMap.get(can)
                                .replaceAll(can.getLeft(), prefix + can.getLeft())
                                .replaceAll(can.getRight(), prefix + can.getRight());
                        num.add(p.convert(conStr));
                    } else {
                        List<Set<Pair<IntFragment, Integer>>> collector = new ArrayList<>();
                        permutation(unfoldInts, i, new HashSet<>(), collector);
                        for (Set<Pair<IntFragment, Integer>> per: collector) {
                            BoolExpr identity = yieldIntIdentity(per, can, loopQueue);
                            Pair<ArithExpr, ArithExpr> interval = p.getInterval(actualMap.get(can));
                            ArithExpr base = w.mkSub(prefix + can.getLeft(),
                                    prefix + can.getRight());
                            List<BoolExpr> seq = new ArrayList<>();
                            RealSort rs = ctx.mkRealSort();
                            for (Pair<IntFragment, Integer> p: per) {
                                base = ctx.mkSub(base, getIntFragTime(p));
                                seq.add(ctx.mkAnd(ctx.mkLe((RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + can.getRight()), rs),
                                            getIntFragHead(p)),
                                        ctx.mkLe(getIntFragTail(p), (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + can.getLeft()), rs))));
                            }
                            // TODO, multi operators
                            BoolExpr cleanCP;
                            if (interval.getLeft() == null) {
                                cleanCP = ctx.mkLe(base, interval.getRight());
                            } else if (interval.getRight() == null) {
                                cleanCP = ctx.mkLe(interval.getLeft(), base);
                            } else {
                                cleanCP = ctx.mkAnd(ctx.mkLe(interval.getLeft(), base),
                                        ctx.mkLe(base, interval.getRight()));
                            }
                            num.add(ctx.mkAnd(identity, cleanCP, w.mkAndNotEmpty(seq)));
                        }
                    }
                }
                w.mkOr(num).ifPresent(exprs::add);
            }
        }
        return w.mkAnd(exprs);
    }

    private BoolExpr yieldIntIdentity(Set<Pair<IntFragment, Integer>> per,
                                      Pair<String, String> interval,
                                      List<Integer> loopQueue)
    {
        List<BoolExpr> exprs = new ArrayList<>();
        String flag = getCPFlag(interval, loopQueue);
        SeqExpr flagExpr = ctx.mkString(flag);
        SeqSort ss = ctx.mkStringSort();
        for (Pair<IntFragment, Integer> p: per) {
            exprs.add(ctx.mkEq(ctx.mkConst(ctx.mkSymbol(getIntFragVar(p)), ss), flagExpr));
        }
        return w.mkAndNotEmpty(exprs);
    }

    private BoolExpr encodeContainer(Fragment f, List<Integer> loopQueue) throws Z3Exception
    {
        encodeProperties(f, loopQueue);
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(f.getCovered(),
                f.getChildren(), loopQueue);
        List<BoolExpr> exprs = new ArrayList<>();
        BoolExpr relationExpr = encodeWholeRelation(copyEvent(f.getVirtualHead(), loopQueue),
                copyEvent(f.getVirtualTail(), loopQueue),
                orders);
        encodeConstraints(f.getChildren(), f.getCovered(), loopQueue).ifPresent(exprs::add);
        scanMask(f.getChildren(), loopQueue).ifPresent(exprs::add);
        exprs.add(encodeGeZero(copyEvent(f.getVirtualHead(), loopQueue)));
        exprs.add(relationExpr);
        encodeChildren(f.getChildren(), loopQueue).ifPresent(exprs::add);
        return w.mkAndNotEmpty(exprs);
    }

    private BoolExpr encodeOptFragment(OptFragment of, List<Integer> loopQueue) throws Z3Exception
    {
        encodeProperties(of, loopQueue);
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(of.getCovered(),
                of.getChildren(), loopQueue);
        List<BoolExpr> exprs = new ArrayList<>();
        encodeConstraints(of.getChildren(), of.getCovered(), loopQueue).ifPresent(exprs::add);
        scanMask(of.getChildren(), loopQueue).ifPresent(exprs::add);
        exprs.add(p.convert(of.getCondition()));
        exprs.add(encodeWholeRelation(copyEvent(of.getVirtualHead(), loopQueue),
                copyEvent(of.getVirtualTail(), loopQueue), orders));
        encodeChildren(of.getChildren(), loopQueue).ifPresent(exprs::add);
        return w.mkAndNotEmpty(exprs);
    }

    private BoolExpr encodeAltFragment(AltFragment af, List<Integer> loopQueue) throws Z3Exception
    {
        encodeProperties(af, loopQueue);
        BoolExpr[] altExpr = new BoolExpr[2];
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(af.getCovered(),
                af.getChildren(), loopQueue);
        Map<Instance, List<Event>> elseOrders = consLifeSpanMapWithLoop(af.getCovered(),
                af.getElseChildren(), loopQueue);
        Event virtualHead = copyEvent(af.getVirtualHead(), loopQueue);
        Event virtualTail = copyEvent(af.getVirtualTail(), loopQueue);
        List<BoolExpr> ifExpers = new ArrayList<>();
        ifExpers.add(p.convert(af.getCondition()));
        ifExpers.add(encodeWholeRelation(virtualHead, virtualTail, orders));
        scanMask(af.getChildren(), loopQueue).ifPresent(ifExpers::add);
        encodeChildren(af.getChildren(), loopQueue).ifPresent(ifExpers::add);
        encodeConstraints(af.getChildren(), af.getCovered(), loopQueue).ifPresent(ifExpers::add);
        List<BoolExpr> elseExprs = new ArrayList<>();
        elseExprs.add(p.convert(af.getAltCondition()));
        elseExprs.add(encodeWholeRelation(virtualHead, virtualTail, elseOrders));
        encodeChildren(af.getElseChildren(), loopQueue).ifPresent(elseExprs::add);
        encodeConstraints(af.getElseChildren(), af.getCovered(), loopQueue).ifPresent(elseExprs::add);
        scanMask(af.getElseChildren(), loopQueue).ifPresent(elseExprs::add);
        altExpr[0] = w.mkAndNotEmpty(ifExpers);
        altExpr[1] = w.mkAndNotEmpty(elseExprs);
        return ctx.mkOr(altExpr);
    }

    private BoolExpr encodeLoopFragment(LoopFragment lf,
                                        List<Integer> loopQueue) throws Z3Exception
    {
        BoolExpr[] maxExpr = new BoolExpr[lf.getMax()];
        for (int i = 0; i < lf.getMax(); i++) {
            List<Integer> nextLoopQueue = $.addToList(loopQueue, i);
            List<BoolExpr> exprs = new ArrayList<>();
            encodeConstraints(lf.getChildren(), lf.getCovered(), nextLoopQueue).ifPresent(exprs::add);
            encodeChildren(lf.getChildren(), nextLoopQueue).ifPresent(exprs::add);
            scanMask(lf.getChildren(), nextLoopQueue).ifPresent(exprs::add);
            maxExpr[i] = w.mkAndNotEmpty(exprs);
            encodeProperties(lf, nextLoopQueue);
        }
        BoolExpr[] loopExpr = new BoolExpr[lf.getMax() - lf.getMin() + 1];
        int index = 0;
        for (int loop = lf.getMin(); loop <= lf.getMax(); loop++) {
            Map<Instance, List<Event>> orders = consLoopBlockMap(lf.getCovered(),
                    lf.getChildren(), loopQueue, loop);
            BoolExpr relationExpr = encodeWholeRelation(copyEvent(lf.getVirtualHead(), loopQueue),
                    copyEvent(lf.getVirtualTail(), loopQueue), orders);
            BoolExpr[] subs = Arrays.copyOfRange(maxExpr, 0, loop);
            loopExpr[index] = ctx.mkAnd(relationExpr, ctx.mkAnd(subs));
            index++;
        }
        return ctx.mkOr(loopExpr);
    }

    private Optional<BoolExpr> encodeChildren(List<SDComponent> children,
                                              List<Integer> loopQueue) throws Z3Exception
    {
        List<BoolExpr> subs = new ArrayList<>();
        for (SDComponent child : children) {
            if (child instanceof Message) {
                subs.add(encodeLoopMessage((Message) child, loopQueue));
            } else {
                subs.add(encodeCleanFrag((Fragment) child, loopQueue));
            }
        }
        return w.mkAnd(subs);
    }

    /**
     * encode relation between a message
     * @param m message
     * @return bool expression
     */
    private BoolExpr encodeLoopMessage(Message m,
                                       List<Integer> loopQueue)
    {
        return encodeTwoEvents(copyEvent(m.getFrom(), loopQueue), copyEvent(m.getTo(), loopQueue));
    }

    /**
     * copy event according to the loop queue
     * @param e the event which needs to be copied
     * @param loopQueue the loop queue
     * @return the copy event
     */
    private Event copyEvent(Event e,
                            List<Integer> loopQueue)
    {
        Event result = new Event();
        result.setName(loopPrefix(loopQueue) + e.getName());
        result.setInstruction(e.getInstruction());
        result.setBelongTo(e.getBelongTo());
        return result;
    }

    /**
     * yield loop prefix according to the loop queue
     * @param loopQueue the loop queue
     * @return the loop prefix
     */
    private String loopPrefix(List<Integer> loopQueue)
    {
        if ($.isBlankList(loopQueue)) {
            return "";
        }
        StringBuilder builder = new StringBuilder("loop_");
        for (Integer i: loopQueue) {
            builder.append(i);
            builder.append("_");
        }
        return builder.toString();
    }

    /**
     * encode the relationship between two events
     * @param pre the previous event
     * @param succ the succinct event
     * @return the bool expression
     */
    private BoolExpr encodeTwoEvents(Event pre, Event succ)
    {
        RealSort rs = ctx.mkRealSort();
        RealExpr start = (RealExpr) ctx.mkConst(pre.getName(), rs);
        RealExpr end = (RealExpr) ctx.mkConst(succ.getName(), rs);
        return ctx.mkGe(end, start);
    }

    private BoolExpr encodeGeZero(Event head)
    {
        RealExpr start = (RealExpr) ctx.mkConst(head.getName(), ctx.mkRealSort());
        return ctx.mkGe(start, ctx.mkReal(0));
    }

    /**
     * convert actual time expressions to simple time expressions
     * @param expr the actual time expression
     * @return the simple time expression
     */
    private String getSimpleExpr(String expr)
    {
        return expr.replaceAll("\\^", "")
                .replaceAll("\\(", "")
                .replaceAll("\\)", "");
    }

    private void permutation(List<Pair<IntFragment, Integer>> list,
                             int n,
                             Set<Pair<IntFragment, Integer>> current,
                             List<Set<Pair<IntFragment, Integer>>> result)
    {
        if (n == 0) {
            result.add(current);
            return;
        }
        for (Pair<IntFragment, Integer> p: list) {
            Set<Pair<IntFragment, Integer>> next = new HashSet<>(current);
            next.add(p);
            permutation($.listSubEle(list, p), n - 1, next, result);
        }
    }

    private String getIntFragVar(Pair<IntFragment, Integer> p)
    {
        return p.getLeft().getName() + "-" + p.getRight();
    }

    private String getCPFlag(Pair<String, String> p, List<Integer> loopQueue)
    {
        String prefix = loopPrefix(loopQueue);
        return prefix + p.getLeft() + "-" + prefix + p.getRight();
    }

    private ArithExpr getIntFragTime(Pair<IntFragment, Integer> p)
    {
        String prefix = "loop_" + p.getRight() + "_";
        return w.mkSub(prefix + p.getLeft().getVirtualTail().getName(),
                prefix + p.getLeft().getVirtualHead().getName());
    }

    private RealExpr getIntFragHead(Pair<IntFragment, Integer> p)
    {
        String prefix = "loop_" + p.getRight() + "_";
        return (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + p.getLeft().getVirtualHead().getName()),
                ctx.mkRealSort());
    }

    private RealExpr getIntFragTail(Pair<IntFragment, Integer> p)
    {
        String prefix = "loop_" + p.getRight() + "_";
        return (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + p.getLeft().getVirtualTail().getName()),
                ctx.mkRealSort());
    }

}

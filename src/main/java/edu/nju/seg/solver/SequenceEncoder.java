package edu.nju.seg.solver;

import com.microsoft.z3.*;
import edu.nju.seg.config.ExperimentalType;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.solver.model.Block;
import edu.nju.seg.solver.model.Duration;
import edu.nju.seg.solver.model.Instant;
import edu.nju.seg.solver.model.Node;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.Z3Wrapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.nju.seg.config.Constants.ACTUAL_MARKER;

public class SequenceEncoder {

    private final SequenceDiagram diagram;

    private final SolverManager manager;

    private final Context ctx;

    private final ExprParser p;

    private final Z3Wrapper w;

    private ExperimentalType type;

    private final List<IntFragment> flow = new ArrayList<>();

    /** the container fragment without int fragment **/
    private final Fragment clean;

    private final List<BoolExpr> propertyExpr = new ArrayList<>();

    private final Map<Pair<String, String>, String> consMap = new HashMap<>();

    private final Map<Pair<String, String>, String> propMap = new HashMap<>();

    private final Map<Pair<String, String>, String> taskConsMap = new HashMap<>();

    private final Map<Pair<String, String>, String> taskPropMap = new HashMap<>();

    private final SortedMap<Integer, List<IntFragment>> priorityMap = new TreeMap<>();

    private Set<Event> cleanEvents = new HashSet<>();

    private final Map<String, IntFragment> maskMap = new HashMap<>();

    private final List<Pair<IntFragment, Integer>> unfoldInts = new ArrayList<>();

    private Set<Sequel> sequelSet;

    private List<Set<Sequel>> intSequelSetList;

    private int bound;

    private List<BoolExpr> allExprs = new ArrayList<>();

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
            this.intSequelSetList = new ArrayList<>();
            this.bound = bound;
        }
    }

    public void encode() throws Z3Exception
    {
        Sequel s= new Sequel();
        s.init(clean.getCovered());
        BoolExpr cleanExpr = encodeCleanFrag(clean, new ArrayList<>(), s, true, sequelSet);
        List<BoolExpr> finalExpr = new ArrayList<>();
        finalExpr.add(cleanExpr);
        encodeIntFrag().ifPresent(finalExpr::add);
        w.mkAnd(propertyExpr).ifPresent(e -> finalExpr.add(ctx.mkNot(e)));
        allExprs.add(w.mkAndNotEmpty(finalExpr));
    }

    public void delegateToManager()
    {
        manager.addClause(w.mkAndNotEmpty(allExprs));
    }

    public void encodeAutomata(List<AutomatonDiagram> diagrams) throws Z3Exception
    {
        encodeCleanNetwork(diagrams);
        encodeIntNetwork(diagrams);
    }

    public void encodeCleanNetwork(List<AutomatonDiagram> diagrams) throws Z3Exception
    {
        encodeNetwork(diagrams, sequelSet);
    }

    public void encodeIntNetwork(List<AutomatonDiagram> diagrams) throws Z3Exception
    {
        for (Set<Sequel> set: intSequelSetList) {
            encodeNetwork(diagrams, set);
        }
    }

    public void encodeNetwork(List<AutomatonDiagram> diagrams,
                              Set<Sequel> set) throws Z3Exception
    {
        Map<String, AutomatonDiagram> autoMap = consAutoMap(diagrams);
        List<BoolExpr> exprs = new ArrayList<>();
        for (Sequel s: set) {
            Map<Instance, List<Node>> m = s.getSequel();
            List<BoolExpr> subs = new ArrayList<>();
            for (Instance key: m.keySet()) {
                if (autoMap.containsKey(key.getName())) {
                    LocalEncoder l = new LocalEncoder(autoMap.get(key.getName()),
                            m.get(key), manager, bound);
                    l.encode().ifPresent(subs::add);
                } else {
                    throw new EncodeException("unmatched instance: " + key.getName());
                }
            }
            w.mkAnd(subs).ifPresent(exprs::add);
            exprs.add(encodePathLabel(s.getLabel()));
        }
        w.mkOr(exprs).ifPresent(allExprs::add);
    }

    /**
     * construct automata diagram map:
     * the little of the diagram -> diagram
     * @param diagrams the list of diagrams
     * @return the map
     */
    private Map<String, AutomatonDiagram> consAutoMap(List<AutomatonDiagram> diagrams)
    {
        return diagrams.stream().collect(
                Collectors.toMap(AutomatonDiagram::getTitle,
                        Function.identity()));
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
                taskConsMap.put(p.getVarFromCP(simple), simple);
            } else {
                consMap.put(p.getVarFromCP(c), c);
            }
        }
        List<String> propStr = diagram.getProperties();
        if (propStr != null) {
            for (String c: propStr) {
                if (c.contains(ACTUAL_MARKER)) {
                    String simple = getSimpleExpr(c);
                    taskPropMap.put(p.getVarFromCP(simple), simple);
                } else {
                    propMap.put(p.getVarFromCP(c), c);
                }
            }
        }
    }

    /**
     * calculate priority map for the interruption fragments
     * priority -> [fragments]
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
     * generate a list of interruption fragments with their corresponding index
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
            result.add(new Pair<>(extendEvent(f.getVirtualHead(), loopQueue),
                    extendEvent(f.getVirtualTail(), loopQueue)));
        }
        return result;
    }

    /**
     * get all events inside a fragment
     * @param f the target fragment
     * @param loopQueue the loop queue
     * @return the event set
     */
    private Set<Event> getAllEventsInFrag(Fragment f,
                                          List<Integer> loopQueue)
    {
        Set<Event> result = new HashSet<>();
        result.add(extendEvent(f.getVirtualHead(), loopQueue));
        result.add(extendEvent(f.getVirtualTail(), loopQueue));
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
                result.add(extendEvent(m.getFrom(), loopQueue));
                result.add(extendEvent(m.getTo(), loopQueue));
            }
        }
        return result;
    }

    /**
     * get lower or equal priority int fragments according to the given int fragment
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

    /**
     * get events of lower or equal priority int fragments
     * @param inf the given int fragment
     * @return events of lower or equal priority int fragment
     */
    private Set<Event> getLeIntEvents(IntFragment inf)
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
                Event from = extendEvent(m.getFrom(), loopQueue);
                result.get(from.getBelongTo()).add(from);
                Event to = extendEvent(m.getTo(), loopQueue);
                result.get(to.getBelongTo()).add(to);
            } else {
                for (List<Event> queue: result.values()) {
                    Fragment block = (Fragment) c;
                    queue.add(extendEvent(block.getVirtualHead(), loopQueue));
                    queue.add(extendEvent(block.getVirtualTail(), loopQueue));
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

    /**
     * sum up multiple life span map
     * @param list the list of life span
     * @return full life span
     */
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
     * @param children the components inside a loop fragment
     * @param loopQueue the loop queue which comes from outer fragment
     * @param loop the loop times of the loop fragment
     * @return the unfolded loop block
     */
    private Map<Instance, List<Event>> consLoopFragmentMap(List<Instance> covered,
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
            // TODO: support full cp candidates
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
            subs.add(encodePreAndSucc(p.getLeft(), p.getRight()));
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
            ht.add(encodePreAndSucc(virtualHead, h));
        }
        for (Event t: tails) {
            ht.add(encodePreAndSucc(t, virtualTail));
        }
        if ($.isBlankList(ht)) {
            ht.add(encodePreAndSucc(virtualHead, virtualTail));
        }
        relationExpr.ifPresent(ht::add);
        return w.mkAndNotEmpty(ht);
    }

    /**
     * encode fragment without interrupt fragments inside the given fragment,
     * but the given parameter f itself maybe an interrupt fragment
     * @param f the given fragment
     * @param loopQueue the loop queue
     * @return bool expression
     * @throws Z3Exception exception
     */
    private BoolExpr encodeCleanFrag(Fragment f,
                                     List<Integer> loopQueue,
                                     Sequel current,
                                     boolean isOuter,
                                     Set<Sequel> collector) throws Z3Exception
    {
        if (f instanceof LoopFragment) {
            LoopFragment lf = (LoopFragment) f;
            return encodeLoopFragment(lf, loopQueue, current, isOuter, collector);
        } else if (f instanceof AltFragment) {
            AltFragment af = (AltFragment) f;
            return encodeAltFragment(af, loopQueue, current, isOuter, collector);
        } else if (f instanceof OptFragment) {
            OptFragment of = (OptFragment) f;
            return encodeOptFragment(of, loopQueue, current, isOuter, collector);
        } else {
            return encodeContainer(f, loopQueue, current, isOuter, collector);
        }
    }

    private BoolExpr encodeContainer(Fragment f,
                                     List<Integer> loopQueue,
                                     Sequel current,
                                     boolean isOuter,
                                     Set<Sequel> collector) throws Z3Exception
    {
        encodeProperties(f, loopQueue);
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(f.getCovered(), f.getChildren(), loopQueue);
        List<BoolExpr> exprs = new ArrayList<>();
        BoolExpr relationExpr = encodeWholeRelation(
                extendEvent(f.getVirtualHead(), loopQueue),
                extendEvent(f.getVirtualTail(), loopQueue),
                orders);
        encodeConstraints(f.getChildren(), f.getCovered(), loopQueue).ifPresent(exprs::add);
        encodeMask(f.getChildren(), loopQueue).ifPresent(exprs::add);
        exprs.add(encodeGeZero(extendEvent(f.getVirtualHead(), loopQueue)));
        exprs.add(relationExpr);
        encodeChildren(f.getChildren(), loopQueue, current, isOuter, collector).ifPresent(exprs::add);
        return w.mkAndNotEmpty(exprs);
    }

    // TODO: we assume that optional fragment can be treated as normal fragment
    private BoolExpr encodeOptFragment(OptFragment of,
                                       List<Integer> loopQueue,
                                       Sequel current,
                                       boolean isOuter,
                                       Set<Sequel> collector) throws Z3Exception
    {
        encodeProperties(of, loopQueue);
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(of.getCovered(), of.getChildren(), loopQueue);
        List<BoolExpr> exprs = new ArrayList<>();
        encodeConstraints(of.getChildren(), of.getCovered(), loopQueue).ifPresent(exprs::add);
        encodeMask(of.getChildren(), loopQueue).ifPresent(exprs::add);
        exprs.add(p.convert(of.getCondition()));
        exprs.add(encodeWholeRelation(extendEvent(of.getVirtualHead(), loopQueue),
                extendEvent(of.getVirtualTail(), loopQueue), orders));
        encodeChildren(of.getChildren(), loopQueue, current, false, collector).ifPresent(exprs::add);
        return w.mkAndNotEmpty(exprs);
    }

    private BoolExpr encodeAltFragment(AltFragment af,
                                       List<Integer> loopQueue,
                                       Sequel current,
                                       boolean isOuter,
                                       Set<Sequel> collector) throws Z3Exception
    {
        encodeProperties(af, loopQueue);
        BoolExpr[] altExpr = new BoolExpr[2];
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(af.getCovered(),
                af.getChildren(), loopQueue);
        Map<Instance, List<Event>> elseOrders = consLifeSpanMapWithLoop(af.getCovered(),
                af.getElseChildren(), loopQueue);
        Event virtualHead = extendEvent(af.getVirtualHead(), loopQueue);
        Event virtualTail = extendEvent(af.getVirtualTail(), loopQueue);
        List<BoolExpr> ifExpers = new ArrayList<>();
        ifExpers.add(p.convert(af.getCondition()));
        ifExpers.add(encodeWholeRelation(virtualHead, virtualTail, orders));
        encodeMask(af.getChildren(), loopQueue).ifPresent(ifExpers::add);
        encodeChildren(af.getChildren(), loopQueue, Sequel.clone(current), isOuter, collector).ifPresent(ifExpers::add);
        encodeConstraints(af.getChildren(), af.getCovered(), loopQueue).ifPresent(ifExpers::add);
        List<BoolExpr> elseExprs = new ArrayList<>();
        elseExprs.add(p.convert(af.getAltCondition()));
        elseExprs.add(encodeWholeRelation(virtualHead, virtualTail, elseOrders));
        encodeChildren(af.getElseChildren(), loopQueue, Sequel.clone(current), isOuter, collector).ifPresent(elseExprs::add);
        encodeConstraints(af.getElseChildren(), af.getCovered(), loopQueue).ifPresent(elseExprs::add);
        encodeMask(af.getElseChildren(), loopQueue).ifPresent(elseExprs::add);
        altExpr[0] = w.mkAndNotEmpty(ifExpers);
        altExpr[1] = w.mkAndNotEmpty(elseExprs);
        return ctx.mkOr(altExpr);
    }

    // TODO: we assume that loop fragments are not the most outer fragments
    private BoolExpr encodeLoopFragment(LoopFragment lf,
                                        List<Integer> loopQueue,
                                        Sequel current,
                                        boolean isOuter,
                                        Set<Sequel> collector) throws Z3Exception
    {
        BoolExpr[] maxExpr = new BoolExpr[lf.getMax()];
        for (int i = 0; i < lf.getMax(); i++) {
            List<Integer> nextLoopQueue = $.addToList(loopQueue, i);
            List<BoolExpr> exprs = new ArrayList<>();
            encodeConstraints(lf.getChildren(), lf.getCovered(), nextLoopQueue).ifPresent(exprs::add);
            encodeChildren(lf.getChildren(), nextLoopQueue, current, false, collector).ifPresent(exprs::add);
            encodeMask(lf.getChildren(), nextLoopQueue).ifPresent(exprs::add);
            maxExpr[i] = w.mkAndNotEmpty(exprs);
            encodeProperties(lf, nextLoopQueue);
        }
        BoolExpr[] loopExpr = new BoolExpr[lf.getMax() - lf.getMin() + 1];
        int index = 0;
        for (int loopTimes = lf.getMin(); loopTimes <= lf.getMax(); loopTimes++) {
            Map<Instance, List<Event>> orders = consLoopFragmentMap(lf.getCovered(),
                    lf.getChildren(), loopQueue, loopTimes);
            BoolExpr relationExpr = encodeWholeRelation(extendEvent(lf.getVirtualHead(), loopQueue),
                    extendEvent(lf.getVirtualTail(), loopQueue), orders);
            BoolExpr[] subs = Arrays.copyOfRange(maxExpr, 0, loopTimes);
            loopExpr[index] = ctx.mkAnd(relationExpr, ctx.mkAnd(subs));
            index++;
        }
        return ctx.mkOr(loopExpr);
    }

    private Optional<BoolExpr> encodeChildren(List<SDComponent> children,
                                              List<Integer> loopQueue,
                                              Sequel current,
                                              boolean isOuter,
                                              Set<Sequel> collector) throws Z3Exception
    {
        List<BoolExpr> subs = new ArrayList<>();
        for (SDComponent child : children) {
            if (child instanceof Message) {
                subs.add(encodeLoopMessage((Message) child, loopQueue));
                appendSequel((Message) child, current, loopQueue);
            } else {
                subs.add(encodeCleanFrag((Fragment) child, loopQueue, current, false, collector));
            }
        }
        if (isOuter) {
            String pathTag = Integer.toString(current.hashCode());
            current.setLabel(pathTag);
            subs.add(encodePathLabel(pathTag));
            collector.add(current);
        }
        return w.mkAnd(subs);
    }

    private void appendSequel(Message m,
                              Sequel s,
                              List<Integer> loopQueue)
    {
        String prefix = loopPrefix(loopQueue);
        Event from = m.getFrom();
        Instant fi = new Instant(from, prefix);
        Event to = m.getTo();
        Instant ti = new Instant(to, prefix);
        Duration d = new Duration(m, prefix);
        Block fb = new Block(fi, d);
        Block tb = new Block(d, ti);
        s.getSequel().get(from.getBelongTo()).add(fb);
        s.getSequel().get(to.getBelongTo()).add(tb);
    }

    /**
     * encode all int fragments
     * @return maybe the boolean expression
     * @throws Z3Exception when encoding single int fragment failed
     */
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
            Sequel s = new Sequel();
            s.init(f.getCovered());
            Set<Sequel> collector = new HashSet<>();
            intSequelSetList.add(collector);
            maxExpr[i] = ctx.mkAnd(encodeCleanFrag(f, loopQueue, s, true, collector),
                    encodeUninterrupted(extendEvent(f.getVirtualHead(), loopQueue),
                            extendEvent(f.getVirtualTail(), loopQueue), f));
        }
        BoolExpr[] timesExpr = new BoolExpr[f.getMax() - f.getMin() + 1];
        int index = 0;
        for (int times = f.getMin(); times <= f.getMax(); times++) {
            timesExpr[index] = ctx.mkAnd(Arrays.copyOfRange(maxExpr, 0, times));
            index++;
        }
        return ctx.mkOr(timesExpr);
    }

    /**
     * make sure that lower priority events or non-interruption events
     * cannot  interrupt the fragments
     * @param head the head of the target int fragment
     * @param tail the tail of the target int fragment
     * @param inf the target fragment
     * @return boolean expression
     */
    private BoolExpr encodeUninterrupted(Event head, Event tail, IntFragment inf)
    {
        Set<Event> events = getLeIntEvents(inf);
        events.addAll(cleanEvents);
        List<BoolExpr> subs = new ArrayList<>();
        for (Event ev: events) {
            subs.add(ctx.mkNot(ctx.mkAnd(encodeStrictPreAndSucc(head, ev),
                    encodeStrictPreAndSucc(ev, tail))));
        }
        return w.mkAndNotEmpty(subs);
    }

    private Optional<BoolExpr> encodeMask(List<SDComponent> children,
                                          List<Integer> loopQueue)
    {
        Map<String, List<Pair<Event, Event>>> map = consMaskInstructionMap(children);
        List<BoolExpr> subs = new ArrayList<>();
        for (String maskVar: map.keySet()) {
            List<Pair<Event, Event>> list = map.get(maskVar);
            for (Pair<Event, Event> p: list) {
                List<Pair<Event, Event>> hts = getIntVirtualHTBatch(maskMap.get(maskVar));
                for (Pair<Event, Event> ht: hts) {
                    subs.add(ctx.mkOr(encodePreAndSucc(ht.getRight(), extendEvent(p.getLeft(), loopQueue)),
                            encodePreAndSucc(extendEvent(p.getRight(), loopQueue), ht.getLeft())));
                }
            }
        }
        return w.mkAnd(subs);
    }

    private Map<String, List<Pair<Event, Event>>> consMaskInstructionMap(List<SDComponent> children)
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
        return map;
    }

    /**
     * if the event contains mask instruction, put it on the map
     * @param map mask variable -> [(turn-off event, turn-on event)...]
     * @param e the given event
     */
    private void appendInstruction(Map<String, List<Pair<Event, Event>>> map,
                                   Event e)
    {
        if (e.getInstruction() != null) {
            Pair<String, Integer> ins = e.parseInstruction();
            String v = ins.getLeft();
            Integer state = ins.getRight();
            if (state == 0) {
                if (!map.containsKey(v)) {
                    map.put(v, new ArrayList<>());
                }
                Pair<Event, Event> p = new Pair<>();
                p.setLeft(e);
                map.get(v).add(p);
            } else if (ins.getRight() == 1) {
                int size = map.get(v).size();
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
        encodeCP(propMap, taskPropMap, candidates, loopQueue).ifPresent(propertyExpr::add);
    }

    private Optional<BoolExpr> encodeConstraints(List<SDComponent> children,
                                                 List<Instance> covered,
                                                 List<Integer> loopQueue) throws Z3Exception
    {
        Set<Pair<String, String>> candidates = new HashSet<>(getCPCandidate(children, covered));
        return encodeCP(consMap, taskConsMap, candidates, loopQueue);
    }

    /**
     * encode constraints or properties
     * @param map constraint or property map
     * @param taskMap  constraint or property map of task
     * @param candidates the candidates where maybe contain constraints or properties
     * @param loopQueue the loop queue
     * @return the boolean expression
     * @throws Z3Exception
     */
    private Optional<BoolExpr> encodeCP(Map<Pair<String, String>, String> map,
                                        Map<Pair<String, String>, String> taskMap,
                                        Set<Pair<String, String>> candidates,
                                        List<Integer> loopQueue) throws Z3Exception
    {
        String prefix = loopPrefix(loopQueue);
        List<BoolExpr> exprs = new ArrayList<>();
        for (Pair<String, String> can: candidates) {
            encodeSingleNormalCP(prefix, map, can, exprs);
            encodeSingleTaskCP(prefix, taskMap, can, exprs, loopQueue);
        }
        return w.mkAnd(exprs);
    }

    private void encodeSingleNormalCP(String prefix,
                                      Map<Pair<String, String>, String> map,
                                      Pair<String, String> can,
                                      List<BoolExpr> exprs) throws Z3Exception
    {
        if (map.containsKey(can)) {
            String conStr = map.get(can)
                    .replaceAll(can.getLeft(), prefix + can.getLeft())
                    .replaceAll(can.getRight(), prefix + can.getRight());
            exprs.add(p.convert(conStr));
        }
    }

    private void encodeSingleTaskCP(String prefix,
                                    Map<Pair<String, String>, String> taskMap,
                                    Pair<String, String> can,
                                    List<BoolExpr> exprs,
                                    List<Integer> loopQueue) throws Z3Exception
    {
        if (taskMap.containsKey(can)) {
            List<BoolExpr> num = new ArrayList<>();
            // count how many interruptions happen in the task
            for (int i = 0; i <= unfoldInts.size(); i++) {
                if (i == 0) { // no interruption happens
                    String conStr = taskMap.get(can)
                            .replaceAll(can.getLeft(), prefix + can.getLeft())
                            .replaceAll(can.getRight(), prefix + can.getRight());
                    num.add(p.convert(conStr));
                } else {
                    List<Set<Pair<IntFragment, Integer>>> collector = new ArrayList<>();
                    permutation(unfoldInts, i, new HashSet<>(), collector);
                    for (Set<Pair<IntFragment, Integer>> per: collector) {
                        BoolExpr identity = encodeIntFlag(per, can, loopQueue);
                        ArithExpr base = w.mkSub(prefix + can.getLeft(), prefix + can.getRight());
                        List<BoolExpr> seq = new ArrayList<>();
                        for (Pair<IntFragment, Integer> p: per) {
                            base = ctx.mkSub(base, getIntFragTime(p));
                            // make sure that int fragment inside the task
                            seq.add(ctx.mkAnd(
                                        ctx.mkLe(w.mkRealVar(prefix + can.getRight()), getIntFragHead(p)),
                                        ctx.mkLe(getIntFragTail(p), w.mkRealVar(prefix + can.getLeft()))
                            ));
                        }
                        // TODO, multiple operators, now support <=
                        Pair<ArithExpr, ArithExpr> interval = p.getInterval(taskMap.get(can));
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

    private BoolExpr encodeIntFlag(Set<Pair<IntFragment, Integer>> per,
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

    /**
     * encode relation between a message
     * @param m message
     * @return bool expression
     */
    private BoolExpr encodeLoopMessage(Message m,
                                       List<Integer> loopQueue)
    {
        return encodePreAndSucc(extendEvent(m.getFrom(), loopQueue), extendEvent(m.getTo(), loopQueue));
    }

    /**
     * copy event according to the loop queue
     * @param e the event which needs to be copied
     * @param loopQueue the loop queue
     * @return the copy event
     */
    private Event extendEvent(Event e,
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
    private BoolExpr encodePreAndSucc(Event pre, Event succ)
    {
        RealExpr start = w.mkRealVar(pre.getName());
        RealExpr end = w.mkRealVar(succ.getName());
        return ctx.mkGe(end, start);
    }

    private BoolExpr encodeStrictPreAndSucc(Event pre, Event succ)
    {
        RealExpr start = w.mkRealVar(pre.getName());
        RealExpr end = w.mkRealVar(succ.getName());
        return ctx.mkGt(end, start);
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

    /**
     * generate permutations for interruption fragments
     * @param list the int fragments
     * @param n the permutation number
     * @param current the current permutation
     * @param collector the result collector
     */
    private void permutation(List<Pair<IntFragment, Integer>> list,
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
            permutation($.listSubEle(list, p), n - 1, next, collector);
        }
    }

    /**
     * construct int fragment name: fragment name + index
     * @param p fragment with index
     * @return the full name of fragment
     */
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

    private BoolExpr encodePathLabel(String label)
    {
        return ctx.mkEq(w.mkStringVar("path_label"), ctx.mkString(label));
    }

}

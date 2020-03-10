package edu.nju.seg.solver;

import com.microsoft.z3.*;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Z3Util;

import java.util.*;

public class SequenceEncoder {

    private SequenceDiagram diagram;

    private SolverManager manager;

    private Context ctx;

    private int bound;

    private ExprParser p;

    private List<IntFragment> flow;

    /** the container fragment without int fragment **/
    private Fragment clean;

    private BoolExpr propertyExpr;

    private Map<Pair<String, String>, String> consMap;

    private Map<Pair<String, String>, String> propMap;

    private Map<Pair<String, String>, String> actualConsMap;

    private Map<Pair<String, String>, String> actualPropMap;

    private SortedMap<Integer, List<IntFragment>> priorityMap;

    private Set<Event> cleanEvents;

    private Map<String, IntFragment> maskMap;

    private List<Pair<IntFragment, Integer>> unfoldInts;

    public SequenceEncoder(SequenceDiagram diagram,
                           SolverManager manager,
                           int bound) {
        this.diagram = diagram;
        this.manager = manager;
        this.ctx = manager.getContext();
        this.bound = bound;
        this.flow = new ArrayList<>();
        this.p = new ExprParser(ctx);
    }

    public void encode() {
        this.clean = extractIntFrag(diagram.getContainer());
        calConsAndProp();
        calPriorityMap();
        calCleanEvents();
        calMaskMap();
        unfoldIntFrag();
        BoolExpr cleanExpr = encodeCleanFrag(clean, new ArrayList<>());
        Optional<BoolExpr> intExpr = encodeIntFrag();
        if (intExpr.isPresent()) {
            manager.addClause(ctx.mkAnd(cleanExpr, intExpr.get(), ctx.mkNot(propertyExpr)));
        } else {
            manager.addClause(ctx.mkAnd(cleanExpr, ctx.mkNot(propertyExpr)));
        }
    }

    /**
     * extract interrupt fragment from the original diagram
     * @param container the container fragment
     */
    private Fragment extractIntFrag(Fragment container) {
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
    private List<SDComponent> filterChildren(List<SDComponent> origin) {
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
    private void calConsAndProp() {
        if (consMap == null) {
            consMap = new HashMap<>();
        }
        if (propMap == null) {
            propMap = new HashMap<>();
        }
        if (actualConsMap == null) {
            actualConsMap = new HashMap<>();
        }
        if (actualPropMap == null) {
            actualPropMap = new HashMap<>();
        }
        List<String> conStr = diagram.getConstraints();
        for (String c: conStr) {
            if (c.contains("^")) {
                String simple = getSimpleExpr(c);
                actualConsMap.put(p.getVarFromConstraint(simple), simple);
            } else {
                consMap.put(p.getVarFromConstraint(c), c);
            }
        }
        List<String> propStr = diagram.getProperties();
        for (String c: propStr) {
            if (c.contains("^")) {
                String simple = getSimpleExpr(c);
                actualPropMap.put(p.getVarFromConstraint(simple), simple);
            } else {
                propMap.put(p.getVarFromConstraint(c), c);
            }
        }
    }

    private void calPriorityMap() {
        if (priorityMap == null) {
            priorityMap = new TreeMap<>();
        }
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

    private void calCleanEvents() {
        this.cleanEvents = getAllEventsInFrag(clean, new ArrayList<>());
    }

    private void calMaskMap() {
        if (maskMap == null) {
            maskMap = new HashMap<>();
        }
        for (IntFragment f: flow) {
            if (f.getMaskVar() != null) {
                maskMap.put(f.getMaskVar(), f);
            }
        }
    }

    private void unfoldIntFrag() {
        if (unfoldInts == null) {
            unfoldInts = new ArrayList<>();
        }
        for (IntFragment f: flow) {
            for (int i = 0; i < f.getMax(); i++) {
                unfoldInts.add(new Pair<>(f, i));
            }
        }
    }

    private List<Pair<Event, Event>> getIntFragHeadTail(IntFragment f) {
        List<Pair<Event, Event>> result = new ArrayList<>();
        for (int i = 0; i < f.getMax(); i++) {
            List<Integer> loopQueue = new ArrayList<>();
            loopQueue.add(i);
            result.add(new Pair<>(copyEvent(f.getVirtualHead(), loopQueue),
                    copyEvent(f.getVirtualTail(), loopQueue)));
        }
        return result;
    }

    private Set<Event> getAllEventsInFrag(Fragment f, List<Integer> loopQueue) {
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

    private Set<IntFragment> getLowerIntFrag(IntFragment inf) {
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

    private Set<Event> getLowerIntEvents(IntFragment inf) {
        Set<Event> result = new HashSet<>();
        Set<IntFragment> frags = getLowerIntFrag(inf);
        for (IntFragment frag: frags) {
            for (int i = 0; i < frag.getMax(); i++) {
                result.addAll(getAllEventsInFrag(frag, $.addToList(new ArrayList<>(), i)));
            }
        }
        return result;
    }

    /**
     * construct the order of each instance inside a fragment
     * @param covered the instances covered by the fragment
     * @param children the elements in the fragment
     * @return the map of the order of the instances: {instance: order list}
     */
    private Map<Instance, List<Event>> consLifeSpanMapWithLoop(List<Instance> covered,
                                                               List<SDComponent> children,
                                                               List<Integer> loopQueue) {
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
                                                       List<SDComponent> children) {
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

    private Map<Instance, List<Event>> sumUpLifeSpanMap(List<Map<Instance, List<Event>>> list) {
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

    private Map<Instance, List<Event>> consLoopBlockMap(List<Instance> covered,
                                                        List<SDComponent> children,
                                                        List<Integer> loopQueue,
                                                        int loop) {
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
    private Set<Pair<Event, Event>> consRelation(Map<Instance, List<Event>> map) {
        Set<Pair<Event, Event>> result = new HashSet<>();
        for (List<Event> order: map.values()) {
            for (int i = 0; i < order.size() - 1; i++) {
                result.add(new Pair<>(order.get(i), order.get(i + 1)));
            }
        }
        return result;
    }

    private Set<Event> getHeadsOf(Map<Instance, List<Event>> map) {
        Set<Event> heads = new HashSet<>();
        for (List<Event> order: map.values()) {
            heads.add(order.get(0));
        }
        return heads;
    }

    private Set<Event> getTailsOf(Map<Instance, List<Event>> map) {
        Set<Event> tails = new HashSet<>();
        for (List<Event> order: map.values()) {
            tails.add(order.get(order.size() - 1));
        }
        return tails;
    }

    private Set<Pair<String, String>> getCPCandidate(List<SDComponent> list,
                                                     List<Instance> covered) {
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

    private BoolExpr encodeRelation(Set<Pair<Event, Event>> set) {
        BoolExpr[] subs = new BoolExpr[set.size()];
        int i = 0;
        for (Pair<Event, Event> p: set) {
            subs[i] = encodeTwoEvents(p.getLeft(), p.getRight());
            i++;
        }
        return ctx.mkAnd(subs);
    }

    private BoolExpr encodeWholeRelation(Event virtualHead,
                                         Event virtualTail,
                                         Map<Instance, List<Event>> orders) {
        Set<Pair<Event, Event>> relation = consRelation(orders);
        BoolExpr relationExpr = encodeRelation(relation);
        Set<Event> heads = getHeadsOf(orders);
        Set<Event> tails = getTailsOf(orders);
        BoolExpr[] ht = new BoolExpr[heads.size() + tails.size()];
        int index = 0;
        for (Event h: heads) {
            ht[index] = encodeTwoEvents(virtualHead, h);
            index++;
        }
        for (Event t: tails) {
            ht[index] = encodeTwoEvents(t, virtualTail);
            index++;
        }
        return ctx.mkAnd(relationExpr, ctx.mkAnd(ht));
    }

    private BoolExpr encodeCleanFrag(Fragment f, List<Integer> loopQueue) {
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

    private Optional<BoolExpr> encodeIntFrag() {
        if ($.isBlankList(flow)) {
            return Optional.empty();
        }
        BoolExpr[] subs = new BoolExpr[flow.size()];
        for (int i = 0; i < flow.size(); i++) {
            subs[i] = encodeSingleIntFrag(flow.get(i));
        }
        return Optional.of(ctx.mkAnd(subs));
    }

    private BoolExpr encodeSingleIntFrag(IntFragment f) {
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

    private BoolExpr encodeUninterrupted(Event head, Event tail, IntFragment inf) {
        Set<Event> events = getLowerIntEvents(inf);
        events.addAll(cleanEvents);
        BoolExpr[] subs = new BoolExpr[events.size()];
        int index = 0;
        for (Event ev: events) {
            subs[index] = ctx.mkNot(ctx.mkAnd(encodeTwoEvents(head, ev),
                    encodeTwoEvents(ev, tail)));
            index++;
        }
        return ctx.mkAnd(subs);
    }

    private BoolExpr scanMask(List<SDComponent> children, List<Integer> loopQueue) {
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
                List<Pair<Event, Event>> hts = getIntFragHeadTail(maskMap.get(maskVar));
                for (Pair<Event, Event> ht: hts) {
                    subs.add(ctx.mkOr(encodeTwoEvents(ht.getRight(), copyEvent(p.getLeft(), loopQueue)),
                            encodeTwoEvents(copyEvent(p.getRight(), loopQueue), ht.getLeft())));
                }
            }
        }
        return ctx.mkAnd(subs.toArray(new BoolExpr[0]));
    }

    private void appendInstruction(Map<String, List<Pair<Event, Event>>> map,
                                   Event e) {
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

    private void encodeProperties(Fragment f, List<Integer> loopQueue) {
        Set<Pair<String, String>> candidates = new HashSet<>();
        if (f instanceof AltFragment) {
            AltFragment af = (AltFragment) f;
            candidates.addAll(getCPCandidate(af.getChildren(), af.getCovered()));
            candidates.addAll(getCPCandidate(af.getElseChildren(), af.getCovered()));
        } else {
            candidates.addAll(getCPCandidate(f.getChildren(), f.getCovered()));
        }
        BoolExpr propExpr = encodeCP(propMap, actualPropMap, candidates, loopQueue);
        if (this.propertyExpr == null) {
            this.propertyExpr = propExpr;
        } else {
            this.propertyExpr = ctx.mkAnd(this.propertyExpr, propExpr);
        }
    }

    private BoolExpr encodeConstraints(List<SDComponent> children,
                                       List<Instance> covered,
                                       List<Integer> loopQueue) {
        Set<Pair<String, String>> candidates = new HashSet<>(getCPCandidate(children, covered));
        return encodeCP(consMap, actualConsMap, candidates, loopQueue);
    }

    private BoolExpr encodeCP(Map<Pair<String, String>, String> map,
                              Map<Pair<String, String>, String> actualMap,
                              Set<Pair<String, String>> candidates,
                              List<Integer> loopQueue) {
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
                        List<Pair<IntFragment, Integer>> list = unfoldInts.subList(0, i);
                        List<Set<Pair<IntFragment, Integer>>> collector = new ArrayList<>();
                        permutation(list, i, new HashSet<>(), collector);
                        for (Set<Pair<IntFragment, Integer>> per: collector) {
                            BoolExpr identity = yieldIntIdentity(per, can, loopQueue);
                            Pair<ArithExpr, ArithExpr> interval = p.getInterval(actualMap.get(can));
                            ArithExpr base = Z3Util.mkSub(prefix + can.getLeft(),
                                    prefix + can.getRight(), ctx);
                            List<BoolExpr> seq = new ArrayList<>();
                            RealSort rs = ctx.mkRealSort();
                            for (Pair<IntFragment, Integer> p: per) {
                                base = ctx.mkSub(base, getIntFragTime(p));
                                seq.add(ctx.mkAnd(ctx.mkLe((RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + can.getRight()), rs),
                                            getIntFragHead(p)),
                                        ctx.mkLe(getIntFragTail(p), (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + can.getLeft()), rs))));
                            }
                            // TODO
                            BoolExpr cleanCP;
                            if (interval.getLeft() == null) {
                                cleanCP = ctx.mkLe(base, interval.getRight());
                            } else if (interval.getRight() == null) {
                                cleanCP = ctx.mkLe(interval.getLeft(), base);
                            } else {
                                cleanCP = ctx.mkAnd(ctx.mkLe(interval.getLeft(), base),
                                        ctx.mkLe(base, interval.getRight()));
                            }
                            num.add(ctx.mkAnd(identity, cleanCP, Z3Util.mkAndNotEmpty(seq, ctx)));
                        }
                    }
                }
                Z3Util.mkOr(num, ctx).ifPresent(exprs::add);
            }
        }
        return ctx.mkAnd(exprs.toArray(new BoolExpr[0]));
    }

    private BoolExpr yieldIntIdentity(Set<Pair<IntFragment, Integer>> per,
                                      Pair<String, String> interval,
                                      List<Integer> loopQueue) {
        List<BoolExpr> exprs = new ArrayList<>();
        String flag = getCPFlag(interval, loopQueue);
        SeqExpr flagExpr = ctx.mkString(flag);
        SeqSort ss = ctx.mkStringSort();
        for (Pair<IntFragment, Integer> p: per) {
            exprs.add(ctx.mkEq(ctx.mkConst(ctx.mkSymbol(getIntFragVar(p)), ss), flagExpr));
        }
        return Z3Util.mkAndNotEmpty(exprs, ctx);
    }

    private BoolExpr encodeContainer(Fragment f, List<Integer> loopQueue) {
        encodeProperties(f, loopQueue);
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(f.getCovered(),
                f.getChildren(), loopQueue);
        BoolExpr relationExpr = encodeWholeRelation(copyEvent(f.getVirtualHead(), loopQueue),
                copyEvent(f.getVirtualTail(), loopQueue),
                orders);
        BoolExpr constraints = encodeConstraints(f.getChildren(), f.getCovered(), loopQueue);
        BoolExpr maskExpr = scanMask(f.getChildren(), loopQueue);
        return ctx.mkAnd(encodeGeZero(copyEvent(f.getVirtualHead(), loopQueue)), relationExpr,
                encodeChildren(f.getChildren(), loopQueue), constraints, maskExpr);
    }

    private BoolExpr encodeOptFragment(OptFragment of, List<Integer> loopQueue) {
        encodeProperties(of, loopQueue);
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(of.getCovered(),
                of.getChildren(), loopQueue);
        BoolExpr constraints = encodeConstraints(of.getChildren(), of.getCovered(), loopQueue);
        BoolExpr maskExpr = scanMask(of.getChildren(), loopQueue);
        return ctx.mkAnd(p.convert(of.getCondition()),
                encodeWholeRelation(copyEvent(of.getVirtualHead(), loopQueue),
                        copyEvent(of.getVirtualTail(), loopQueue), orders),
                encodeChildren(of.getChildren(), loopQueue), constraints, maskExpr);
    }

    private BoolExpr encodeAltFragment(AltFragment af, List<Integer> loopQueue) {
        encodeProperties(af, loopQueue);
        BoolExpr[] altExpr = new BoolExpr[2];
        Map<Instance, List<Event>> orders = consLifeSpanMapWithLoop(af.getCovered(),
                af.getChildren(), loopQueue);
        Map<Instance, List<Event>> elseOrders = consLifeSpanMapWithLoop(af.getCovered(),
                af.getElseChildren(), loopQueue);
        Event virtualHead = copyEvent(af.getVirtualHead(), loopQueue);
        Event virtualTail = copyEvent(af.getVirtualTail(), loopQueue);
        BoolExpr ifConstraints = encodeConstraints(af.getChildren(), af.getCovered(), loopQueue);
        BoolExpr elseConstraints = encodeConstraints(af.getElseChildren(), af.getCovered(), loopQueue);
        BoolExpr ifMaskExpr = scanMask(af.getChildren(), loopQueue);
        BoolExpr elseMaskExpr = scanMask(af.getElseChildren(), loopQueue);
        altExpr[0] = ctx.mkAnd(p.convert(af.getCondition()),
                encodeWholeRelation(virtualHead, virtualTail, orders),
                encodeChildren(af.getChildren(), loopQueue), ifConstraints, ifMaskExpr);
        altExpr[1] = ctx.mkAnd(p.convert(af.getAltCondition()),
                encodeWholeRelation(virtualHead, virtualTail, elseOrders),
                encodeChildren(af.getElseChildren(), loopQueue), elseConstraints, elseMaskExpr);
        return ctx.mkOr(altExpr);
    }

    private BoolExpr encodeLoopFragment(LoopFragment lf, List<Integer> loopQueue) {
        BoolExpr[] maxExpr = new BoolExpr[lf.getMax()];
        for (int i = 0; i < lf.getMax(); i++) {
            List<Integer> nextLoopQueue = $.addToList(loopQueue, i);
            BoolExpr constraints = encodeConstraints(lf.getChildren(), lf.getCovered(), nextLoopQueue);
            BoolExpr maskExpr = scanMask(lf.getChildren(), nextLoopQueue);
            maxExpr[i] = ctx.mkAnd(encodeChildren(lf.getChildren(), nextLoopQueue), constraints, maskExpr);
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

    private BoolExpr encodeChildren(List<SDComponent> children, List<Integer> loopQueue) {
        BoolExpr[] subs = new BoolExpr[children.size()];
        for (int i = 0; i < children.size(); i++) {
            SDComponent child = children.get(i);
            if (child instanceof Message) {
                subs[i] = encodeLoopMessage((Message) child, loopQueue);
            } else {
                subs[i] = encodeCleanFrag((Fragment) child, loopQueue);
            }
        }
        return ctx.mkAnd(subs);
    }

    /**
     * encode relation between a message
     * @param m message
     * @return bool expression
     */
    private BoolExpr encodeLoopMessage(Message m, List<Integer> loopQueue) {
        return encodeTwoEvents(copyEvent(m.getFrom(), loopQueue), copyEvent(m.getTo(), loopQueue));
    }

    /**
     * copy event according to the loop queue
     * @param e the event which needs to be copied
     * @param loopQueue the loop queue
     * @return the copy event
     */
    private Event copyEvent(Event e, List<Integer> loopQueue) {
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
    private String loopPrefix(List<Integer> loopQueue) {
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
    private BoolExpr encodeTwoEvents(Event pre, Event succ) {
        RealSort rs = ctx.mkRealSort();
        RealExpr start = (RealExpr) ctx.mkConst(pre.getName(), rs);
        RealExpr end = (RealExpr) ctx.mkConst(succ.getName(), rs);
        return ctx.mkGt(end, start);
    }

    private BoolExpr encodeGeZero(Event head) {
        RealExpr start = (RealExpr) ctx.mkConst(head.getName(), ctx.mkRealSort());
        return ctx.mkGe(start, ctx.mkReal(0));
    }

    /**
     * convert actual time expressions to simple time expressions
     * @param expr the actual time expression
     * @return the simple time expression
     */
    private String getSimpleExpr(String expr) {
        return expr.replaceAll("\\^", "")
                .replaceAll("\\(", "")
                .replaceAll("\\)", "");
    }

    private void permutation(List<Pair<IntFragment, Integer>> list,
                             int n,
                             Set<Pair<IntFragment, Integer>> current,
                             List<Set<Pair<IntFragment, Integer>>> result) {
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

    private String getIntFragVar(Pair<IntFragment, Integer> p) {
        return p.getLeft().getName() + "-" + p.getRight();
    }

    private String getCPFlag(Pair<String, String> p, List<Integer> loopQueue) {
        String prefix = loopPrefix(loopQueue);
        return prefix + p.getLeft() + "-" + prefix + p.getRight();
    }

    private ArithExpr getIntFragTime(Pair<IntFragment, Integer> p) {
        String prefix = "loop_" + p.getRight() + "_";
        return Z3Util.mkSub(prefix + p.getLeft().getVirtualTail().getName(),
                prefix + p.getLeft().getVirtualHead().getName(), ctx);
    }

    private RealExpr getIntFragHead(Pair<IntFragment, Integer> p) {
        String prefix = "loop_" + p.getRight() + "_";
        return (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + p.getLeft().getVirtualHead().getName()),
                ctx.mkRealSort());
    }

    private RealExpr getIntFragTail(Pair<IntFragment, Integer> p) {
        String prefix = "loop_" + p.getRight() + "_";
        return (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + p.getLeft().getVirtualTail().getName()),
                ctx.mkRealSort());
    }

}

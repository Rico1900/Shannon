package edu.nju.seg.parser;

import com.microsoft.z3.*;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Z3Wrapper;
import org.scijava.parse.ExpressionParser;
import org.scijava.parse.Operator;
import org.scijava.parse.SyntaxTree;
import org.scijava.parse.Variable;


public class ExprParser {

    private Context ctx;

    private Z3Wrapper w;

    public ExprParser(Context ctx)
    {
        this.ctx = ctx;
        this.w = new Z3Wrapper(ctx);
    }

    /**
     * convert arithmetic string to z3 bool expression
     * @param expr arithmetic expression string
     * @return z3 bool expression
     */
    public BoolExpr convert(String expr) throws Z3Exception
    {
        SyntaxTree tree = new ExpressionParser().parseTree(expr);
        int depth = treeDepth(tree);
        if (depth == 2) {
            return handleSimpleExpr(tree);
        } else if (depth == 3) {
            return handleExpr(tree);
        } else if (depth == 4) {
            return handleComplexExpr(tree);
        } else {
            throw new EncodeException("wrong expression: " + expr);
        }
    }

    /**
     * convert string to expression according to the bound,
     * used by the automation encoder
     * @param expr the expression
     * @param bound the bound k
     * @return the bool expression
     * @throws Z3Exception
     */
    public BoolExpr convertWithBound(String expr,
                                     int bound) throws Z3Exception
    {
        SyntaxTree tree = new ExpressionParser().parseTree(expr);
        int depth = treeDepth(tree);
        if (depth == 2) {
            String boundExpr = getTokenStr(tree.child(0)) + "_" + bound +
                    ((Operator) tree.token()).getToken() +
                    getTokenStr(tree.child(1));
            return convert(boundExpr);
        } else {
            throw new EncodeException("can not extract variable from " + expr);
        }
    }

    public Pair<String, String> getVarFromCP(String cons)
    {
        SyntaxTree tree = new ExpressionParser().parseTree(cons);
        int depth = treeDepth(tree);
        if (depth == 4) {
            String leftVar;
            String rightVar;
            SyntaxTree left = tree.child(0);
            SyntaxTree right = tree.child(1);
            if (isBlankOperator(left.token()) || $.isNumber(left.token())) {
                leftVar = getTokenStr(right.child(0).child(0));
                rightVar = getTokenStr(right.child(0).child(1));
            } else {
                leftVar = getTokenStr(left.child(1).child(0));
                rightVar = getTokenStr(left.child(1).child(1));
            }
            return new Pair<>(leftVar, rightVar);
        } else if (depth == 3) {
            String leftVar;
            String rightVar;
            SyntaxTree left = tree.child(0);
            SyntaxTree right = tree.child(1);
            if (isBlankOperator(left.token()) || $.isNumber(left.token())) {
                leftVar = getTokenStr(right.child(0));
                rightVar = getTokenStr(right.child(1));
            } else {
                leftVar = getTokenStr(left.child(0));
                rightVar = getTokenStr(left.child(1));
            }
            return new Pair<>(leftVar, rightVar);
        } else {
            throw new EncodeException("wrong constraints: " + cons);
        }
    }

    public Pair<ArithExpr, ArithExpr> getInterval(String cons) throws Z3Exception
    {
        SyntaxTree tree = new ExpressionParser().parseTree(cons);
        int depth = treeDepth(tree);
        if (depth == 4) {
            String leftVar;
            String rightVar;
            SyntaxTree left = tree.child(0);
            SyntaxTree right = tree.child(1);
            if (isBlankOperator(left.token()) || $.isNumber(left.token())) {
                leftVar = getTokenStr(left);
                rightVar = getTokenStr(right.child(1));
            } else {
                leftVar = getTokenStr(left.child(0));
                rightVar = getTokenStr(right);
            }
            return new Pair<>(w.mkRealExpr(leftVar),
                    w.mkRealExpr(rightVar));
        } else if (depth == 3) {
            String leftVar = null;
            String rightVar = null;
            SyntaxTree left = tree.child(0);
            SyntaxTree right = tree.child(1);
            if (isBlankOperator(left.token()) || $.isNumber(left.token())) {
                leftVar = getTokenStr(left);
            } else {
                rightVar = getTokenStr(right);
            }
            return new Pair<>(w.mkRealExpr(leftVar),
                    w.mkRealExpr(rightVar));
        } else {
            throw new EncodeException("wrong constraints: " + cons);
        }
    }

    private BoolExpr handleSimpleExpr(SyntaxTree tree) throws Z3Exception
    {
        String op = getTokenStr(tree);
        SyntaxTree left = tree.child(0);
        SyntaxTree right = tree.child(1);
        return w.mkOperatorExpr(op, mkExprByToken(left), mkExprByToken(right));
    }

    private BoolExpr handleExpr(SyntaxTree tree) throws Z3Exception
    {
        String op = getTokenStr(tree);
        SyntaxTree left = tree.child(0);
        SyntaxTree right = tree.child(1);
        if (getTokenStr(left).equals("-")) {
            return w.mkOperatorExpr(op, mkSubtractExpr(left), mkExprByToken(right));
        } else {
            return w.mkOperatorExpr(op, mkExprByToken(left), mkSubtractExpr(right));
        }
    }

    private BoolExpr handleComplexExpr(SyntaxTree tree) throws Z3Exception
    {
        String leftOp;
        String rightOp;
        ArithExpr leftNum;
        ArithExpr rightNum;
        ArithExpr middle;
        String op = getTokenStr(tree);
        SyntaxTree left = tree.child(0);
        SyntaxTree right = tree.child(1);
        if (isBlankOperator(left.token()) || $.isNumber(left.token())) {
            leftOp = op;
            rightOp = getTokenStr(right);
            leftNum = mkExprByToken(left);
            rightNum = mkExprByToken(right.child(1));
            middle = mkSubtractExpr(right.child(0));
        } else {
            leftOp = getTokenStr(left);
            rightOp = op;
            leftNum = mkExprByToken(left.child(0));
            rightNum = mkExprByToken(right);
            middle = mkSubtractExpr(left.child(1));
        }
        return ctx.mkAnd(w.mkOperatorExpr(leftOp, leftNum, middle),
                w.mkOperatorExpr(rightOp, middle, rightNum));
    }

    private ArithExpr mkSubtractExpr(SyntaxTree tree)
    {
        String left = getTokenStr(tree.child(0));
        String right = getTokenStr(tree.child(1));
        return w.mkSub(left, right);
    }

    private ArithExpr mkExprByToken(SyntaxTree tree) throws Z3Exception
    {
        return w.mkRealExpr(getTokenStr(tree));
    }

    /**
     * calculate the depth of the tree
     * @param tree  the tree
     * @return the depth of the tree
     */
    private int treeDepth(SyntaxTree tree)
    {
        if (tree.count() == 0 || isBlankOperator(tree.token())) {
            return 1;
        }
        int childDepth = 0;
        for (int i = 0; i < tree.count(); i++) {
            int temp = treeDepth(tree.child(i));
            childDepth = temp > childDepth ? temp : childDepth;
        }
        return 1 + childDepth;
    }

    /**
     * check blank operator
     * @param o blank operator candidate
     * @return if it is blank operator
     */
    private boolean isBlankOperator(Object o)
    {
        if (o instanceof Operator) {
            return ((Operator) o).getToken().equals("");
        }
        return false;
    }

    /**
     * get token according to the syntax tree
     * @param tree the syntax tree
     * @return string
     */
    private String getTokenStr(SyntaxTree tree)
    {
        Object t = tree.token();
        if (t instanceof Operator) {
            return ((Operator) t).getToken();
        } else if (t instanceof Variable) {
            return ((Variable) t).getToken();
        } else if (t instanceof Number) {
            return t.toString();
        } else if (t.toString().equals("")) {
            return getTokenStr(tree.child(0));
        } else {
            return t.toString();
        }
    }

}

package edu.nju.seg.parser;

import com.microsoft.z3.*;
import edu.nju.seg.model.EncodeException;
import org.scijava.parse.ExpressionParser;
import org.scijava.parse.Operator;
import org.scijava.parse.SyntaxTree;
import org.scijava.parse.Variable;

public class ExprParser {

    private Context ctx;

    public ExprParser(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * convert arithmetic string to z3 bool expression
     * @param expr arithmetic expression string
     * @return z3 bool expression
     */
    public BoolExpr convert(String expr) {
        try {
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
        } catch (Exception e) {
            throw new EncodeException("wrong expression: " + expr + ", message: " + e.getMessage());
        }
    }

    /**
     * get variable from expression
     * @param expr arithmetic expression string
     * @return variable string
     */
    public String getVar(String expr) {
        SyntaxTree tree = new ExpressionParser().parseTree(expr);
        int depth = treeDepth(tree);
        if (depth == 2) {
            return ((Variable) tree.child(0).token()).getToken();
        } else {
            throw new EncodeException("can not extract variable from " + expr);
        }
    }

    public BoolExpr convertK(String expr, int bound) {
        SyntaxTree tree = new ExpressionParser().parseTree(expr);
        int depth = treeDepth(tree);
        if (depth == 2) {
            String boundExpr = ((Variable) tree.child(0).token()).getToken() + "_" + bound +
                    ((Operator) tree.token()).getToken() +
                    getNum(tree.child(1));
            return convert(boundExpr);
        } else {
            throw new EncodeException("can not extract variable from " + expr);
        }
    }

    private BoolExpr handleSimpleExpr(SyntaxTree tree) {
        String op = getToken(tree);
        SyntaxTree left = tree.child(0);
        SyntaxTree right = tree.child(1);
        return mkBoolExpr(op, mkExpr(left), mkExpr(right));
    }

    private BoolExpr handleExpr(SyntaxTree tree) {
        String op = getToken(tree);
        SyntaxTree left = tree.child(0);
        SyntaxTree right = tree.child(1);
        if (((Operator) left.token()).getToken().equals("-")) {
            return mkBoolExpr(op, mkSubExpr(left), mkExpr(right));
        } else {
            return mkBoolExpr(op, mkExpr(left), mkSubExpr(right));
        }
    }

    private BoolExpr handleComplexExpr(SyntaxTree tree) {
        String leftOp;
        String rightOp;
        ArithExpr leftNum;
        ArithExpr rightNum;
        ArithExpr middle;
        String op = getToken(tree);
        SyntaxTree left = tree.child(0);
        SyntaxTree right = tree.child(1);
        if (isBlankOperator(left.token())) {
            leftOp = op;
            rightOp = getToken(right);
            leftNum = mkExpr(left);
            rightNum = mkExpr(right.child(1));
            middle = mkSubExpr(right.child(0));
        } else {
            leftOp = getToken(left);
            rightOp = op;
            leftNum = mkExpr(left.child(0));
            rightNum = mkExpr(right);
            middle = mkSubExpr(left.child(1));
        }
        return ctx.mkAnd(mkBoolExpr(leftOp, leftNum, middle), mkBoolExpr(rightOp, middle, rightNum));
    }

    private ArithExpr mkSubExpr(SyntaxTree tree) {
        Variable left = (Variable) tree.child(0).token();
        Variable right = (Variable) tree.child(1).token();
        RealSort rs = ctx.mkRealSort();
        RealExpr v1 = (RealExpr) ctx.mkConst(ctx.mkSymbol(left.getToken()), rs);
        RealExpr v2 = (RealExpr) ctx.mkConst(ctx.mkSymbol(right.getToken()), rs);
        return ctx.mkSub(v1, v2);
    }

    private ArithExpr mkExpr(SyntaxTree tree) {
        Object token = tree.token();
        if (token instanceof Variable) {
            String tokenStr = ((Variable) token).getToken();
            RealSort rs = ctx.mkRealSort();
            return (RealExpr) ctx.mkConst(ctx.mkSymbol(tokenStr), rs);
        } else if (token instanceof Number) {
            return mkExprDouble(token.toString());
        } else if (token instanceof Operator) {
            return mkExprDouble(getNum(tree.child(0)));
        } else {
            throw new EncodeException("wrong syntax tree: " + tree);
        }
    }

    private ArithExpr mkExprDouble(String s) {
        return ctx.mkReal(s);
    }

    private BoolExpr mkBoolExpr(String operator,
                                ArithExpr left,
                                ArithExpr right) {
        switch (operator) {
            case "<":
                return ctx.mkLt(left, right);
            case "<=":
                return ctx.mkLe(left, right);
            case ">":
                return ctx.mkGt(left, right);
            case ">=":
                return ctx.mkGe(left, right);
            case "=":
                return ctx.mkEq(left, right);
            case "!=":
                return ctx.mkNot(ctx.mkEq(left, right));
            default:
                throw new EncodeException("wrong expression operator: " + operator);
        }
    }

    /**
     * calculate the depth of the tree
     * @param tree  the tree
     * @return the depth of the tree
     */
    private int treeDepth(SyntaxTree tree) {
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
    private boolean isBlankOperator(Object o) {
        if (o instanceof Operator) {
            return ((Operator) o).getToken().equals("");
        }
        return false;
    }

    private String getToken(SyntaxTree tree) {
        return ((Operator) tree.token()).getToken();
    }

    private String getNum(SyntaxTree tree) {
        return tree.token().toString();
    }


}

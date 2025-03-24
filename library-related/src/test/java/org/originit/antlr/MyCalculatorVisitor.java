package org.originit.antlr;

import org.originit.antlr.gen.CalculatorBaseVisitor;
import org.originit.antlr.gen.CalculatorParser;

public class MyCalculatorVisitor extends CalculatorBaseVisitor<Object> {

    @Override
    public Object visitAddSub(CalculatorParser.AddSubContext ctx) {
        Float left = (Float) visit(ctx.expr(0));
        Float right = (Float) visit(ctx.expr(1));
        String cal = ctx.getChild(1).getText();
        if (cal.equals("+")) {
            return left + right;
        } else if (cal.equals("-")) {
            return left - right;
        }
        return 0f;
    }

    @Override
    public Object visitMulDiv(CalculatorParser.MulDivContext ctx) {
        Float left = (Float) visit(ctx.expr(0));
        Float right = (Float) visit(ctx.expr(1));
        String cal = ctx.getChild(1).getText();
        if (cal.equals("*")) {
            return left * right;
        } else if (cal.equals("/")) {
            return left / right;
        }
        return 0f;
    }

    @Override
    public Object visitParenExpr(CalculatorParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Object visitFloat(CalculatorParser.FloatContext ctx) {
        return Float.parseFloat(ctx.getText());
    }
}

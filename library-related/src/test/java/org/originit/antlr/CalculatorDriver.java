package org.originit.antlr;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.originit.antlr.gen.CalculatorLexer;
import org.originit.antlr.gen.CalculatorParser;
import org.originit.antlr.gen.CalculatorVisitor;

public class CalculatorDriver {

    public static void main(String[] args) {
        CalculatorLexer lexer = new CalculatorLexer(CharStreams.fromString("1 + 1 * (1+ 1/2 - 100)"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CalculatorParser parser = new CalculatorParser(tokens);
        ParseTree tree = parser.expr();
        CalculatorVisitor<Object> visitor = new MyCalculatorVisitor();
        System.out.println(visitor.visit(tree));
    }
}

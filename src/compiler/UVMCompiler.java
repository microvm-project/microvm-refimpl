package compiler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import parser.uIRLexer;
import parser.uIRListenerImpl;
import parser.uIRParser;
import uvm.BasicBlock;
import uvm.Function;
import uvm.Instruction;
import uvm.MicroVM;

public class UVMCompiler {
    
    public static final String file = "/Users/apple/uvm-bench/micro-bm/int-prime-number/prime-number.uir";
    
    public static void main(String[] args) {

        try {
            // create a CharStream that reads from standard input
            ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(file));
            // create a lexer that feeds off of input CharStream
            uIRLexer lexer = new uIRLexer(input);
            // create a buffer of tokens pulled from the lexer
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            // create a parser that feeds off the tokens buffer
            uIRParser parser = new uIRParser(tokens);
            ParseTree tree = parser.ir(); // begin parsing at init rule
            
            System.out.println("Parsing Tree:");
            System.out.println(tree.toStringTree(parser));
            System.out.println();
            
            // Create a generic parse tree walker that can trigger callbacks
            ParseTreeWalker walker = new ParseTreeWalker();
            // Walk the tree created during the parse, trigger callbacks
            walker.walk(new uIRListenerImpl(), tree);
            System.out.println(); // print a \n after translation
            
            // see the tree
            for (Function f : MicroVM.v.funcs.values()) {
                System.out.println("function " + f.getName() + " of " + f.getSig());
                for (BasicBlock bb : f.getBBs()) {
                    System.out.println("BB[" + bb.getName() + "]:");
                    for (Instruction inst : bb.getInsts()) {
                        System.out.println(inst.prettyPrint());
                    }
                    System.out.println();
                }
                System.out.println();
            }
            
            // get uses
            DefUseGeneration.execute();
            
            IRTreeGeneration.execute();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    public static final void error(String message) {
        System.err.print(message);
        Thread.dumpStack();
        System.exit(1);
    }
}
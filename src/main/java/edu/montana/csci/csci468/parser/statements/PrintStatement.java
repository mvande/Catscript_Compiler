package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

public class PrintStatement extends Statement {
    private Expression expression;

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }


    public Expression getExpression() {
        return expression;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        getProgram().print(expression.evaluate(runtime));
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        code.addVarInstruction(Opcodes.ALOAD, 0); //puts the "this" pointer onto operand stack
        expression.compile(code);                     // will add all bytecode of the expression to the program
        box(code, expression.getType());
        code.addMethodInstruction(Opcodes.INVOKEVIRTUAL, //invoke the virtual method print on the parent class
                code.getProgramInternalName(),
                "print",
                "(Ljava/lang/Object;)V");
    }

}

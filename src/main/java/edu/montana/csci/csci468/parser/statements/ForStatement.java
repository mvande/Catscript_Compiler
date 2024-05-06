package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import javax.lang.model.type.PrimitiveType;
import java.util.*;

public class ForStatement extends Statement {
    private Expression expression;
    private String variableName;
    private List<Statement> body;

    public void setExpression(Expression expression) {
        this.expression = addChild(expression);
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setBody(List<Statement> statements) {
        this.body = new LinkedList<>();
        for (Statement statement : statements) {
            this.body.add(addChild(statement));
        }
    }

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public List<Statement> getBody() {
        return body;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        symbolTable.pushScope();
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            expression.validate(symbolTable);
            CatscriptType type = expression.getType();
            if (type instanceof CatscriptType.ListType) {
                symbolTable.registerSymbol(variableName, getComponentType());
            } else {
                addError(ErrorType.INCOMPATIBLE_TYPES, getStart());
                symbolTable.registerSymbol(variableName, CatscriptType.OBJECT);
            }
        }
        for (Statement statement : body) {
            statement.validate(symbolTable);
        }
        symbolTable.popScope();
    }

    private CatscriptType getComponentType() {
        return ((CatscriptType.ListType) expression.getType()).getComponentType();
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        List evaluate = (List) expression.evaluate(runtime);
        for (Object o : evaluate) {
            runtime.pushScope();
            runtime.setValue(variableName, o);
            for (Statement statement : body) {
                statement.execute(runtime);
            }
            runtime.popScope();
        }
//        super.execute(runtime);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        Integer iteratorSlot = code.nextLocalStorageSlot();
        Integer loopVariableSlot = code.createLocalStorageSlotFor(variableName);
        Label startOfLoop = new Label();
        Label endOfLoop = new Label();

        expression.compile(code); //leaves a list on the top of the operand stack
        //invoke interface List.iterator
        code.addMethodInstruction(Opcodes.INVOKEINTERFACE,
                ByteCodeGenerator.internalNameFor(List.class),
                "iterator",
                "()Ljava/util/Iterator;");
        //store iterator into the iterator slot
        code.addVarInstruction(Opcodes.ASTORE, iteratorSlot);

        //add startOfLoop label
        code.addLabel(startOfLoop);
        //ALOAD iterator slot
        code.addVarInstruction(Opcodes.ALOAD, iteratorSlot);
        //invoke interface hasNext
        code.addMethodInstruction(Opcodes.INVOKEINTERFACE,
                ByteCodeGenerator.internalNameFor(Iterator.class),
                "hasNext",
                "()Z");
        //IFEQ jump to endOfLoop label
        code.addJumpInstruction(Opcodes.IFEQ, endOfLoop);

        //ALOAD iterator again
        code.addVarInstruction(Opcodes.ALOAD, iteratorSlot);
        //invoke interface next() on it
        code.addMethodInstruction(Opcodes.INVOKEINTERFACE,
                ByteCodeGenerator.internalNameFor(Iterator.class),
                "next",
                "()Ljava/lang/Object;");
        //do a checkcast
        code.addTypeInstruction(Opcodes.CHECKCAST, ByteCodeGenerator.internalNameFor(getComponentType().getJavaType()));
        //save that into the loop variable slot (might be a boolean/int or ref type)
        if(getComponentType().equals(CatscriptType.INT) || getComponentType().equals(CatscriptType.BOOLEAN)) {
            unbox(code, getComponentType());
            code.addVarInstruction(Opcodes.ISTORE, loopVariableSlot);
        } else {
            code.addVarInstruction(Opcodes.ASTORE, loopVariableSlot);
        }

        //compile loop body statement
        for(Statement stmt : body) {
            stmt.compile(code);
        }

        //unconditional goto start of loop
        code.addJumpInstruction(Opcodes.GOTO, startOfLoop);

        //add end of loop variable
        code.addLabel(endOfLoop);
    }

}

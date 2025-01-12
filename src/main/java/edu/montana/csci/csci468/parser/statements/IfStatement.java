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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class IfStatement extends Statement {
    private Expression expression;
    private List<Statement> trueStatements = Collections.emptyList();
    private List<Statement> elseStatements = Collections.emptyList();

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = addChild(expression);
    }

    public List<Statement> getTrueStatements() {
        return trueStatements;
    }

    public void setTrueStatements(List<Statement> statements) {
        this.trueStatements = new LinkedList<>();
        for (Statement statement : statements) {
            this.trueStatements.add(addChild(statement));
        }
    }

    public List<Statement> getElseStatements() {
        return elseStatements;
    }

    public void setElseStatements(List<Statement> statements) {
        this.elseStatements = new LinkedList<>();
        for (Statement statement : statements) {
            this.elseStatements.add(addChild(statement));
        }
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
        if (!expression.getType().equals(CatscriptType.BOOLEAN)) {
            expression.addError(ErrorType.INCOMPATIBLE_TYPES);
        }
        symbolTable.pushScope();
        for (Statement trueStatement : trueStatements) {
            trueStatement.validate(symbolTable);
        }
        symbolTable.popScope();
        symbolTable.pushScope();
        for (Statement elseStatement : elseStatements) {
            elseStatement.validate(symbolTable);
        }
        symbolTable.popScope();
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        if((Boolean) expression.evaluate(runtime)) {
            for (Statement statement : trueStatements) {
                statement.execute(runtime);
            }
        } else if (!(Boolean) expression.evaluate(runtime)) {
            for (Statement statement : elseStatements) {
                statement.execute(runtime);
            }
        } else {
            super.execute(runtime);
        }
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        Label endLabel = new Label();
        Label falseLabel = new Label();

        expression.compile(code);
        code.addJumpInstruction(Opcodes.IFEQ, falseLabel);
        //generate bytecode for all the true branch
        for (Statement stmt : trueStatements) {
            stmt.compile(code);
        }
        //unconditional jump over any else statements
        code.addJumpInstruction(Opcodes.GOTO, endLabel);
        //add the false label
        code.addLabel(falseLabel);
        //generate bytecode for else (if any)
        for (Statement stmt : elseStatements) {
            stmt.compile(code);
        }
        //add the end label
        code.addLabel(endLabel);
    }
}

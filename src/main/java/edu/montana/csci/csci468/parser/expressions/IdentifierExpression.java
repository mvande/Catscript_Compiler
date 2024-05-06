package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import org.objectweb.asm.Opcodes;

public class IdentifierExpression extends Expression {
    private final String name;
    private CatscriptType type;

    public IdentifierExpression(String value) {
        this.name = value;
    }

    public String getName() {
        return name;
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        boolean hasName = symbolTable.hasSymbol(name);
        CatscriptType type = symbolTable.getSymbolType(getName());
        if (type == null) {
            addError(ErrorType.UNKNOWN_NAME);
        } else {
            this.type = type;
        }
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        return runtime.getValue(name);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        Integer i = code.resolveLocalStorageSlotFor(name);
        if(i != null) {
            //look up in the given slot
            if(getType().equals(CatscriptType.INT) || getType().equals(CatscriptType.BOOLEAN)) {
                code.addVarInstruction(Opcodes.ILOAD, i);
            } else {
                code.addVarInstruction(Opcodes.ALOAD, i);
            }
        } else {
            //load as field
            //test the type again
            code.addVarInstruction(Opcodes.ALOAD, 0);
            if(getType().equals(CatscriptType.INT) || getType().equals(CatscriptType.BOOLEAN)) {
                code.addFieldInstruction(Opcodes.GETFIELD, name, "I", code.getProgramInternalName());
            } else {
                code.addFieldInstruction(Opcodes.GETFIELD, name, "L" + ByteCodeGenerator.internalNameFor(getType().getJavaType()) + ";", code.getProgramInternalName());
            }
        }
    }


}

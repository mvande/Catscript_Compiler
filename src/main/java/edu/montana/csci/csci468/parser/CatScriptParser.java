package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement funcStatement = parseFunctionDef();
        if (funcStatement != null) {
            return funcStatement;
        }
        return parseStatement();
    }

    private Statement parseStatement() {
        Statement forStmt = parseForStatement();
        if (forStmt != null) {
            return forStmt;
        }
        Statement ifStmt = parseIfStatement();
        if (ifStmt != null) {
            return ifStmt;
        }
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        Statement varStmt = parseVarStatement();
        if (varStmt != null) {
            return varStmt;
        }
        Statement assignOrFuncCallStmt = parseAssignOrFuncCallStatement();
        if (assignOrFuncCallStmt != null) {
            return assignOrFuncCallStmt;
        }
        if(currentFunctionDefinition != null) {
            Statement returnStmt = parseReturnStatement();
            if(returnStmt != null) {
                return returnStmt;
            }
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseReturnStatement() {
        if (tokens.match(RETURN)) {
            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setStart(tokens.consumeToken());

            if(tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                returnStatement.setExpression(parseExpression());
                returnStatement.setEnd(returnStatement.getExpression().getEnd());
            } else {
                returnStatement.setEnd(returnStatement.getStart());
            }
            returnStatement.setFunctionDefinition(currentFunctionDefinition);

            return returnStatement;
        }
        return null;
    }

    private Statement parseFunctionDef() {
        if (tokens.match(FUNCTION)) {
            FunctionDefinitionStatement funcDefStatement = new FunctionDefinitionStatement();
            funcDefStatement.setStart(tokens.consumeToken());

            funcDefStatement.setName(require(IDENTIFIER, funcDefStatement).getStringValue());

            require(LEFT_PAREN, funcDefStatement);
            if(!tokens.match(RIGHT_PAREN)) {
                do {
                    Token parameter = tokens.consumeToken();
                    TypeLiteral paramType = new TypeLiteral();
                    paramType.setType(CatscriptType.OBJECT);
                    if (tokens.matchAndConsume(COLON)) {
                        paramType.setType(parseTypeExpression().getType());
                    }
                    funcDefStatement.addParameter(parameter.getStringValue(), paramType);
                } while(tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
            }
            require(RIGHT_PAREN, funcDefStatement);
            if (tokens.matchAndConsume(COLON))  {
                funcDefStatement.setType(parseTypeExpression());
            } else {
                TypeLiteral type = new TypeLiteral();
                type.setType(CatscriptType.VOID);
                funcDefStatement.setType(type);
            }

            require(LEFT_BRACE, funcDefStatement);
            currentFunctionDefinition = funcDefStatement;
            //TODO implement function body
            LinkedList<Statement> body = new LinkedList<>();
            while (!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()) {
                body.add(parseStatement());
            }
            funcDefStatement.setEnd(require(RIGHT_BRACE, funcDefStatement));
            funcDefStatement.setBody(body);
            return funcDefStatement;
        }
        return null;
    }

    private Statement parseForStatement() {
        if(tokens.match(FOR)) {
            ForStatement forStatement = new ForStatement();
            forStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, forStatement);
            forStatement.setVariableName(require(IDENTIFIER, forStatement).getStringValue());
            require(IN, forStatement);
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);

            require(LEFT_BRACE, forStatement);
            List<Statement> stms = new ArrayList<>();
            while(tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                Statement statement = parseStatement();
                stms.add(statement);
            }
            forStatement.setEnd(require(RIGHT_BRACE, forStatement));
            forStatement.setBody(stms);

            return forStatement;
        }
        return null;
    }

    private Statement parseIfStatement() {
        if (tokens.match(IF)) {
            Token ifToken = tokens.consumeToken();
            IfStatement ifStatement = new IfStatement();
            ifStatement.setStart(ifToken);

            require(LEFT_PAREN, ifStatement);
            Expression testExpression = parseExpression();
            ifStatement.setExpression(testExpression);
            require(RIGHT_PAREN, ifStatement);

            require(LEFT_BRACE, ifStatement);
            List<Statement> ifStms = new ArrayList<>();
            while(tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                Statement trueStatement = parseStatement();
                ifStms.add(trueStatement);
            }
            ifStatement.setEnd(require(RIGHT_BRACE, ifStatement));
            ifStatement.setTrueStatements(ifStms);

            if (tokens.matchAndConsume(ELSE)) {
                List<Statement> elseStms = new ArrayList<>();
                if (tokens.match(IF)) {
                    elseStms.add(parseIfStatement());
                } else {
                    require(LEFT_BRACE, ifStatement);
                    while (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                        Statement elseStatement = parseStatement();
                        elseStms.add(elseStatement);
                    }
                }
                ifStatement.setEnd(require(RIGHT_BRACE, ifStatement));
                ifStatement.setElseStatements(elseStms);
            }
            return ifStatement;
        }
        return null;
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    private Statement parseVarStatement() {
        if(tokens.match(VAR)) {
            VariableStatement varStatement = new VariableStatement();
            varStatement.setStart(tokens.consumeToken());

            varStatement.setVariableName(require(IDENTIFIER, varStatement).getStringValue());

            if (tokens.matchAndConsume(COLON)) {
                varStatement.setExplicitType(parseTypeExpression().getType()); //TODO implement parseTypeExpression function
            }

            require(EQUAL, varStatement);
            varStatement.setExpression(parseExpression());
            varStatement.setEnd(varStatement.getExpression().getEnd());

            return varStatement;
        }
        return null;
    }

    private Statement parseAssignOrFuncCallStatement() {
        if (tokens.match(IDENTIFIER)) {
            Token identifierToken = tokens.consumeToken();
            if(tokens.matchAndConsume(LEFT_PAREN)) {
                FunctionCallExpression funcCallExpr = parseFunctionCallExpression(identifierToken);
                FunctionCallStatement funcCallStmt = new FunctionCallStatement(funcCallExpr);
                funcCallStmt.setStart(identifierToken);
                funcCallStmt.setEnd(funcCallStmt.getEnd());
                return funcCallStmt;
            } else {
                AssignmentStatement assignStatement = new AssignmentStatement();
                assignStatement.setStart(identifierToken);
                assignStatement.setVariableName(identifierToken.getStringValue());

                require(EQUAL, assignStatement);
                assignStatement.setExpression(parseExpression());
                assignStatement.setEnd(assignStatement.getExpression().getEnd());

                return assignStatement;
            }
        }
        return null;
    }

    //============================================================
    //  Expressions
    //============================================================

    private TypeLiteral parseTypeExpression() { //TODO implement types?
        if (tokens.match(IDENTIFIER)) {
            Token type = tokens.consumeToken();
            TypeLiteral typeLiteral = new TypeLiteral();
            switch (type.getStringValue()) {
                case "int":
                    typeLiteral.setType(CatscriptType.INT);
                    break;
                case "string":
                    typeLiteral.setType(CatscriptType.STRING);
                    break;
                case "bool":
                    typeLiteral.setType(CatscriptType.BOOLEAN);
                    break;
                case "list":
                    if (tokens.matchAndConsume(LESS)) {
                        typeLiteral.setType(new CatscriptType.ListType(parseTypeExpression().getType()));
                        System.out.println(typeLiteral.getType());
                        tokens.consumeToken();
                    } else {
                        typeLiteral.setType(new CatscriptType.ListType(CatscriptType.NULL));
                    }
                    break;
                case "object":
                    typeLiteral.setType(CatscriptType.OBJECT);
                    break;
                case "void":
                    typeLiteral.setType(CatscriptType.VOID);
                    break;
                default:
                    typeLiteral.setType(CatscriptType.NULL);
                    break;
            }
            return typeLiteral;
        }
        return null;
    }

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();
        while (tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rightHandSide.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rightHandSide.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }
    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(IDENTIFIER)) {
            Token identifierToken = tokens.consumeToken();
            if (tokens.matchAndConsume(LEFT_PAREN)) {
                return parseFunctionCallExpression(identifierToken);
            }
            IdentifierExpression identifierExpression = new IdentifierExpression(identifierToken.getStringValue());
            identifierExpression.setToken(identifierToken);
            return identifierExpression;
        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        } else if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(TRUE, FALSE)) {
            Token booleanToken = tokens.consumeToken();
            boolean val = booleanToken.getType().equals(TRUE);
            BooleanLiteralExpression booleanExpression = new BooleanLiteralExpression(val);
            booleanExpression.setToken(booleanToken);
            return booleanExpression;
        } 
        else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        }
        else if (tokens.match(LEFT_BRACKET)) {
            Token leftBracket = tokens.consumeToken();
            List<Expression> values = new ArrayList<>();
            if(!tokens.match(RIGHT_BRACKET)) {
                do {
                    Expression expression = parseExpression();
                    values.add(expression);
                } while(tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
            }
            ListLiteralExpression listExpression = new ListLiteralExpression(values);
            listExpression.setStart(leftBracket);
            listExpression.setEnd(require(RIGHT_BRACKET, listExpression, ErrorType.UNTERMINATED_LIST));
            return listExpression;
        }
        else if (tokens.match(LEFT_PAREN)) {
            Token rightParen = tokens.consumeToken();
            ParenthesizedExpression parenExpression = new ParenthesizedExpression(parseExpression());
            parenExpression.setStart(rightParen);
            parenExpression.setEnd(require(RIGHT_PAREN, parenExpression));
            return parenExpression;
        }
        else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    private FunctionCallExpression parseFunctionCallExpression(Token functionToken) {
        List<Expression> args = new ArrayList<>();
        if(!tokens.match(RIGHT_PAREN)) {
            do {
                Expression expression = parseExpression();
                args.add(expression);
            } while(tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
        }
        FunctionCallExpression funcExpression = new FunctionCallExpression(functionToken.getStringValue(), args);
        funcExpression.setStart(functionToken);
        funcExpression.setEnd(require(RIGHT_PAREN, funcExpression, ErrorType.UNTERMINATED_ARG_LIST));
        return funcExpression;
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}

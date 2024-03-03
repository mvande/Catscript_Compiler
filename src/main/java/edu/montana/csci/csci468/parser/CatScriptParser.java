package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.ArrayList;
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
        Statement funcStatement = parseFunctionStatement();
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
        Statement assignStmt = parseAssignStatement();
        if (assignStmt != null) {
            return assignStmt;
        }
        Statement funcCallStmt = parseFuncCallStatement();
        if (funcCallStmt != null) {
            return funcCallStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseFunctionStatement() {
        return null;
    }

    private Statement parseForStatement() {
        return null;
    }

    private Statement parseIfStatement() {
        if (tokens.match(IF)) {
            Token ifToken = tokens.consumeToken();
            IfStatement ifStatement = new IfStatement();

            require(LEFT_PAREN, ifStatement);
            Expression testExpression = parseExpression();
            ifStatement.setExpression(testExpression);
            require(RIGHT_PAREN, ifStatement);

            require(LEFT_BRACE, ifStatement);
            List<Statement> ifStms = new ArrayList<>();
            while(tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                Statement trueStatment = parseStatement();
                ifStms.add(trueStatment);
            }
            require(RIGHT_BRACE, ifStatement);
            ifStatement.setTrueStatements(ifStms);

            if (tokens.matchAndConsume(ELSE)) {
                List<Statement> elseStms = new ArrayList<>();
                if (tokens.match(IF)) {
                    elseStms.add(parseIfStatement());
                }
                require(LEFT_BRACE, ifStatement);
                while(tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                    Statement elseStatment = parseStatement();
                    elseStms.add(elseStatment);
                }
                require(RIGHT_BRACE, ifStatement);
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
        return null;
    }

    private Statement parseAssignStatement() {
        return null;
    }

    private Statement parseFuncCallStatement() {
        if (tokens.match(IDENTIFIER)) {
            Token functionToken = tokens.consumeToken();
            FunctionCallStatement funcCallStmt = new FunctionCallStatement(parseFunctionCallExpression(functionToken));
            return funcCallStmt;
        }
        return null;
    }

    //============================================================
    //  Expressions
    //============================================================

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

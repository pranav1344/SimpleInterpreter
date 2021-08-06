package newpack.language.interprettest;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static newpack.language.interprettest.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }
    private Stmt declaration() {
        try {
            if (match(FUNCTION))
                return function("function");
            if(match(VAR))
                return varDeclaration();
            return statement();
        } catch (ParserError error) {
            synchronize();
            return null;
        }
    }
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Variable name expected");
        Expr initializer = null;
        if (match(ASSIGN)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after a variable declaration");
        return new Stmt.Var(name, initializer);
    }
    private Stmt statement() {
        if (match(FOR))
            return forStatement();
        if (match(IF))
            return ifStatement();
        if (match(PRINT))
            return printStatement();
        if (match(RETURN))
            return returnStatement();
        if (match(WHILE))
            return whileStatement();
        if (match(LEFT_BRACE))
            return new  Stmt.Block(block());
        return expressionStatement();
    }
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after for");
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }
        Expr condition = null;
        if (!check(SEMICOLON))
            condition = expression();
        consume(SEMICOLON, "Expect ';' in for loop");
        Expr change = null;
        if (!check(RIGHT_PAREN)) {
            change = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for loop clauses");
        Stmt body = statement();
        if (change != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(change)
                    )
            );
        }
        if (condition == null)
            condition = new Expr.Literal(TRUE);
        body = new Stmt.While(condition, body);
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after if");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value");
        return new Stmt.Print(value);
    }
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expected ';' after return statement");
        return new Stmt.Return(keyword, value);
    }
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after while");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after while");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }
    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value");
        return new Stmt.Expression(value);
    }
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Exptect" + kind + "name");
        consume(LEFT_PAREN, "Expect '(' after " + " name");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                parameters.add(consume(IDENTIFIER, "Expect parameter name"));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters");
        consume(LEFT_BRACE, "Expect '}' before" + kind + " body");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd())
            statements.add(declaration());
        consume(RIGHT_BRACE, "Right brace '}' expected");
        return statements;
    }

    private static class ParserError extends RuntimeException {
        
    }
    private Expr expression()   {
        return assignment();
    }
    private Expr assignment() {
        Expr expr = or();
        if (match(ASSIGN)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid target assignment");
        }
        return expr;
    }
    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr factor() {
        Expr expr = exponent();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = exponent();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr exponent() {
        Expr expr = unary();
        while (match(EXPON)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr unary() {
        if(match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after function arguments");
        return new Expr.Call(callee, paren, arguments);
    }
    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }
    private Expr primary() {
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(NONE))
            return new Expr.Literal(null);
        if (match(NUMBER, STRING, INTEGER))
            return new Expr.Literal(previous().literal);
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();
        throw error(peek(), message);
    }
    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        if (peek().type == type)
            return true;
        return false;
    }
    private Token advance() {
        if(!isAtEnd())
            current++;
        return previous();
    }
    private boolean isAtEnd() {
        if(peek().type == EOF)
            return true;
        return false;
    }
    private Token peek() {
        return tokens.get(current);
    }
    private Token previous() {
        return tokens.get(current - 1);
    }
    private ParserError error(Token token, String message) {
        Language.error(token, message);
        return new ParserError();
    }
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;
            switch (peek().type) {
                case CLASS:
                case FUNCTION:
                case VAR:
                case FOR:
                case DO:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}

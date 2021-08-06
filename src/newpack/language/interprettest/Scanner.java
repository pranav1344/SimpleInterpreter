package newpack.language.interprettest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.*;

import static newpack.language.interprettest.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("or", OR);
        keywords.put("else", ELSE);
        keywords.put("for", FOR);
        keywords.put("while", WHILE);
        keywords.put("function", FUNCTION);
        keywords.put("do", DO);
        keywords.put("if", IF);
        keywords.put("none", NONE);
        keywords.put("class", CLASS);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("this", THIS);
        keywords.put("var", VAR);
        keywords.put("super", SUPER);
        keywords.put("true", TRUE);
        keywords.put("false", FALSE);
    }
    Scanner(String source) {
        this.source = source;
    }
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }
    private boolean isAtEnd() {
        if (current >= source.length())
            return true;
        return false;
    }
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA);  break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '^': addToken(EXPON); break;
            case '%': addToken(PERCENT); break;
            case '/': addToken(SLASH); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL : ASSIGN);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '#':
                while (peek() != '\n' && !isAtEnd()) {
                    advance();
                }
                break;
            case '"':
                string();
                break;
            case ' ':
            case '\r':
            case  '\t':
                break;
            case '\n':
                line++;
                break;
            default:
                if(isDigit(c))
                    number();
                else if (isAlphaNumeric(c)) {
                    identifier();
                }else
                    Language.error(line, "Unexpected character");
                break;
        }
    }
    private char advance() {
        return source.charAt(current++);
    }
    private void addToken(TokenType type) {
        addToken(type, null);
    }
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start,current);
        tokens.add(new Token(type, text, literal, line));
    }
    private char peek () {
        if(isAtEnd())
            return '\0';
        return source.charAt(current);
    }
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++;
            advance();
        }
        if(isAtEnd()) {
            Language.error(line, "A string literal hasn't been terminiated");
            return;
        }
        advance();
        String value = source.substring(start+1, current-1);
        addToken(STRING, value);
    }
    private boolean isDigit(char c) {
        if(c>='0' && c<= '9')
            return true;
        return false;
    }
    private void number() {
        while (isDigit(peek()))
            advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek()))
                advance();
            addToken(NUMBER, new BigDecimal(source.substring(start, current)));
        }
        else
            addToken(INTEGER, new BigInteger(source.substring(start, current)));
    }
    private boolean isAlpha(char c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_'))
            return true;
        return false;
    }
    private boolean isAlphaNumeric(char c) {
        if (isDigit(c) || isAlpha(c))
            return true;
        return false;
    }
    private char peekNext() {
        if (current + 1 > source.length())
            return '\0';
        return source.charAt(current+1);
    }
    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (source.charAt(current) == expected) {
            current++;
            return true;
        }
        return false;
    }
    private void identifier() {
        while (isAlphaNumeric(peek()))
            advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null)
            type = IDENTIFIER;
        addToken(type);
    }

}

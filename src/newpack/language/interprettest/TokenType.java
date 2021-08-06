package newpack.language.interprettest;

enum TokenType {
    // Single character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COMMA, HASH,
    DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR, PERCENT, EXPON,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, ASSIGN,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER, INTEGER,

    //Keywords
    AND, CLASS, ELSE, FALSE, FUNCTION, FOR, IF, NONE, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, DO,


    EOF
}
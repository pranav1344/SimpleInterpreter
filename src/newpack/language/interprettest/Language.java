package newpack.language.interprettest;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import  java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;

public class Language {
    static boolean  hadError = false;
    static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if(args.length > 1) {
            System.out.println("In use [script]");
            System.exit(64);
        } else if(args.length == 1) {
            runFile(args[0]);
        } else {
            runFilePrompt();
        }
    }
    private static void runFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        run(new String(bytes, Charset.defaultCharset()));
        if(hadError)
            System.exit(65);
        if (hadRuntimeError)
            System.exit(70);
    }
    private static void runFilePrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        while (true) {
            System.out.println("> ");
            String line = reader.readLine();
            if (line == null)
                break;
            run(line);
            hadError = false;
        }
    }
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List <Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if (hadError)
            return;
        interpreter.interpret(statements);
    }
    static void error(int line, String errorMessage) {
        reportError(line, "", errorMessage);
    }
    private static void reportError(int line, String location, String message) {
        System.err.println("[line : " + line + "] Error" + location +": "+ message);
    }
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF)
            reportError(token.line, " at end ", message);
        else
            reportError(token.line, " at '" + token.lexeme + "' ", message);
    }
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line : " + error.token.line);
        hadRuntimeError = true;
    }
}

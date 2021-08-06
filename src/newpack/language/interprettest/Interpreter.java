package newpack.language.interprettest;
import java.lang.Math;
import java.util.List;
import java.math.*;
import java.util.ArrayList;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new LanguageCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return new BigDecimal((double)System.currentTimeMillis()/1000.0);
            }
        });
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (truthify(left)) {
                return left;
            } else if (!truthify(left)) {
                return left;
            }
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case BANG:
                return !truthify(right);
            case MINUS:
                checkIfOperandNumber(expr.operator, right);
                return ((BigDecimal)right).multiply(BigDecimal.valueOf(-1));
        }
        return null;
    }


    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LanguageFunction function = new LanguageFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (truthify(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        }
        else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null)
            value = evaluate(stmt.value);
        throw new Return(value);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (truthify(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                int operatType = checkIfOperandNumber(expr.operator, left, right);
                switch (operatType){
                    case 0:
                        return ((BigDecimal)left).subtract((BigDecimal)right);
                    case 1:
                        return ((BigInteger)left).subtract((BigInteger)right);
                    case 2:
                        return (new BigDecimal((BigInteger)left).subtract((BigDecimal)right));
                    case 3:
                        return ((BigDecimal)left).subtract(new BigDecimal((BigInteger)right));
                }
            case PLUS:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                switch (operatType){
                    case 0:
                        return ((BigDecimal)left).add((BigDecimal)right);
                    case 1:
                        return ((BigInteger)left).add((BigInteger)right);
                    case 2:
                        return (new BigDecimal((BigInteger)left).add((BigDecimal)right));
                    case 3:
                        return ((BigDecimal)left).add(new BigDecimal((BigInteger)right));
                }
            case STAR:
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                operatType = checkIfOperandNumber(expr.operator, left, right);
                switch (operatType){
                    case 0:
                        return ((BigDecimal)left).multiply((BigDecimal)right);
                    case 1:
                        return ((BigInteger)left).multiply((BigInteger)right);
                    case 2:
                        return (new BigDecimal((BigInteger)left).multiply((BigDecimal)right));
                    case 3:
                        return ((BigDecimal)left).multiply(new BigDecimal((BigInteger)right));
                }
                break;
            case SLASH:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                switch (operatType){
                    case 0:
                        return new BigDecimal(((BigDecimal)left).divide((BigDecimal)right, 10000, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString());
                    case 1:
                        return ((BigInteger)left).divide((BigInteger)right);
                    case 2:
                        return new BigDecimal((new BigDecimal((BigInteger)left).divide((BigDecimal)right, 10000, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()));
                    case 3:
                        return new BigDecimal(((BigDecimal)left).divide(new BigDecimal((BigInteger)right), 10000, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString());
                }
            case PERCENT:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                if (operatType == 1) {
                    ((BigInteger)left).mod((BigInteger)right);
                }
                return 0.00;
            case EXPON:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                switch (operatType){
                    case 0:
                    case 2:
                    case 1:
                    case 3:
                        return "infinity";
                }
            case GREATER:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                int a = 0;
                switch (operatType){
                    case 0:
                        a = ((BigDecimal)left).compareTo((BigDecimal)right);
                        break;
                    case 1:
                        a = ((BigInteger)left).compareTo((BigInteger) right);
                        break;
                    case 2:
                        a = (new BigDecimal((BigInteger)left)).compareTo((BigDecimal)right);
                        break;
                    case 3:
                        a = ((BigDecimal)left).compareTo(new BigDecimal((BigInteger)right));
                }
                return a==1;
            case LESS:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                a = 0;
                switch (operatType){
                    case 0:
                        a = ((BigDecimal)left).compareTo((BigDecimal)right);
                        break;
                    case 1:
                        a = ((BigInteger)left).compareTo((BigInteger) right);
                        break;
                    case 2:
                        a = (new BigDecimal((BigInteger)left)).compareTo((BigDecimal)right);
                        break;
                    case 3:
                        a = ((BigDecimal)left).compareTo(new BigDecimal((BigInteger)right));
                }
                return a==-1;
            case GREATER_EQUAL:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                a = 0;
                switch (operatType){
                    case 0:
                        a = ((BigDecimal)left).compareTo((BigDecimal)right);
                        break;
                    case 1:
                        a = ((BigInteger)left).compareTo((BigInteger) right);
                        break;
                    case 2:
                        a = (new BigDecimal((BigInteger)left)).compareTo((BigDecimal)right);
                        break;
                    case 3:
                        a = ((BigDecimal)left).compareTo(new BigDecimal((BigInteger)right));
                }
                return a==1 || a==0;
            case LESS_EQUAL:
                operatType = checkIfOperandNumber(expr.operator, left, right);
                a = 0;
                switch (operatType){
                    case 0:
                        a = ((BigDecimal)left).compareTo((BigDecimal)right);
                        break;
                    case 1:
                        a = ((BigInteger)left).compareTo((BigInteger) right);
                        break;
                    case 2:
                        a = (new BigDecimal((BigInteger)left)).compareTo((BigDecimal)right);
                        break;
                    case 3:
                        a = ((BigDecimal)left).compareTo(new BigDecimal((BigInteger)right));
                }
                return a==-1 || a==0;
            case BANG_EQUAL:
                return !checkequality(left, right);
            case EQUAL:
                return checkequality(left, right);

        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        if (!(callee instanceof LanguageCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }

        LanguageCallable function = (LanguageCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + "arguments but got" +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initialization != null) {
            value = evaluate(stmt.initialization);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    private boolean truthify(Object object) {
        if(object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean)object;
        return true;
    }
    private boolean checkequality(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        if (a instanceof  BigInteger && b instanceof BigDecimal)
            return (new BigDecimal((BigInteger)a).compareTo((BigDecimal)b) == 0);
        if (a instanceof BigDecimal && b instanceof BigInteger)
            return (new BigDecimal((BigInteger)b).compareTo((BigDecimal)a) == 0);
        return a.equals(b);
    }
    private void checkIfOperandNumber(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    private int checkIfOperandNumber(Token operator, Object left, Object right) {
        if (left instanceof BigDecimal && right instanceof BigDecimal)
            return 0;
        if (left instanceof BigInteger && right instanceof BigInteger)
            return 1;
        if (left instanceof  BigInteger && right instanceof BigDecimal)
            return 2;
        if (left instanceof BigDecimal && right instanceof BigInteger)
            return 3;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements)
                execute(statement);
        } catch (RuntimeError error) {
            Language.runtimeError(error);
        }
    }
    private void execute(Stmt statement) {
        statement.accept(this);
    }
    void executeBlock(List <Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private String stringify(Object object) {
        if (object == null)
            return "None";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

}

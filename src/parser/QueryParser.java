package parser;

import core.logic.Column;
import core.logic.DataType;
import core.logic.Database;
import core.logic.Row;
import parser.Lexer.Token;
import parser.Lexer.Type;
import parser.cmd.Command;
import parser.cmd.CreateIndexCmd;
import parser.cmd.CreateTableCmd;
import parser.cmd.DeleteCmd;
import parser.cmd.DropTableCmd;
import parser.cmd.InsertCmd;
import parser.cmd.SelectCmd;
import parser.cmd.UpdateCmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class QueryParser {

    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList(
            "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "FROM", "WHERE", "INTO",
            "VALUES", "SET", "TABLE", "INDEX", "ON", "PRIMARY", "KEY", "AND", "OR",
            "TRUE", "FALSE", "NULL"));

    private final Database db;

    public QueryParser(Database db) {
        if (db == null) throw new IllegalArgumentException("База данных не может быть null");
        this.db = db;
    }

    public ArrayList<Command> parse(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new QueryParseException("Пустой запрос", 0);
        }
        Cursor c = new Cursor(Lexer.tokenize(sql));
        ArrayList<Command> out = new ArrayList<>();
        while (!c.atEof()) {
            if (c.acceptSymbol(";")) continue;
            parseStatement(c, out);
            if (!c.atEof() && !c.peek().isSymbol(";")) {
                throw c.error("Лишний токен " + c.peek().describe() + " после конца запроса");
            }
        }
        if (out.isEmpty()) {
            throw new QueryParseException("Пустой запрос", 0);
        }
        return out;
    }

    private void parseStatement(Cursor c, ArrayList<Command> out) {
        Token t = c.peek();
        if (t.isKeyword("INSERT")) {
            parseInsert(c, out);
        } else if (t.isKeyword("SELECT")) {
            out.add(parseSelect(c));
        } else if (t.isKeyword("UPDATE")) {
            out.add(parseUpdate(c));
        } else if (t.isKeyword("DELETE")) {
            out.add(parseDelete(c));
        } else if (t.isKeyword("CREATE")) {
            c.next();
            if (c.peek().isKeyword("TABLE")) {
                out.add(parseCreateTable(c));
            } else if (c.peek().isKeyword("INDEX")) {
                out.add(parseCreateIndex(c));
            } else {
                throw c.error("После CREATE ожидалось TABLE или INDEX, найдено " + c.peek().describe());
            }
        } else if (t.isKeyword("DROP")) {
            c.next();
            c.expectKeyword("TABLE");
            out.add(new DropTableCmd(c.expectIdent("имя таблицы")));
        } else {
            throw c.error("Ожидалось INSERT, SELECT, UPDATE, DELETE, CREATE или DROP, найдено " + t.describe());
        }
    }

    private void parseInsert(Cursor c, ArrayList<Command> out) {
        c.expectKeyword("INSERT");
        c.expectKeyword("INTO");
        String table = c.expectIdent("имя таблицы");

        ArrayList<String> explicit_cols = null;
        if (c.acceptSymbol("(")) {
            explicit_cols = new ArrayList<>();
            do {
                String col = c.expectIdent("имя колонки");
                if (explicit_cols.contains(col)) {
                    throw c.error("Колонка '" + col + "' указана дважды");
                }
                explicit_cols.add(col);
            } while (c.acceptSymbol(","));
            c.expectSymbol(")");
        }
        c.expectKeyword("VALUES");

        do {
            int tuple_pos = c.peek().pos;
            c.expectSymbol("(");
            List<Object> values = new ArrayList<>();
            do {
                values.add(parseLiteral(c));
            } while (c.acceptSymbol(","));
            c.expectSymbol(")");

            ArrayList<String> cols = explicit_cols != null
                    ? new ArrayList<>(explicit_cols)
                    : columnsOf(table, out, tuple_pos);
            if (cols.size() != values.size()) {
                throw new QueryParseException("Для таблицы '" + table + "' ожидалось значений: "
                        + cols.size() + ", передано: " + values.size(), tuple_pos);
            }
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < cols.size(); i++) {
                map.put(cols.get(i), values.get(i));
            }
            out.add(new InsertCmd(table, cols, new Row(map)));
        } while (c.acceptSymbol(","));
    }

    private ArrayList<String> columnsOf(String table, List<Command> parsed_so_far, int pos) {
        for (int i = parsed_so_far.size() - 1; i >= 0; i--) {
            Command cmd = parsed_so_far.get(i);
            if (cmd instanceof CreateTableCmd && cmd.table().equals(table)) {
                return new ArrayList<>(((CreateTableCmd) cmd).getScheme().keySet());
            }
        }
        if (!db.hasTable(table)) {
            throw new QueryParseException("Неизвестная таблица '" + table + "'", pos);
        }
        return new ArrayList<>(db.getTable(table).getScheme().keySet());
    }

    private SelectCmd parseSelect(Cursor c) {
        c.expectKeyword("SELECT");
        ArrayList<String> cols = new ArrayList<>();
        if (c.acceptSymbol("*")) {
            cols.add("*");
        } else {
            do {
                cols.add(c.expectIdent("имя колонки или *"));
            } while (c.acceptSymbol(","));
        }
        c.expectKeyword("FROM");
        String table = c.expectIdent("имя таблицы");
        Where w = parseOptionalWhere(c);
        return new SelectCmd(table, cols, w.col, w.op, w.val);
    }

    private UpdateCmd parseUpdate(Cursor c) {
        c.expectKeyword("UPDATE");
        String table = c.expectIdent("имя таблицы");
        c.expectKeyword("SET");
        LinkedHashMap<String, Object> set = new LinkedHashMap<>();
        do {
            String col = c.expectIdent("имя колонки");
            if (set.containsKey(col)) {
                throw c.error("Колонке '" + col + "' значение присваивается дважды");
            }
            c.expectSymbol("=");
            set.put(col, parseLiteral(c));
        } while (c.acceptSymbol(","));
        Where w = parseOptionalWhere(c);
        return new UpdateCmd(table, set, w.col, w.op, w.val);
    }

    private DeleteCmd parseDelete(Cursor c) {
        c.expectKeyword("DELETE");
        c.expectKeyword("FROM");
        String table = c.expectIdent("имя таблицы");
        Where w = parseOptionalWhere(c);
        return new DeleteCmd(table, w.col, w.op, w.val);
    }

    private CreateTableCmd parseCreateTable(Cursor c) {
        c.expectKeyword("TABLE");
        String table = c.expectIdent("имя таблицы");
        c.expectSymbol("(");
        LinkedHashMap<String, Column> scheme = new LinkedHashMap<>();
        do {
            String name = c.expectIdent("имя колонки");
            if (scheme.containsKey(name)) {
                throw c.error("Колонка '" + name + "' объявлена дважды");
            }
            DataType type = parseType(c);
            boolean pk = false;
            if (c.acceptKeyword("PRIMARY")) {
                c.expectKeyword("KEY");
                pk = true;
            }
            scheme.put(name, new Column(name, type, pk));
        } while (c.acceptSymbol(","));
        c.expectSymbol(")");
        return new CreateTableCmd(table, scheme);
    }

    private CreateIndexCmd parseCreateIndex(Cursor c) {
        c.expectKeyword("INDEX");
        String index_name = c.expectIdent("имя индекса");
        c.expectKeyword("ON");
        String table = c.expectIdent("имя таблицы");
        c.expectSymbol("(");
        String col = c.expectIdent("имя колонки");
        c.expectSymbol(")");
        return new CreateIndexCmd(index_name, table, col);
    }

    private DataType parseType(Cursor c) {
        Token t = c.peek();
        if (t.type != Type.IDENT) {
            throw c.error("Ожидался тип колонки, найдено " + t.describe());
        }
        DataType type = switch (t.text.toUpperCase(Locale.ROOT)) {
            case "INT", "INTEGER" -> DataType.INT;
            case "FLOAT", "DOUBLE", "REAL" -> DataType.FLOAT;
            case "STRING", "VARCHAR", "TEXT" -> DataType.STRING;
            case "DATE" -> DataType.DATE;
            case "BOOLEAN", "BOOL" -> DataType.BOOLEAN;
            default -> throw c.error("Неизвестный тип " + t.describe() + " (доступны INT, FLOAT, STRING, DATE, BOOLEAN)");
        };
        c.next();
        if (c.acceptSymbol("(")) {
            if (c.peek().type != Type.NUMBER) {
                throw c.error("Ожидалась длина, найдено " + c.peek().describe());
            }
            c.next();
            c.expectSymbol(")");
        }
        return type;
    }

    private static final class Where {
        final String col;
        final String op;
        final Object val;

        Where(String col, String op, Object val) {
            this.col = col;
            this.op = op;
            this.val = val;
        }
    }

    private Where parseOptionalWhere(Cursor c) {
        if (!c.acceptKeyword("WHERE")) {
            return new Where(null, null, null);
        }
        String col = c.expectIdent("имя колонки");
        String op = parseOperator(c);
        int val_pos = c.peek().pos;
        Object val = parseLiteral(c);
        if (val == null) {
            throw new QueryParseException("NULL нельзя использовать в WHERE", val_pos);
        }
        if (c.peek().isKeyword("AND") || c.peek().isKeyword("OR")) {
            throw c.error("Поддерживается только одно условие в WHERE");
        }
        return new Where(col, op, val);
    }

    private String parseOperator(Cursor c) {
        Token t = c.peek();
        if (t.type == Type.SYMBOL) {
            switch (t.text) {
                case "=", "!=", "<", ">", "<=", ">=" -> {
                    c.next();
                    return t.text;
                }
                case "<>" -> {
                    c.next();
                    return "!=";
                }
            }
        }
        throw c.error("Ожидался оператор сравнения (=, !=, <>, <, >, <=, >=), найдено " + t.describe());
    }

    private Object parseLiteral(Cursor c) {
        Token t = c.peek();
        if (t.type == Type.STRING) {
            c.next();
            return t.text;
        }
        if (t.type == Type.NUMBER) {
            c.next();
            return toNumber(t, false);
        }
        if (t.isSymbol("-") || t.isSymbol("+")) {
            c.next();
            Token num = c.peek();
            if (num.type != Type.NUMBER) {
                throw c.error("После '" + t.text + "' ожидалось число, найдено " + num.describe());
            }
            c.next();
            return toNumber(num, t.isSymbol("-"));
        }
        if (t.isKeyword("TRUE")) {
            c.next();
            return Boolean.TRUE;
        }
        if (t.isKeyword("FALSE")) {
            c.next();
            return Boolean.FALSE;
        }
        if (t.isKeyword("NULL")) {
            c.next();
            return null;
        }
        throw c.error("Ожидалось значение, найдено " + t.describe());
    }

    private static Object toNumber(Token t, boolean negative) {
        String text = (negative ? "-" : "") + t.text;
        boolean is_float = t.text.indexOf('.') >= 0 || t.text.indexOf('e') >= 0 || t.text.indexOf('E') >= 0;
        try {
            return is_float ? (Object) Double.valueOf(text) : (Object) Integer.valueOf(text);
        } catch (NumberFormatException e) {
            throw new QueryParseException("Число вне допустимого диапазона: " + text, t.pos);
        }
    }

    private static final class Cursor {
        private final List<Token> tokens;
        private int pos = 0;

        Cursor(List<Token> tokens) {
            this.tokens = tokens;
        }

        Token peek() {
            return tokens.get(pos);
        }

        Token next() {
            Token t = tokens.get(pos);
            if (t.type != Type.EOF) pos++;
            return t;
        }

        boolean atEof() {
            return peek().type == Type.EOF;
        }

        boolean acceptKeyword(String kw) {
            if (peek().isKeyword(kw)) {
                pos++;
                return true;
            }
            return false;
        }

        void expectKeyword(String kw) {
            if (!acceptKeyword(kw)) {
                throw error("Ожидалось " + kw + ", найдено " + peek().describe());
            }
        }

        boolean acceptSymbol(String s) {
            if (peek().isSymbol(s)) {
                pos++;
                return true;
            }
            return false;
        }

        void expectSymbol(String s) {
            if (!acceptSymbol(s)) {
                throw error("Ожидалось '" + s + "', найдено " + peek().describe());
            }
        }

        String expectIdent(String what) {
            Token t = peek();
            if (t.type != Type.IDENT || RESERVED.contains(t.text.toUpperCase(Locale.ROOT))) {
                throw error("Ожидалось " + what + ", найдено " + t.describe());
            }
            pos++;
            return t.text;
        }

        QueryParseException error(String message) {
            return new QueryParseException(message, peek().pos);
        }
    }
}

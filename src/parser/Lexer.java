package parser;

import java.util.ArrayList;
import java.util.List;

final class Lexer {

    enum Type { IDENT, NUMBER, STRING, SYMBOL, EOF }

    static final class Token {
        final Type type;
        final String text;
        final int pos;

        Token(Type type, String text, int pos) {
            this.type = type;
            this.text = text;
            this.pos = pos;
        }

        boolean isKeyword(String kw) {
            return type == Type.IDENT && text.equalsIgnoreCase(kw);
        }

        boolean isSymbol(String s) {
            return type == Type.SYMBOL && text.equals(s);
        }

        String describe() {
            return type == Type.EOF ? "конец запроса" : "'" + text + "'";
        }
    }

    private Lexer() {
    }

    static List<Token> tokenize(String sql) {
        List<Token> out = new ArrayList<>();
        int n = sql.length();
        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                while (i < n && sql.charAt(i) != '\n') i++;
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) i++;
                out.add(new Token(Type.IDENT, sql.substring(start, i), start));
            } else if (isDigit(c)) {
                int start = i;
                i = scanNumber(sql, i);
                if (i < n && (Character.isLetter(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    throw new QueryParseException("Некорректное число '" + sql.substring(start, i + 1) + "'", start);
                }
                out.add(new Token(Type.NUMBER, sql.substring(start, i), start));
            } else if (c == '\'' || c == '"') {
                int start = i;
                StringBuilder string_builder = new StringBuilder();
                boolean closed = false;
                i++;
                while (i < n) {
                    char cur = sql.charAt(i);
                    if (cur == c) {
                        if (i + 1 < n && sql.charAt(i + 1) == c) {
                            string_builder.append(c);
                            i += 2;
                            continue;
                        }
                        closed = true;
                        i++;
                        break;
                    }
                    string_builder.append(cur);
                    i++;
                }
                if (!closed) {
                    throw new QueryParseException("Строка не закрыта кавычкой", start);
                }
                out.add(new Token(Type.STRING, string_builder.toString(), start));
            } else {
                int start = i;
                String two = i + 1 < n ? sql.substring(i, i + 2) : "";
                if (two.equals("<=") || two.equals(">=") || two.equals("!=") || two.equals("<>")) {
                    out.add(new Token(Type.SYMBOL, two, start));
                    i += 2;
                } else if ("(),;*=<>+-".indexOf(c) >= 0) {
                    out.add(new Token(Type.SYMBOL, String.valueOf(c), start));
                    i++;
                } else {
                    throw new QueryParseException("Неожиданный символ '" + c + "'", start);
                }
            }
        }
        out.add(new Token(Type.EOF, "", n));
        return out;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int scanNumber(String s, int i) {
        int n = s.length();
        while (i < n && isDigit(s.charAt(i))) i++;
        if (i < n && s.charAt(i) == '.') {
            i++;
            while (i < n && isDigit(s.charAt(i))) i++;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) j++;
            if (j < n && isDigit(s.charAt(j))) {
                while (j < n && isDigit(s.charAt(j))) j++;
                i = j;
            }
        }
        return i;
    }
}

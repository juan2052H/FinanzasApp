package com.finanzas.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJson {
    private SimpleJson() {
    }

    static Object parse(String json) throws IOException {
        return new Parser(json).parse();
    }

    static String stringify(Map<String, ?> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append(quote(entry.getKey())).append(':').append(value(entry.getValue()));
        }
        builder.append('}');
        return builder.toString();
    }

    static String quote(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder("\"");
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        builder.append('"');
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object value) throws IOException {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new IOException("Respuesta JSON inesperada.");
    }

    @SuppressWarnings("unchecked")
    static List<Object> asArray(Object value) throws IOException {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        throw new IOException("Arreglo JSON inesperado.");
    }

    static String string(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    static boolean bool(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    static BigDecimal decimal(Map<String, Object> object, String key) throws IOException {
        Object value = object.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IOException("Numero JSON invalido para " + key + ".");
        }
    }

    static long longValue(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map) {
            return stringifyMap(value);
        }
        if (value instanceof Iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(value(item));
            }
            builder.append(']');
            return builder.toString();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static String stringifyMap(Object value) {
        return stringify((Map<String, ?>) value);
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parse() throws IOException {
            Object value = readValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("Contenido JSON extra.");
            }
            return value;
        }

        private Object readValue() throws IOException {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("JSON vacio.");
            }
            char ch = text.charAt(index);
            if (ch == '{') {
                return readObject();
            }
            if (ch == '[') {
                return readArray();
            }
            if (ch == '"') {
                return readString();
            }
            if (ch == 't' && text.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (ch == 'f' && text.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            if (ch == 'n' && text.startsWith("null", index)) {
                index += 4;
                return null;
            }
            return readNumber();
        }

        private Map<String, Object> readObject() throws IOException {
            Map<String, Object> object = new LinkedHashMap<String, Object>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                index++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                object.put(key, readValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> readArray() throws IOException {
            List<Object> array = new ArrayList<Object>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                index++;
                return array;
            }
            while (true) {
                array.add(readValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return array;
                }
                expect(',');
            }
        }

        private String readString() throws IOException {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw error("Escape JSON incompleto.");
                    }
                    char escape = text.charAt(index++);
                    switch (escape) {
                        case '"':
                        case '\\':
                        case '/':
                            builder.append(escape);
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'u':
                            if (index + 4 > text.length()) {
                                throw error("Unicode JSON incompleto.");
                            }
                            builder.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                            index += 4;
                            break;
                        default:
                            throw error("Escape JSON invalido.");
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw error("String JSON sin cerrar.");
        }

        private Object readNumber() throws IOException {
            int start = index;
            while (index < text.length()) {
                char ch = text.charAt(index);
                if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E') {
                    index++;
                } else {
                    break;
                }
            }
            if (start == index) {
                throw error("Valor JSON invalido.");
            }
            String number = text.substring(start, index);
            try {
                if (number.contains(".") || number.contains("e") || number.contains("E")) {
                    return new BigDecimal(number);
                }
                return Long.valueOf(number);
            } catch (NumberFormatException ex) {
                throw error("Numero JSON invalido.");
            }
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("Se esperaba '" + expected + "'.");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }

        private IOException error(String message) {
            return new IOException(message + " Posicion " + index + ".");
        }
    }
}

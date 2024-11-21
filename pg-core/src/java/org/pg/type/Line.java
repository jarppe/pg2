package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import org.pg.error.PGError;
import org.pg.util.NumTool;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public record Line (double a, double b, double c)
        implements Counted, Indexed, ILookup, IDeref, Iterable<Double> {

    public static Line of(final double a, final double b, final double c) {
        return new Line(a, b, c);
    }

    public ByteBuffer toByteBuffer() {
        final ByteBuffer bb = ByteBuffer.allocate(24);
        bb.putDouble(a);
        bb.putDouble(b);
        bb.putDouble(c);
        return bb;
    }

    public static Line fromByteBuffer(final ByteBuffer bb) {
        final double a = bb.getDouble();
        final double b = bb.getDouble();
        final double c = bb.getDouble();
        return Line.of(a, b, c);
    }

    public static Line fromMap(final Map<?,?> map) {
        final double a = NumTool.toDouble(map.get(KW.a));
        final double b = NumTool.toDouble(map.get(KW.b));
        final double c = NumTool.toDouble(map.get(KW.c));
        return Line.of(a, b, c);
    }

    public static Line fromList(final List<?> list) {
        final double a = NumTool.toDouble(list.get(0));
        final double b = NumTool.toDouble(list.get(1));
        final double c = NumTool.toDouble(list.get(2));
        return Line.of(a, b, c);
    }

    public static Line fromString(final String text) {
        final List<String> parts = StrTool.split(text, "[{,}]");
        if (parts.size() != 3) {
            throw new PGError("wrong line string: %s", text);
        }
        final double a = Double.parseDouble(parts.get(0));
        final double b = Double.parseDouble(parts.get(1));
        final double c = Double.parseDouble(parts.get(2));
        return Line.of(a, b, c);
    }

    @SuppressWarnings("unused")
    public static Line fromObject(final Object x) {
        if (x instanceof Map<?,?> m) {
            return Line.fromMap(m);
        } else if (x instanceof List<?> l) {
            return Line.fromList(l);
        } else if (x instanceof ByteBuffer bb) {
            return Line.fromByteBuffer(bb);
        } else if (x instanceof String s) {
            return Line.fromString(s);
        } else if (x instanceof Line l) {
            return l;
        } else {
            throw PGError.error("wrong line input: %s", x);
        }
    }

    @Override
    public String toString() {
        return "{" + a + "," + b + "," + c + "}";
    }

    @Override
    public Object nth(final int i) {
        return switch (i) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            default -> throw new IndexOutOfBoundsException("index is out of range: " + i);
        };
    }

    @Override
    public Object nth(final int i, final Object notFound) {
        return switch (i) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            default -> notFound;
        };
    }

    @Override
    public int count() {
        return 3;
    }

    @Override
    public Object valAt(final Object key) {
        return valAt(key, null);
    }

    @Override
    public Object valAt(final Object key, final Object notFound) {
        if (key instanceof Keyword kw) {
            if (kw == KW.a) {
                return a;
            } else if (kw == KW.b) {
                return b;
            } else if (kw == KW.c) {
                return c;
            }
        }
        return notFound;
    }

    @Override
    public Object deref() {
        return PersistentHashMap.create(KW.a, a, KW.b, b, KW.c, c);
    }

    @Override
    public Iterator<Double> iterator() {
        return List.of(a, b, c).iterator();
    }

    public static void main(String... args) {
        System.out.println(Line.fromString(" { 1.23342 , -3.23234, 555.22 } "));
    }

}

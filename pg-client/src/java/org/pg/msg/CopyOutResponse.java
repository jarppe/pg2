package org.pg.msg;

import org.pg.enums.Format;

import java.nio.ByteBuffer;

public record CopyOutResponse (
        Format format,
        short columnCount,
        Format[] columnFormats
) {
    public static CopyOutResponse fromByteBuffer(final ByteBuffer buf) {
        final Format format = Format.ofShort(buf.get());
        final short columnCount = buf.getShort();
        final Format[] columnFormats = new Format[columnCount];
        for (short i = 0; i < columnCount; i++) {
            columnFormats[i] = Format.ofShort(buf.getShort());
        }
        return new CopyOutResponse(format, columnCount, columnFormats);
    }
}

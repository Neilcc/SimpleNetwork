package com.zcc.simplenetwork.ws;

import static com.zcc.simplenetwork.ws.WebSocketProtocol.B0_FLAG_FIN;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.B0_FLAG_RSV1;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.B0_FLAG_RSV2;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.B0_FLAG_RSV3;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.B0_MASK_OPCODE;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.B1_FLAG_MASK;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.B1_MASK_LENGTH;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.OPCODE_BINARY;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.OPCODE_CONTINUATION;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.OPCODE_CONTROL_CLOSE;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.OPCODE_CONTROL_PING;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.OPCODE_CONTROL_PONG;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.OPCODE_TEXT;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.PAYLOAD_BYTE_MAX;
import static com.zcc.simplenetwork.ws.WebSocketProtocol.PAYLOAD_SHORT;

import com.zcc.simplenetwork.L;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class WSParser {
    private static final String TAG = "WSParser";
    private static final List<Integer> OPCODES = Arrays.asList(OPCODE_CONTINUATION,
            OPCODE_TEXT, OPCODE_BINARY, OPCODE_CONTROL_CLOSE, OPCODE_CONTROL_PING,
            OPCODE_CONTROL_PONG);
    private static final List<Integer> FRAGMENTED_OPCODES = Arrays.asList(0, 1, 2);
    private WebSocketClient mClient;
    private boolean mMasking = true;
    private int mStage;
    private boolean mFinal;
    private boolean mMasked;
    private int mOpcode;
    private int mLengthSize;
    private int mLength;
    private int mMode;
    private byte[] mMask = new byte[0];
    private byte[] mPayload = new byte[0];
    private boolean mClosed = false;
    private ByteArrayOutputStream mBuffer = new ByteArrayOutputStream();

    public WSParser(WebSocketClient client) {
        this.mClient = client;
    }

    private static byte[] mask(byte[] payload, byte[] mask, int offset) {
        if (mask.length == 0) {
            return payload;
        } else {
            for (int i = 0; i < payload.length - offset; ++i) {
                payload[offset + i] ^= mask[i % 4];
            }
            return payload;
        }
    }

    private static byte[] copyOfRange(byte[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        } else {
            int originalLength = original.length;
            if (start >= 0 && start <= originalLength) {
                int resultLength = end - start;
                int copyLength = Math.min(resultLength, originalLength - start);
                byte[] result = new byte[resultLength];
                System.arraycopy(original, start, result, 0, copyLength);
                return result;
            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }

    private static long byteArrayToLong(byte[] b, int offset, int length) {
        if (b.length < length) {
            throw new IllegalArgumentException("length must be less than or equal to b.length");
        } else {
            long value = 0L;
            for (int i = 0; i < length; ++i) {
                int shift = (length - 1 - i) * 8;
                value += (long) ((b[i + offset] & 255) << shift);
            }
            return value;
        }
    }

    public void start(HappyDataInputStream stream) throws IOException {
        while (true) {
            if (this.mClient.isConnected() && this.mClient.getSocket().isConnected()) {
                if (stream.available() == -1) {
                    WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
                    if (listener != null)
                        listener.onDisconnect(10, "EOF");
                    return;
                }
                try {
                    switch (this.mStage) {
                        case 0:
                            this.parseOpcode(stream.readByte());
                            break;
                        case 1:
                            this.parseLength(stream.readByte());
                            break;
                        case 2:
                            this.parseExtendedLength(stream.readBytes(this.mLengthSize));
                            break;
                        case 3:
                            this.mMask = stream.readBytes(4);
                            this.mStage = 4;
                            break;
                        case 4:
                            this.mPayload = stream.readBytes(this.mLength);
                            this.emitFrame();
                            this.mStage = 0;
                    }
                } catch (EOFException e) {
                    e.printStackTrace();
//                    WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
//                    if (listener != null)
//                        listener.onDisconnect(10, "EOF");
//                    return;
                    continue;
                }
            } else {
                WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
                if (listener != null)
                    listener.onDisconnect(10, "client disconnect");
//                this.mClient.getListener().onDisconnect(10, "client disconnect");
                this.mClient.disconnect();
                return;
            }
            if (mClosed) {
                return;
            }
        }
    }

    private void parseOpcode(byte data) throws ProtocolError {
        boolean rsv1 = (data & B0_FLAG_RSV1) == B0_FLAG_RSV1;
        boolean rsv2 = (data & B0_FLAG_RSV2) == B0_FLAG_RSV2;
        boolean rsv3 = (data & B0_FLAG_RSV3) == B0_FLAG_RSV3;
        if (!rsv1 && !rsv2 && !rsv3) {
            this.mFinal = (data & B0_FLAG_FIN) == B0_FLAG_FIN;
            this.mOpcode = data & B0_MASK_OPCODE;
            this.mMask = new byte[0];
            this.mPayload = new byte[0];
            if (!OPCODES.contains(this.mOpcode)) {
                throw new ProtocolError("Bad opcode");
            } else if (!FRAGMENTED_OPCODES.contains(this.mOpcode) && !this.mFinal) {
                throw new ProtocolError("Expected non-final packet");
            } else {
                this.mStage = 1;
            }
        } else {
            throw new ProtocolError("RSV not zero");
        }
    }

    private void parseLength(byte data) {
        this.mMasked = (data & B1_FLAG_MASK) == B1_FLAG_MASK;
        this.mLength = data & B1_MASK_LENGTH;
        if (this.mLength <= PAYLOAD_BYTE_MAX) {
            this.mStage = this.mMasked ? 3 : 4;
        } else {
            this.mLengthSize = this.mLength == PAYLOAD_SHORT ? 2 : 8;
            this.mStage = 2;
        }

    }

    private void parseExtendedLength(byte[] buffer) throws ProtocolError {
        this.mLength = this.getInteger(buffer);
        this.mStage = this.mMasked ? 3 : 4;
    }

    public byte[] frame(String data) {
        return this.frame((String) data, OPCODE_TEXT, -1);
    }

    public byte[] frame(byte[] data) {
        return this.frame((byte[]) data, OPCODE_BINARY, -1);
    }

    private byte[] frame(byte[] data, int opcode, int errorCode) {
        return this.frame((Object) data, opcode, errorCode);
    }

    private byte[] frame(String data, int opcode, int errorCode) {
        return this.frame((Object) data, opcode, errorCode);
    }

    private byte[] frame(Object data, int opcode, int errorCode) {
        if (this.mClosed) {
            return null;
        } else {
            byte[] buffer = data instanceof String ? this.decode((String) data) : (byte[]) ((byte[]) data);
            int insert = errorCode > 0 ? 2 : 0;
            int length = (buffer == null ? 0 : buffer.length) + insert;
            int header = length <= 125 ? 2 : (length <= 65535 ? 4 : 10);
            int offset = header + (this.mMasking ? 4 : 0);
            int masked = this.mMasking ? 128 : 0;
            byte[] frame = new byte[length + offset];
            frame[0] = (byte) (-128 | (byte) opcode);
            if (length <= 125) {
                frame[1] = (byte) (masked | length);
            } else if (length <= 65535) {
                frame[1] = (byte) (masked | 126);
                frame[2] = (byte) ((int) Math.floor((double) (length / 256)));
                frame[3] = (byte) (length & 255);
            } else {
                frame[1] = (byte) (masked | 127);
                frame[2] = (byte) ((int) Math.floor((double) length / Math.pow(2.0D, 56.0D)) & 255);
                frame[3] = (byte) ((int) Math.floor((double) length / Math.pow(2.0D, 48.0D)) & 255);
                frame[4] = (byte) ((int) Math.floor((double) length / Math.pow(2.0D, 40.0D)) & 255);
                frame[5] = (byte) ((int) Math.floor((double) length / Math.pow(2.0D, 32.0D)) & 255);
                frame[6] = (byte) ((int) Math.floor((double) length / Math.pow(2.0D, 24.0D)) & 255);
                frame[7] = (byte) ((int) Math.floor((double) length / Math.pow(2.0D, 16.0D)) & 255);
                frame[8] = (byte) ((int) Math.floor((double) length / Math.pow(2.0D, 8.0D)) & 255);
                frame[9] = (byte) (length & 255);
            }

            if (errorCode > 0) {
                frame[offset] = (byte) ((int) Math.floor((double) (errorCode / 256)) & 255);
                frame[offset + 1] = (byte) (errorCode & 255);
            }

            if (buffer != null) {
                System.arraycopy(buffer, 0, frame, offset + insert, buffer.length);
            }

            if (this.mMasking) {
                byte[] mask = new byte[]{(byte) ((int) Math.floor(Math.random() * 256.0D)), (byte) ((int) Math.floor(Math.random() * 256.0D)), (byte) ((int) Math.floor(Math.random() * 256.0D)), (byte) ((int) Math.floor(Math.random() * 256.0D))};
                System.arraycopy(mask, 0, frame, header, mask.length);
                mask(frame, mask, offset);
            }

            return frame;
        }
    }

    public void ping(String message) {
        this.mClient.send(this.frame((String) message, OPCODE_CONTROL_PING, -1));
    }

    public void close(int code, String reason) {
        if (!this.mClosed) {
            this.mClient.send(this.frame((String) reason, OPCODE_CONTROL_CLOSE, code));
            this.mClosed = true;
        }
    }

    private void emitFrame() throws IOException {
        byte[] payload = mask(this.mPayload, this.mMask, 0);
        int opcode = this.mOpcode;
        if (opcode == 0) {
            if (this.mMode == 0) {
                throw new ProtocolError("Mode was not set.");
            }

            this.mBuffer.write(payload);
            if (this.mFinal) {
                byte[] message = this.mBuffer.toByteArray();
                if (this.mMode == 1) {
                    WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
                    if (listener != null)
                        this.mClient.getListener().onMessage(this.encode(message));
                } else {
                    WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
                    if (listener != null)
                        this.mClient.getListener().onMessage(message);
                }

                this.reset();
            }
        } else if (opcode == OPCODE_TEXT) {
            if (this.mFinal) {
                String messageText = this.encode(payload);
                WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
                if (listener != null)
                    this.mClient.getListener().onMessage(messageText);
            } else {
                this.mMode = 1;
                this.mBuffer.write(payload);
            }
        } else if (opcode == OPCODE_BINARY) {
            if (this.mFinal) {
                WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
                if (listener != null)
                    this.mClient.getListener().onMessage(payload);
            } else {
                this.mMode = 2;
                this.mBuffer.write(payload);
            }
        } else if (opcode == OPCODE_CONTROL_CLOSE) {
            int code = payload.length >= 2 ? ((payload[0] & 255) << 8) + (payload[1] & 255) : 0;
            String reason = payload.length > 2 ? this.encode(this.slice(payload, 2)) : null;
            L.d("Got close op! " + code + " " + reason);
            WebSocketClient.IWebSocketStateListener listener = this.mClient.getListener();
            if (listener != null)
                this.mClient.getListener().onDisconnect(code, reason);
        } else if (opcode == OPCODE_CONTROL_PING) {
            if (payload.length > 125) {
                throw new ProtocolError("Ping payload too large");
            }

            this.mClient.sendFrame(this.frame((byte[]) payload, OPCODE_CONTROL_PONG, -1));
        } else if (opcode == OPCODE_CONTROL_PONG) {
            this.encode(payload);
        }

    }

    private void reset() {
        this.mMode = 0;
        this.mBuffer.reset();
    }

    private String encode(byte[] buffer) {
        try {
            return new String(buffer, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            throw new RuntimeException(var3);
        }
    }

    private byte[] decode(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException var3) {
            throw new RuntimeException(var3);
        }
    }

    private int getInteger(byte[] bytes) throws ProtocolError {
        long i = byteArrayToLong(bytes, 0, bytes.length);
        if (i >= 0L && i <= Integer.MAX_VALUE) {
            return (int) i;
        } else {
            throw new ProtocolError("Bad integer: " + i);
        }
    }

    private byte[] slice(byte[] array, int start) {
        return copyOfRange(array, start, array.length);
    }

    public static class HappyDataInputStream extends DataInputStream {
        public HappyDataInputStream(InputStream in) {
            super(in);
        }

        public byte[] readBytes(int length) throws IOException {
            byte[] buffer = new byte[length];

            int total;
            int count;
            for (total = 0; total < length; total += count) {
                count = this.read(buffer, total, length - total);
                if (count == -1) {
                    break;
                }
            }

            if (total != length) {
                throw new IOException(String.format("Read wrong number of bytes. Got: %s, Expected: %s.", total, length));
            } else {
                return buffer;
            }
        }
    }

    public static class ProtocolError extends IOException {
        public ProtocolError(String detailMessage) {
            super(detailMessage);
        }
    }
}

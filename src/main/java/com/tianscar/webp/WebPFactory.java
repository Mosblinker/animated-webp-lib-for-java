package com.tianscar.webp;

import static com.tianscar.webp.ALPHChunk.ALPH;
import static com.tianscar.webp.ANIMChunk.ANIM;
import static com.tianscar.webp.ANMFChunk.ANMF;
import static com.tianscar.webp.EXIFChunk.EXIF;
import static com.tianscar.webp.ICCPChunk.ICCP;
import static com.tianscar.webp.RIFFChunk.RIFF;
import static com.tianscar.webp.Util.*;
import static com.tianscar.webp.VP8Chunk.VP8;
import static com.tianscar.webp.VP8LChunk.VP8L;
import static com.tianscar.webp.VP8XChunk.VP8X;
import static com.tianscar.webp.WebPChunk.WEBP;
import static com.tianscar.webp.XMPChunk.XMP;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jnr.ffi.Pointer;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.PointerByReference;

public final class WebPFactory {
    /**
     * This is the default name of the libwebp library.
     */
    public static final String DEFAULT_WEBP_LIBRARY_NAME = "libwebp";
    /**
     * This is the name of the libwebp library.
     */
    private static String libraryName = DEFAULT_WEBP_LIBRARY_NAME;
    /**
     * This is a list of additional search directories to use to search for the 
     * libwebp library.
     */
    private static final List<String> searchPaths = new ArrayList<>();
    /**
     * This returns the name of the library used to handle WebP images. This is 
     * set to "{@value DEFAULT_WEBP_LIBRARY_NAME}" by default, but this is here 
     * for instances where the libwebp library may be using a different name.
     * @return The name of the libwebp library.
     * @see #setWebPLibraryName(java.lang.String) 
     * @see #DEFAULT_WEBP_LIBRARY_NAME
     * @see jnr.ffi.LibraryLoader#load(java.lang.String) 
     */
    public static String getWebPLibraryName(){
        return libraryName;
    }
    /**
     * This sets the name to use to access the library used to handle WebP 
     * images. This is set to "{@value DEFAULT_WEBP_LIBRARY_NAME}" by default, 
     * but this is here in case the libwebp library can be found using a 
     * different name.
     * @param name The name of the libwebp library, or null to reset it to the 
     * default name.
     * @see #getWebPLibraryName() 
     * @see #DEFAULT_WEBP_LIBRARY_NAME
     * @see jnr.ffi.LibraryLoader#load(java.lang.String) 
     */
    public static void setWebPLibraryName(String name){
            // If the name is null, reset the name to default
        libraryName = (name!=null)?name:DEFAULT_WEBP_LIBRARY_NAME;
    }
    /**
     * This returns a list containing the additional directories to search in 
     * when searching for the library used for WebP images. This list can be 
     * edited to add, remove, or change the additional directories.
     * @return A list containing additional directories to search for the 
     * library in.
     * @see #addSearchDirectoy(java.lang.String) 
     * @see jnr.ffi.LibraryLoader#search(java.lang.String) 
     */
    public static List<String> getAdditionalSearchDirectories(){
        return searchPaths;
    }
    /**
     * This adds the given path to the list of additional directories to search 
     * when searching for the library used for WebP images. This will not add a 
     * path that has already been added to the additional search directories.
     * @param path The directory to search (cannot be null).
     * @throws NullPointerException If the given path is null.
     * @see #getAdditionalSearchDirectories() 
     * @see jnr.ffi.LibraryLoader#search(java.lang.String) 
     */
    public static void addSearchDirectoy(String path){
            // If the given path is null
        if (path == null)
            throw new NullPointerException();
            // If the search paths doesn't contain the given path
        if (!getAdditionalSearchDirectories().contains(path))
            getAdditionalSearchDirectories().add(path);
    }

    private WebPFactory() {
        throw new UnsupportedOperationException();
    }

    public static WebPChunk demux(InputStream in) throws IOException {
        Objects.requireNonNull(in);
        byte[] riffChunkHeader = readFourCC(in);
        if (!arrayEquals(riffChunkHeader, RIFF)) {
            throw new IOException("Illegal magic number: " + new String(riffChunkHeader));
        }
        long fileSize = readUInt32(in);
        byte[] webpChunkHeader = readFourCC(in);
        if (!arrayEquals(webpChunkHeader, WEBP)) {
            throw new IOException("Illegal magic number: " + new String(riffChunkHeader) + new String(webpChunkHeader));
        }
        WebPChunk webPChunk;
        boolean filePad = false;
        if (isOdd(fileSize)) {
            fileSize -= 1;
            filePad = true;
        }
        fileSize -= 4;
        byte[] vp8ChunkHeader = readFourCC(in);
        long vp8ChunkSize = readUInt32(in);
        fileSize -= 8;
        fileSize -= vp8ChunkSize;
        boolean vp8ChunkPad = false;
        if (isOdd(vp8ChunkSize)) {
            fileSize -= 1;
            vp8ChunkPad = true;
        }
        if (arrayEquals(vp8ChunkHeader, VP8X)) {
            List<Chunk> chunks = new ArrayList<>();
            chunks.add(new VP8XChunk(readInt32(in), read1Based(in), read1Based(in)));
            if (vp8ChunkPad) skip1Byte(in);
            byte[] chunkHeader;
            long chunkSize;
            boolean chunkPad = false;
            while (fileSize > 0) {
                chunkHeader = readFourCC(in);
                chunkSize = readUInt32(in);
                fileSize -= 8;
                fileSize -= chunkSize;
                if (isOdd(chunkSize)) {
                    fileSize -= 1;
                    chunkPad = true;
                }
                if (chunkSize > Integer.MAX_VALUE) throw new IOException("chunk too large to read");
                if (arrayEquals(chunkHeader, ICCP)) {
                    chunks.add(new ICCPChunk(readNBytes(in, (int) chunkSize)));
                }
                else if (arrayEquals(chunkHeader, EXIF)) {
                    chunks.add(new EXIFChunk(readNBytes(in, (int) chunkSize)));
                }
                else if (arrayEquals(chunkHeader, XMP)) {
                    chunks.add(new XMPChunk(readNBytes(in, (int) chunkSize)));
                }
                else if (arrayEquals(chunkHeader, VP8)) {
                    chunks.add(new VP8Chunk(readNBytes(in, (int) chunkSize)));
                }
                else if (arrayEquals(chunkHeader, VP8L)) {
                    chunks.add(new VP8LChunk(readNBytes(in, (int) chunkSize)));
                }
                else if (arrayEquals(chunkHeader, ALPH)) {
                    chunks.add(new ALPHChunk(readNBytes(in, (int) chunkSize)));
                }
                else if (arrayEquals(chunkHeader, ANIM)) {
                    chunks.add(new ANIMChunk(readInt32(in), readUInt16(in)));
                }
                else if (arrayEquals(chunkHeader, ANMF)) {
                    int x = readUInt24(in);
                    int y = readUInt24(in);
                    int width = read1Based(in);
                    int height = read1Based(in);
                    int duration = readUInt24(in);
                    int reservedBD = readInt8(in);
                    chunkSize -= 16;
                    List<Chunk> framesubchunks = new ArrayList<>();
                    byte[] framesubchunkHeader;
                    long framesubchunkSize;
                    boolean framesubchunkPad = false;
                    while (chunkSize > 0) {
                        framesubchunkHeader = readFourCC(in);
                        framesubchunkSize = readUInt32(in);
                        if (isOdd(framesubchunkSize)) {
                            chunkSize -= 1;
                            framesubchunkPad = true;
                        }
                        chunkSize -= 8;
                        chunkSize -= framesubchunkSize;
                        if (framesubchunkSize > Integer.MAX_VALUE) throw new IOException("chunk too large to read");
                        if (arrayEquals(framesubchunkHeader, VP8)) {
                            framesubchunks.add(new VP8Chunk(readNBytes(in, (int) framesubchunkSize)));
                        }
                        else if (arrayEquals(framesubchunkHeader, VP8L)) {
                            framesubchunks.add(new VP8LChunk(readNBytes(in, (int) framesubchunkSize)));
                        }
                        else if (arrayEquals(framesubchunkHeader, ALPH)) {
                            framesubchunks.add(new ALPHChunk(readNBytes(in, (int) framesubchunkSize)));
                        }
                        else {
                            framesubchunks.add(new UnknownChunk(framesubchunkHeader, readNBytes(in, (int) framesubchunkSize)));
                        }
                    }
                    chunks.add(new ANMFChunk(x, y, width, height, duration, reservedBD, framesubchunks.toArray(new Chunk[0])));
                    if (framesubchunkPad) skip1Byte(in);
                }
                else {
                    chunks.add(new UnknownChunk(chunkHeader, readNBytes(in, (int) chunkSize)));
                }
                if (chunkPad) skip1Byte(in);
            }
            webPChunk = new WebPChunk(chunks.toArray(new Chunk[0]));
        }
        else if (arrayEquals(vp8ChunkHeader, VP8)) {
            if (vp8ChunkSize > Integer.MAX_VALUE) throw new IOException("chunk too large to read");
            webPChunk = new WebPChunk(new VP8Chunk(readNBytes(in, (int) vp8ChunkSize)));
        }
        else if (arrayEquals(vp8ChunkHeader, VP8L)) {
            if (vp8ChunkSize > Integer.MAX_VALUE) throw new IOException("chunk too large to read");
            webPChunk = new WebPChunk(new VP8LChunk(readNBytes(in, (int) vp8ChunkSize)));
        }
        else {
            throw new IOException("No VP8 data found");
        }
        if (filePad) skip1Byte(in);
        in.close();
        return webPChunk;
    }

    public static byte[] decodeRGBA(BitstreamChunk chunk, int[] size) {
        Objects.requireNonNull(chunk);
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        Pointer webPData = webP.WebPDecodeRGBA(chunk.getRawData(), chunk.getSize(), width, height);
        size[0] = width.intValue();
        size[1] = height.intValue();
        byte[] buf = new byte[size[0] * size[1] * 4];
        webPData.get(0, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return buf;
    }

    public static byte[] decodeARGB(BitstreamChunk chunk, int[] size) {
        Objects.requireNonNull(chunk);
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        Pointer webPData = webP.WebPDecodeARGB(chunk.getRawData(), chunk.getSize(), width, height);
        size[0] = width.intValue();
        size[1] = height.intValue();
        byte[] buf = new byte[size[0] * size[1] * 4];
        webPData.get(0, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return buf;
    }

    public static int[] decodeBGRA(BitstreamChunk chunk, int[] size) {
        Objects.requireNonNull(chunk);
        if (size == null || size.length != 2) throw new IllegalArgumentException("size length must be 2");
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        Pointer webPData = webP.WebPDecodeBGRA(chunk.getRawData(), chunk.getSize(), width, height);
        size[0] = width.intValue();
        size[1] = height.intValue();
        int[] buf = new int[size[0] * size[1]];
        webPData.get(0, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return buf;
    }

    public static byte[] decodeARGB(ALPHChunk alphChunk, VP8Chunk vp8Chunk, int[] size) {
        Objects.requireNonNull(alphChunk);
        Objects.requireNonNull(vp8Chunk);
        if (size == null || size.length != 2) throw new IllegalArgumentException("size length must be 2");
        long chunkFullSize = alphChunk.getFullSize() + vp8Chunk.getFullSize();
        if (chunkFullSize > Integer.MAX_VALUE) throw new IllegalArgumentException("chunk too large to read");
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        byte[] chunkData = new byte[(int) chunkFullSize];
        System.arraycopy(alphChunk.getRawData(), 0, chunkData, 0, (int) alphChunk.getFullSize());
        System.arraycopy(vp8Chunk.getRawData(), 0, chunkData, (int) alphChunk.getFullSize(), (int) vp8Chunk.getFullSize());
        Pointer webPData = webP.WebPDecodeBGRA(chunkData, chunkFullSize, width, height);
        size[0] = width.intValue();
        size[1] = height.intValue();
        byte[] buf = new byte[size[0] * size[1] * 4];
        webPData.get(0, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return buf;
    }

    public static int[] decodeBGRA(ALPHChunk alphChunk, VP8Chunk vp8Chunk, int[] size) {
        Objects.requireNonNull(alphChunk);
        Objects.requireNonNull(vp8Chunk);
        if (size == null || size.length != 2) throw new IllegalArgumentException("size length must be 2");
        long chunkFullSize = alphChunk.getFullSize() + vp8Chunk.getFullSize();
        if (chunkFullSize > Integer.MAX_VALUE) throw new IllegalArgumentException("chunk too large to read");
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        byte[] chunkData = new byte[(int) chunkFullSize];
        System.arraycopy(alphChunk.getRawData(), 0, chunkData, 0, (int) alphChunk.getFullSize());
        System.arraycopy(vp8Chunk.getRawData(), 0, chunkData, (int) alphChunk.getFullSize(), (int) vp8Chunk.getFullSize());
        Pointer webPData = webP.WebPDecodeBGRA(chunkData, chunkFullSize, width, height);
        size[0] = width.intValue();
        size[1] = height.intValue();
        int[] buf = new int[size[0] * size[1]];
        webPData.get(0, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return buf;
    }

    public static byte[] decodeRGB(BitstreamChunk chunk, int[] size) {
        Objects.requireNonNull(chunk);
        if (size == null || size.length != 2) throw new IllegalArgumentException("size length must be 2");
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        Pointer webPData = webP.WebPDecodeRGB(chunk.getRawData(), chunk.getSize(), width, height);
        size[0] = width.intValue();
        size[1] = height.intValue();
        byte[] buf = new byte[width.intValue() * height.intValue() * 3];
        webPData.get(0, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return buf;
    }

    public static byte[] decodeBGR(BitstreamChunk chunk, int[] size) {
        Objects.requireNonNull(chunk);
        if (size == null || size.length != 2) throw new IllegalArgumentException("size length must be 2");
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        Pointer webPData = webP.WebPDecodeBGR(chunk.getRawData(), chunk.getSize(), width, height);
        size[0] = width.intValue();
        size[1] = height.intValue();
        byte[] buf = new byte[width.intValue() * height.intValue() * 3];
        webPData.get(0, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return buf;
    }

    public static VP8LChunk encodeLosslessBGRA(int[] bgra, int width, int height, int stride) {
        Objects.requireNonNull(bgra);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeLosslessBGRA(bgra, width, height, stride * 4, webPDataRef);
        return getVP8LChunk(webPDataRef);
    }

    private static VP8LChunk getVP8LChunk(PointerByReference webPDataRef) {
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        Pointer webPData = webPDataRef.getValue();
        byte[] lengthBuf = new byte[4];
        webPData.get(16, lengthBuf, 0, 4);
        long length = Util.toUInt32(lengthBuf);
        byte[] buf = new byte[(int) length];
        webPData.get(20, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return new VP8LChunk(buf);
    }

    public static VP8LChunk encodeLosslessRGBA(byte[] rgba, int width, int height, int stride) {
        Objects.requireNonNull(rgba);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeLosslessRGBA(rgba, width, height, stride * 4, webPDataRef);
        return getVP8LChunk(webPDataRef);
    }

    public static VP8LChunk encodeLosslessRGB(byte[] rgb, int width, int height, int stride) {
        Objects.requireNonNull(rgb);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeLosslessRGB(rgb, width, height, stride * 3, webPDataRef);
        return getVP8LChunk(webPDataRef);
    }

    public static VP8LChunk encodeLosslessBGR(byte[] bgr, int width, int height, int stride) {
        Objects.requireNonNull(bgr);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeLosslessBGR(bgr, width, height, stride * 3, webPDataRef);
        return getVP8LChunk(webPDataRef);
    }

    public static Chunk[] encodeBGRA(int[] bgra, int width, int height, int stride, float quality) {
        Objects.requireNonNull(bgra);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeBGRA(bgra, width, height, stride * 4, quality, webPDataRef);
        return getLossyChunks(webPDataRef);
    }

    public static Chunk[] encodeRGBA(byte[] rgba, int width, int height, int stride, float quality) {
        Objects.requireNonNull(rgba);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeRGBA(rgba, width, height, stride * 4, quality, webPDataRef);
        return getLossyChunks(webPDataRef);
    }

    private static Chunk[] getLossyChunks(PointerByReference webPDataRef) {
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        Pointer webPData = webPDataRef.getValue();
        byte[] chunkHeaderBuf = new byte[4];
        webPData.get(12, chunkHeaderBuf, 0, 4);
        byte[] lengthBuf = new byte[4];
        if (arrayEquals(chunkHeaderBuf, VP8)) {
            return new Chunk[] { getVP8Chunk(webPDataRef) };
        }
        else {
            webPData.get(34, lengthBuf, 0, 4);
            long lengthALPH = Util.toUInt32(lengthBuf);
            int pad = isOdd(lengthALPH) ? 1 : 0;
            byte[] bufALPH = new byte[(int) lengthALPH];
            webPData.get(38, bufALPH, 0, bufALPH.length);
            webPData.get(42 + bufALPH.length + pad, lengthBuf, 0, 4);
            long lengthVP8 = Util.toUInt32(lengthBuf);
            byte[] bufVP8 = new byte[(int) lengthVP8];
            webPData.get(46 + bufALPH.length + pad, bufVP8, 0, bufVP8.length);
            webP.WebPFree(webPData);
            return new Chunk[] { new ALPHChunk(bufALPH), new VP8Chunk(bufVP8) };
        }
    }

    public static VP8Chunk encodeBGR(byte[] bgr, int width, int height, int stride, float quality) {
        Objects.requireNonNull(bgr);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeBGR(bgr, width, height, stride * 3, quality, webPDataRef);
        return getVP8Chunk(webPDataRef);
    }

    public static VP8Chunk encodeRGB(byte[] rgb, int width, int height, int stride, float quality) {
        Objects.requireNonNull(rgb);
        PointerByReference webPDataRef = new PointerByReference();
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        webP.WebPEncodeRGB(rgb, width, height, stride * 3, quality, webPDataRef);
        return getVP8Chunk(webPDataRef);
    }

    private static VP8Chunk getVP8Chunk(PointerByReference webPDataRef) {
        JNRFFI.WebP webP = JNRFFI.WebP.INSTANCE;
        Pointer webPData = webPDataRef.getValue();
        byte[] lengthBuf = new byte[4];
        webPData.get(16, lengthBuf, 0, 4);
        long length = Util.toUInt32(lengthBuf);
        byte[] buf = new byte[(int) length];
        webPData.get(20, buf, 0, buf.length);
        webP.WebPFree(webPData);
        return new VP8Chunk(buf);
    }

}

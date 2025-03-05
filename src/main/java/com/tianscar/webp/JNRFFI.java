package com.tianscar.webp;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.PointerByReference;

final class JNRFFI {

    private JNRFFI() {
        throw new UnsupportedOperationException();
    }
    /**
     * This loads and returns an instance of the WebP class.
     * @return An instance of the WebP class.
     */
    private static WebP loadLibrary(){
        com.luciad.imageio.webp.WebP.loadNativeLibrary();
            // Get the LibraryLoader to use to load libwebp
        LibraryLoader<WebP> libwebpLoader = LibraryLoader.create(WebP.class);
            // Go through the additional search paths
        for (String path : WebPFactory.getAdditionalSearchDirectories())
                // Add the search path.
            libwebpLoader.search(path);
            // Load the libwebp library
        return libwebpLoader.load(WebPFactory.getWebPLibraryName());
    }

    protected interface WebP {

        WebP INSTANCE = loadLibrary();

        void WebPFree(@In Pointer ptr);

        long WebPEncodeRGB(@In byte[] rgb, @In int width, @In int height, @In int stride,
                           @In float quality_factor, @Out PointerByReference output);

        long WebPEncodeBGR(@In byte[] bgr, @In int width, @In int height, @In int stride,
                           @In float quality_factor, @Out PointerByReference output);

        long WebPEncodeRGBA(@In byte[] rgba, @In int width, @In int height, @In int stride,
                            @In float quality_factor, @Out PointerByReference output);

        long WebPEncodeBGRA(@In int[] bgra, @In int width, @In int height, @In int stride,
                            @In float quality_factor, @Out PointerByReference output);

        long WebPEncodeLosslessRGB(@In byte[] rgb, @In int width, @In int height,
                                   @In int stride, @Out PointerByReference output);

        long WebPEncodeLosslessBGR(@In byte[] bgr, @In int width, @In int height,
                                   @In int stride, @Out PointerByReference output);

        long WebPEncodeLosslessRGBA(@In byte[] rgba, @In int width, @In int height,
                                    @In int stride, @Out PointerByReference output);

        long WebPEncodeLosslessBGRA(@In int[] bgra, @In int width, @In int height,
                                    @In int stride, @Out PointerByReference output);

        Pointer WebPDecodeRGBA(@In byte[] data, @In long data_size,
                               @Out IntByReference width, @Out IntByReference height);

        Pointer WebPDecodeARGB(@In byte[] data, @In long data_size,
                               @Out IntByReference width, @Out IntByReference height);

        Pointer WebPDecodeBGRA(@In byte[] data, @In long data_size,
                               @Out IntByReference width, @Out IntByReference height);

        Pointer WebPDecodeRGB(@In byte[] data, @In long data_size,
                              @Out IntByReference width, @Out IntByReference height);

        Pointer WebPDecodeBGR(@In byte[] data, @In long data_size,
                              @Out IntByReference width, @Out IntByReference height);

    }

}

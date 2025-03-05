package com.tianscar.webp;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.PointerByReference;

final class JNRFFI {
    /**
     * This is the default name of the libwebp library.
     */
    public static final String DEFAULT_WEBP_LIBRARY_NAME = "libwebp";
    /**
     * This is the name of the libwebp library.
     */
    private static String libraryName = DEFAULT_WEBP_LIBRARY_NAME;
    /**
     * This returns the name of the library used to handle WebP images. This is 
     * set to "{@value DEFAULT_WEBP_LIBRARY_NAME}" by default, but this is here 
     * for instances where the libwebp library may be using a different name.
     * @return The name of the libwebp library.
     * @see #setWebPLibraryName(java.lang.String) 
     * @see #DEFAULT_WEBP_LIBRARY_NAME
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
     */
    public static void setWebPLibraryName(String name){
            // If the name is null, reset the name to default
        libraryName = (name!=null)?name:DEFAULT_WEBP_LIBRARY_NAME;
    }

    private JNRFFI() {
        throw new UnsupportedOperationException();
    }
    /**
     * This loads and returns an instance of the WebP class.
     * @return An instance of the WebP class.
     */
    private static WebP loadLibrary(){
        return LibraryLoader.create(WebP.class).load(getWebPLibraryName());
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

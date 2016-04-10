package de.jpaw.bonaparte.api.media;

import de.jpaw.bonaparte.pojos.api.media.MediaType;
import de.jpaw.bonaparte.pojos.api.media.MediaXType;

public interface MediaTypes {
    public static final MediaXType MEDIA_XTYPE_BONAPARTE         = MediaXType.of(MediaType.BONAPARTE);
    public static final MediaXType MEDIA_XTYPE_XML               = MediaXType.of(MediaType.XML);
    public static final MediaXType MEDIA_XTYPE_JSON              = MediaXType.of(MediaType.JSON);
    public static final MediaXType MEDIA_XTYPE_COMPACT_BONAPARTE = MediaXType.of(MediaType.COMPACT_BONAPARTE);
    public static final MediaXType MEDIA_XTYPE_XLSX              = MediaXType.of(MediaType.XLSX);
    public static final MediaXType MEDIA_XTYPE_XLS               = MediaXType.of(MediaType.XLS);
    public static final MediaXType MEDIA_XTYPE_CSV               = MediaXType.of(MediaType.CSV);
    public static final MediaXType MEDIA_XTYPE_TEXT              = MediaXType.of(MediaType.TEXT);
    public static final MediaXType MEDIA_XTYPE_HTML              = MediaXType.of(MediaType.HTML);
    public static final MediaXType MEDIA_XTYPE_CSS               = MediaXType.of(MediaType.CSS);
    public static final MediaXType MEDIA_XTYPE_MARKDOWN          = MediaXType.of(MediaType.MARKDOWN);
    public static final MediaXType MEDIA_XTYPE_TEX               = MediaXType.of(MediaType.TEX);
    public static final MediaXType MEDIA_XTYPE_LATEX             = MediaXType.of(MediaType.LATEX);
    public static final MediaXType MEDIA_XTYPE_URL               = MediaXType.of(MediaType.URL);
    public static final MediaXType MEDIA_XTYPE_PDF               = MediaXType.of(MediaType.PDF);
    public static final MediaXType MEDIA_XTYPE_DVI               = MediaXType.of(MediaType.DVI);
    public static final MediaXType MEDIA_XTYPE_POSTSCRIPT        = MediaXType.of(MediaType.POSTSCRIPT);
    public static final MediaXType MEDIA_XTYPE_GIF               = MediaXType.of(MediaType.GIF);
    public static final MediaXType MEDIA_XTYPE_JPG               = MediaXType.of(MediaType.JPG);
    public static final MediaXType MEDIA_XTYPE_PNG               = MediaXType.of(MediaType.PNG);
    public static final MediaXType MEDIA_XTYPE_WAV               = MediaXType.of(MediaType.WAV);
    public static final MediaXType MEDIA_XTYPE_MP3               = MediaXType.of(MediaType.MP3);
    public static final MediaXType MEDIA_XTYPE_MP4               = MediaXType.of(MediaType.MP4);
    public static final MediaXType MEDIA_XTYPE_MPG               = MediaXType.of(MediaType.MPG);
    public static final MediaXType MEDIA_XTYPE_RAW               = MediaXType.of(MediaType.RAW);
    public static final MediaXType MEDIA_XTYPE_UNDEFINED         = MediaXType.of(MediaType.UNDEFINED);
}

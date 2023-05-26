package com.quectel.openglyuv.display.Qencoder;

public class Constants {
    //resolution
    public static final int WIDTH_720P = 1280;
    public static final int HEIGHT_720P = 720;

    public static final int WIDTH_1080P = 1920;
    public static final int HEIGHT_1080P = 1080;

    //bit rate
    public static final int BIT_RATE_1_M = 1024 * 1024;
    public static final int BIT_RATE_2_M = 2 * 1024 * 1024;
    public static final int BIT_RATE_4_M = 4 * 1024 * 1024;

    //mime type
    public static final String VIDEO_AVC = "video/avc";
    public static final String VIDEO_HEVC = "video/hevc";

    //frame rate
    public static final int FRAME_RATE_25 = 25;
    public static final int FRAME_RATE_30 = 30;

    // i frame interval
    public static final int I_FRAME_INTERVAL_1S = 1;
    public static final int I_FRAME_INTERVAL_2S = 2;

    //database
    public static final String DATA_BASE_RECORDER = "AISRecorder.db";

    //video type
    public static final String VIDEO_TYPE = "video_type";
    public static final String VIDEO_TYPE_MAIN = "main";
    public static final String VIDEO_TYPE_SUB = "sub";

    //video type
    public static final String CHANNEL_ID = "channel_id";

    //segmentation type
    public static final String VIDEO_SEGMENTATION_TYPE = "segmentation_type";
    public static final String SEGMENTATION_BY_DURATION = "segmentation_by_duration";
    //单位秒
    public static final String DURATION_SECONDS = "duration_seconds";

    public static final String SEGMENTATION_BY_SIZE = "segmentation_by_size";
    //单位MB
    public static final String SIZE_MB = "size_mb";

}

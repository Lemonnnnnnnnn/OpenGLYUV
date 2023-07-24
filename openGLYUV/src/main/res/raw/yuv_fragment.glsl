precision mediump float;

varying vec2 v_TextureCoordinates;

uniform sampler2D y_texture;
uniform sampler2D uv_texture;

void main() {
    //    float r, g, b, y, u, v;
    //
    //    y = texture2D(y_texture, v_TextureCoordinates).r;
    //    u = texture2D(uv_texture, v_TextureCoordinates).a - 0.5;
    //    v = texture2D(uv_texture, v_TextureCoordinates).r - 0.5;
    //
    //    r = y + 1.13983 * v;
    //    g = y - 0.39465 * u - 0.58060 * v;
    //    b = y + 2.03211 * u;
    //
    //    gl_FragColor = vec4(r, g, b, 1.0);

    vec3 yuv;
    vec3 rgb;
    // shader 会将数据归一化，而 uv 的取值区间本身存在-128到正128 然后归一化到0-1 为了正确计算成rgb，则需要归一化到 -0.5 - 0.5的区间
    //GL_LUMINANCE_ALPHA ：表示的是带 alpha通道的灰度图，即有2个通道，包含r和 a
    yuv.x = texture2D(y_texture, v_TextureCoordinates).r;
    yuv.y = texture2D(uv_texture, v_TextureCoordinates).a - 0.5;
    yuv.z = texture2D(uv_texture, v_TextureCoordinates).r - 0.5;
    rgb = mat3(
    1.0, 1.0, 1.0,
    0.0, -0.39465, 2.03211,
    1.13983, -0.5806, 0.0
    ) * yuv;
    //gl_FragColor是OpenGL内置的
    gl_FragColor = vec4(rgb, 1);
}
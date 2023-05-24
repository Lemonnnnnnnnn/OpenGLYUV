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
    //分别取yuv各个分量的采样纹理（r表示？）
    //
    yuv.x = texture2D(y_texture, v_TextureCoordinates).r;
    yuv.y = texture2D(uv_texture, v_TextureCoordinates).r - 0.5;
    yuv.z = texture2D(uv_texture, v_TextureCoordinates).a - 0.5;
    rgb = mat3(
    1.0, 1.0, 1.0,
    0.0, -0.39465, 2.03211,
    1.13983, -0.5806, 0.0
    ) * yuv;
    //gl_FragColor是OpenGL内置的
    gl_FragColor = vec4(rgb, 1);
}
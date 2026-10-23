/* SPDX-License-Identifier: MIT */

#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 t = texCoord0 * size;
    vec2 texelsPerPixel = clamp(fwidth(t), vec2(1e-5), vec2(1.0));
    vec2 seam = floor(t + 0.5);
    vec2 snapped = seam + clamp((t - seam) / texelsPerPixel, vec2(-0.5), vec2(0.5));
    vec4 color = textureGrad(Sampler0, snapped / size, dFdx(texCoord0), dFdy(texCoord0)) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}

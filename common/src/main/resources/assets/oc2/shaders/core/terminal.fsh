/* SPDX-License-Identifier: MIT */

#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

// Must match Terminal.Renderer.TEXTURE_RESOLUTION and Terminal.CHAR_WIDTH / Terminal.CHAR_HEIGHT.
const vec2 TEXTURE_SIZE = vec2(256.0);
const vec2 CELL_SIZE = vec2(8.0, 16.0);

void main() {
    vec2 t = texCoord0 * TEXTURE_SIZE;
    vec2 texelsPerPixel = max(fwidth(t), vec2(1e-5));
    vec2 seam = floor(t + 0.5);
    vec2 snapped = seam + clamp((t - seam) / texelsPerPixel, vec2(-0.5), vec2(0.5));
    vec2 cellOrigin = floor(t / CELL_SIZE) * CELL_SIZE;
    snapped = clamp(snapped, cellOrigin + 0.5, cellOrigin + CELL_SIZE - 0.5);
    float coverage = texture(Sampler0, snapped / TEXTURE_SIZE).a;

    vec4 color = vec4(vertexColor.rgb, vertexColor.a * coverage);
    if (color.a < (1.0 / 255.0)) {
        discard;
    }

    fragColor = color * ColorModulator;
}

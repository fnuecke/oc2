/* SPDX-License-Identifier: MIT */

#version 150

uniform int Count;

// Main camera depth buffer.
uniform sampler2D MainCameraDepth;
// Inverse matrix of main camera view rotation and projection. Not that this
// explicitly does *not* include the camera's actual position. All projector
// matrices are computed relative to the main camera position, for precision.
uniform mat4 InverseMainCamera;

// Projector 1
uniform sampler2D ProjectorColor0;
uniform sampler2D ProjectorDepth0;
uniform mat4 ProjectorCamera0;

// Projector 2
uniform sampler2D ProjectorColor1;
uniform sampler2D ProjectorDepth1;
uniform mat4 ProjectorCamera1;

// Projector 3
uniform sampler2D ProjectorColor2;
uniform sampler2D ProjectorDepth2;
uniform mat4 ProjectorCamera2;

const float PROJECTOR_NEAR = 1.0/16.0;
const float PROJECTOR_FAR = 32.0;
const float MAX_DISTANCE = 16;
const float START_FADE_DISTANCE = 12;
const float DOT_EPSILON = 0.25;
const float DEPTH_BIAS = 0.0001;
const vec2 IMAGE_SIZE = vec2(640.0, 480.0);
const mat4 CLIP_TO_TEX = mat4(
    0.5,   0,   0, 0,
      0, 0.5,   0, 0,
      0,   0, 0.5, 0,
    0.5, 0.5, 0.5, 1
);

in vec2 texCoord;

out vec4 fragColor;

vec3 getWorldPos(vec2 uv, float clipDepth) {
    vec2 clipUv = uv * 2 - 1;
    vec4 clipPos = vec4(clipUv, clipDepth, 1);
    vec4 worldPos = InverseMainCamera * clipPos;
    return worldPos.xyz / worldPos.w;
}

vec3 getNormal(vec3 worldPos) {
    return normalize(cross(dFdx(worldPos), dFdy(worldPos)));
}

vec3 sampleImage(sampler2D imageSampler, vec2 uv) {
    vec2 t = uv * IMAGE_SIZE;
    vec2 texelsPerPixel = max(fwidth(t), vec2(1e-5));
    vec2 seam = floor(t + 0.5);
    vec2 snapped = seam + clamp((t - seam) / texelsPerPixel, vec2(-0.5), vec2(0.5));
    return texture(imageSampler, snapped / IMAGE_SIZE).rgb;
}

bool isInClipBounds(vec3 v) {
    return v.x >= -1 && v.x <= 1 &&
           v.y >= -1 && v.y <= 1 &&
           v.z >= -1 && v.z <= 1;
}

bool getProjectorColor(vec3 worldPos, vec3 worldNormal,
                       mat4 projectorCamera,
                       sampler2D projectorColorSampler,
                       sampler2D projectorDepthSampler,
                       out vec3 result) {
    vec4 projectorClipPosPrediv = projectorCamera * vec4(worldPos, 1);
    vec3 projectorClipPos = projectorClipPosPrediv.xyz / projectorClipPosPrediv.w;
    vec4 projectorUvPrediv = CLIP_TO_TEX * projectorClipPosPrediv;
    vec2 projectorUv = projectorUvPrediv.xy / projectorUvPrediv.w;
    vec3 projectorColor = sampleImage(projectorColorSampler, vec2(projectorUv.s, 1 - projectorUv.t));

    // Project world normal into projector clip space.
    vec3 projectorClipNormal = (projectorCamera * vec4(worldNormal, 0)).xyz;

    // Cull sides and back-faces.
    float d = dot(projectorClipNormal, vec3(0, 0, -1));
    if (d <= DOT_EPSILON) {
        return false;
    }

    if (!isInClipBounds(projectorClipPos)) {
        return false;
    }

    float projectorDepth = texture(projectorDepthSampler, projectorUv).r;
    float projectorClipDepth = projectorDepth * 2 - 1;
    if (projectorClipPos.z > projectorClipDepth + DEPTH_BIAS) {
        return false;
    }

    float linearDepth = projectorClipPosPrediv.z;
    float dotAttenuation = clamp((d - DOT_EPSILON) / (1 - DOT_EPSILON), 0, 1);
    float distanceAttenuation = clamp(1 - (linearDepth - START_FADE_DISTANCE) / (MAX_DISTANCE - START_FADE_DISTANCE), 0, 1);
    result = projectorColor * dotAttenuation * distanceAttenuation;
    return true;
}

void main() {
    float depth = texture(MainCameraDepth, texCoord).r;

    // Check for no hit, for early exit.
    if (depth >= 1) {
        discard;
    }

    float clipDepth = depth * 2 - 1;
    vec3 worldPos = getWorldPos(texCoord, clipDepth);
    vec3 worldNormal = getNormal(worldPos);

    vec3 colorAcc = vec3(0);
    vec3 color;
    if (getProjectorColor(worldPos, worldNormal, ProjectorCamera0, ProjectorColor0, ProjectorDepth0, color)) {
        colorAcc += color;
    }
    if (Count > 1 && getProjectorColor(worldPos, worldNormal, ProjectorCamera1, ProjectorColor1, ProjectorDepth1, color)) {
        colorAcc += color;
    }
    if (Count > 2 && getProjectorColor(worldPos, worldNormal, ProjectorCamera2, ProjectorColor2, ProjectorDepth2, color)) {
        colorAcc += color;
    }

    fragColor = vec4(colorAcc, 1);
}

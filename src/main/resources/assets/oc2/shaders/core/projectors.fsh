#version 150

uniform int Count;

// Projector 1
uniform sampler2D Sampler0; // Main Depth
uniform mat4 InverseMainCamera; // Inverse Main View Projection Matrix

// Projector 2
uniform sampler2D Sampler1; // Color
uniform sampler2D Sampler2; // Depth
uniform mat4 ProjectorCamera0;

// Projector 3
uniform sampler2D Sampler3; // Color
uniform sampler2D Sampler4; // Depth
uniform mat4 ProjectorCamera1;

// Projector 4
uniform sampler2D Sampler5; // Color
uniform sampler2D Sampler6; // Depth
uniform mat4 ProjectorCamera2;

const float PROJECTOR_NEAR = 1.0/16.0;
const float PROJECTOR_FAR = 32.0;
const float MAX_DISTANCE = 16;
const float START_FADE_DISTANCE = 12;
const float DOT_EPSILON = 0.25;
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

vec3 getWorldPos(vec2 uv) {
    float depth = texture2D(Sampler1, uv).r;
    float clipDepth = depth * 2 - 1;
    return getWorldPos(uv, clipDepth);
}

vec3 getNormal(vec3 worldPos) {
    return normalize(cross(dFdx(worldPos), dFdy(worldPos)));
}

bool isInClipBounds(vec3 v) {
    return v.x >= -1 && v.x <= 1 &&
           v.y >= -1 && v.y <= 1 &&
           v.z >= -1 && v.z <= 1;
}

float toLinearDepth(float depth, float zNear, float zFar) {
    return zNear * zFar / (zFar + depth * (zNear - zFar));
}

bool getProjectorColor(vec3 worldPos, vec3 worldNormal, mat4 projectorCamera,
                       sampler2D projectorColorSampler, sampler2D projectorDepthSampler,
                       out vec4 result) {
    // Project world normal into projector clip space.
    vec3 projectorClipNormal = (projectorCamera * vec4(worldNormal, 0)).xyz;

    // Cull sides and back-faces.
    float d = dot(projectorClipNormal, vec3(0, 0, -1));
    if (d <= DOT_EPSILON) {
        return false;
    }

    vec4 projectorClipPosPrediv = projectorCamera * vec4(worldPos, 1);
    float linearDepth = projectorClipPosPrediv.z;
    if (linearDepth >= MAX_DISTANCE) {
        return false;
    }

    vec3 projectorClipPos = projectorClipPosPrediv.xyz / projectorClipPosPrediv.w;
    if (!isInClipBounds(projectorClipPos)) {
        return false;
    }

    vec4 projectorUvPrediv = CLIP_TO_TEX * projectorClipPosPrediv;
    vec2 projectorUv = projectorUvPrediv.xy / projectorUvPrediv.w;
    float projectorDepth = texture2D(projectorDepthSampler, projectorUv).r;
    float projectorClipDepth = projectorDepth * 2 - 1;

    if (projectorClipPos.z <= projectorClipDepth + 0.0001) {
        vec4 projectorColor = texture2D(projectorColorSampler, vec2(projectorUv.s, 1 - projectorUv.t));
        float dotAttenuation = clamp((d - DOT_EPSILON) / (1 - DOT_EPSILON), 0, 1);
        float distanceAttenuation = clamp(1 - (linearDepth - START_FADE_DISTANCE) / (MAX_DISTANCE - START_FADE_DISTANCE), 0, 1);
        result = projectorColor * dotAttenuation * distanceAttenuation;
        return true;
    }
    return false;
}

void main() {
    float depth = texture2D(Sampler0, texCoord).r;

    // Check for no hit, e.g. sky, for early exit.
    if (depth >= 1) {
        discard;
    }

    float clipDepth = depth * 2 - 1;
    vec3 worldPos = getWorldPos(texCoord, clipDepth);
    vec3 worldNormal = getNormal(worldPos);

    vec4 colorAcc = vec4(0, 0, 0, 0);
    vec4 color;
    int accCount = 0;
    if (Count > 0 && getProjectorColor(worldPos, worldNormal, ProjectorCamera0, Sampler1, Sampler2, color)) {
        colorAcc += color;
        accCount += 1;
    }
    if (Count > 1 && getProjectorColor(worldPos, worldNormal, ProjectorCamera1, Sampler3, Sampler4, color)) {
        colorAcc += color;
        accCount += 1;
    }
    if (Count > 2 && getProjectorColor(worldPos, worldNormal, ProjectorCamera2, Sampler5, Sampler6, color)) {
        colorAcc += color;
        accCount += 1;
    }

    // Check if we had any projections at all.
    if (accCount == 0) {
        discard;
    }

    fragColor = colorAcc / accCount;
}

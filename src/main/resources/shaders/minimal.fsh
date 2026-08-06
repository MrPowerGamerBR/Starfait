#version 330 core

in vec2 vTexCoord;
in vec3 vBlendColor;

uniform sampler2D uTexture;

out vec4 fragColor;

void main() {
    fragColor = texture(uTexture, vTexCoord) * vec4(vBlendColor.r, vBlendColor.g, vBlendColor.b, 1.0);
}
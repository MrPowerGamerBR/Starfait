#version 330 core

in vec2 vTexCoord;
in vec4 vBlendColor;

uniform sampler2D uTexture;

out vec4 fragColor;

void main() {
    fragColor = texture(uTexture, vTexCoord) * vBlendColor;
}
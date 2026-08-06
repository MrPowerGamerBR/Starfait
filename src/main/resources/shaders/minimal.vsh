#version 330 core

// For batching, we store the position of each quad directly on the position, so we don't actually *need* to have a uModel
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

uniform mat4 uProjection;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position =  uProjection * vec4(aPosition, 0.0, 1.0);
}
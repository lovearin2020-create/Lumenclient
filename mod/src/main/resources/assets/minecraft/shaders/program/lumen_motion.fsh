#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;
varying vec2 texCoord;
uniform float Amount;

void main() {
    vec4 current = texture2D(DiffuseSampler, texCoord);
    vec4 previous = texture2D(PrevSampler, texCoord);
    gl_FragColor = vec4(mix(current.rgb, previous.rgb, Amount), 1.0);
}

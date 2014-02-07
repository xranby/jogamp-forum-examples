#version 150 core

in vec3 Color;
in vec2 Texcoord;

uniform sampler2D Tex1;

out vec4 fragmentColor;

void main(void) 
{  	
	vec3 tex = vec3(texture(Tex1, Texcoord));
	fragmentColor =   vec4(Color, 1.0) * vec4(tex, 1.0);
}

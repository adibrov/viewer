/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */



__kernel void noise(
    __global int *output,
    int sizeX, int sizeY,
       float amplitude, float phi)
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);

        float vecX = 89.0f;
        float vecY = 99.0f;

        float dot = (float)ix*vecX + (float)iy*vecY;
      //  float phi = 1.0f;
        float holder;
        holder = 255.0f;
        float f = fract(1289878*sin(dot + phi*2.0f*3.14159265f), &holder);
        int res = ((int)(f*amplitude))%255;
       // if (holder > 255)
            output[iy*sizeX + ix] += ((res<<16) | (res<<8)| (res));
        //output[iy*sizeX + ix] = 100;

}
/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */

// A very simple OpenCL kernel for computing the mandelbrot set
//
// output        : A buffer with sizeX*sizeY elements, storing
//                 the colors as RGB ints
// sizeX, sizeX  : The width and height of the buffer
// x0,y0,x1,y1   : The rectangle in which the mandelbrot
//                 set will be computed
// maxIterations : The maximum number of iterations
// colorMap      : A buffer with colorMapSize elements,
//                 containing the pixel colors

__kernel void GaussianBlur(
    __global int *input,
    __global int *output,
    int sizeX, int sizeY,
    int delX, int delY,
    float sigmaX, float sigmaY
    )
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);
        int res = 0;
        float resF = 0.0f;
        float norm = 0.0f;
        float ga = 0.0f;

        for (float i = ix - delX; i < ix + delX; i+=1.0f) {
            for (float j = iy - delY; j < iy + delY; j+=1.0f) {
                if (i>=0 & i<sizeX & j>=0 & j<sizeY)
                {
                    float arg = -(ix-i)*(ix-i)/(2*sigmaX*sigmaX) - (iy-j)*(iy-j)/(2*sigmaY*sigmaY);
                    ga = exp(arg);

                    norm += ga;
                    resF += ((float)(input[(int)j*sizeX + (int)i] & 0xFF))*ga;
                    //printf("%f\n", resF);
                }
            }
        }


        res = (int)(resF/norm);

        output[iy*sizeX+ix] = ((res<<16) | (res<<8) | res);
        //output[iy*sizeX+ix] = res;

}
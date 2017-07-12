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

__kernel void computeMandelbrot(
    __global uint *output,
    int sizeX, int sizeY,
    float x0, float y0,
     float x1, float y1)
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);


        float venorm = 1.0f/sqrt((x1-x0)*(x1-x0) + (y1-y0)* (y1-y0));
        float scalar = venorm*((x0-x1)*(ix-x1) + (y0-y1)*(iy-y1) + (x0-x1)*(ix-x0)+(y0-y1)*(iy-y0));

        //float diff = fabs(scalar);
        int res = (int)(0.0001f*scalar);
        if (((ix-x1)*(ix-x1)+ (iy-y1)*(iy-y1) < 9) | ((ix-x0)*(ix-x0)+ (iy-y0)*(iy-y0) < 9)) {
            output[iy*sizeX+ix] = (255<<16) | (255<<8) | 255;
        }
        else {
              output[iy*sizeX+ix] = (res<<16) | (res<<8) | res;
        }


}
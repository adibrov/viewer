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
    __global short *output,
    int sizeX, int sizeY,
    float x0, float y0)
{
    unsigned int ix = get_global_id(0);
    unsigned int iy = get_global_id(1);

    float dx = ix - x0;
    float dy = iy - y0;



    if (dx*dx+ dy*dy <= 400) {
        output[iy*sizeX+ix] = 512;
       // printf("%d\n", output[iy*sizeX+ix]);
    }
    else {
        output[iy*sizeX+ix] = 0.0f;
    }
}
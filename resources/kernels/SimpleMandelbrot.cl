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
    float x1, float y1,
    float x2, float y2
  //  int maxIterations
 // __global uint *colorMap,
 //   int colorMapSize
    )
{
    unsigned int ix = get_global_id(0);
    unsigned int iy = get_global_id(1);


    float dx0 = ix - x0;
    float dy0 = iy - y0;

    float dx1 = ix - x1;
    float dy1 = iy - y1;

    float dx2 = ix - x2;
    float dy2 = iy - y2;



    float dist0 = (dx0*dx0 + dy0*dy0);
    float dist1 = (dx1*dx1 + dy1*dy1);
    float dist2 = (dx2*dx2 + dy2*dy2);

    float dist_min1 = min(dist0, dist1);
    float dist_min2 = 0;
    if (fabs(min(dist_min1,dist2) - dist_min1) < 0.1) {
        dist_min2 = max(dist0, dist1);
    }
    else {
        dist_min2 = dist2;
    }
    output[iy*sizeX+ix] = 255.0f*1.0f/(1.0f + 0.001f*fabs(dist_min2 - dist_min1));
    //output[iy*sizeX+ix] = fabs(dist_min2 - dist_min1);


    //if ((ix <= 250) && (iy <= 250) ) {
    //    output[iy*sizeX+ix] = 100;
	//}
	//else{
	 //   output[iy*sizeX+ix] = 0;
	//}
}
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
    __global int *output,
    int sizeX, int sizeY,
    float x0, float y0,
     __global float *pointsX,
     __global float *pointsY,
       int psize)
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);

        float dx = 0;
        float dy = 0;

        float dist = 0;

        float px1 = 0;
        float py1 = 0;
        float px2 = 0;
        float py2 = 0;


         float minDist1 = 100000000;
         float minDist2 = 100000000;

         for (int i = 0; i < psize; i++) {
            dx = pointsX[i] - ix;
            dy = pointsY[i] - iy;
            dist = sqrt(dx*dx + dy*dy);

            if (min(minDist1, dist) == dist) {
                    minDist2 = minDist1;
                    minDist1 = dist;
                    px2 = px1;
                    py2 = py1;
                    px1 = pointsX[i];
                    py1 = pointsY[i];
            }

            else if (min(minDist2, dist) == dist) {
                minDist2 = dist;
                px2 = pointsX[i];
                py2 = pointsY[i];
            }
         }



        if (px1 > px2) {
            float aux = px1;
            px1 = px2;
            px2 = aux;
            aux = py1;
            py1 = py2;
            py2 = aux;
        }



        float eps = 3.f;

        float venorm = 1.0f/sqrt((px2-px1)*(px2-px1) + (py2-py1)* (py2-py1));

        //float diff = venorm*venorm*fabs(minDist1*minDist1 - minDist2*minDist2);

        float scalar = venorm*((px1-px2)*(ix-px2) + (py1-py2)*(iy-py2) + (px1-px2)*(ix-px1)+(py1-py2)*(iy-py1));

        float diff = fabs(scalar);
        if (diff < eps) {
            int res = 510;
           output[iy*sizeX+ix] = ((res<<16) | (res<<8) | res);
           // output[iy*sizeX+ix] = 510;
        }
        else{
            output[iy*sizeX+ix] = ((0<<16) | (0<<8) | 0);
            //output[iy*sizeX+ix] = 0;
        }

}
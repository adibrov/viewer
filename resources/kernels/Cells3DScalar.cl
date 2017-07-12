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
    int sizeX, int sizeY, int sizeZ,
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


        int res = 510;
        float sigmaX = 50.0f;
        float sigmaY = 50.0f;
        float arg = -((float)ix-((float)sizeX/2.0f))*((float)ix-((float)sizeX/2.0f))/(2.0f*sigmaX*sigmaX) - (
        (float)iy-((float)sizeY/2.0f))*((float)iy-((float)sizeY/2.0f))/(2.0f*sigmaY*sigmaY);

        float ga = exp(arg);
        float sizeZf = (float)sizeZ;
        ga = ga*sizeZf;
        // printf("%f\n", arg);
        int z = (int)ga;



        float eps = 1.0f;

        float venorm = 1.0f/sqrt((px2-px1)*(px2-px1) + (py2-py1)* (py2-py1));

        float diff = fabs(venorm*((px1-px2)*(ix-px2) + (py1-py2)*(iy-py2) + (px1-px2)*(ix-px1)+(py1-py2)*(iy-py1)));
        if (diff < eps) {

            output[z*sizeX*sizeY + iy*sizeX+ix] = res;
        }
        else{

            output[z*sizeX*sizeY +iy*sizeX+ix] = 0;
        }

}
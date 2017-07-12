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
     __global float *pointsX,
     __global float *pointsY,
       int psize)
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);





        float dx0 = 0;
        float dy0 = 0;
        float dist0 = 0;

        //float dx1 = ix - x1;
        //float dy1 = iy - y1;

        //float dx2 = ix - x2;
        //float dy2 = iy - y2;

         float minDist = 1000000000;
         float minDist2 = 100000000000;
         for (int i = 0; i < psize; i++) {
            dx0 = pointsX[i] - ix;
            dy0 = pointsY[i] - iy;

            dist0 = sqrt(dx0*dx0 + dy0*dy0);
            if (min(minDist, dist0) == dist0) {

                    minDist2 = minDist;
                    minDist = dist0;

            }


           // if ((ix == 250) && (iy == 250)) {
            //      printf("%f\n", pointsX[0]);
           // }

         }




        //float dist0 = (dx0*dx0 + dy0*dy0);
        //float dist1 = (dx1*dx1 + dy1*dy1);
      // float dist2 = (dx2*dx2 + dy2*dy2);

        //float dist_min1 = min(dist0, dist1);
        //float dist_min2 = 0;
        //if (fabs(min(dist_min1,dist2) - dist_min1) < 0.1) {
//            dist_min2 = max(dist0, dist1);
 //       }
  //      else {
    //        dist_min2 = dist2;
      //  }
        //float res1 = 255.0f*1.0f/(1.0f + 0.001f*fabs(dist0 - 62500));
        float norm = 1.0f/((sqrt(2.0f)*500.0f));
        //float res1 = 255.0f*minDist*norm*3;
        //float res1 = 255.0f*1.0f/(1.0f + fabs(pow(minDist2 - minDist,2)));
        //float res1 = 255.0f*exp(-(pow(minDist2 - minDist,2))/(100));
        //float res1 = minDist*norm*255.0f;

        float eps = 20.0f;
        float diff = fabs(minDist*minDist - minDist2*minDist2);
        if (  diff < eps) {
           // printf("%f\n", diff);
            float res1 = 255.0f;
            int res = (int) res1;
            output[iy*sizeX+ix] = ((res<<16) | (res<<8) | res);
        }
        else{
            output[iy*sizeX+ix] = 0.0f;
        }

      //  int res = (int) res1;
      //  output[iy*sizeX+ix] = ((res<<16) | (res<<8) | res);
}
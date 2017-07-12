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
    __global short *input,
    __global short *output,
    int sizeX, int sizeY, int sizeZ,
    int delX, int delY, int delZ,
    float sigmaX, float sigmaY, float sigmaZ
    )
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);
        unsigned int iz = get_global_id(2);

        int res = 0;
        float resF = 0.0f;
        float norm = 0.0f;
        float ga = 0.0f;
        float denomX = 2.0f*sigmaX*sigmaX;
        float denomY = 2.0f*sigmaY*sigmaY;
        float denomZ = 2.0f*sigmaZ*sigmaZ;

        for (int i = ix - delX; i < ix + delX; i++) {
            for (int j = iy - delY; j < iy + delY; j++) {
                for (int k = iz - delZ; k < iz + delZ; k++) {

                    if (i>=0 & i<sizeX & j>=0 & j<sizeY & k>=0 & k<sizeZ)
                    {
                        float arg = -(ix-(float)i)*(ix-(float)i)/(denomX) - (iy-(float)j)*(iy-(float)j)/(denomY) -
                        (iz-(float)k)*
                        (iz-(float)k)/(denomZ);
                        ga = exp(arg);

                        norm += ga;
                        resF += ((float)input[(int)k*sizeX*sizeY + (int)j*sizeX + (int)i])*ga;
                        //printf("%f\n", resF);

                    }
                }
            }
        }


        res = (int)(resF/norm);

        //output[iy*sizeX+ix] = ((res<<16) | (res<<8) | res);
        output[iz*sizeX*sizeY + iy*sizeX+ix] = res;

}
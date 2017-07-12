/*
 * A kernel to compute tiles.
 */


__kernel void ComputeTiles(
    __global short *input,
    __global int *tilesOriX,
    __global int *tilesOriY,
    __global int *output,
    int sizeX, int sizeY, int sizeZ,
    int tilesInX, int tilesInY,
    int threshold
    )
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);


        int x0 = tilesOriX[ix];
        int y0 = tilesOriY[iy];

        int x1; int y1;

        if (ix + 1 >= tilesInX) {
            x1 =sizeX;
        }
        else {
            x1 = tilesOriX[ix+1];
        }

        if (iy + 1 >= tilesInY) {
            y1 =sizeY;
        }
        else {
            y1 = tilesOriY[iy+1];
        }

        // z loop


        short hist[512];

        for (int j = 0; j<512; j++) {
             hist[j]=0;
        }

        int max = 0;
        int indMax = 0;
        int sum = 0;

        int norm = 0;


        int colMax = 0;

        for (int k = 0; k < sizeZ; k++) {
            for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                       hist[input[k*sizeX*sizeY + sizeX*j + i]] +=1;
                }
            }

        }

        int num = 0;
        int th = 511;
        while (num < (int)(0.1f*(y1-y0)*(x1-x0)*sizeZ)) {
             num +=  hist[th];
             th--;
        }

       // norm = (int)((float)norm*1.0f/((float)(x1-x0)*(float)(y1-y0)*(float)sizeZ));

        for (int k = 0; k < sizeZ; k++) {
            for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                       if (input[sizeX*sizeY*k + sizeX*j + i] >th+1) {
                            sum+=1;
                            //printf("hello");
                       }
                }
            }

            if (sum > max) {
                max = sum;
                indMax = k;
            }
           sum = 0;

        }

        output[tilesInX*iy + ix] = indMax;


}
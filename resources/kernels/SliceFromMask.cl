/*
 * A kernel to produce a slice of a 3D stack according to a give tiling mask.
 */


__kernel void SliceFromMask(
    __global short *input,
    __global short *output,
    int sizeX, int sizeY, int sizeZ,
    __global int *tilesOriX,
    __global int *tilesOriY,
    __global int *tilesCoordZ,
    int tilesInX,
    int tilesInY)
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

        int k = tilesCoordZ[tilesInX*iy + ix];

        for (int i = x0; i < x1; i++) {
            for (int j = y0; j < y1; j++) {
                   output[sizeX*j + i] = input[sizeX*sizeY*k + sizeX*j + i];
            }
        }




}
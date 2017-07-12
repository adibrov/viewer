/*
 * A kernel to produce a slice of a 3D stack.
 */


__kernel void Slicer(
    __global short *input,
    __global short *output,
    int sizeX, int sizeY, int sizeZ,
    int sliceDim, int slicePos)
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);
        unsigned int iz = get_global_id(2);

        if (sliceDim == 0) {
            output[sizeY*iz + iy] = input[sizeX*sizeY*iz + sizeX*iy + slicePos];
        }
        else if (sliceDim == 1) {
            output[sizeX*iz + ix] = input[sizeX*sizeY*iz + sizeX*slicePos + ix];
        }
        else {
           // printf("here");
            if (iz == slicePos) {
                output[sizeX*iy + ix] = input[sizeX*sizeY*iz+sizeX*iy+ix];
                //output[sizeX*iy + ix] = 100;
           }
        }
}
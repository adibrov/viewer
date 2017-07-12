/*
 * A kernel to scale a grid.
 */


__kernel void ScaleGrid(
    __global int *input,
    __global int *output,
    int sizeX, int sizeY, int scaleX, int scaleY)
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);

        output[iy*sizeX + ix] = input[(iy/scaleY)*sizeX/scaleX +ix/scaleX ];
      //  output[iy*sizeX + ix] = 1;



}
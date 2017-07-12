/*
 * A kernel to convert a 16 bit image to an 8 bit one.
 */


__kernel void SixteenToEightBit(
    __global short *input,
    __global char *output,
    int sizeX, int sizeY,
    short min, short max)
{
        unsigned int ix = get_global_id(0);
        unsigned int iy = get_global_id(1);
        short intensity = input[sizeX * iy + ix];
        if (intensity >= min) {
            output[sizeX * iy + ix] = (char)(fmin(((float)(intensity - min)*255.0f/((float)(max-min))),255.0f));
        }
        else {
            output[sizeX * iy + ix] = 0;
        }


}
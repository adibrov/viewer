/*
 * A kernel to refine the tile selection based on variance.
 */


__kernel void RefineWithVariance(
    __global short *input,
    __global int *tilesOriX,
    __global int *tilesOriY,
    __global int *output,
    int sizeX, int sizeY, int sizeZ,
    int tilesInX, int tilesInY,
    int deltaZ
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

        int currZ = output[tilesInX*iy + ix];
        int down = max(0,currZ-deltaZ);
        int up =  min(sizeZ-1, currZ+deltaZ);
        int curr =  currZ;

        int currMean = 0;
        int currVar = 0;
        int norm = (x1-x0)*(y1-y0);
        int maxVar = 0;
        int maxVarInd = currZ;


        // curr
        for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                   currMean += input[sizeX*sizeY*currZ + sizeX*j + i];
                }
        }
        currMean /= norm;
        for (int i = x0; i < x1; i++) {
            for (int j = y0; j < y1; j++) {
               int aux = (input[sizeX*sizeY*currZ + sizeX*j + i] - currMean);
               currVar += aux*aux;
            }
        }

        currVar /= norm;
        maxVar = currVar;
        currVar = 0;
        currMean = 0;



        // below
        for (int k = down; k < curr; k++) {
            for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                   currMean += input[sizeX*sizeY*k + sizeX*j + i];
                }
            }
            currMean /= norm;
            for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                   int aux = (input[sizeX*sizeY*k + sizeX*j + i] - currMean);
                   currVar += aux*aux;
                }
            }

            currVar /= norm;

            if (currVar > maxVar) {
                currVar = maxVar;
                maxVarInd = k;
            }

            currVar = 0;
            currMean = 0;
        }

        // above
        for (int k = curr+1; k <= up; k++) {
            for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                   currMean += input[sizeX*sizeY*k + sizeX*j + i];
                }
            }
            currMean /= norm;
            for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                   int aux = (input[sizeX*sizeY*k + sizeX*j + i] - currMean);
                   currVar += aux*aux;
                }
            }

            currVar /= norm;

            if (currVar > maxVar) {
                currVar = maxVar;
                maxVarInd = k;
            }

            currVar = 0;
            currMean = 0;
        }



        output[tilesInX*iy + ix] = maxVarInd;


}
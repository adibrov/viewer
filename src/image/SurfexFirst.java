package image;

import coremem.offheap.OffHeapMemory;
import coremem.offheap.OffHeapMemoryAccess;
import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.img.planar.OffHeapPlanarImg;
import net.imglib2.img.planar.OffHeapPlanarImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.jocl.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static clearcontrol.simulation.loaders.SampleSpaceSaveAndLoad.loadUnsignedShortSampleSpaceFromDisk;
import static org.jocl.CL.*;

/**
 * Created by dibrov on 20/04/17.
 */
public class SurfexFirst {

    // Image
    private OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> srcImg;

    // OpenCL - general
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_mem image;
    private cl_mem tileOriX;
    private cl_mem tileOriY;
    private cl_mem tiles;

    // OpenCL - kernels

    // ComputeTiles - intensity based
    private String kernelSourceComputeTiles;
    private cl_kernel kernelComputeTiles;

    // Refinement with variance
    private String kernelSourceRefineWithVariance;
    private cl_kernel kernelRefineWithVariance;

    // State
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    private int tilesInX;
    private int tilesInY;

    private int[] mTilesOriX;
    private int[] mTilesOriY;

    private int[] tilesZCoordInt;
    private int[] tilesZCoordVar;


    private long[] workSizeTiles;
    private int threshold;
    private int deltaZ;


    public SurfexFirst(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> inputImg, int[] pTilesOriX, int[]
            pTilesOriY) {

        this.srcImg = inputImg;

        this.sizeX = (int) inputImg.dimension(0);
        this.sizeY = (int) inputImg.dimension(1);
        this.sizeZ = (int) inputImg.dimension(2);

        this.kernelSourceComputeTiles = readFile("resources/kernels/ComputeTiles.cl");
        this.kernelSourceRefineWithVariance = readFile("resources/kernels/RefineWithVariance.cl");

        initCL();

        this.mTilesOriY = pTilesOriY;
        this.mTilesOriX = pTilesOriX;
        this.tilesInX = mTilesOriX.length;
        this.tilesInY = mTilesOriY.length;
        System.out.println("arrs: " + mTilesOriX[0] + " " + mTilesOriX[1] + " " + mTilesOriX[2] + " " + mTilesOriY[0] +
                "" +
                " " +
                mTilesOriY[1] + " " + mTilesOriY[2]);
        System.out.println("arrs length: " + mTilesOriX.length + " " + mTilesOriY.length);


        this.workSizeTiles = new long[2];
        workSizeTiles[0] = tilesInX;
        workSizeTiles[1] = tilesInY;

        this.tilesZCoordInt = new int[tilesInX * tilesInY];
        this.tilesZCoordVar = new int[tilesInX * tilesInY];
        this.threshold = 100;
        this.deltaZ = 2;

        image = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                sizeX * sizeY * sizeZ * Sizeof.cl_short, null, null);
        tileOriX = clCreateBuffer(context, CL_MEM_READ_WRITE, tilesInX * Sizeof.cl_int, null,
                null);
        tileOriY = clCreateBuffer(context, CL_MEM_READ_WRITE, tilesInY * Sizeof.cl_int, null,
                null);

        tiles = clCreateBuffer(context, CL_MEM_READ_WRITE, tilesInX * tilesInY * Sizeof.cl_int, null,
                null);

        clEnqueueWriteBuffer(commandQueue, image, true, 0,
                Sizeof.cl_short * sizeY * sizeX * sizeZ, Pointer.to(srcImg.getContiguousMemory().getByteBuffer()), 0,
                null, null);
        clEnqueueWriteBuffer(commandQueue, tileOriX, true, 0,
                Sizeof.cl_int * tilesInX, Pointer.to(mTilesOriX), 0,
                null, null);
        clEnqueueWriteBuffer(commandQueue, tileOriY, true, 0,
                Sizeof.cl_int * tilesInY, Pointer.to(mTilesOriY), 0,
                null, null);
//        clEnqueueWriteBuffer(commandQueue, tiles, true, 0,
//                Sizeof.cl_short * tilesInY * tilesInX, Pointer.to(tilesZCoord), 0,
//                null, null);


        clSetKernelArg(kernelComputeTiles, 0, Sizeof.cl_mem, Pointer.to(image));
        clSetKernelArg(kernelComputeTiles, 1, Sizeof.cl_mem, Pointer.to(tileOriX));
        clSetKernelArg(kernelComputeTiles, 2, Sizeof.cl_mem, Pointer.to(tileOriY));
        clSetKernelArg(kernelComputeTiles, 3, Sizeof.cl_mem, Pointer.to(tiles));
        clSetKernelArg(kernelComputeTiles, 4, Sizeof.cl_int, Pointer.to(new int[]{sizeX}));
        clSetKernelArg(kernelComputeTiles, 5, Sizeof.cl_int, Pointer.to(new int[]{sizeY}));
        clSetKernelArg(kernelComputeTiles, 6, Sizeof.cl_int, Pointer.to(new int[]{sizeZ}));
        clSetKernelArg(kernelComputeTiles, 7, Sizeof.cl_int, Pointer.to(new int[]{tilesInX}));
        clSetKernelArg(kernelComputeTiles, 8, Sizeof.cl_int, Pointer.to(new int[]{tilesInY}));
        clSetKernelArg(kernelComputeTiles, 9, Sizeof.cl_int, Pointer.to(new int[]{threshold}));

        clSetKernelArg(kernelRefineWithVariance, 0, Sizeof.cl_mem, Pointer.to(image));
        clSetKernelArg(kernelRefineWithVariance, 1, Sizeof.cl_mem, Pointer.to(tileOriX));
        clSetKernelArg(kernelRefineWithVariance, 2, Sizeof.cl_mem, Pointer.to(tileOriY));
        clSetKernelArg(kernelRefineWithVariance, 3, Sizeof.cl_mem, Pointer.to(tiles));
        clSetKernelArg(kernelRefineWithVariance, 4, Sizeof.cl_int, Pointer.to(new int[]{sizeX}));
        clSetKernelArg(kernelRefineWithVariance, 5, Sizeof.cl_int, Pointer.to(new int[]{sizeY}));
        clSetKernelArg(kernelRefineWithVariance, 6, Sizeof.cl_int, Pointer.to(new int[]{sizeZ}));
        clSetKernelArg(kernelRefineWithVariance, 7, Sizeof.cl_int, Pointer.to(new int[]{tilesInX}));
        clSetKernelArg(kernelRefineWithVariance, 8, Sizeof.cl_int, Pointer.to(new int[] {tilesInY}));
        clSetKernelArg(kernelRefineWithVariance, 9, Sizeof.cl_int, Pointer.to(new int[] {deltaZ}));


    }

    public int[] computeTiles() {
        clEnqueueNDRangeKernel(commandQueue, kernelComputeTiles, 2, null,
                workSizeTiles, null, 0, null, null);
        clEnqueueReadBuffer(commandQueue, tiles, CL_TRUE, 0,
                Sizeof.cl_int * tilesInX * tilesInY, Pointer.to(tilesZCoordInt), 0, null,
                null);

        return tilesZCoordInt;
    }

    public int[] refineWithVariance() {
        clEnqueueNDRangeKernel(commandQueue, kernelRefineWithVariance, 2, null,
                workSizeTiles, null, 0, null, null);
        clEnqueueReadBuffer(commandQueue, tiles, CL_TRUE, 0,
                Sizeof.cl_int * tilesInX * tilesInY, Pointer.to(tilesZCoordVar), 0, null,
                null);

        return tilesZCoordVar;
    }


    private void initCL() {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_GPU;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Program Setup


        // Create the program
        cl_program cpProgramTiles = clCreateProgramWithSource(context, 1,
                new String[]{kernelSourceComputeTiles}, null, null);

        cl_program cpProgramVar = clCreateProgramWithSource(context, 1,
                new String[]{kernelSourceRefineWithVariance}, null, null);

        // Build the program
        clBuildProgram(cpProgramTiles, 0, null, "-cl-mad-enable", null, null);
        clBuildProgram(cpProgramVar, 0, null, "-cl-mad-enable", null, null);

        // Create the kernelComputeTiles
        kernelComputeTiles = clCreateKernel(cpProgramTiles, "ComputeTiles", null);
        kernelRefineWithVariance = clCreateKernel(cpProgramVar, "RefineWithVariance", null);


    }


    private String readFile(String fileName) {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fileName)));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public static void main(String[] args) {

//        convertTIFFToRaw("resources/img/smallWingStack.tif", "resources/img/smallWingStack");
        String path = "resources/img/smallWingStack692x520x50.raw";
        int[] dims = {692, 520, 50};
        long[] dimsLong = {692, 520, 50};
        short[] rawArr = new short[dims[0] * dims[1] * dims[2]];
        rawArr = loadUnsignedShortSampleSpaceFromDisk(path, dims[0], dims[1], dims[2]);

        Object h = new Object();
        OffHeapMemory cmIn = null;
        OffHeapMemory cmOut = null;
        cmIn = new OffHeapMemory("mem", h, OffHeapMemoryAccess.allocateMemory((2 * rawArr.length)), 2 * rawArr.length);
        cmIn.copyFrom(rawArr);


        OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> imgIn = (OffHeapPlanarImg<UnsignedShortType,
                ShortOffHeapAccess>) new OffHeapPlanarImgFactory().createShortInstance(cmIn, dimsLong, new
                UnsignedShortType
                ());


        short[] hist = new short[512];
        short[] arr = new short[dims[0] * dims[1] * dims[2]];
        imgIn.getContiguousMemory().copyTo(arr);
        int x0 = 350;
        int y0 = 20;
        int x1 = 400;
        int y1 = 60;
        int sizeZ = (int) imgIn.dimension(2);
        int sizeY = dims[1];
        int sizeX = dims[0];
        for (int k = 0; k < sizeZ; k++) {
            for (int i = x0; i < x1; i++) {
                for (int j = y0; j < y1; j++) {
                    hist[arr[k * sizeX * sizeY + sizeX * j + i]] += 1;
                }
            }

        }

        int num = 0;
        int th = 511;
        while (num < (int) (0.1f * (y1 - y0) * (x1 - x0) * sizeZ)) {
            num += hist[th];
            th--;
        }


        System.out.println("hist last terms: ");
        for (int i = 200; i > 20; i--) {
            System.out.println(hist[i]);
        }


        System.out.println("th: " + th + " numpix: " + (y1 - y0) * (x1 - x0) * sizeZ + " fract num: " + num);
    }

}

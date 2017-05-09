package image;

import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.img.planar.OffHeapPlanarImg;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.jocl.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import static org.jocl.CL.*;

/**
 * Created by dibrov on 20/04/17.
 */
public class Slicer {

    OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> srcImg;
    OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> trgImg;

    private int sizeX;
    private int sizeY;
    private int sizeZ;

    private Plane mPlane;
    private int planeSizeX;
    private int planeSizeY;
    private ByteBuffer imageBuffer;
    DirectAccessImageByteGray image;
    private short min;
    private short max;

    private String kernelSourceSlice;
    private String kernelSourceConvert;
    private String kernelSourceSliceFromMask;

    private cl_context context;
    private cl_command_queue commandQueue;

    private cl_kernel kernelSlice;
    private cl_kernel kernelConvert;
    private cl_kernel kernelSliceFromMask;

    private cl_mem pixelMem;
    private cl_mem interm;
    private cl_mem outputImgCL;
    private cl_mem tilesOriX;
    private cl_mem tilesOriY;
    private cl_mem tilesCoordZ;

    private long[] workSizeSlice;
    private long[] workSizeConvert;


    public Slicer(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> inputImg,
                  DirectAccessImageByteGray img, Plane pPlane) {
        this.image = img;
        this.mPlane = pPlane;
        this.sizeX = (int) inputImg.dimension(0);
        this.sizeY = (int) inputImg.dimension(1);
        this.sizeZ = (int) inputImg.dimension(2);
        int[] dimsInd = mPlane.getActiveDims();
        this.planeSizeX = (int) inputImg.dimension(dimsInd[0]);
        this.planeSizeY = (int) inputImg.dimension(dimsInd[1]);

        this.min = 0;
        this.max = 511;

        this.srcImg = inputImg;
        this.trgImg = trgImg;
        this.kernelSourceSlice = readFile("resources/kernels/Slicer.cl");
        this.kernelSourceConvert = readFile("resources/kernels/SixteenToEightBit.cl");
        this.kernelSourceSliceFromMask = readFile("resources/kernels/SliceFromMask.cl");

        initCL();

        this.workSizeSlice = new long[3];
        workSizeSlice[0] = sizeX;
        workSizeSlice[1] = sizeY;
        workSizeSlice[2] = sizeZ;

        this.workSizeConvert = new long[2];
        workSizeConvert[0] = planeSizeX;
        workSizeConvert[1] = planeSizeY;

        clEnqueueWriteBuffer(commandQueue, pixelMem, true, 0,
                Sizeof.cl_short * sizeY * sizeX*sizeZ, Pointer.to(srcImg.getContiguousMemory().getByteBuffer()), 0,
                null, null);


        clSetKernelArg(kernelSlice, 0, Sizeof.cl_mem, Pointer.to(pixelMem));
        clSetKernelArg(kernelSlice, 1, Sizeof.cl_mem, Pointer.to(interm));
        clSetKernelArg(kernelSlice, 2, Sizeof.cl_int, Pointer.to(new int[]{sizeX}));
        clSetKernelArg(kernelSlice, 3, Sizeof.cl_int, Pointer.to(new int[]{sizeY}));
        clSetKernelArg(kernelSlice, 4, Sizeof.cl_int, Pointer.to(new int[]{sizeZ}));
        clSetKernelArg(kernelSlice, 5, Sizeof.cl_int, Pointer.to(new int[]{mPlane.getSlideDim()}));

        clSetKernelArg(kernelConvert, 0, Sizeof.cl_mem, Pointer.to(interm));
        clSetKernelArg(kernelConvert, 1, Sizeof.cl_mem, Pointer.to(outputImgCL));
        clSetKernelArg(kernelConvert, 2, Sizeof.cl_int, Pointer.to(new int[] {planeSizeX}));
        clSetKernelArg(kernelConvert, 3, Sizeof.cl_int, Pointer.to(new int[] {planeSizeY}));
        clSetKernelArg(kernelConvert, 4, Sizeof.cl_short, Pointer.to(new short[] {this.min}));
        clSetKernelArg(kernelConvert, 5, Sizeof.cl_short, Pointer.to(new short[] {this.max}));

        clSetKernelArg(kernelSliceFromMask, 0, Sizeof.cl_mem, Pointer.to(pixelMem));
        clSetKernelArg(kernelSliceFromMask, 1, Sizeof.cl_mem, Pointer.to(interm));
        clSetKernelArg(kernelSliceFromMask, 2, Sizeof.cl_int, Pointer.to(new int[]{sizeX}));
        clSetKernelArg(kernelSliceFromMask, 3, Sizeof.cl_int, Pointer.to(new int[]{sizeY}));
        clSetKernelArg(kernelSliceFromMask, 4, Sizeof.cl_int, Pointer.to(new int[]{sizeZ}));

    }

    public void setMin(short min) {
        this.min = min;
        clSetKernelArg(kernelConvert, 4, Sizeof.cl_short, Pointer.to(new short[] {this.min}));

    }

    public void setMax(short max) {
        this.max = max;
        clSetKernelArg(kernelConvert, 5, Sizeof.cl_short, Pointer.to(new short[] {this.max}));

    }


    public void getSlice(int slicePos) {
        clSetKernelArg(kernelSlice, 6, Sizeof.cl_int, Pointer.to(new int[]{slicePos}));
        clEnqueueNDRangeKernel(commandQueue, kernelSlice, 3, null,
                workSizeSlice, null, 0, null, null);
        convert();
    }

    public void getSliceFromTileMask(int[] tilesOriX, int[] tilesOriY, int[] tilesCoordZ, DirectAccessImageByteGray
            image) {
        this.tilesOriX = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                tilesOriX.length * Sizeof.cl_int, null, null);
        this.tilesOriY = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                tilesOriY.length * Sizeof.cl_int, null, null);
        this.tilesCoordZ = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                tilesCoordZ.length * Sizeof.cl_int, null, null);

       long[] workSizeSliceFromMask = new long[2];
        workSizeSliceFromMask[0] = tilesOriX.length;
        workSizeSliceFromMask[1] = tilesOriY.length;

        for (int i = 0; i < tilesOriY.length; i++) {
            for (int j = 0; j < tilesOriX.length; j++) {
                System.out.print(tilesCoordZ[i*tilesOriX.length + j] + " ");
            }
            System.out.println();
        }

        System.out.println("tiles ori sizes " + tilesOriX.length + " " + tilesOriY.length);
        System.out.println("tiles z size " + tilesCoordZ.length);

        clEnqueueWriteBuffer(commandQueue, this.tilesOriX, true, 0,
                Sizeof.cl_int * tilesOriX.length, Pointer.to(tilesOriX), 0,
                null, null);

        clEnqueueWriteBuffer(commandQueue, this.tilesOriY, true, 0,
                Sizeof.cl_int * tilesOriY.length, Pointer.to(tilesOriY), 0,
                null, null);

        clEnqueueWriteBuffer(commandQueue, this.tilesCoordZ, true, 0,
                Sizeof.cl_int * tilesCoordZ.length, Pointer.to(tilesCoordZ), 0,
                null, null);

        clSetKernelArg(kernelSliceFromMask, 5, Sizeof.cl_mem, Pointer.to(this.tilesOriX));
        clSetKernelArg(kernelSliceFromMask, 6, Sizeof.cl_mem, Pointer.to(this.tilesOriY));
        clSetKernelArg(kernelSliceFromMask, 7, Sizeof.cl_mem, Pointer.to(this.tilesCoordZ));
        clSetKernelArg(kernelSliceFromMask, 8, Sizeof.cl_int, Pointer.to(new int[] {tilesOriX.length}));
        clSetKernelArg(kernelSliceFromMask, 9, Sizeof.cl_int, Pointer.to(new int[] {tilesOriY.length}));

        clEnqueueNDRangeKernel(commandQueue, kernelSliceFromMask, 2, null,
                workSizeSliceFromMask, null, 0, null, null);
        clEnqueueNDRangeKernel(commandQueue, kernelConvert, 2, null,
                workSizeConvert, null, 0, null, null);
        clEnqueueReadBuffer(commandQueue, outputImgCL, CL_TRUE, 0,
                Sizeof.cl_char * (int)image.getWidth()*(int)image.getHeight(), Pointer.to(image.getBuffer()), 0,
                null,
                null);
        try {
            image.update();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void convert() {
        clEnqueueNDRangeKernel(commandQueue, kernelConvert, 2, null,
                workSizeConvert, null, 0, null, null);
        clEnqueueReadBuffer(commandQueue, outputImgCL, CL_TRUE, 0,
                Sizeof.cl_char * planeSizeX * planeSizeY, Pointer.to(this.image.getBuffer()), 0, null,
                null);
        try {
            this.image.update();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        cl_program cpProgramSlice = clCreateProgramWithSource(context, 1,
                new String[]{kernelSourceSlice}, null, null);
        cl_program cpProgramConvert = clCreateProgramWithSource(context, 1,
                new String[]{kernelSourceConvert}, null, null);
        cl_program cpProgramSliceFromMask = clCreateProgramWithSource(context, 1,
                new String[]{kernelSourceSliceFromMask}, null, null);

        // Build the program
        clBuildProgram(cpProgramSlice, 0, null, "-cl-mad-enable", null, null);
        clBuildProgram(cpProgramConvert, 0, null, "-cl-mad-enable", null, null);
        clBuildProgram(cpProgramSliceFromMask, 0, null, "-cl-mad-enable", null, null);

        // Create the kernelSlice
        kernelSlice = clCreateKernel(cpProgramSlice, "Slicer", null);
        kernelConvert = clCreateKernel(cpProgramConvert, "SixteenToEightBit", null);
        kernelSliceFromMask = clCreateKernel(cpProgramSliceFromMask, "SliceFromMask", null);


        // Create the memory object which will be filled with the
        // pixel data
        pixelMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                sizeX * sizeY*sizeZ * Sizeof.cl_short, null, null);

        outputImgCL = clCreateBuffer(context, CL_MEM_READ_WRITE, planeSizeX * planeSizeY * Sizeof.cl_char, null,
                null);

        interm = clCreateBuffer(context, CL_MEM_READ_WRITE, planeSizeX * planeSizeY * Sizeof.cl_short, null,
                null);

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

        Plane sth = Plane.XZ;
        String s = sth.toString();
        System.out.println(s);
    }

}

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

    private String kernelSourceSlice;
    private String kernelSourceConvert;
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_kernel kernelSlice;
    private cl_kernel kernelConvert;
    private cl_mem pixelMem;
    private cl_mem interm;
    private cl_mem outputImgCL;


    public Slicer(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> inputImg,
                  DirectAccessImageByteGray img, Plane pPlane) {
        this.image = img;
        this.sizeX = (int) inputImg.dimension(0);
        this.sizeY = (int) inputImg.dimension(1);
        this.sizeZ = (int) inputImg.dimension(2);
        this.planeSizeX = (int) inputImg.dimension(0);
        this.planeSizeY = (int) inputImg.dimension(1);
        this.mPlane = pPlane;

        boolean compat = false;

        int[] dims = selectedPlaneSize(mPlane);
        compat = (dims[0] == planeSizeX & dims[1] == planeSizeY);

        if (!compat) {
            throw new IllegalArgumentException(String.format("Wrong output img parameters: %s slice requires %dx%d, " +
                    "provided: %dx%d", mPlane, dims[0], dims[1], planeSizeX, planeSizeY));
        }

//        System.out.println(String.format("img size: %dx%d", sizeX, sizeY));

        this.srcImg = inputImg;
        this.trgImg = trgImg;
        this.kernelSourceSlice = readFile("resources/kernels/Slicer.cl");
        this.kernelSourceConvert = readFile("resources/kernels/SixteenToEightBit.cl");

        initCL();
    }

    private int[] selectedPlaneSize(Plane pPlane) {
        int[] res = new int[2];
        switch (mPlane) {
            case XY:
                res[0] = sizeX;
                res[1] = sizeY;
                break;
            case XZ:
                res[0] = sizeX;
                res[1] = sizeZ;
                break;
            case YZ:
                res[0] = sizeY;
                res[1] = sizeZ;
        }
        return res;
    }

    public void getSlice(int slicePos) {

        System.out.println("slicer pos: " + slicePos);
        System.out.println("slicer dim: " + mPlane.getSlideDim());

        long workSizeSlice[] = new long[3];
        workSizeSlice[0] = sizeX;
        workSizeSlice[1] = sizeY;
        workSizeSlice[2] = sizeZ;
        System.out.println(String.format("3d stack %dx%dx%d", sizeX, sizeY, sizeZ));

        long workSizeConvert[] = new long[2];
        workSizeConvert[0] = planeSizeX;
        workSizeConvert[1] = planeSizeY;
        System.out.println(String.format("2d slice: %dx%d",planeSizeX, planeSizeY));


        clEnqueueWriteBuffer(commandQueue, pixelMem, true, 0,
                Sizeof.cl_short * sizeY * sizeX*sizeZ, Pointer.to(srcImg.getContiguousMemory().getByteBuffer()), 0,
                null, null);

        clSetKernelArg(kernelSlice, 0, Sizeof.cl_mem, Pointer.to(pixelMem));
        clSetKernelArg(kernelSlice, 1, Sizeof.cl_mem, Pointer.to(interm));
        clSetKernelArg(kernelSlice, 2, Sizeof.cl_int, Pointer.to(new int[]{sizeX}));
        clSetKernelArg(kernelSlice, 3, Sizeof.cl_int, Pointer.to(new int[]{sizeY}));
        clSetKernelArg(kernelSlice, 4, Sizeof.cl_int, Pointer.to(new int[]{sizeZ}));
        clSetKernelArg(kernelSlice, 5, Sizeof.cl_int, Pointer.to(new int[]{mPlane.getSlideDim()}));
        clSetKernelArg(kernelSlice, 6, Sizeof.cl_int, Pointer.to(new int[]{slicePos}));

        clSetKernelArg(kernelConvert, 0, Sizeof.cl_mem, Pointer.to(interm));
        clSetKernelArg(kernelConvert, 1, Sizeof.cl_mem, Pointer.to(outputImgCL));
        clSetKernelArg(kernelConvert, 2, Sizeof.cl_int, Pointer.to(new int[] {planeSizeX}));
        clSetKernelArg(kernelConvert, 3, Sizeof.cl_int, Pointer.to(new int[] {planeSizeY}));


        clEnqueueNDRangeKernel(commandQueue, kernelSlice, 3, null,
                workSizeSlice, null, 0, null, null);

        clEnqueueNDRangeKernel(commandQueue, kernelConvert, 2, null,
                workSizeConvert, null, 0, null, null);


        clEnqueueReadBuffer(commandQueue, outputImgCL, CL_TRUE, 0,
                Sizeof.cl_char * planeSizeX * planeSizeY, Pointer.to(this.image.getBuffer()), 0, null,
                null);

        ShortBuffer sh = ShortBuffer.allocate(planeSizeX*planeSizeY);
        clEnqueueReadBuffer(commandQueue, interm, CL_TRUE, 0,
                Sizeof.cl_short * planeSizeX * planeSizeY, Pointer.to(sh), 0, null,
                null);

        System.out.println("what we've got:");
        for (int i = 0; i <500; i++) {
            System.out.println(sh.get(i));
        }

        this.image.getBuffer().rewind();
        try {
            this.image.update();
            System.out.println("upd image");
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

        // Build the program
        clBuildProgram(cpProgramSlice, 0, null, "-cl-mad-enable", null, null);
        clBuildProgram(cpProgramConvert, 0, null, "-cl-mad-enable", null, null);

        // Create the kernelSlice
        kernelSlice = clCreateKernel(cpProgramSlice, "Slicer", null);
        kernelConvert = clCreateKernel(cpProgramConvert, "SixteenToEightBit", null);


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

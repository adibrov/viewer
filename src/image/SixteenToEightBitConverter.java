package image;

import coremem.offheap.OffHeapMemory;
import coremem.offheap.OffHeapMemoryAccess;
import net.imglib2.img.basictypeaccess.offheap.ByteOffHeapAccess;
import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.img.planar.OffHeapPlanarImg;
import net.imglib2.img.planar.OffHeapPlanarImgFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.jocl.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ShortBuffer;

import static net.imglib2.img.utils.Convert.load16bitPngToByteBuffer;
import static net.imglib2.img.utils.Convert.load8bitPngToByteBuffer;
import static org.jocl.CL.*;

/**
 * Created by dibrov on 20/04/17.
 */
public class SixteenToEightBitConverter {


    OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> srcImg;
    OffHeapPlanarImg<UnsignedByteType, ByteOffHeapAccess> trgImg;
    private int sizeX;
    private int sizeY;

    private String kernelSource;
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_kernel kernel;
    private cl_mem pixelMem;
    private cl_mem outputImgCL;


    public SixteenToEightBitConverter(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> inputImg, OffHeapPlanarImg<UnsignedByteType, ByteOffHeapAccess> trgImg) {
        this.sizeX = (int) inputImg.dimension(0);
        this.sizeY = (int) inputImg.dimension(1);
        int sizeZ = (int) inputImg.dimension(2);
        System.out.println(String.format("img size: %dx%d", sizeX, sizeY));

        if (sizeZ != 1) {
            throw new IllegalArgumentException(String.format("sizeZ of the input image is supposed to be 1, found: %d",
                    sizeZ));
        }
        this.srcImg = inputImg;
        this.trgImg = trgImg;
        this.kernelSource = readFile("resources/kernels/SixteenToEightBit.cl");

//        OffHeapMemory outPutImageMemory = new OffHeapMemory("mem", this, OffHeapMemoryAccess.allocateMemory(
//                (2 * sizeX*sizeY)), 2*sizeX*sizeY);
//
//        trgImg = (OffHeapPlanarImg<UnsignedByteType,
//                ByteOffHeapAccess>) new OffHeapPlanarImgFactory()
//                .createShortInstance(outPutImageMemory, new long[] {sizeX, sizeY, sizeZ}, new UnsignedShortType());


        initCL();
    }

    public void convert() {
        long globalWorkSize[] = new long[2];
        globalWorkSize[0] = sizeX;
        globalWorkSize[1] = sizeY;

        short[] arrShort = new short[sizeX*sizeY];
        srcImg.getContiguousMemory().copyTo(arrShort);

        clEnqueueWriteBuffer(commandQueue, pixelMem, true, 0,
                Sizeof.cl_char * sizeY * sizeX, Pointer.to(srcImg.getContiguousMemory().getByteBuffer()), 0,
                null, null);


        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(pixelMem));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputImgCL));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{sizeX}));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{sizeY}));

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, null, 0, null, null);

        byte[] arrByte = new byte[sizeY*sizeX];
        clEnqueueReadBuffer(commandQueue, outputImgCL, CL_TRUE, 0,
                Sizeof.cl_char * sizeY * sizeX, Pointer.to(trgImg.getContiguousMemory().getByteBuffer()), 0, null,
                null);
//        trgImg.getContiguousMemory().copyFrom(arrByte);

//        return trgImg;
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
        cl_program cpProgram = clCreateProgramWithSource(context, 1,
                new String[]{kernelSource}, null, null);

        // Build the program
        clBuildProgram(cpProgram, 0, null, "-cl-mad-enable", null, null);

        // Create the kernel
        kernel = clCreateKernel(cpProgram, "SixteenToEightBit", null);

        // Create the memory object which will be filled with the
        // pixel data
        pixelMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                sizeX * sizeY * Sizeof.cl_char, null, null);

        this.outputImgCL = clCreateBuffer(context, CL_MEM_READ_WRITE, sizeX * sizeY * Sizeof.cl_char, null, null);

    }

    /**
     * Helper function which reads the file with the given name and returns
     * the contents of this file as a String. Will exit the application
     * if the file can not be read.
     *
     * @param fileName The name of the file to read.
     * @return The contents of the file
     */
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


        byte[] arrb = new byte[432*428];
        short[] arrs = load16bitPngToByteBuffer("resources/img/wing16bit_crop.png");

        for (int i = 0; i < 10; i++) {
            System.out.println(arrb[i]);
        }
        System.out.println("");
        for (int i = 0; i < 10; i++) {
            System.out.println(arrs[i]);
        }


        OffHeapMemory cmIn = null;
        OffHeapMemory cmOut = null;
        cmIn = new OffHeapMemory("mem", cmIn, OffHeapMemoryAccess.allocateMemory(
                (2 * arrs.length)), 2 * arrs.length);
        cmOut = new OffHeapMemory("mem", cmOut, OffHeapMemoryAccess.allocateMemory(
                (arrs.length)), arrs.length);
        cmIn.copyFrom(arrs);

        OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> imgIn = (OffHeapPlanarImg<UnsignedShortType,
                ShortOffHeapAccess>) new OffHeapPlanarImgFactory().createShortInstance(cmIn, new long[]{432, 428, 1}, new UnsignedShortType());

        OffHeapPlanarImg<UnsignedByteType, ByteOffHeapAccess> imgOut = (OffHeapPlanarImg<UnsignedByteType,
                ByteOffHeapAccess>) new OffHeapPlanarImgFactory().createByteInstance(cmOut, new long[]{432, 428, 1}, new
                UnsignedByteType());
        System.out.println("size of the output byte image: " + imgOut.getContiguousMemory().getSizeInBytes());

        SixteenToEightBitConverter converter = new SixteenToEightBitConverter(imgIn, imgOut);
        converter.convert();

        cmOut.copyTo(arrb);



        imgIn.getContiguousMemory().copyTo(arrs);
        imgIn.getContiguousMemory().getByteBuffer().rewind();
        for (int i = 0; i < 10; i++) {
//            System.out.println(imgIn.getContiguousMemory().getByteBuffer().get());
            System.out.println(imgOut.getContiguousMemory().getByteBuffer().get(i));
        }

        System.out.println("");
        for (int i = 0; i < 10; i++) {
            System.out.println(arrs[i]);
        }

        System.out.println("done");


    }

}

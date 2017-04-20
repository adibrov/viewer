package utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * Created by dibrov on 20/04/17.
 */
public class Loaders {
    public static short[] loadUnsignedShortSampleSpaceFromDisk(String pPath, int pDimX, int pDimY, int pDimZ) {
        System.out.println("Loading a SampleSpace from file " + pPath);
        byte[] arr = new byte[2*pDimX * pDimY * pDimZ];
        short[] arrInt = new short[pDimX*pDimY*pDimZ];
        int mask1 = 0B1111111100000000;
        int mask2 = 0B0000000011111111;

        long h = 0;
        try (FileInputStream fis = new FileInputStream(pPath); BufferedInputStream bis = new BufferedInputStream(fis)) {
            long t1 = System.nanoTime();
            bis.read(arr);

            for (int i = 0; i < pDimX*pDimY*pDimZ; i++) {
                int b1 = arr[2*i];
                int b2 = arr[2*i+1];
//                System.out.println("bytes b1 and b2: " + b1 + " " + b2);
//                System.out.println("bytes b1 and b2: " + ((b1<<8)&mask1) + " " + (b2&mask2));
//                System.out.println("bytes b1 and b2 sum: " + (((b1<<8)&mask1)  + (b2&mask2)));
                arrInt[i] = (short)(((b1<<8)&mask1) + (b2&mask2));
//                System.out.println("just loaded: " + h);
            }
            long t2 = System.nanoTime();
            System.out.println("---Buffering: yes. Loaded file " + pPath + " in: " + ((t2 - t1) / 1000000.) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            System.out.println("Success.");
            System.out.println();
        }
        return arrInt;
    }
}

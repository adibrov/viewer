package image;

import com.sun.prism.Image;
import javafx.beans.NamedArg;
import javafx.scene.image.WritableImage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Created by dibrov on 19/04/17.
 */
public class DirectAccessImageByteGray extends WritableImage{

    private ByteBuffer buffer;
    private int width;
    private int height;
    private Image prismImg;
    private  boolean logMsgOn;

    public DirectAccessImageByteGray(@NamedArg("width") int width, @NamedArg("height") int height) {
        super(width, height);

        this.width = (int)getWidth();
        this.height = (int)getHeight();
        this.logMsgOn = false;
        try {
            Method init = this.getClass().getSuperclass().getSuperclass().getDeclaredMethod("initialize", Object.class);
            init.setAccessible(true);
            this.buffer = ByteBuffer.allocate(width*height);
            prismImg = com.sun.prism.Image.fromByteGrayData(this.buffer,width,height);
            init.invoke(this,prismImg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ByteBuffer getBuffer() {
        buffer.rewind();
        return buffer;
    }

    public void setLogMsgOn(boolean logMsgOn) {
        this.logMsgOn = logMsgOn;
    }

    public void setBuffer(ByteBuffer extBuffer) throws Exception {
        if (buffer.array().length != extBuffer.array().length) {
            throw new Exception(String.format("Buffer sizes don't match. Native buffer size: %d, provided buffer size: %d",
                    buffer.array().length, extBuffer.array().length));
        }
        extBuffer.rewind();
        this.buffer = extBuffer;
        Field pixelBufferField = prismImg.getClass().getDeclaredField("pixelBuffer");
        pixelBufferField.setAccessible(true);
        pixelBufferField.set(prismImg, this.buffer);
        update();
    }

    public void copyBuffer(ByteBuffer extBuffer) throws Exception {
        if (buffer.array().length != extBuffer.array().length) {
            throw new Exception(String.format("Buffer sizes don't match. Native buffer size: %d, provided buffer size: %d",
                    buffer.array().length, extBuffer.array().length));
        }

        buffer.rewind();
        extBuffer.rewind();
        buffer.put(extBuffer);
        extBuffer.rewind();
        update();
    }

    public void update() throws Exception {
        Field serial = prismImg.getClass().getDeclaredField("serial");
        serial.setAccessible(true);
        int[] h = (int[])serial.get(prismImg);
        h[0]++;
        serial.set(prismImg,h);

        Method mm = this.getClass().getSuperclass().getSuperclass().getDeclaredMethod("pixelsDirty", null);
        mm.setAccessible(true);
//        this.getClass().superc
        mm.invoke(this, null);

        if (logMsgOn) {
            System.out.println("updated");
        }
        this.buffer.rewind();

    }

    public void setBufferUniform(byte value) {
        ByteBuffer buffToWrite = ByteBuffer.allocate((int)(getWidth()*getHeight()));
        for (int i = 0; i < buffToWrite.array().length; i++) {
            buffToWrite.put(value);
        }
        try {
            copyBuffer(buffToWrite);
            update();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        int w = 500;
        int h = 500;
        DirectAccessImageByteGray testImg = new DirectAccessImageByteGray(w,h);

        // let's see what's in the native buffer...
        ByteBuffer nativeBuffer = testImg.getBuffer();
        System.out.println("Old buffer content:");
        for (int i = 0; i< 10; i++) {
            System.out.println(String.format("nativeBuff[%d] = %d",i,nativeBuffer.array()[i]));
        }

        System.out.println();

        // let's change the buffer...
        ByteBuffer b = ByteBuffer.allocate(w*h);
        for (int i = 0; i<b.array().length; i++) {
            b.put((byte)10);
        }
        try {
            testImg.copyBuffer(b);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("New buffer content:");
        // let's see what's in the new buffer
        for (int i = 0; i< 10; i++) {
            System.out.println(String.format("nativeBuff[%d] = %d",i,nativeBuffer.array()[i]));
        }
    }
}

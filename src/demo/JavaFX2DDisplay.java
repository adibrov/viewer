package demo;


import coremem.offheap.OffHeapMemory;
import coremem.offheap.OffHeapMemoryAccess;
import image.DirectAccessImageByteGray;
import image.Listener;
import image.Plane;
import image.Slicer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.img.planar.OffHeapPlanarImg;
import net.imglib2.img.planar.OffHeapPlanarImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import static clearcontrol.simulation.loaders.SampleSpaceSaveAndLoad.loadUnsignedShortSampleSpaceFromDisk;
import static net.imglib2.img.utils.Convert.convertTIFFToRaw;

/**
 * Created by dibrov on 12/04/17.
 */
public class JavaFX2DDisplay extends Application {
    private ImageView imageView_Source;
    private DirectAccessImageByteGray imgIn;
    private int currentSlice;
    private LinkedList<Listener> listenerList;


    public JavaFX2DDisplay(DirectAccessImageByteGray img) {
        this.imgIn = img;
        this.currentSlice = 0;
        listenerList = new LinkedList<>();
    }

    @Override
    public void start(Stage primaryStage) {


        imageView_Source = new ImageView();
        imageView_Source.setFitWidth(500);
        imageView_Source.setFitHeight(500);
        imageView_Source.setImage(imgIn);


        HBox hBoxImage = new HBox();
        //e hBoxImage.getChildren().addAll(imageView_Source);

        Slider slider = new Slider(0, 65, 0);
        slider.setBlockIncrement(1);
        slider.valueProperty().addListener((ob, o, n) -> {
            currentSlice = n.intValue();
            System.out.println("slide!");
            notifyListeners();
        });

        hBoxImage.getChildren().addAll(imageView_Source, slider);

        StackPane root = new StackPane();
        root.getChildren().add(hBoxImage);
        Scene scene = new Scene(root, 1400, 1000);
        primaryStage.setTitle("Current slice");
        primaryStage.setScene(scene);
        primaryStage.show();


    }

    private void notifyListeners() {
        for (Listener listener : listenerList) {
            listener.fire();
        }
    }

    public void addListener(Listener listener) {
        listenerList.add(listener);
    }

    public void setImgIn(DirectAccessImageByteGray imgIn) {

        this.imgIn = imgIn;
        imageView_Source.setImage(this.imgIn);
    }

    public void updateImgBuffer(ByteBuffer buffer) throws Exception {
        imgIn.setBuffer(buffer);
    }

    public void run(String[] args) {
        launch(args);
    }

    public static void main(String[] args) {

//        convertTIFFToRaw("resources/img/smallWingStack.tif", "resources/img/smallWingStack");
        String path = "resources/img/smallWingStack692x520x50.raw";
        int[] dims = {692, 520, 50};
        long[] dimsLong = {692, 520, 50};
        short[] rawArr = new short[dims[0]*dims[1]*dims[2]];
        rawArr = loadUnsignedShortSampleSpaceFromDisk(path, dims[0], dims[1], dims[2]);

        System.out.println("length:" + rawArr.length);

        for (int i = 0; i < 100; i++) {
            System.out.println(rawArr[i]);
        }

        Object h = new Object();
        OffHeapMemory cmIn = null;
        OffHeapMemory cmOut = null;
        cmIn = new OffHeapMemory("mem", h, OffHeapMemoryAccess.allocateMemory((2 * rawArr.length)), 2 * rawArr.length);
        cmOut = new OffHeapMemory("mem", cmOut, OffHeapMemoryAccess.allocateMemory((rawArr.length)), rawArr.length);
        cmIn.copyFrom(rawArr);



        OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> imgIn = (OffHeapPlanarImg<UnsignedShortType,
                ShortOffHeapAccess>) new OffHeapPlanarImgFactory().createShortInstance(cmIn, dimsLong, new
                UnsignedShortType
                ());



//        System.out.println("image:");
//        for (int i = 0; i < 100; i++) {
//            System.out.println(cmIn.getByteBuffer().get(i));
//        }


        DirectAccessImageByteGray img = new DirectAccessImageByteGray(dims[0], dims[1]);

        Slicer slicer = new Slicer(imgIn, img, Plane.XY);

        JavaFX2DDisplay display = new JavaFX2DDisplay(img);
        display.addListener(() -> {
            slicer.getSlice(display.currentSlice);
        });

        new JFXPanel();
        Platform.runLater(() -> {
            Stage s = new Stage();
            display.start(s);
        });


    }

}

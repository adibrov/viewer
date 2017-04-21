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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.img.planar.OffHeapPlanarImg;
import net.imglib2.img.planar.OffHeapPlanarImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import static clearcontrol.simulation.loaders.SampleSpaceSaveAndLoad.loadUnsignedShortSampleSpaceFromDisk;

/**
 * Created by dibrov on 12/04/17.
 */

public class JavaFX2DDisplay extends Application {

    private ImageView imageView_Source;
    private OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> imgIn;
    private DirectAccessImageByteGray imgToDisplay;
    private int currentSlice;
    private LinkedList<Listener> listenerList;
    Slicer slicer;
    private Plane mPlane;

    public JavaFX2DDisplay(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> img, Plane pPlane) {
        this.imgIn = img;
        this.currentSlice = 0;
        this.mPlane = pPlane;
        int[] activeDims = mPlane.getActiveDims();
        this.imgToDisplay = new DirectAccessImageByteGray((int)img.dimension(activeDims[0]), (int)img.dimension
                (activeDims[1]));
        this.slicer = new Slicer(imgIn, imgToDisplay, Plane.XY);
        slicer.getSlice(currentSlice);


        listenerList = new LinkedList<>();
    }

    @Override
    public void start(Stage primaryStage) {


        imageView_Source = new ImageView();
        imageView_Source.setImage(imgToDisplay);

        VBox imageBox = new VBox(20);
        imageBox.setPadding(new Insets(20,20,20,20));
        imageBox.setMaxWidth(primaryStage.getMaxWidth());
        imageBox.setAlignment(Pos.TOP_CENTER);


        imageView_Source.fitWidthProperty().bind(primaryStage.widthProperty().multiply(0.85));
        imageView_Source.fitHeightProperty().bind(imageView_Source.fitWidthProperty());
        VBox vBoxSliders = new VBox(15);
        vBoxSliders.setMinHeight(100);
        vBoxSliders.setPadding(new Insets(20,20,20, 20));
        vBoxSliders.setAlignment(Pos.BOTTOM_CENTER);

        Slider sliderZ = new Slider(0, imgIn.dimension(mPlane.getSlideDim()), 0);
        sliderZ.setBlockIncrement(1);
        sliderZ.valueProperty().addListener((ob, o, n) -> {
            currentSlice = n.intValue();
            slicer.getSlice(currentSlice);
        });
        Slider sliderMin = new Slider(0,511,0);
        sliderMin.setBlockIncrement(1);
        sliderMin.valueProperty().addListener((ob,o,n)->{
            slicer.setMin(n.shortValue());
            slicer.convert();
        });

        Slider sliderMax = new Slider(0,511,511);
        sliderMax.setBlockIncrement(1);
        sliderMax.valueProperty().addListener((ob,o,n)->{
            slicer.setMax(n.shortValue());
            slicer.convert();
        });

        vBoxSliders.getChildren().addAll(sliderZ, sliderMin, sliderMax);
        imageBox.getChildren().addAll(imageView_Source,vBoxSliders);



        StackPane root = new StackPane();
        root.getChildren().add(imageBox);
        Scene scene = new Scene(root, 500, 600);



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

    public void run(String[] args) {
        launch(args);
    }

    public static void main(String[] args) {

//        convertTIFFToRaw("resources/img/smallWingStack.tif", "resources/img/smallWingStack");
        String path = "resources/img/smallWingStack692x520x50.raw";
        int[] dims = {692, 520, 50};
        long[] dimsLong = {692, 520, 50};
        short[] rawArr = new short[dims[0] * dims[1] * dims[2]];
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



        JavaFX2DDisplay display = new JavaFX2DDisplay(imgIn, Plane.XY);
//        display.addListener(() -> {
//            slicer.getSlice(display.currentSlice);
//        });

        new JFXPanel();
        Platform.runLater(() -> {
            Stage s = new Stage();
            display.start(s);
        });


    }

}

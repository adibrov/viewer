package demo;


import coremem.offheap.OffHeapMemory;
import coremem.offheap.OffHeapMemoryAccess;
import image.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.img.planar.OffHeapPlanarImg;
import net.imglib2.img.planar.OffHeapPlanarImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

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
    private Rectangle rect;
    private Color full = new Color(0,1,0,.5);
    private Color empty = new Color(0,0,0, 0);
    private ArrayList<Pane> mPaneList;
    private SurfexFirst sf;
    private GridOverlay grov;
    private int splitX;
    private int splitY;

    public JavaFX2DDisplay(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> img, Plane pPlane) {
        this.imgIn = img;
        this.mPaneList = new ArrayList<>();
        this.currentSlice = 0;
        this.mPlane = pPlane;
        int[] activeDims = mPlane.getActiveDims();
        this.imgToDisplay = new DirectAccessImageByteGray((int) img.dimension(activeDims[0]), (int) img.dimension
                (activeDims[1]));
        System.out.println("image to display dims: " + imgToDisplay.getWidth() + " " + imgToDisplay.getHeight());
        this.slicer = new Slicer(imgIn, imgToDisplay, mPlane);

        this.splitX = 10;
        this.splitY = 10;

        grov = new GridOverlay((int)imgToDisplay.getWidth(), (int)imgToDisplay.getHeight(), (int)imgIn
                .dimension(mPlane
                        .getSlideDim()), splitX,splitY);

//        grov.initializeGrid();
        slicer.getSlice(currentSlice);


        listenerList = new LinkedList<>();
        sf =  new SurfexFirst(img, grov.getTileOriX(), grov.getTileOriY());
    }

    @Override
    public void start(Stage primaryStage) {


        imageView_Source = new ImageView();
        imageView_Source.setImage(imgToDisplay);

        rect = new Rectangle();
//        rect.setX(10);
//        rect.setY(10);
        rect.setFill(new Color(0,1,0,0.2));

        Rectangle rect1 = new Rectangle();
        rect1.setFill(new Color(0,0,1,0.2));

        VBox imageBox = new VBox(20);
        imageBox.setPadding(new Insets(20, 20, 20, 20));
        imageBox.setMaxWidth(primaryStage.getMaxWidth());
        imageBox.setAlignment(Pos.TOP_CENTER);


        imageView_Source.fitWidthProperty().bind(primaryStage.widthProperty().subtract(20));
        imageView_Source.fitHeightProperty().bind(primaryStage.heightProperty().subtract(250));

//        imageView_Source.setX(150.0);
        System.out.println(imageView_Source.getX());
        imageView_Source.xProperty().addListener((ob, o, n)->{
            System.out.println(n.intValue());
        });
//        imageView_Source.fitHeightProperty().bind(imageView_Source.fitWidthProperty());
        VBox vBoxSliders = new VBox(15);
        vBoxSliders.setMinHeight(100);
        vBoxSliders.setPadding(new Insets(20, 20, 20, 20));
        vBoxSliders.setAlignment(Pos.BOTTOM_CENTER);
        vBoxSliders.getChildren().toArray();
//        vBoxSliders.setVisible(false);

       // Node cont = new
        Pane p1 = new Pane();
        Pane p2 = new Pane();

        Slider sliderZ = new Slider(0, imgIn.dimension(mPlane.getSlideDim())-1, 0);
        sliderZ.setBlockIncrement(1);



        int[] indices = new int[this.splitX*this.splitY];

        Random rand = new Random();
//        for (int i = 0; i < indices.length; i++) {
//            indices[i] = rand.nextInt((int)imgIn
//                    .dimension(mPlane
//                            .getSlideDim()));
//        }

        grov.initializeGrid(indices);
        mPaneList = grov.getPaneList();



        sliderZ.valueProperty().addListener((ob, o, n) -> {
            currentSlice = n.intValue();
            slicer.getSlice(currentSlice);
            mPaneList.get(o.intValue()).setVisible(false);
            mPaneList.get(n.intValue()).setVisible(true);


        });
        Slider sliderMin = new Slider(0, 511, 0);
        sliderMin.setBlockIncrement(1);
        sliderMin.valueProperty().addListener((ob, o, n) -> {
            slicer.setMin(n.shortValue());
            slicer.convert();
        });


        Slider sliderMax = new Slider(0, 511, 511);
        sliderMax.setBlockIncrement(1);
        sliderMax.valueProperty().addListener((ob, o, n) -> {
            slicer.setMax(n.shortValue());
            slicer.convert();
        });


        HBox hboxBig = new HBox();


//        grid.getColumnConstraints().addAll(cc);
        Button computeTilesButton = new Button("Compute Tiles");

        computeTilesButton.setOnAction((e)->{
            grov.initializeGrid(sf.computeTiles());
            mPaneList = grov.getPaneList();
        });
        vBoxSliders.getChildren().addAll(sliderZ, sliderMin, sliderMax, computeTilesButton);
        StackPane imgAndOverlay = new StackPane();
        StackPane grovPane = grov.getMainStackPane();

        grovPane.prefWidthProperty().bind(primaryStage.widthProperty().subtract(20));
        grovPane.prefHeightProperty().bind(primaryStage.heightProperty().subtract(250));

        imgAndOverlay.getChildren().addAll(imageView_Source, grovPane);
        imageBox.getChildren().addAll(imgAndOverlay, vBoxSliders);
//        hboxBig.getChildren().addAll(imageBox, computeTilesButton);
//        hboxBig.setPadding(new Insets(120,20,20,20));
//
//        hboxBig.setMaxWidth(primaryStage.getMaxWidth());
//        hboxBig.setAlignment(Pos.TOP_CENTER);
//        imgAndOverlay.prefWidthProperty().bind(primaryStage.widthProperty().subtract(20));
//        imgAndOverlay.prefHeightProperty().bind(primaryStage.heightProperty().subtract(150));
//        imgAndOverlay.setPadding(new Insets(50,50,50,50));

//        p1.prefWidthProperty().bind(primaryStage.widthProperty().subtract(20));
//        p1.prefHeightProperty().bind(primaryStage.heightProperty().subtract(150));

        StackPane root = new StackPane();





        root.getChildren().addAll(imageBox);
        Scene scene = new Scene(root, 500, imgToDisplay.getHeight() + 150);
//
//        int[] sth = sf.computeTiles();
//        System.out.println("compute tiles returned an array of size: " + sth.length);
//        for (int i = 0; i < 100; i++) {
//            System.out.println(sth[i]);
//        }


        primaryStage.setTitle("Current slice");
        primaryStage.setScene(scene);
        primaryStage.show();


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

        Object h = new Object();
        OffHeapMemory cmIn = null;
        OffHeapMemory cmOut = null;
        cmIn = new OffHeapMemory("mem", h, OffHeapMemoryAccess.allocateMemory((2 * rawArr.length)), 2 * rawArr.length);
        cmIn.copyFrom(rawArr);


        OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> imgIn = (OffHeapPlanarImg<UnsignedShortType,
                ShortOffHeapAccess>) new OffHeapPlanarImgFactory().createShortInstance(cmIn, dimsLong, new
                UnsignedShortType
                ());

        JavaFX2DDisplay display = new JavaFX2DDisplay(imgIn, Plane.XY);

        new JFXPanel();
        Platform.runLater(() -> {
            Stage s = new Stage();
            display.start(s);
        });
    }
}

package demo;


import coremem.offheap.OffHeapMemory;
import coremem.offheap.OffHeapMemoryAccess;
import image.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.imglib2.img.basictypeaccess.offheap.ShortOffHeapAccess;
import net.imglib2.img.planar.OffHeapPlanarImg;
import net.imglib2.img.planar.OffHeapPlanarImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.miginfocom.layout.Grid;

import java.util.ArrayList;

import static clearcontrol.simulation.loaders.SampleSpaceSaveAndLoad.loadUnsignedShortSampleSpaceFromDisk;

/**
 * Created by dibrov on 12/04/17.
 */

public class JavaFX2DDisplay extends Application {

    // Image Views
    private ImageView imageViewSource;
    private ImageView imageViewMaxProjection;

    // Input 16 bit stack from the scope
    private OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> imgIn;

    // Modified 8bit Images datastructure to display in JavaFX
    private DirectAccessImageByteGray imageSource;
    private DirectAccessImageByteGray imageMaxProjection;

    // Grid Overlays to visualise the surface detection process
    private GridOverlay gridOverlayIntensities;
    private GridOverlay gridOverlayVariance;
    private GridOverlay gridOverlayMedian;

    // OpenCL based modules for image processing
    private Slicer slicer;
    private SurfexFirst sf;

    // Displayed Panes from Overlays
    private ArrayList<Pane> mPaneListIntensities;
    private ArrayList<Pane> mPaneListVariance;
    private ArrayList<Pane> mPaneListMedian;

    // Other
    private int currentSlice;
    private Plane mPlane;
    private int splitX;
    private int splitY;

    public JavaFX2DDisplay(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> img, Plane pPlane) {
        this.imgIn = img;
        this.mPaneListIntensities = new ArrayList<>();
        this.mPaneListVariance = new ArrayList<>();
        this.mPaneListMedian = new ArrayList<>();
        this.currentSlice = 0;
        this.mPlane = pPlane;
        int[] activeDims = mPlane.getActiveDims();

        // Images
        this.imageSource = new DirectAccessImageByteGray((int) img.dimension(activeDims[0]), (int) img.dimension
                (activeDims[1]));
        this.imageMaxProjection = new DirectAccessImageByteGray((int) img.dimension(activeDims[0]), (int) img.dimension
                (activeDims[1]));

        System.out.println("image to display dims: " + imageSource.getWidth() + " " + imageSource.getHeight());
        this.slicer = new Slicer(imgIn, imageSource, mPlane);

        this.splitX = 40;
        this.splitY = 20;

        // Initialising overlays
        gridOverlayIntensities = GridOverlay.getGridOverlayRandomInit((int) imageSource.getWidth(), (int) imageSource.getHeight(), (int) imgIn
                .dimension(mPlane
                        .getSlideDim()), splitX, splitY, new Color(0, 1, 0, 0.1));
//        gridOverlayVariance = GridOverlay.getGridOverlayPredefinedInit((int) imageSource.getWidth(), (int) imageSource.getHeight()
//                , (int) imgIn
//                .dimension(mPlane.getSlideDim()), splitX, splitY, new Color(0, 0, 1, .1), gridOverlayIntensities
//                        .getXsizes(), gridOverlayIntensities.getYsizes());
        gridOverlayVariance = GridOverlay.getGridOverlaySubsplit((int) imageSource.getWidth(), (int) imageSource.getHeight()
                , (int) imgIn
                        .dimension(mPlane.getSlideDim()), splitX, splitY, new Color(0, 0, 1, .1), gridOverlayIntensities
                        .getXsizes(), gridOverlayIntensities.getYsizes(), 2, 2);
        gridOverlayMedian = GridOverlay.getGridOverlayPredefinedInit((int) imageSource.getWidth(), (int) imageSource.getHeight()
                , (int) imgIn
                        .dimension(mPlane.getSlideDim()), splitX, splitY, new Color(1, 0, 0, .1),
                gridOverlayIntensities
                        .getXsizes(), gridOverlayIntensities.getYsizes());

        gridOverlayVariance.initializeGridWithZeros();
        gridOverlayIntensities.initializeGridWithZeros();
        gridOverlayMedian.initializeGridWithZeros();

        slicer.getSlice(currentSlice);


        sf = new SurfexFirst(img, gridOverlayIntensities.getTileOriX(), gridOverlayIntensities.getTileOriY());
    }

    @Override
    public void start(Stage primaryStage) {

        // Setting the image views
        imageViewSource = new ImageView();
        imageViewSource.setImage(imageSource);

        imageViewMaxProjection = new ImageView();
        imageViewMaxProjection.setImage(imageMaxProjection);

        // Setting sliders
        Slider sliderZ = new Slider(0, imgIn.dimension(mPlane.getSlideDim()) - 1, 0);
        sliderZ.setBlockIncrement(1);

        sliderZ.valueProperty().addListener((ob, o, n) -> {
            currentSlice = n.intValue();
            slicer.getSlice(currentSlice);
            mPaneListIntensities.get(o.intValue()).setVisible(false);
            mPaneListIntensities.get(n.intValue()).setVisible(true);
            mPaneListVariance.get(o.intValue()).setVisible(false);
            mPaneListVariance.get(n.intValue()).setVisible(true);
            mPaneListMedian.get(o.intValue()).setVisible(false);
            mPaneListMedian.get(n.intValue()).setVisible(true);
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





        // Setting a VBox for sliders
        VBox vBoxSlidersSource = new VBox(15);
        vBoxSlidersSource.setMinHeight(100);
        vBoxSlidersSource.setPadding(new Insets(20, 20, 20, 20));
        vBoxSlidersSource.setAlignment(Pos.BOTTOM_CENTER);
        vBoxSlidersSource.getChildren().addAll(sliderZ, sliderMin, sliderMax);




        // Initializing panes
        mPaneListIntensities = gridOverlayIntensities.getPaneList();
        mPaneListVariance = gridOverlayVariance.getPaneList();
        mPaneListMedian = gridOverlayMedian.getPaneList();


        // Menu buttons
        Button computeTilesButton = new Button("Compute Tiles");
        Button modifiedMedianButton = new Button("Modified Median Filter");
        Button refineWithVarianceButton = new Button("Refine With Variance");
        Button maxProjectBasicButton = new Button("Max Project Basic");
        Button maxProjectModMedButton = new Button("Max Project modified median");
        Button maxProjectVarianceButton = new Button("Max Project + Variance");


        computeTilesButton.setOnAction((e) -> {
            gridOverlayIntensities.initializeGridPane(sf.computeTiles());
//            gridOverlayVariance.initializeGridPane(sf.computeTiles());
            mPaneListIntensities = gridOverlayIntensities.getPaneList();
        });

        modifiedMedianButton.setOnAction((e)-> {
            gridOverlayMedian.initializeGridPane(sf.applyModifiedMedian());
            mPaneListMedian = gridOverlayMedian.getPaneList();
        });

        refineWithVarianceButton.setOnAction((e) -> {
            sf.updateVarState(gridOverlayVariance.getTileOriX(),gridOverlayVariance.getTileOriY());
            sf.scaleGrid();
            gridOverlayVariance.initializeGridPane(sf.refineWithVariance());
            mPaneListVariance = gridOverlayVariance.getPaneList();
        });

        maxProjectBasicButton.setOnAction((e)->{
            slicer.getSliceFromTileMask(sf.getTilesOriX(), sf.getTilesOriY(), sf.getTilesZCoordInt(), imageMaxProjection);
        });

        maxProjectModMedButton.setOnAction((e)->{
            slicer.getSliceFromTileMask(sf.getTilesOriX(), sf.getTilesOriY(), sf.getTilesZCoordMedian(),
                    imageMaxProjection);
        });

        maxProjectVarianceButton.setOnAction((e)-> {
            slicer.getSliceFromTileMask(gridOverlayVariance.getTileOriX(), gridOverlayVariance.getTileOriY(), sf.getTilesZCoordVar
                            (),
                    imageMaxProjection);
        });

        //Checkboxes
        CheckBox cbTilesInt = new CheckBox("Intensity overlay");
        cbTilesInt.selectedProperty().addListener((ob,o,n)->{
            gridOverlayIntensities.getMainStackPane().setVisible(n);
        });
        cbTilesInt.setSelected(true);

        CheckBox cbMedian = new CheckBox("Median overlay");
        cbMedian.selectedProperty().addListener((ob,o,n)->{
            gridOverlayMedian.getMainStackPane().setVisible(n);
        });
        cbMedian.setSelected(true);

        CheckBox cbVariance = new CheckBox("Variance overlay");
        cbVariance.selectedProperty().addListener((ob,o,n)->{
            gridOverlayVariance.getMainStackPane().setVisible(n);
        });
        cbVariance.setSelected(true);



        // Stackpane to host image and overlays aligned on top of each other
        StackPane imgAndOverlay = new StackPane();
        StackPane grovPaneInt = gridOverlayIntensities.getMainStackPane();
        StackPane grovPaneVar = gridOverlayVariance.getMainStackPane();
        StackPane grovPaneMed = gridOverlayMedian.getMainStackPane();
        imgAndOverlay.getChildren().addAll(imageViewSource, grovPaneInt, grovPaneVar, grovPaneMed);

        // a VBox to hold source and sliders for the source
        VBox sourceAndSlidersBox = new VBox(20);
        // sourceAndSlidersBox.setMinHeight(imageViewSource.getFitHeight() + vBoxSlidersSource.getMinHeight());
//        sourceAndSlidersBox.setPadding(new Insets(20, 20, 20, 20));
        sourceAndSlidersBox.setAlignment(Pos.TOP_CENTER);
        sourceAndSlidersBox.getChildren().addAll(imgAndOverlay, vBoxSlidersSource);


        // A VBox to hold the max projection image with sliders
        VBox maxprojAndSlidersBox = new VBox(20);
        maxprojAndSlidersBox.getChildren().addAll(imageViewMaxProjection);

        // Root HBox
        HBox root = new HBox();
        root.setSpacing(5);
        root.setPadding(new Insets(20, 20, 20, 20));

        // Menu - stuff on the left
        VBox menu = new VBox();
        menu.setAlignment(Pos.TOP_CENTER);
        menu.setSpacing(10);
        menu.getChildren().addAll(computeTilesButton, modifiedMedianButton, refineWithVarianceButton,
                maxProjectBasicButton, maxProjectModMedButton,
                maxProjectVarianceButton, cbTilesInt, cbMedian, cbVariance);


        // Binding width and height parameters
        imageViewSource.fitWidthProperty().bind(imgAndOverlay.prefWidthProperty());
        imageViewSource.fitHeightProperty().bind(imgAndOverlay.prefHeightProperty());

        imageViewMaxProjection.fitWidthProperty().bind(maxprojAndSlidersBox.prefWidthProperty());
        imageViewMaxProjection.fitHeightProperty().bind(maxprojAndSlidersBox.prefHeightProperty().multiply(0.7));

        grovPaneInt.prefWidthProperty().bind(imgAndOverlay.prefWidthProperty());
        grovPaneInt.prefHeightProperty().bind(imgAndOverlay.prefHeightProperty());
        grovPaneVar.prefWidthProperty().bind(imgAndOverlay.prefWidthProperty());
        grovPaneVar.prefHeightProperty().bind(imgAndOverlay.prefHeightProperty());


        imgAndOverlay.prefWidthProperty().bind(sourceAndSlidersBox.prefWidthProperty());
        imgAndOverlay.prefHeightProperty().bind(sourceAndSlidersBox.prefHeightProperty().multiply(0.7));

        vBoxSlidersSource.prefWidthProperty().bind(sourceAndSlidersBox.prefWidthProperty());
        vBoxSlidersSource.prefHeightProperty().bind(sourceAndSlidersBox.prefHeightProperty().multiply(0.3));

        sourceAndSlidersBox.prefWidthProperty().bind(root.prefWidthProperty().multiply(0.35));
        sourceAndSlidersBox.prefHeightProperty().bind(root.prefHeightProperty());
        menu.prefWidthProperty().bind(root.prefWidthProperty().multiply(0.25));
        maxprojAndSlidersBox.prefWidthProperty().bind(root.prefWidthProperty().multiply(0.35));
        maxprojAndSlidersBox.prefHeightProperty().bind(root.prefHeightProperty());

        root.getChildren().addAll(menu, sourceAndSlidersBox, maxprojAndSlidersBox);

        // Initialising scene
        Scene scene = new Scene(root, 1500, 800);
        root.prefWidthProperty().bind(scene.widthProperty());
        root.prefHeightProperty().bind(scene.heightProperty());

        primaryStage.setTitle("Surface extraction");
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

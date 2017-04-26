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

import java.util.ArrayList;

import static clearcontrol.simulation.loaders.SampleSpaceSaveAndLoad.loadUnsignedShortSampleSpaceFromDisk;

/**
 * Created by dibrov on 12/04/17.
 */

public class JavaFX2DDisplay extends Application {

    // Image Viewes
    private ImageView imageViewSource;
    private ImageView imageViewMaxProjection;

    // Input 16 bit stack from the scope
    private OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> imgIn;

    // Modified 8bit Image datastructure to display in JavaFX
    private DirectAccessImageByteGray imgToDisplay;

    // Grid Overlays to visualise the surface detection process
    private GridOverlay gridOverlayIntensities;
    private GridOverlay gridOverlayVariance;

    // OpenCL based modules for image processing
    private Slicer slicer;
    private SurfexFirst sf;

    // Displayed Panes from Overlays
    private ArrayList<Pane> mPaneListIntensities;
    private ArrayList<Pane> mPaneListVariance;

    // Other
    private int currentSlice;
    private Plane mPlane;
    private int splitX;
    private int splitY;

    public JavaFX2DDisplay(OffHeapPlanarImg<UnsignedShortType, ShortOffHeapAccess> img, Plane pPlane) {
        this.imgIn = img;
        this.mPaneListIntensities = new ArrayList<>();
        this.mPaneListVariance = new ArrayList<>();
        this.currentSlice = 0;
        this.mPlane = pPlane;
        int[] activeDims = mPlane.getActiveDims();
        this.imgToDisplay = new DirectAccessImageByteGray((int) img.dimension(activeDims[0]), (int) img.dimension
                (activeDims[1]));
        System.out.println("image to display dims: " + imgToDisplay.getWidth() + " " + imgToDisplay.getHeight());
        this.slicer = new Slicer(imgIn, imgToDisplay, mPlane);

        this.splitX = 10;
        this.splitY = 10;

        gridOverlayIntensities = new GridOverlay((int) imgToDisplay.getWidth(), (int) imgToDisplay.getHeight(), (int) imgIn
                .dimension(mPlane
                        .getSlideDim()), splitX, splitY, new Color(0, 1, 0, 0.1));
        gridOverlayVariance = new GridOverlay((int) imgToDisplay.getWidth(), (int) imgToDisplay.getHeight(), (int) imgIn
                .dimension(mPlane.getSlideDim()), splitX, splitY, new Color(0, 0, 1, .1));

        gridOverlayVariance.initializeGridWithZeros();
        gridOverlayIntensities.initializeGridWithZeros();

        slicer.getSlice(currentSlice);


        sf = new SurfexFirst(img, gridOverlayIntensities.getTileOriX(), gridOverlayIntensities.getTileOriY());
    }

    @Override
    public void start(Stage primaryStage) {

        // Setting the source image view

        imageViewSource = new ImageView();
        imageViewSource.setImage(imgToDisplay);

        // Setting a VBox for sliders
        VBox vBoxSlidersSource = new VBox(15);
        vBoxSlidersSource.setMinHeight(100);
        vBoxSlidersSource.setPadding(new Insets(20, 20, 20, 20));
        vBoxSlidersSource.setAlignment(Pos.BOTTOM_CENTER);

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


        // a VBox to hold source and sliders for the source
        VBox sourceAndSlidersBox = new VBox(20);
        // sourceAndSlidersBox.setMinHeight(imageViewSource.getFitHeight() + vBoxSlidersSource.getMinHeight());
        sourceAndSlidersBox.setPadding(new Insets(20, 20, 20, 20));
        sourceAndSlidersBox.setAlignment(Pos.TOP_CENTER);
        vBoxSlidersSource.getChildren().addAll(sliderZ, sliderMin, sliderMax);

        // Initializing panes
        mPaneListIntensities = gridOverlayIntensities.getPaneList();
        mPaneListVariance = gridOverlayVariance.getPaneList();


        // Menu buttons
        Button computeTilesButton = new Button("Compute Tiles");
        Button refineWithVarianceButton = new Button("Refine With Variance");

        computeTilesButton.setOnAction((e) -> {
            gridOverlayIntensities.initializeGrid(sf.computeTiles());
//            gridOverlayVariance.initializeGrid(sf.computeTiles());
            mPaneListIntensities = gridOverlayIntensities.getPaneList();
        });

        refineWithVarianceButton.setOnAction((e) -> {
//            gridOverlayVariance.initializeGrid(sf.computeTiles());
            gridOverlayVariance.initializeGrid(sf.refineWithVariance());
            mPaneListVariance = gridOverlayVariance.getPaneList();
        });


        // Stackpane to host image and overlays aligned on top of each other
        StackPane imgAndOverlay = new StackPane();
        StackPane grovPaneInt = gridOverlayIntensities.getMainStackPane();
        StackPane grovPaneVar = gridOverlayVariance.getMainStackPane();
        grovPaneInt.prefWidthProperty().bind(imgAndOverlay.prefWidthProperty());
        grovPaneInt.prefHeightProperty().bind(imgAndOverlay.prefHeightProperty());
        grovPaneVar.prefWidthProperty().bind(imgAndOverlay.prefWidthProperty());
        grovPaneVar.prefHeightProperty().bind(imgAndOverlay.prefHeightProperty());

        // Root HBox
        HBox root = new HBox();
        root.setSpacing(5);
        root.setPadding(new Insets(20, 20, 20, 20));

        // Menu - stuff on the left
        VBox menu = new VBox();
        menu.setAlignment(Pos.TOP_CENTER);
        menu.setSpacing(10);
        menu.getChildren().addAll(computeTilesButton, refineWithVarianceButton);


        // Binding width and height parameters
        imageViewSource.fitWidthProperty().bind(imgAndOverlay.prefWidthProperty());
        imageViewSource.fitHeightProperty().bind(imgAndOverlay.prefHeightProperty());

        imgAndOverlay.getChildren().addAll(imageViewSource, grovPaneInt, grovPaneVar);
        imgAndOverlay.prefWidthProperty().bind(sourceAndSlidersBox.prefWidthProperty());
        imgAndOverlay.prefHeightProperty().bind(sourceAndSlidersBox.prefHeightProperty().multiply(0.7));

        sourceAndSlidersBox.getChildren().addAll(imgAndOverlay, vBoxSlidersSource);

        vBoxSlidersSource.prefWidthProperty().bind(sourceAndSlidersBox.prefWidthProperty());
        vBoxSlidersSource.prefHeightProperty().bind(sourceAndSlidersBox.prefHeightProperty().multiply(0.3));

        sourceAndSlidersBox.prefWidthProperty().bind(root.prefWidthProperty().multiply(0.75));
        sourceAndSlidersBox.prefHeightProperty().bind(root.prefHeightProperty());
        menu.prefWidthProperty().bind(root.prefWidthProperty().multiply(0.25));

        root.getChildren().addAll(menu, sourceAndSlidersBox);

        // Initialising scene
        Scene scene = new Scene(root, 1000, 800);
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

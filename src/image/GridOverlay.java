package image;

import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by dibrov on 25/04/17.
 */
public class GridOverlay {
    private ArrayList<Rectangle> mRectList;
    private ArrayList<Pane> mPaneList;
    private int mImageSizeX;
    private int mImageSizeY;
    private int mImageSizeZ;
    private int mRectSizeX;
    private int mRectSizeY;
    private int mRectsX;
    private int mRectsY;
    private int[] xsizes;
    private int[] ysizes;
    private StackPane mMainStackPane;
    private Color color;
    private int[] mTilesOriX;
    private int[] mTilesOriY;


    public static GridOverlay getGridOverlayRandomInit(int pImageSizeX, int pImageSizeY, int pImageSizeZ, int splitX,
                                                       int
                                                               splitY, Color pColor) {
        int restX = pImageSizeX % splitX;
        int restY = pImageSizeY % splitY;

        int[] xsizes = new int[splitX];
        int[] ysizes = new int[splitY];

        Random rand = new Random();


        for (int i = 0; i < restX; i++) {
            boolean done = false;
            while (!done) {
                int r = rand.nextInt(splitX);
                if (xsizes[r] == 0) {
                    xsizes[r] = 1;
                    done = true;
                }
            }
        }

        for (int i = 0; i < restY; i++) {
            boolean done = false;
            while (!done) {
                int r = rand.nextInt(splitY);
                if (ysizes[r] == 0) {
                    ysizes[r] = 1;
                    done = true;
                }
            }
        }

        return new GridOverlay(pImageSizeX, pImageSizeY, pImageSizeZ, splitX, splitY, pColor, xsizes, ysizes);
    }

    public static GridOverlay getGridOverlayPredefinedInit(int pImageSizeX, int pImageSizeY, int pImageSizeZ, int
            splitX, int splitY, Color pColor, int[]
                                                                   xsizes, int[] ysizes) {
        if (xsizes.length != splitX | ysizes.length != splitY) {
            throw new IllegalArgumentException("Sizes of xsizes and ysizes don't match to the split sizes.");
        }
        return new GridOverlay(pImageSizeX, pImageSizeY, pImageSizeZ, splitX, splitY, pColor, xsizes, ysizes);
    }

    public static GridOverlay getGridOverlaySubsplit(int pImageSizeX, int pImageSizeY, int pImageSizeZ, int
            splitX, int splitY, Color pColor, int[] xsizes, int[] ysizes, int subSplitX, int subSplitY) {

        int normSizeX = pImageSizeX/splitX;
        int abnormSizeX = normSizeX + 1;

        int normSizeY = pImageSizeY/splitY;
        int abnormSizeY = normSizeY + 1;

        int normSizeXSub = pImageSizeX/(splitX*subSplitX);
        int abnormSizeXSub = normSizeXSub + 1;

        int normSizeYSub = pImageSizeY/(splitY*subSplitY);
        int abnormSizeYSub = normSizeYSub + 1;

        int[] xsizesN = new int[splitX*subSplitX];
        int[] ysizesN = new int[splitY*subSplitY];

        Random rand = new Random();

        for (int k = 0; k < splitX; k++) {
            int size = 0;

            if (xsizes[k] == 0) {
                size = normSizeX;
            }
            else {
                size = abnormSizeX;
            }

            int restX = size%normSizeXSub;
            for (int i = 0; i < restX; i++) {
                boolean done = false;
                while (!done) {
                    int r = rand.nextInt(subSplitX);
                    if (xsizesN[subSplitX*k+r] == 0) {
                        xsizesN[subSplitX*k+r] = 1;
                        done = true;
                    }
                }
            }
        }

        for (int k = 0; k < splitY; k++) {
            int size = 0;
            if (ysizes[k] == 0) {
                size = normSizeY;
            }
            else {
                size = abnormSizeY;
            }
            int restY = size%normSizeYSub;

            for (int i = 0; i < restY; i++) {
                boolean done = false;
                while (!done) {
                    int r = rand.nextInt(subSplitY);
                    if (ysizesN[subSplitY*k+r] == 0) {
                        ysizesN[subSplitY*k+r] = 1;
                        done = true;
                    }
                }
            }
        }

        return new GridOverlay(pImageSizeX, pImageSizeY, pImageSizeZ, splitX*subSplitX, splitY*subSplitY, pColor,
                xsizesN, ysizesN);
    }




    private GridOverlay(int pImageSizeX, int pImageSizeY, int pImageSizeZ, int splitX, int splitY, Color pColor, int[]
            xsizes, int[] ysizes) {

        mMainStackPane = new StackPane();
        mTilesOriX = new int[splitX];
        mTilesOriY = new int[splitY];
        this.color = pColor;

        this.mImageSizeX = pImageSizeX;
        this.mImageSizeY = pImageSizeY;
        this.mImageSizeZ = pImageSizeZ;

        this.mPaneList = new ArrayList<>(mImageSizeZ);

        this.mRectsX = splitX;
        this.mRectsY = splitY;

        this.mRectList = new ArrayList<>(mRectsX * mRectsY);
        System.out.println(mRectList.size());

        this.xsizes = Arrays.copyOf(xsizes, xsizes.length);
        this.ysizes = Arrays.copyOf(ysizes, ysizes.length);

        int normSizeX = mImageSizeX / mRectsX;
        int normSizeY = mImageSizeY / mRectsY;
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        int anomX = 0;
        int anomY = 0;
        Rectangle rect = null;

        for (int i = 0; i < mRectsY; i++) {
            for (int j = 0; j < mRectsX; j++) {
                x = j * normSizeX + anomX;
                y = i * normSizeY + anomY;


                mTilesOriX[j] = x;
                mTilesOriY[i] = y;

                width = xsizes[j] == 1 ? normSizeX + 1 : normSizeX;
                height = ysizes[i] == 1 ? normSizeY + 1 : normSizeY;
                rect = new Rectangle();
                rect.setFill(color);
                rect.setStroke(new Color(0, 1, 0, .1));
                rect.setStrokeWidth(2);
                rect.widthProperty().bind(mMainStackPane.widthProperty().multiply((double) width / mImageSizeX));
                rect.heightProperty().bind(mMainStackPane.heightProperty().multiply((double) height / mImageSizeY));
                rect.xProperty().bind(mMainStackPane.widthProperty().multiply((double) x / mImageSizeX));
                rect.yProperty().bind(mMainStackPane.heightProperty().multiply((double) y / mImageSizeY));


                if (xsizes[j] == 1) {
                    anomX += 1;
//                    rect.setStroke(new Color(1,0,0,0.3));
                }


                mRectList.add(rect);
            }
            if (ysizes[i] == 1) {
                anomY += 1;
//                rect.setStroke(new Color(0,0,1,0.3));
            }


            anomX = 0;
        }


    }

    public void initializeGridWithZeros() {

        int[] zeros = new int[mRectsX * mRectsY];
        initializeGridPane(zeros);
    }

    public void initializeGridPane(int[] indZ) {

        mPaneList = new ArrayList<>(mImageSizeZ);
        for (int i = 0; i < mImageSizeZ; i++) {
            mPaneList.add(new Pane());
        }
        for (int i = 1; i < mImageSizeZ; i++) {
            mPaneList.get(i).setVisible(false);
        }

        if (indZ.length != mRectsX * mRectsY) {
            throw new IllegalArgumentException(String.format("Wrong size of the index matrix. Provided: %d, needed: " +
                    "%d.", indZ.length, mRectsX * mRectsY));
        }
        for (int i = 0; i < mRectsX; i++) {
            for (int j = 0; j < mRectsY; j++) {
                mPaneList.get(indZ[mRectsX * j + i]).getChildren().add(mRectList.get(mRectsX * j + i));
            }
        }
        mMainStackPane.getChildren().addAll(mPaneList);

    }

    public ArrayList<Pane> getPaneList() {
        System.out.println("current panelist size is: " + mPaneList.size());
        return mPaneList;
    }

    public StackPane getMainStackPane() {
        return mMainStackPane;
    }

    public int[] getTileOriX() {
        return mTilesOriX;
    }

    public int[] getTileOriY() {
        return mTilesOriY;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int[] getXsizes() {
        return xsizes;
    }

    public int[] getYsizes() {
        return ysizes;
    }
}

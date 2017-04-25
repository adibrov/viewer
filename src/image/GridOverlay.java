package image;

import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by dibrov on 25/04/17.
 */
public class GridOverlay {
    private ArrayList<Rectangle> mRectList;
    private ArrayList<Pane> mPaneList;
    private int mImageSizeX;
    private  int mImageSizeY;
    private int mImageSizeZ;
    private int mRectSizeX;
    private int mRectSizeY;
    private int mRectsX;
    private int mRectsY;
    private int[] xsizes;
    private int[] ysizes;
    private StackPane mMainStackPane;
    private Color color;
    private int[] mTileOriX;
    private int[] mTileOriY;


    public GridOverlay(int pImageSizeX, int pImageSizeY, int pImageSizeZ, int splitX, int splitY) {

        mMainStackPane = new StackPane();
        mTileOriX = new int[splitX];
        mTileOriY = new int[splitY];
        this.color = new Color(0,1,0,0.2);

        this.mImageSizeX = pImageSizeX;
        this.mImageSizeY = pImageSizeY;
        this.mImageSizeZ = pImageSizeZ;

        this.mPaneList = new ArrayList<>(mImageSizeZ);

        this.mRectsX = splitX;
        this.mRectsY = splitY;
        this.mRectList = new ArrayList<>(mRectsX*mRectsY);
        System.out.println(mRectList.size());

        int restX = mImageSizeX%splitX;
        int restY = mImageSizeY%splitY;

        xsizes = new int[splitX];
        ysizes = new int[splitY];

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
        int normSizeX = mImageSizeX/mRectsX;
        int normSizeY = mImageSizeY/mRectsY;
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        int anomX = 0;
        int anomY = 0;
        Rectangle rect = null;

        for (int i = 0; i < mRectsX; i++) {
            for (int j = 0; j < mRectsY; j++) {
                x = i*normSizeX + anomX;
                y = j*normSizeY + anomY;


                mTileOriX[i] = x;
                mTileOriY[j] = y;

                width = xsizes[i] == 1?  normSizeX+1 : normSizeX;
                height = ysizes[j] == 1?  normSizeY+1 : normSizeY;
                rect = new Rectangle();
                rect.setFill(color);
                rect.setStroke(new Color(0,1,0,.1));
                rect.setStrokeWidth(2);
                rect.widthProperty().bind(mMainStackPane.widthProperty().multiply((double)width/mImageSizeX));
                rect.heightProperty().bind(mMainStackPane.heightProperty().multiply((double)height/mImageSizeY));
                rect.xProperty().bind(mMainStackPane.widthProperty().multiply((double)x/mImageSizeX));
                rect.yProperty().bind(mMainStackPane.heightProperty().multiply((double)y/mImageSizeY));

                if (ysizes[j] == 1) {
                    anomY+=1;
                    rect.setStroke(new Color(0,0,1,0.3));
                }
                if (xsizes[i] == 1) {
                  //  anomX+=1;
                    rect.setStroke(new Color(1,0,0,0.3));
                }


                mRectList.add( rect);
            }

            if (xsizes[i] == 1) {
                anomX+=1;
                rect.setStroke(new Color(1,0,0,0.3));
            }
            anomY = 0;
        }




    }

    public void initializeGrid(int[] indZ){

        mPaneList = new ArrayList<>(mImageSizeZ);
        for (int i = 0; i < mImageSizeZ; i++) {
            mPaneList.add(new Pane());
        }
        for (int i = 1; i < mImageSizeZ; i++) {
            mPaneList.get(i).setVisible(false);
        }

        if (indZ.length != mRectsX * mRectsY) {
            throw new IllegalArgumentException(String.format("Wrong size of the index matrix. Provided: %d, needed: " +
                    "%d.", indZ.length, mRectsX*mRectsY));
        }
        for (int i = 0; i < mRectsX; i++ ) {
            for (int j = 0; j < mRectsY; j++) {
                mPaneList.get(indZ[mRectsX*j + i]).getChildren().add(mRectList.get(mRectsX*j + i));
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
        return mTileOriX;
    }

    public int[] getTileOriY() {
        return mTileOriY;
    }
}

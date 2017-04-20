package image;

/**
 * Created by dibrov on 20/04/17.
 */
public enum Plane {
    XZ(1), XY(2), YZ(0);

    private int slideDim;

    Plane(int slideDim) {
        this.slideDim = slideDim;
    }

    public int getSlideDim() {
        return slideDim;
    }
}

package image;

/**
 * Created by dibrov on 20/04/17.
 */
public enum Plane {
    XZ(1, new int[]{0,2}), XY(2, new int[]{0,1}), YZ(0, new int[]{1,2});

    private int slideDim;
    private int[] activeDims;

    Plane(int slideDim, int[] activeDims) {
        this.slideDim = slideDim;
        this.activeDims = activeDims;
    }

    public int getSlideDim() {
        return slideDim;
    }

    public int[] getActiveDims(){
        return activeDims;
    }
}

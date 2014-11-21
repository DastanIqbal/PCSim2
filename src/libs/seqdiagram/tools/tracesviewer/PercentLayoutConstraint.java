package tools.tracesviewer;

public class PercentLayoutConstraint {
	/**
	 * Creates a Constraint Object.
	 * @param x The X position of the top left corner of the component (0-100)
	 * @param y The Y position of the top left corner of the component (0-100)
	 * @param width The percentage width of the component (0-100)
	 * @param height The percentage height of the component (0-100)
	 */
	public PercentLayoutConstraint(double x, double y, double width, double height) {
	    if (width < 0 || width > 100) {
            throw new IllegalArgumentException("width(" + width + ") must be in the range 0-100");
        }
        if (height < 0 || height > 100) {
            throw new IllegalArgumentException("height(" + height + ") must be in the range 0-100");
        }
	    
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	protected double x, y, width, height;
}

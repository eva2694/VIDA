package si.uni_lj.fe.diplomsko_delo.pomocnik.models

/**
 * Represents a bounding box for object detection.
 * @property x1,y1 Top-left corner coordinates
 * @property x2,y2 Bottom-right corner coordinates
 * @property cx,cy Center point coordinates
 * @property w,h Width and height of the box
 * @property cnf Confidence score of the detection
 * @property cls Class index of the detected object
 * @property clsName Human-readable class name
 */
data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val cls: Int,
    val clsName: String
)
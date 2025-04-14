package si.uni_lj.fe.diplomsko_delo.pomocnik.models

/**
 * Represents the result of object detection and analysis.
 * @property boundingBox The detected object's bounding box
 * @property depth Depth estimation
 * @property ocrText Extracted text from the object, if OCR was performed and successful
 */
data class AssistResult(
    val boundingBox: BoundingBox,
    val depth: Int, // ~ 0 - 1100, the result of the midas depth estimator
    val ocrText: String? = null // Null if no OCR was performed or successful
)

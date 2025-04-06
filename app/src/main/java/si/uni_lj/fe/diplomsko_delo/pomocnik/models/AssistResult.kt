package si.uni_lj.fe.diplomsko_delo.pomocnik.models

/**
 * Represents the result of object detection and analysis.
 * @property boundingBox The detected object's bounding box
 * @property depthScale Depth estimation on a scale of 1-11, or -1 if depth estimation failed
 * @property ocrText Extracted text from the object, if OCR was performed and successful
 */
data class AssistResult(
    val boundingBox: BoundingBox,
    val depthScale: Int, // 1-11 scale, -1 if depth failed
    val ocrText: String? = null // Null if no OCR was performed or successful
)

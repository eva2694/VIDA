package si.uni_lj.fe.diplomsko_delo.pomocnik.models

data class AssistResult(
    val boundingBox: BoundingBox,
    val depthScale: Int, // 1-11 scale, -1 if depth failed
    val ocrText: String? = null // Null if no OCR was performed or successful
)

package si.uni_lj.fe.diplomsko_delo.pomocnik

/**
 * Application-wide constants for model paths and configuration.
 */
object Constants {
    // Uncomment to use 601 classes from openimages, ultralytics model
    //const val YOLO_PATH = "yolov8n-oiv7_float32.tflite"
    //const val LABELS_PATH_SI = "oznake.txt"
    //const val LABELS_PATH_EN = "labels.txt"

    // our model:
    const val YOLO_PATH = "our_float32.tflite"
    const val LABELS_PATH_SI = "oznake_new.txt"
    const val LABELS_PATH_EN = "labels_new.txt"

    const val MIDAS_PATH = "midas.tflite"
    const val SCENE_DETECTOR_PATH = "resnet18_places365_float32.tflite"
    const val SCENE_LABELS_PATH_EN = "places365_labels_en.txt"
    const val SCENE_LABELS_PATH_SI = "places365_labels_si.txt"

    const val SCENE_READ_CONFIDENCE = 0.4f
}
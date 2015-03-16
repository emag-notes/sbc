package kanjava;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * @author Yoshimasa Tanabe
 */
@Component
public class FaceDetector {

  private static final Logger log = LoggerFactory.getLogger(FaceDetector.class);

  @Value("${classifierFile:classpath:/haarcascade_frontalface_default.xml}")
  private File classifierFile;

  private CascadeClassifier classifier;

  @PostConstruct
  void init() throws IOException {
    if (log.isInfoEnabled()) {
      log.info("load {}", classifierFile.toPath());
    }
    this.classifier = new CascadeClassifier(classifierFile.toPath().toString());
  }

  public void detectFaces(opencv_core.Mat source, BiConsumer<opencv_core.Mat, opencv_core.Rect> detectAction) {
    opencv_core.Rect faceDetections = new opencv_core.Rect();
    classifier.detectMultiScale(source, faceDetections);
    int numOfFaces = faceDetections.limit();

    log.info("{} faces are detected!", numOfFaces);

    for (int i = 0; i < numOfFaces; i++) {
      opencv_core.Rect r = faceDetections.position(i);
      detectAction.accept(source, r);
    }
  }

}

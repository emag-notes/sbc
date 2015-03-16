package kanjava;

import org.bytedeco.javacpp.opencv_core;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * @author Yoshimasa Tanabe
 */
public class FaceTranslator {
  public static void duker(opencv_core.Mat source, opencv_core.Rect r) {
    int x = r.x();
    int y = r.y();
    int h = r.height();
    int w = r.width();

    rectangle(source,
              new opencv_core.Point(x, y),
              new opencv_core.Point(x + w, y + h / 2),
              new opencv_core.Scalar(0, 0, 0, 0),
              -1,
              opencv_core.CV_AA,
              0);

    rectangle(source,
      new opencv_core.Point(x, y + h / 2),
      new opencv_core.Point(x + w, y + h),
      new opencv_core.Scalar(255, 255, 255, 0),
      -1,
      opencv_core.CV_AA,
      0);

    circle(source,
      new opencv_core.Point(x + h / 2, y + h / 2),
      (w + h) / 12,
      new opencv_core.Scalar(0, 0, 255, 0),
      -1,
      opencv_core.CV_AA,
      0);
  }
}

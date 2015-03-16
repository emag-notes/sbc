package kanjava;

import org.bytedeco.javacpp.opencv_core;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Yoshimasa Tanabe
 */
@RestController
public class DukerController {

  @Autowired
  FaceDetector faceDetector;

  @Bean
  BufferedImageHttpMessageConverter bufferedImageHttpMessageConverter() {
    return new BufferedImageHttpMessageConverter();
  }

  @RequestMapping(value = "/duker")
  BufferedImage duker(@RequestParam Part file) throws IOException {
    opencv_core.Mat source = opencv_core.Mat.createFrom(ImageIO.read(file.getInputStream()));
    faceDetector.detectFaces(source, FaceTranslator::duker);
    BufferedImage image = source.getBufferedImage();
    return image;
  }

}

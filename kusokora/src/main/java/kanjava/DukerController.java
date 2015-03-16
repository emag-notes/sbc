package kanjava;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * @author Yoshimasa Tanabe
 */
@RestController
public class DukerController {

  private static final Logger log = LoggerFactory.getLogger(DukerController.class);

  @Autowired
  FaceDetector faceDetector;

  @Autowired
  JmsMessagingTemplate jmsMessagingTemplate;

  @Autowired
  SimpMessagingTemplate simpMessagingTemplate;

  @Bean
  BufferedImageHttpMessageConverter bufferedImageHttpMessageConverter() {
    return new BufferedImageHttpMessageConverter();
  }

  @Value("${faceduker.width:200}")
  int resizeWidth;

  @RequestMapping(value = "/")
  String hello() {
    return "Hello World!";
  }

  @RequestMapping(value = "/duker")
  BufferedImage duker(@RequestParam Part file) throws IOException {
    opencv_core.Mat source = opencv_core.Mat.createFrom(ImageIO.read(file.getInputStream()));
    faceDetector.detectFaces(source, FaceTranslator::duker);
    BufferedImage image = source.getBufferedImage();
    return image;
  }

  @RequestMapping(value = "/queue")
  String send(@RequestParam Part file) throws IOException {
    byte[] src = StreamUtils.copyToByteArray(file.getInputStream());
    Message<byte[]> message = MessageBuilder.withPayload(src).build();
    jmsMessagingTemplate.send("faceConverter", message);
    return "OK";
  }

  @JmsListener(destination = "faceConverter", concurrency = "1-5")
  void onMessage(Message<byte[]> message) throws IOException {
    log.info("received! {}", message);
    try(InputStream stream = new ByteArrayInputStream(message.getPayload())) {
      opencv_core.Mat source = opencv_core.Mat.createFrom(ImageIO.read(stream));
      faceDetector.detectFaces(source, FaceTranslator::duker);

      double ratio = ((double) resizeWidth) / source.cols();
      int height = (int) (ratio * source.rows());
      opencv_core.Mat out = new opencv_core.Mat(height, resizeWidth, source.type());
      resize(source, out, new opencv_core.Size(), ratio, ratio, opencv_imgproc.INTER_LINEAR);

      BufferedImage image = out.getBufferedImage();

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        ImageIO.write(image, "png", baos);
        baos.flush();
        simpMessagingTemplate.convertAndSend(
          "/topic/faces", Base64.getEncoder().encodeToString(baos.toByteArray()));
      }
    }
  }

  @MessageMapping(value = "/greet")
  @SendTo(value = "/topic/greetings")
  String greet(String name) {
    log.info("received {}", name);
    return "Hello " + name;
  }

  @MessageMapping(value = "/faceConverter")
  void faceConverter(String base64Image) {
    Message<byte[]> message = MessageBuilder.withPayload(Base64.getDecoder().decode(base64Image)).build();
    jmsMessagingTemplate.send("faceConverter", message);
  }
}

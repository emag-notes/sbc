package kanjava;

import org.bytedeco.javacpp.opencv_core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
      BufferedImage image = source.getBufferedImage();
    }
  }

  @MessageMapping(value = "/greet")
  @SendTo(value = "/topic/greetings")
  String greet(String name) {
    log.info("received {}", name);
    return "Hello " + name;
  }

}

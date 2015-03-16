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
import org.springframework.messaging.support.MessageBuilder;
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

  @RequestMapping(value = "/send")
  String send(@RequestParam String msg) {
    Message<String> message = MessageBuilder.withPayload(msg).build();
    jmsMessagingTemplate.send("hello", message);
    return "OK";
  }

  @JmsListener(destination = "hello", concurrency = "1-5")
  void onMessage(Message<String> message) {
    log.info("received! {}", message);
    log.info("msg={}", message.getPayload());
  }
}

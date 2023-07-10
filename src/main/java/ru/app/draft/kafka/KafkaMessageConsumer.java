//package ru.app.draft.kafka;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//@Component
//public class KafkaMessageConsumer {
//
//    @KafkaListener(topics = "${spring.kafka.consumer.topic}")
//    public void listen(String message) {
//        System.out.println("Received message: " + message);
//    }
//}
//    package com.example.CollaborationService.Redis;
//
//    import io.lettuce.core.StrAlgoArgs;
//    import org.springframework.amqp.core.Message;
//    import org.springframework.amqp.core.MessageListener;
//    import org.springframework.stereotype.Component;
//
//
//    @Component
//    public class RedisUpdateSubscriber implements MessageListener {
//
//
//
//        @Override
//        public void onMessage(Message message, Byte[] pattern) {
//            String channel = new String(message.getChannel());
//            Byte[] data = message.getBody();
//
//            String documentId = channel.split(":")[2];
//            websockerhandler.broadcastBinartMessage(
//                    documentId,
//                    data
//            ));
//        }
//    }
//
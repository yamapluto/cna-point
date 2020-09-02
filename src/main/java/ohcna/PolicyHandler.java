package ohcna;

import ohcna.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyHandler{

    @Autowired
    PointController pc;

    @Autowired
    PointRepository pointRepo;



    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRoomDeleted_PointDelete(@Payload RoomDeleted roomDeleted){

        if(roomDeleted.isMe()){
            System.out.println("##### listener PointDelete : " + roomDeleted.toJson());
            //Point point = new Point();

            List<Point> pointList = pointRepo.findByIdOrderByChangeDtm(roomDeleted.getId());
            if(!pointList.isEmpty()){

                pc.cancelled(pointList.get(0));
                System.out.println("PointDelete call cancelled:"+pointList.get(0).getId());

            }
        }
    }

}

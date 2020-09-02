package ohcna;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@RestController
 public class PointController {
   @Autowired
   PointRepository pointRepo;
   @PostMapping("/points/create")
   public Point created(@RequestBody Point postPoint) {

     Point point = new Point();
     point.setId(postPoint.getId());
     point.setPoint(0);
     point.setStatus(postPoint.getStatus());
     point.setChangeDtm(postPoint.getChangeDtm());
    try {
     System.out.println("##### onPostPersist currentThread : " );
     Thread.currentThread().sleep((long) (400 + Math.random() * 320));
    } catch (InterruptedException e) {
     e.printStackTrace();
    }



     Optional<Point> findPoint = pointRepo.findById(postPoint.getId());
     if(findPoint!= null&& findPoint.isPresent()){
      System.out.println("##### Point Exist : " +findPoint);

     }else{
      pointRepo.save(point);
      System.out.println("##### PointRepository created : "+ point.getId()  + "<<<");
     }


     return point;
  }

    @PostMapping("/points/save")
    public Point saved(@RequestBody Point postPoint) {

        Point point = new Point();
        Integer ratedPoint = postPoint.getPoint();
        if(ratedPoint == null){
            ratedPoint = 0;
        }


        Optional<Point> score = pointRepo.findById(postPoint.getId());
        if(score!=null) {
//            point.setId(postPoint.getId());
            point.setId(postPoint.getId());
            point.setStatus("saved");
            point.setPoint(score.get().getPoint()+ratedPoint);
            String chgDtm = "20200902"+(100000 + Math.random() * 2);
            point.setChangeDtm(chgDtm);
            pointRepo.save(point);
            System.out.println("##### PointRepository saved");


        }else{
            System.out.println("##### PointRepository saved >>>> "+postPoint.getId()+"<<<< NOT EXISTS");
        }
        return point;
    }

    @PostMapping("/points/cancel")
    public Point cancelled(@RequestBody Point postPoint) {

        Point point = postPoint;


        Optional<Point> score = pointRepo.findById(postPoint.getId());
        if(score!=null) {
//            point.setId(score.getId());

            point.setId(score.get().getId());
            point.setStatus("cancelled");
            pointRepo.save(point);
            System.out.println("##### PointRepository cancelled >> "+point.getId());

        }

        return point;
    }
/*
    @RequestMapping(value = "/points/select/{id}",
            method = RequestMethod.POST,
            produces = "application/json;charset=UTF-8")


    public void select(@PathVariable("id") Long id) {
        System.out.println("##### MenuScoreRepository select"+id);

        Optional<Point> score = pointRepo.findById(id);

        Point printScore =score.get();
        System.out.println("##### PointRepository id"+printScore.getId());
        System.out.println("##### PointRepository score"+printScore.getPoint());
        System.out.println("##### PointRepository Status"+printScore.getStatus());

    }
*/
 }

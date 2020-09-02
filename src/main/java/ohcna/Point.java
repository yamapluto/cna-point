package ohcna;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;


@Entity
@Table(name="Point_table")
public class Point {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Integer point;
    private String status;
    private String changeDtm;

    public Point() {
        this.point = 0;
    }

    @PostPersist
    public void onPostPersist(){

        if(this.getStatus().equals("created")){
            System.out.println("##### Point onPostPersist created ");
            PointCreated pointCreated = new PointCreated();
            BeanUtils.copyProperties(this, pointCreated);
            pointCreated.publishAfterCommit();

        }else if(this.getStatus().equals("cancelled")){
            System.out.println("##### cancelled Point onPostPersist pointDeleted ");
            PointDeleted pointDeleted = new PointDeleted();
            BeanUtils.copyProperties(this, pointDeleted);
            pointDeleted.publishAfterCommit();
        }
    }

    @PostUpdate
    public void onPostUpdate(){
        /*if(this.getStatus().equals("created")){
            System.out.println("##### Point onPostUpdate created ");
            PointCreated pointCreated = new PointCreated();
            BeanUtils.copyProperties(this, pointCreated);
            pointCreated.publishAfterCommit();
        }else*/

            if(this.getStatus().equals("saved")){
            System.out.println("##### Point onPostUpdate saved ");
            PointSaved pointSaved = new PointSaved();
            BeanUtils.copyProperties(this, pointSaved);
            pointSaved.publishAfterCommit();
        }else
            if(this.getStatus().equals("cancelled")){
                System.out.println("##### cancelled Point onPostUpdate pointDeleted ");
                PointDeleted pointDeleted = new PointDeleted();
                BeanUtils.copyProperties(this, pointDeleted);
                pointDeleted.publishAfterCommit();
            }
            else{
                System.out.println("##### Point onPostUpdate pointDeleted ");
                PointDeleted pointDeleted = new PointDeleted();
                BeanUtils.copyProperties(this, pointDeleted);
                pointDeleted.publishAfterCommit();
            }


    }

    @PostRemove
    public void onPostRemove(){
        System.out.println("##### Point onPostRemove pointDeleted ");
        PointDeleted pointDeleted = new PointDeleted();
        BeanUtils.copyProperties(this, pointDeleted);
        pointDeleted.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getChangeDtm() {
        return changeDtm;
    }

    public void setChangeDtm(String changeDtm) {
        this.changeDtm = changeDtm;
    }




}



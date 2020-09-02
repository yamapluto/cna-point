package ohcna;

public class PointDeleted extends AbstractEvent {

    private Long id;
    private Integer point;
    private String status;
    private String changeDtm;


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
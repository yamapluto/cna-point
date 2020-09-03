# 주제 - 회의실 시스템

회의실 시스템에 room을 생성하고, room의 환경에 대한 점수를 부여하는 시스템
(room의 점수는 차 후 환경 개선 시 참고할 수 있는 인테리어 기준이 됨)

------

# 구현 Repository

1. https://github.com/yamapluto/cna-room
2. https://github.com/yamapluto/cna-point
3. https://github.com/yamapluto/cna-pointList
4. https://github.com/yamapluto/cna-gateway


# 서비스 시나리오 및 분석/설계
![모델링 검증](https://user-images.githubusercontent.com/1927756/91792550-bbc4d800-ec50-11ea-9960-83a0899d51cb.png)

![이벤트 스토밍](https://user-images.githubusercontent.com/67448171/91993962-f887f080-ed70-11ea-9f60-74bec26b94ad.JPG)

## 기능적 요구사항

1. 사용자가 room을 생성한다.(roomCreate)
2. 사용자는 room에 point를 줄 수 있다.(pointSave)
3. 사용자는 room을 삭제 할 수 있다. (roomDelete)

## 비기능적 요구사항
1. 트랜잭션
  - room이 생성(roomCreated) 되었을 경우 room에 Point를 생성한다(Sync 호출)
  
2. 장애격리
  - Point service가 장애날 경우 point는 줄 수 없더라도 room 삭제는 가능함
  - Circuit Breaker
  
3. 성능
  - room에 부여된 point 목록은 시스템에서 확인 가능하다.(CQRS)
  - 예약/승인 상태가 변경될때 이메일로 알림을 줄 수 있다.(Event Driven)


## 헥사고날 아키텍처 다이어그램 도출  
![핵사고날](https://user-images.githubusercontent.com/67448171/92055473-4f211900-edc9-11ea-9f33-cf0b9ff03fa0.jpg)

- Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
- 호출관계에서 PubSub 과 Req/Resp 를 구분함
- 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐

# 구현

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현함. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)
booking/  confirm/  gateway/  notification/  bookinglist/

```
cd booking
mvn spring-boot:run

cd confirm
mvn spring-boot:run 

cd gateway
mvn spring-boot:run  

cd notification
mvn spring-boot:run

cd bookinglist
mvn spring-boot:run

cd room
mvn spring-boot:run  

cd point
mvn spring-boot:run

cd pointlist
mvn spring-boot:run


```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언. 
  ```booking, confirm, notification, room, point, pointlist```

```package ohcna;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Room_table")
public class Room {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private String floor;

    @PostPersist
    public void onPostPersist(){

        RoomCreated roomCreated = new RoomCreated();
        BeanUtils.copyProperties(this, roomCreated);
        roomCreated.publishAfterCommit();

        //Following code causes dependency to external APIs 
        ohcna.external.Point point = new ohcna.external.Point();
        // mappings goes here
        
        point.setId(roomCreated.getId());
        point.setStatus("created");
        point.setChangeDtm(chgDtm);
        point.setPoint(0);
        RoomApplication.applicationContext.getBean(ohcna.external.PointService.class)
            .pointCreate(point);
    }

    @PostUpdate
    public void onPostUpdate(){
        RoomChangeed roomChangeed = new RoomChangeed();
        BeanUtils.copyProperties(this, roomChangeed);
        roomChangeed.publishAfterCommit();
    }

    @PostRemove
    public void onPostRemove(){
        System.out.println("##### point onPostRemove <<<<<<<");
        RoomDeleted roomDeleted = new RoomDeleted();
        BeanUtils.copyProperties(this, roomDeleted);
        roomDeleted.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }
}

```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다

```java
public interface PointRepository extends PagingAndSortingRepository<Point, Long>{
    Optional<Point>  findById(Long id);
    List<Point> findByIdOrderByChangeDtm(Long id);

}
```

## 적용 후  테스트
* [room] room 생성
```
❯ http POST http://aa96a9baa39964102b0cbd52f5562218-266835466.ap-northeast-2.elb.amazonaws.com:8080/rooms name="red" floor="5"  
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Thu, 03 Sep 2020 00:45:21 GMT
Location: http://room:8080/rooms/13326
transfer-encoding: chunked

{
    "_links": {
        "room": {
            "href": "http://room:8080/rooms/13326"
        },
        "self": {
            "href": "http://room:8080/rooms/13326"
        }
    },
    "floor": "5",
    "name": "red"
}

{"eventType":"PointCreated","timestamp":"20200903005407","id":13236,"point":0,"status":"created","changeDtm":"20200903100000.46602695993","me":true}
{"eventType":"RoomCreated","timestamp":"20200903005407","id":13327,"name":"red","floor":"5","me":true}
```
```
>http GET http://aa96a9baa39964102b0cbd52f5562218-266835466.ap-northeast-2.elb.amazonaws.com:8080/points/1
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 03 Sep 2020 00:49:27 GMT
transfer-encoding: chunked

{
    "_links": {
        "point": {
            "href": "http://point:8080/points/1"
        },
        "self": {
            "href": "http://point:8080/points/1"
        }
    },
    "changeDtm": "20200903100000.01320973277",
    "point": 0,
    "status": "created"
}

>http GET http://aa96a9baa39964102b0cbd52f5562218-266835466.ap-northeast-2.elb.amazonaws.com:8080/pointLists/1
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 03 Sep 2020 01:24:32 GMT
transfer-encoding: chunked

{
    "_links": {
        "pointList": {
            "href": "http://pointList:8080/pointLists/1"
        },
        "self": {
            "href": "http://pointList:8080/pointLists/1"
        }
    },
    "point": 0,
    "roomFloor": "5",
    "roomName": "red"
}


```
* [point] room에 점수 부여
``` 
❯ http POST http://aa96a9baa39964102b0cbd52f5562218-266835466.ap-northeast-2.elb.amazonaws.com:8080/points/save id=1 point=3
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Date: Thu, 03 Sep 2020 00:50:52 GMT
transfer-encoding: chunked

{
    "changeDtm": "20200903100001.09237449037",
    "id": 1,
    "point": 3,
    "status": "saved"
}

{"eventType":"PointSaved","timestamp":"20200903005457","id":1,"point":3,"status":"saved","changeDtm":"20200903100001.89862713129","me":true}


>http GET http://aa96a9baa39964102b0cbd52f5562218-266835466.ap-northeast-2.elb.amazonaws.com:8080/pointLists/1
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 03 Sep 2020 01:49:32 GMT
transfer-encoding: chunked

{
    "_links": {
        "pointList": {
            "href": "http://pointList:8080/pointLists/1"
        },
        "self": {
            "href": "http://pointList:8080/pointLists/1"
        }
    },
    "point": 3,
    "roomFloor": "5",
    "roomName": "red"
}

```

* [room] room정보 삭제
```
❯ http DELETE http://aa96a9baa39964102b0cbd52f5562218-266835466.ap-northeast-2.elb.amazonaws.com:8080/rooms/1 
HTTP/1.1 204 No Content
Date: Thu, 03 Sep 2020 00:52:32 GMT

{"eventType":"RoomDeleted","timestamp":"20200903005232","id":1,"name":"red","floor":"5","me":true}
{"eventType":"PointDeleted","timestamp":"20200903005232","id":1,"point":3,"status":"cancelled","changeDtm":"20200903100001.09237449037","me":true}
```

## 동기식 호출 과 비동기식 

분석단계에서의 조건 중 하나로 room생성(roomCreate)->point 생성(pointCreate) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

### 동기식 호출(FeignClient 사용)
```java
// cna-room/../externnal/PointService.java

// feign client 로 booking method 호출
@FeignClient(name="point", url="${api.url.point}")
public interface PointService {

    @RequestMapping(method= RequestMethod.POST, path="/points/create")
    public void pointCreate(@RequestBody Point point);
}

```
### 비동기식 호출(Kafka Message 사용)
* Publish
```java
// cna-room/../room.java
    @PostRemove
    public void onPostRemove(){
        System.out.println("##### point onPostRemove <<<<<<<");
        RoomDeleted roomDeleted = new RoomDeleted();
        BeanUtils.copyProperties(this, roomDeleted);
        roomDeleted.publishAfterCommit();
    }
```
* Subscribe
```java
// cna-point/../PolicyHandler.java

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
```

## Gateway 적용
각 서비스는 ClusterIP 로 선언하여 외부로 노출되지 않고, Gateway 서비스 만을 LoadBalancer 타입으로 선언하여 Gateway 서비스를 통해서만 접근할 수 있다.
```yml
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: booking
          uri: http://booking:8080
          predicates:
            - Path=/bookings/** 
        - id: confirm
          uri: http://confirm:8080
          predicates:
            - Path=/confirms/** 
        - id: notification
          uri: http://notification:8080
          predicates:
            - Path=/notifications/** 
        - id: user
          uri: http://user:8080
          predicates:
            - Path=/users/**, /userLists/**
        - id: room
          uri: http://room:8080
          predicates:
            - Path=/rooms/** 
        - id: point
          uri: http://point:8080
          predicates:
            - Path=/points/** 
        - id: pointList
          uri: http://pointList:8080
          predicates:
            - Path= /pointLists/**
        - id: bookingList
          uri: http://bookingView:8080
          predicates:
            - Path= /bookingLists/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true
```

```yml
## gateway/../kubernetes/service.yml

apiVersion: v1
kind: Service
metadata:
  name: gateway
  labels:
    app: gateway
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: gateway
  type:
    LoadBalancer
```



# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS CodeBuild를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 buildspec.yml 에 포함되었다.
![CI/CD Pipeline](https://user-images.githubusercontent.com/3872380/91843678-1bdb6e80-ec91-11ea-87ac-dc2e90b24798.png)
1. 변경된 소스 코드를 GitHub에 push
2. CodeBuild에서 webhook으로 GitHub의 push 이벤트를 감지하고 build, test 수행
3. Docker image를 생성하여 ECR에 push
4. Kubernetes(EKS)에 도커 이미지 배포 요청
5. ECR에서 도커 이미지 pull

[ 구현 사항]
 * CodeBuild에 EKS 권한 추가
 ```json
         {
            "Action": [
                "ecr:BatchCheckLayerAvailability",
                "ecr:CompleteLayerUpload",
                "ecr:GetAuthorizationToken",
                "ecr:InitiateLayerUpload",
                "ecr:PutImage",
                "ecr:UploadLayerPart",
                "eks:DescribeCluster"
            ],
            "Resource": "*",
            "Effect": "Allow"
        }
 ```
  * EKS 역할에 CodeBuild 서비스 추가하는 내용을 EKS 의 ConfigMap 적용
```yaml
## aws-auth.yml
apiVersion: v1
data:
  mapRoles: |
    - groups:
      - system:bootstrappers
      - system:nodes
      rolearn: arn:aws:iam::052937454741:role/eksctl-TeamE-nodegroup-standard-w-NodeInstanceRole-GXDWDGLPWR40
      username: system:node:{{EC2PrivateDNSName}}
    - rolearn: arn:aws:iam::052937454741:role/CodeBuildServiceRoleForTeamE
      username: CodeBuildServiceRoleForTeamE
      groups:
        - system:masters
  mapUsers: |
    []
kind: ConfigMap
metadata:
  creationTimestamp: "2020-08-31T09:06:31Z"
  name: aws-auth
  namespace: kube-system
  resourceVersion: "854"
  selfLink: /api/v1/namespaces/kube-system/configmaps/aws-auth
  uid: cf038f09-ab94-4b60-9937-33acc0be86d8

```
```shell
kubectl apply -f aws-auth.yml --force
```
  * buildspec.yml
  ```yaml
  version: 0.2

phases:
  install:
    runtime-versions:
      java: corretto8 # Amazon Corretto 8 - production-ready distribution of the OpenJDK
      docker: 18
    commands:
      - curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.15.11/2020-07-08/bin/darwin/amd64/kubectl # Download kubectl 
      - chmod +x ./kubectl
      - mkdir ~/.kube
      - aws eks --region $AWS_DEFAULT_REGION update-kubeconfig --name TeamE # Set cluster TeamE as default cluster
  pre_build:
    commands:
      - echo Region = $AWS_DEFAULT_REGION # Check Environment Variables
      - echo Account ID = $AWS_ACCOUNT_ID # Check Environment Variables
      - echo ECR Repo = $IMAGE_REPO_NAME # Check Environment Variables
      - echo Docker Image Tag = $IMAGE_TAG # Check Environment Variables
      - echo Logging in to Amazon ECR...
      - $(aws ecr get-login --no-include-email --region $AWS_DEFAULT_REGION) # Login ECR
  build:
    commands:
      - echo Build started on `date`
      - echo Building the Docker image...
      - mvn clean
      - mvn package -Dmaven.test.skip=true # Build maven
      - docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG . # Build docker image
  post_build:
    commands:
      - echo Build completed on `date`
      - echo Pushing the Docker image...
      - docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG # Push docker image to ECR
      - echo Deploy service into EKS
      - kubectl apply -f ./kubernetes/deployment.yml # Deploy
      - kubectl apply -f ./kubernetes/service.yml # Service

cache:
  paths:
    - '/root/.m2/**/*'
  ```
## CodeBuild 를 통한 CI/CD 동작 결과

아래 이미지는 aws pipeline에 각각의 서비스들을 올려, 코드가 업데이트 될때마다 자동으로 빌드/배포 하도록 하였다.
![CodeBuild](https://user-images.githubusercontent.com/67448171/92060413-a5dd2180-edce-11ea-86d8-7da351bc24ce.JPG)


## Service Mesh
###  istio 를 통해 booking, confirm service 에 적용
 ```sh
 kubectl get deploy room -o yaml > room_deploy.yaml
 kubectl apply -f <(istioctl kube-inject -f room_deploy.yaml)

 kubectl get deploy point -o yaml > point_deploy.yaml
 kubectl apply -f <(istioctl kube-inject -f point_deploy.yaml)
 ```
 ![istio적용 결과](https://user-images.githubusercontent.com/1927756/91917876-2ed75880-ecfc-11ea-85f3-3e3dc6759df8.png)

### AutoScale(room) 적용
```yml
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```
```
kubectl autoscale deploy room --min=1 --max=10 --cpu-percent=20
```
![CB_100](https://user-images.githubusercontent.com/67448171/92060992-2ea88d00-edd0-11ea-92f4-53828e9bf2f0.JPG)
![autoscale_watch_pod](https://user-images.githubusercontent.com/67448171/92063796-ffe1e500-edd6-11ea-8a3d-10711b28033e.JPG)
![autoscale_setting_cpu](https://user-images.githubusercontent.com/67448171/92063774-f5bfe680-edd6-11ea-8caa-ac0eca48049a.JPG)

### confirm 에 Circuit Break 적용
```sh
spring:
  profiles: docker
  cloud:
    stream:
      kafka:
        binder:
          brokers: my-kafka.kafka.svc.cluster.local:9092
        streams:
          binder:
            configuration:
              default:
                key:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
                value:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
      bindings:
        event-in:
          group: room
          destination: ohcna
          contentType: application/json
        event-out:
          destination: ohcna
          contentType: application/json

api:
  url:
    point: http://point:8080

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```
```sh
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



     Optional<Point> findPoint = pointRepo.findById(postPoint.getId());
     if(findPoint!= null&& findPoint.isPresent()){
      System.out.println("##### Point Exist : " +findPoint);

     }else{
      pointRepo.save(point);
      System.out.println("##### PointRepository created : "+ point.getId()  + "<<<");
     }


     return point;
  }
```
![CB_100](https://user-images.githubusercontent.com/67448171/92060992-2ea88d00-edd0-11ea-92f4-53828e9bf2f0.JPG)
![CB_100_kafka](https://user-images.githubusercontent.com/67448171/92061015-38ca8b80-edd0-11ea-8d7b-10bf95009b97.JPG)
![CB_64](https://user-images.githubusercontent.com/67448171/92061034-44b64d80-edd0-11ea-9eb7-b98146e9bc79.JPG)


## Self Healing 을 위한 Readiness, Liveness 적용

```yaml
## cna-room/../deplyment.yml
readinessProbe:
    httpGet:
        path: '/actuator/health'
        port: 8080
    initialDelaySeconds: 10
    timeoutSeconds: 2
    periodSeconds: 5
    failureThreshold: 10
livenessProbe:
    httpGet:
        path: '/actuator/health'
        port: 8080
    initialDelaySeconds: 120
    timeoutSeconds: 2
    periodSeconds: 5
    failureThreshold: 5
```

## 무정지 재배포

Autoscaler설정과 Readiness 제거를 한뒤, 부하를 넣었다. 

이후 Readiness를 제거한 코드를 업데이트하여 새 버전으로 배포를 시작했다.

그 결과는 아래는 같다.

![image](https://user-images.githubusercontent.com/18453570/80060605-eea3e480-8569-11ea-9825-a375530f1953.png)


다시 Readiness 설정을 넣고 부하를 넣었다.

그리고 새버전으로 배포한 뒤 그 결과는 아래와 같다.

![image](https://user-images.githubusercontent.com/18453570/80060776-5823f300-856a-11ea-89a9-7c945ea05278.png)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.

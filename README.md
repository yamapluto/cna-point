# 주제 - 회의실 시스템

회의실 시스템에 room을 생성하고, room의 환경에 대한 점수를 부여하는 시스템
(room의 점수는 차 후 환경 개선 시 참고할 수 있는 인테리어 기준이 됨)

------

# 구현 Repository

1. https://github.com/yamapluto/cna-room
2. https://github.com/yamapluto/cna-point
3. https://github.com/yamapluto/cna-pointList
4. https://github.com/yamapluto/cna-gateway


# 서비스 시나리오

## 기능적 요구사항

1. 사용자가 room을 생성한다.(roomCreate)
2. 사용자는 회의실 예약을 취소 할 수 있다.(bookingCancel)
3. 회의실을 예약하면 관리자에게 승인요청이 간다.
4. 관리자는 승인을 할 수 있다.(confirmComplete)
5. 관리자는 승인 거절 할 수 있다.(confirmDeny)
6. 관리자가 승인 거절하면 예약취소한다.(bookingCancel)
7. 예약취소하면 예약정보는 삭제한다. 
8. 에약/승인 상태가 바뀔때마다 이메일로 알림을 준다.
9. 예약이 취소(bookingCancelled) 되면 컴펌 내역이 삭제 된다. (confirmDelete)

## 비기능적 요구사항
1. 트랜잭션
  - 승인거절(confirmDenied) 되었을 경우 예약을 취소한다.(Sync 호출)
  
2. 장애격리
  - 알림기능이 취소되더라도 예약과 승인 기능은 가능하다.
  - Circuit Breaker, fallback
  
3. 성능
  - 예약/승인 상태는 예약목록 시스템에서 확인 가능하다.(CQRS)
  - 예약/승인 상태가 변경될때 이메일로 알림을 줄 수 있다.(Event Driven)
  
# 분석 설계
## 이벤트 도출

![이벤트 스토밍](https://user-images.githubusercontent.com/67448171/91698324-7e5b3e80-ebad-11ea-8b16-48120bf8e92a.jpg)

## 기능적 요구사항을 커버하는지 검증
![이벤트 스토밍](https://user-images.githubusercontent.com/67448171/91993962-f887f080-ed70-11ea-9f60-74bec26b94ad.JPG)

1. 사용자가 회의실을 생성한다.(roomCreate)
2. 사용자는 회의실 예약을 취소 할 수 있다.(bookingCancel)
3. 회의실을 예약하면 관리자에게 승인요청이 간다. 
4. 관리자는 승인을 할 수 있다.(confirmComplete) 
5. 관리자는 승인 거절 할 수 있다.(confirmDeny) 
6. 관리자가 승인 거절하면 예약취소한다.(bookingCancel) 
7. 예약취소하면 예약정보는 삭제하고 confirm 대상에서 삭제한다.(confirmDelete)
8. 에약/승인 상태가 바뀔때마다 이메일로 알림을 준다.  
9. 예약이 취소(bookingCancelled) 되면 컴펌 내역이 삭제 된다. (confirmDelete)  --> ```Saga 적용```
10. 예약 및 승인 현황을 조회할 수 있다.(bookingList)

## 비 기능적 요구사항을 커버하는지 검증
1. 트랜잭션
  - 승인거절(confirmDenied) 되었을 경우 예약을 취소한다.(Sync 호출)
  
2. 장애격리
  - 알림기능이 취소되더라도 예약과 승인 기능은 가능하다.
  - Circuit Breaker, fallback
  
3. 성능
  - 예약/승인 상태는 예약목록 시스템에서 확인 가능하다.(CQRS)
  - 예약/승인 상태가 변경될때 이메일로 알림을 줄 수 있다.(Event Driven)
  
## 헥사고날 아키텍처 다이어그램 도출  
![핵사고날](https://user-images.githubusercontent.com/67448171/91702775-2f64d780-ebb4-11ea-9a18-5ce245db3691.jpg)

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
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언. 
  ```booking, confirm, notification```

```java
package ohcna;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Booking_table")
public class Booking {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long roomId;
    private String useStartDtm;
    private String useEndDtm;
    private String bookingUserId;
    private String status;

...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    public String getUseStartDtm() {
        return useStartDtm;
    }

    public void setUseStartDtm(String useStartDtm) {
        this.useStartDtm = useStartDtm;
    }
    public String getUseEndDtm() {
        return useEndDtm;
    }

    public void setUseEndDtm(String useEndDtm) {
        this.useEndDtm = useEndDtm;
    }
    public String getBookingUserId() {
        return bookingUserId;
    }

    public void setBookingUserId(String bookingUserId) {
        this.bookingUserId = bookingUserId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다

```java
package ohcna;
import org.springframework.data.repository.PagingAndSortingRepository;
public interface BookingRepository extends PagingAndSortingRepository<Booking, Long>{
}
```

- 적용 후 REST API 의 테스트
* [booking] 회의실 예약처리
```
❯ http  POST http://a87089e89ff2c465cb235f13b552bd86-1362531007.ap-northeast-2.elb.amazonaws.com:8080/bookings roomId="101" useStartDtm="20200831183000" useEndDtm="20200831193000" bookingUserId="06675"
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Tue, 01 Sep 2020 10:47:06 GMT
Location: http://booking:8080/bookings/7
transfer-encoding: chunked

{
    "_links": {
        "booking": {
            "href": "http://booking:8080/bookings/7"
        },
        "self": {
            "href": "http://booking:8080/bookings/7"
        }
    },
    "bookingUserId": "06675",
    "roomId": 101,
    "useEndDtm": "20200831193000",
    "useStartDtm": "20200831183000"
}

```
* [booking] 회의실 예약정보 수정
``` 
❯ http PATCH http://a87089e89ff2c465cb235f13b552bd86-1362531007.ap-northeast-2.elb.amazonaws.com:8080/bookings/7 bookingUserId="99999"
```

* [booking] 회의실 예약정보 삭제
```
❯ http DELETE http://a87089e89ff2c465cb235f13b552bd86-1362531007.ap-northeast-2.elb.amazonaws.com:8080/bookings/7
```

## 동기식 호출 과 비동기식 

분석단계에서의 조건 중 하나로 컨펌 반려(confirmDeny)->회의실 예약 취소(bookingCancel) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

### 동기식 호출(FeignClient 사용)
```java
// cna-confirm/../externnal/BookingService.java

// feign client 로 booking method 호출
// URL 은 application.yml 정의함(api.url.booking)
//@FeignClient(name="booking", url="http://booking:8080")
@FeignClient(name="booking", url="${api.url.booking}")
public interface BookingService {

    // Booking Cancel 을 위한 삭제 mapping
    @DeleteMapping(value = "/bookings/{id}")
    public void bookingCancel(@PathVariable long id);
}






// cna-confirm/../Confirm.java
    @PostUpdate
    public void onPostUpdate(){

        // 이벤트 인스턴스 생성
        // BookingChanged bookingChanged = new BookingChanged();

        // Confirmed
        if(this.getStatus().equals("CONFIRMED"))
        {
            ConfirmCompleted confirmCompleted = new ConfirmCompleted();
            BeanUtils.copyProperties(this, confirmCompleted);
             // 속성값 할당
            confirmCompleted.publishAfterCommit();
        }
        
        // Denied
        else if(this.getStatus().equals("DENIED"))
        {
            // 이벤트 인스턴스 생성
            ConfirmDenied confirmDenied = new ConfirmDenied();

            // 속성값 할당
            BeanUtils.copyProperties(this, confirmDenied);
            confirmDenied.publishAfterCommit();

            // mappings goes here
            ConfirmApplication.applicationContext.getBean(ohcna.external.BookingService.class)
                .bookingCancel(this.getBookingId());
        }

        // Exception Error
        else{
            System.out.println("Error");
        }
    }
```
### 비동기식 호출(Kafka Message 사용)
* Publish
```java
// cna-booking/../Booking.java
@PostPersist
public void onPostPersist(){
    BookingCreated bookingCreated = new BookingCreated();
    BeanUtils.copyProperties(this, bookingCreated);

    // AbstractEvent.java 의 publishAfterCommit --> publish --> KafkaChannel(outputChannel).send
    bookingCreated.publishAfterCommit();
}
```
* Subscribe
```java
// cna-notification/../PolicyHandler.java

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookingCreated_SendNotification(@Payload BookingCreated bookingCreated){

        if(bookingCreated.isMe()){
            // 노티 내용 SET
            Notification notification = new Notification();
            notification.setUserId(bookingCreated.getBookingUserId());
            notification.setContents("conference room[" + bookingCreated.getRoomId() + "] reservation is complete");
            String nowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            notification.setSendDtm(nowDate);
            notificationRepository.save(notification);
            System.out.println("##### listener SendNotification : " + bookingCreated.toJson());
        }
    }
```

## Gateway 적용
각 서비스는 ClusterIP 로 선언하여 외부로 노출되지 않고, Gateway 서비스 만을 LoadBalancer 타입으로 선언하여 Gateway 서비스를 통해서만 접근할 수 있다.
```yml
## gateway/../resources/application.yml

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
        - id: bookinglist
          uri: http://bookingList:8080
          predicates:
            - Path=/bookingLists/**
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

## 전체 시나리오 테스트
1. 회의실 예약(bookingCreate)
```sh
http POST http://ae0865d6fab6f4939b945502eec3b95f-35623661.ap-northeast-2.elb.amazonaws.com:8080/bookings roomId="556677" bookingUserId="45678" useStartDtm="202009021330" useEndDtm="202009021430"
```
```json
{
    "_links": {
        "booking": {
            "href": "http://booking:8080/bookings/3"
        },
        "self": {
            "href": "http://booking:8080/bookings/3"
        }
    },
    "bookingUserId": "45678",
    "roomId": 556677,
    "useEndDtm": "202009021430",
    "useStartDtm": "202009021330"
}
```

2. 승인내역 등록 확인(confirmRequest)
```sh
http GET  http://ae0865d6fab6f4939b945502eec3b95f-35623661.ap-northeast-2.elb.amazonaws.com:8080/confirms/2
```
```json
{
    "_links": {
        "confirm": {
            "href": "http://confirm:8080/confirms/2"
        },
        "self": {
            "href": "http://confirm:8080/confirms/2"
        }
    },
    "bookingId": 3,
    "confirmDtm": null,
    "status": "BOOKED",
    "userId": "45678"
}
```

3. 알림(notification)내역 확인
```sh
http GET  http://ae0865d6fab6f4939b945502eec3b95f-35623661.ap-northeast-2.elb.amazonaws.com:8080/notifications/5
```
```json
{
    "_links": {
        "notification": {
            "href": "http://notification:8080/notifications/5"
        },
        "self": {
            "href": "http://notification:8080/notifications/5"
        }
    },
    "contents": "conference room[556677] reservation is complete",
    "sendDtm": "2020-09-02 02:03:56",
    "userId": "45678"
}
```

4. CQRS(bookingList) 확인
```sh
http GET  http://ae0865d6fab6f4939b945502eec3b95f-35623661.ap-northeast-2.elb.amazonaws.com:8080/bookingLists/7
```
```json
{
    "_links": {
        "bookingList": {
            "href": "http://bookingList:8080/bookingLists/7"
        },
        "self": {
            "href": "http://bookingList:8080/bookingLists/7"
        }
    },
    "bookingDtm": "2020-09-02 02:03:56",
    "bookingId": 3,
    "bookingUserId": "45678",
    "confirmDtm": null,
    "confirmId": null,
    "confirmStatus": null,
    "confirmUserId": null,
    "roomId": 556677,
    "useEndDtm": "202009021430",
    "useStartDtm": "202009021330"
}
```

5. 승인거절(confirmDenied)
```sh
http  PATCH http://ae0865d6fab6f4939b945502eec3b95f-35623661.ap-northeast-2.elb.amazonaws.com:8080/confirms/2 status="DENIED"
```
```json
{
    "_links": {
        "confirm": {
            "href": "http://confirm:8080/confirms/2"
        },
        "self": {
            "href": "http://confirm:8080/confirms/2"
        }
    },
    "bookingId": 3,
    "confirmDtm": null,
    "status": "DENIED",
    "userId": "45678"
}
```

6. 승인거절 Notification
```sh
http GET  http://ae0865d6fab6f4939b945502eec3b95f-35623661.ap-northeast-2.elb.amazonaws.com:8080/notifications/6
```
```json
{
    "_links": {
        "notification": {
            "href": "http://notification:8080/notifications/6"
        },
        "self": {
            "href": "http://notification:8080/notifications/6"
        }
    },
    "contents": "reservation has been canceled",
    "sendDtm": "2020-09-02 02:10:23",
    "userId": "45678"
}
```

7. 승인거절시 bookingCancelled 호출 --> booking 내역 삭제
```sh
http GET  http://ae0865d6fab6f4939b945502eec3b95f-35623661.ap-northeast-2.elb.amazonaws.com:8080/bookings/3
```
```json
HTTP/1.1 404 Not Found
Date: Wed, 02 Sep 2020 02:12:16 GMT
content-length: 0
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
![CodeBuild 결과](https://user-images.githubusercontent.com/1927756/91916332-da31de80-ecf7-11ea-85eb-a2fe6e4ce82b.png)
![K8S 결과](https://user-images.githubusercontent.com/1927756/91916387-fafa3400-ecf7-11ea-8263-7351976b50cc.png)

## Service Mesh
###  istio 를 통해 booking, confirm service 에 적용
 ```sh
 kubectl get deploy booking -o yaml > booking_deploy.yaml
 kubectl apply -f <(istioctl kube-inject -f booking_deploy.yaml)

 kubectl get deploy confirm -o yaml > confirm_deploy.yaml
 kubectl apply -f <(istioctl kube-inject -f confirm_deploy.yaml)
 ```
 ![istio적용 결과](https://user-images.githubusercontent.com/1927756/91917876-2ed75880-ecfc-11ea-85f3-3e3dc6759df8.png)

### Scaleout(confirm) 적용
```sh
kubectl scale deploy confirm --replicas=2
```
![scaleout 적용](https://user-images.githubusercontent.com/1927756/91918221-4d8a1f00-ecfd-11ea-9cad-92a35a08edd2.png)

### confirm 에 Circuit Break 적용
```sh
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: confirm
spec:
  host: confirm
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 2
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1
    outlierDetection:
      consecutiveErrors: 5
      interval: 1s
      baseEjectionTime: 30s
      maxEjectionPercent: 100
EOF
```

## Self Healing 을 위한 Readiness, Liveness 적용

```yaml
## cna-booking/../deplyment.yml
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

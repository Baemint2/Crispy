## 프로젝트  명 : Crispy
### 주제: 치킨 프랜차이즈 ERP 그룹웨어
## 맡은 역할
### 테크리더
- 깃 버전 관리
- 코드 리뷰
- 학원에서 배우지 않은 Spring Security와 Thymeleaf 기술 공유
## 프로젝트 기간
2024.05.07 ~ 2024.06.20 (약 7주)
## 개발 환경
- JDK 17
- Oracle 18c
- Spring Boot 3.2.5
- Mybatis
- Thymeleaf
## 사용한 라이브러리
- BadWordFiltering
- JWT
- Lombok
- SockJs
- Spring mail
- Spring Security
- bcprov-jdk15on:1.69
- Spring validation
- Spring AOP
- Spring websocket
- Thymeleaf


## 담당 기능
### 사용자 및 가맹점 관리
- 가맹점 등록 및 조회/수정
- 점주 및 직원 등록, 조회/수정
- 로그인 및 계정 관리 (아이디/비밀번호 찾기, 비밀번호 변경)
- 직원 등록 시 임시 비밀번호 이메일 전송

### 데이터 유효성 검증
- Spring Validation 및 Validator를 구현하여 검증
- 커스텀 Validator를 구현하여 아이디, 이메일, 비밀번호 검증
- 검증 할때  instanceof를 활용하여 Owner, Employee를 구분

### 채팅 기능
- WebSokect STOMP 프로토콜을 사용
- 1:1 및 단체 채팅
- 채팅방 초대 및 조회, 나가기
- 채팅 메시지 조회, 메시지 삭제
- 채팅 메시지는 최초 조회시 50개 조회
  - 스크롤이 상단에 닿으면 이전 메시지 50개씩 조회하는 스크롤링 구현
- 메시지는 삭제하면 실시간으로 다른 사람들에게서도 삭제됨
- 실시간 알림 ( 읽지 않은 메시지 개수 )

### 보안
- JWT + Spring Security를 활용한 로그인
- argon2 해시 알고리즘을 이용한 비밀번호 암호화
- Spring Security를 활용한 동적 권한 관리
  - 관리자는 권한에 대한 컬럼 없이 서버에서 권한을 부여
  - 직원은 직책 번호에 따라 switch로 권한을 부여
- JWT 토큰 생성 및 검증
- 로그인 시 instanceof를 활용하여 관리자와 직원(Owner, Employee) 구분,
  - SecurityContextHolder를 통해 현재 인증된 사용자의 Authentication 객체에 동적으로 정보를 부여
- 자동로그인 ( 액세스 토큰 만료시 서버에 저장되어있는 리프레시 토큰을 검증하여 재 발급)

### 알림
- 알림 시스템 (SSE를 사용한 실시간 알림 전송)

### 게시판 기능
- 게시글 및 댓글(대댓글) CRUD (생성, 조회, 수정, 삭제)
- 좋아요 기능, 쿠키를 이용한 조회수 증가 ( 1일에 1회 증가)

### 필터링
- 게시판과 채팅 기능에 필터링 라이브러리를 활용
  - ex) 욕설 혹은 필터 리스트에 추가한 단어를 **로 표출
## ERD
![Copy of CRISPY (배영욱) (1)](https://github.com/user-attachments/assets/5421d506-1b35-4907-932b-b65cc3049b17)
## 폴더 구조
```📦 Crispy
├── 📁 src/main
│   ├── 📁 java/com/mcp/crispy
│   │   ├── 📁 admin
│   │   ├── 📁 annual
│   │   ├── 📁 approval
│   │   ├── 📁 attendance
│   │   ├── 📁 auth
│   │   ├── 📁 board
│   │   ├── 📁 calendar
│   │   ├── 📁 chat
│   │   ├── 📁 comment
│   │   ├── 📁 common
│   │   ├── 📁 email
│   │   ├── 📁 employee
│   │   ├── 📁 franchise
│   │   ├── 📁 freeboard
│   │   ├── 📁 map
│   │   ├── 📁 notification
│   │   ├── 📁 sales
│   │   ├── 📁 schedule
│   │   ├── 📁 stock
│   │   ├── CrispyApplication.java
│   │   └── MainController.java
│   └── 📁 resources
│       ├── 📁 mapper
│       ├── 📁 static
│       │   ├── 📁 css
│       │   ├── 📁 img
│       │   └── 📁 js
│       ├── 📁 templates
│       │   ├── 📁 approval
│       │   ├── 📁 attendance
│       │   ├── 📁 board
│       │   ├── 📁 calendar
│       │   ├── 📁 chat
│       │   ├── 📁 document
│       │   ├── 📁 employee
│       │   ├── 📁 error
│       │   ├── 📁 franchise
│       │   ├── 📁 freeboard
│       │   ├── 📁 layout
│       │   ├── 📁 owner
│       │   ├── 📁 sales
│       │   ├── 📁 stock
│       │   └── 📁 trash
│       ├── application.properties
│       ├── db.properties
│       ├── email.properties
│       ├── image.properties
│       └── jwt.properties
├── build.gradle
└── README.md
```

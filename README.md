# 🛡️ G-Shield: 실시간 스팸 전화 식별 앱
> **AI(Gemini)와의 Pair Programming을 통해 기획부터 배포 단계까지 구축한 안드로이드 스팸 차단 솔루션**

---

## **1. 프로젝트 개요**
본 프로젝트는 **전화가 걸려오는 즉시(Real-time)** 클라우드 데이터베이스를 조회하여 발신자 정보를 파악하고, 사용자가 즉각 대응할 수 있도록 돕는 안드로이드 애플리케이션입니다.

기존 스마트폰 환경에서는 수신 화면이 전체를 가려 모르는 번호를 식별하기 어렵다는 **사용자의 불편함(Pain Point)을 해결**하는 데 목적이 있으며, 특히 **생성형 AI(Gemini)를 기술 파트너로 활용**하여 1인 개발의 한계를 극복하고 효율성을 극대화한 프로젝트입니다.

### 🤖 AI Collaboration (AI 활용 경험)
- **기획 및 설계**: 사용자 시나리오 분석을 통해 `CallScreeningService` 기반의 아키텍처를 AI와 함께 설계
- **문제 해결(Troubleshooting)**: 안드로이드 최신 보안 정책(권한 제약)으로 인한 기능 구현의 한계를 AI와의 토론을 통해 '우회 UX(시스템 설정 유도 가이드)'로 해결
- **풀스택 개발**: 별도의 백엔드 엔지니어 없이 AI의 가이드를 통해 Firebase Firestore를 연동, 실시간 스팸 DB 구축

### 📜 핵심 가치
- **집단지성**: 사용자들이 신고한 스팸 데이터를 실시간으로 공유하여 최신 유형(보이스피싱, 대출 등)을 파악합니다.
- **편의성**: 검색부터 신고, 차단, 거절까지 원터치로 수행합니다.
- **실용성**: 불필요한 자동 차단 대신, 사용자에게 판단 근거(신고 횟수, 사유)를 제공하여 선택권을 보장합니다.

---

## **2. 주요 기능 상세**
### 1) 수신 번호 자동 감지 및 조회
- **트리거**: 전화 수신 시 `CallScreeningService`가 백그라운드에서 즉시 동작합니다.
- **정보 조회**:
  - **Firebase Firestore**를 실시간으로 조회하여 해당 번호의 신고 이력을 확인합니다.
  - **시각적 알림**:
    - 🚨 **위험**: 신고 이력이 있는 경우, 신고 횟수와 주요 사유(예: "신고 5건 - 보이스피싱")를 붉은색으로 경고
    - ✅ **안전**: 신고 이력이 없는 경우, 녹색으로 안전 표시

### 2) 실시간 오버레이 UI
- **플로팅 팝업**: 기본 전화 화면 위에 정보 창(`WindowManager` 활용)을 띄워 정보를 제공합니다.
- **UX 최적화**: 전화 수신 화면을 가리지 않으면서도 핵심 정보를 전달하는 직관적인 레이아웃을 적용했습니다.

### 3) 사용자 대응 액션
검색 결과 팝업 하단에 4가지 핵심 버튼을 제공하여, 사용자가 상황에 맞춰 즉각 대응할 수 있습니다.

| 버튼명 | 동작 로직 | 비고 |
|:---:|---|---|
| **무시** | 오버레이 팝업을 종료하고 무음(Silence) 처리합니다. 시스템 전화 수신 화면은 유지됩니다. | 회의 중이거나 받기 곤란할 때 |
| **신고만 하기** | 사용자가 입력한 사유를 Firebase DB에 저장(Count 증가)합니다. 통화는 끊지 않습니다. | 스팸 정보를 공유하고 싶을 때 |
| **거절** | `TelecomManager`를 통해 즉시 통화를 종료합니다. | 단순 광고/스팸일 때 |
| **차단 및 신고** | **①DB 신고 + ②통화 종료 + ③번호 복사 + ④설정 이동 가이드**를 원터치로 수행합니다. | 악성 스팸 영구 차단 시 |

---

## **3. 기술 스택 및 구현 전략**
### 1) Development Environment
- **Language**: Kotlin
- **Min SDK**: API 24 (Android 7.0) / **Target SDK**: API 34 (Android 14)
- **Tool**: Android Studio, Git/GitHub

### 2) Core Technologies
- **System Components**:
  - `CallScreeningService`: 최신 안드로이드 표준 수신 차단 서비스
  - `WindowManager`: 다른 앱 위에 그리기(`SYSTEM_ALERT_WINDOW`) 구현
  - `RoleManager`: 기본 스팸 앱 권한 관리

- **Database (BaaS)**:
  - `Firebase Firestore`: 실시간 스팸 번호 및 사유 데이터 저장/동기화 (NoSQL)

- **Asynchronous Processing**:
  - `Kotlin Coroutines`: DB 조회 및 비동기 작업 처리

- **Permissions Handling**:
  - `ANSWER_PHONE_CALLS`: 통화 강제 종료 제어
  - `SYSTEM_ALERT_WINDOW`: 오버레이 팝업 표시

---

## **4. 개발 로드맵 (Development Roadmap)**
### Phase 1: 기반 구축 (완료)
- [x] 안드로이드 프로젝트 세팅 및 Git 연동
- [x] `CallScreeningService`를 통한 수신 감지 아키텍처 구현
- [x] `WindowManager`를 이용한 오버레이 팝업 UI 구현

### Phase 2: 핵심 로직 구현 (완료)
- [x] **Firebase Firestore 연동**: 스팸 데이터베이스 구축 및 연동
- [x] **신고 프로세스**: 사용자 입력 사유 저장 및 카운팅 로직 구현
- [x] **UX 고도화**: 시스템 차단 목록 연동을 위한 '복사 후 설정 이동' 가이드 UI 구현
- [x] **버튼 기능 연결**: `TelecomManager`를 활용한 강제 통화 종료 기능

### Phase 3: 고도화 예정
- [ ] **통화 기록 연동**: 부재중 전화 목록에도 스팸 태그(🚨) 표시 기능
- [ ] **통계 대시보드**: "내가 막은 스팸 수" 등 사용자 리포트 제공
- [ ] **예외 처리 강화**: 네트워크 미연결 시 로컬 캐시 활용 로직 추가
---
*Created by **이유비** | Contact eo2jdkk@gmail.com | ![Generic badge](https://img.shields.io/badge/Vibe%20Coding-Gemini-4E88D4?style=flat-square&logo=google-gemini&logoColor=white)*

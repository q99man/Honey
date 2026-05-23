# 📱 Honeytong Flutter 모바일 앱 배포 및 릴리즈 가이드

이 문서는 허니통 모바일 애플리케이션의 프로덕션 배포용 안드로이드 APK 및 App Bundle(AAB) 빌드 절차와 릴리즈 서명 설정 방법을 설명합니다.

---

## 🔒 1. 릴리즈 서명 키(Keystore) 생성

프로덕션 빌드를 생성하기 전, 앱에 서명할 고유 Keystore 파일(.jks)을 생성해야 합니다.

JDK가 설치된 터미널에서 다음 명령어를 실행합니다:

```powershell
keytool -genkey -v -keystore key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias honeytong-key
```

- **생성 위치**: 생성된 `key.jks` 파일은 `mobile/android/` 디렉토리에 보관하는 것을 권장합니다.
- **보안 주의**: `key.jks` 파일과 키 비밀번호는 구글 플레이 스토어에 등록된 앱의 정체성을 증명하므로, 절대 분실하지 말고 외부나 Git 저장소에 공개되지 않도록 주의하십시오.

---

## ⚙️ 2. 로컬 서명 프로퍼티 설정

서명 키 정보를 빌드 스크립트가 참조할 수 있도록 `key.properties` 파일을 구성해야 합니다.

1. `mobile/android/key.properties.example` 파일을 복사하여 `mobile/android/key.properties` 파일을 생성합니다.
2. 파일 내의 정보를 다음과 같이 본인의 Keystore 설정에 맞게 수정합니다:

```properties
storePassword=your_keystore_password
keyPassword=your_key_password
keyAlias=honeytong-key
storeFile=key.jks
```

> [!NOTE]
> `key.properties` 및 `.jks` 파일은 안드로이드 프로젝트의 `.gitignore`에 등록되어 있어 Git 커밋 대상에서 자동으로 제외되므로 안심하셔도 됩니다.

---

## 🛠️ 3. 빌드 자동화 스크립트 실행

허니통 프로젝트는 Windows 환경에서 원클릭으로 정적 분석 및 릴리즈 빌드를 완료할 수 있는 자동화 스크립트를 제공합니다. 이 스크립트는 개발(dev) 환경과 프로덕션(prod) 환경용 빌드를 분리하여 생성합니다.

프로젝트 루트 디렉토리에서 PowerShell을 열고 다음 스크립트를 호출합니다:

```powershell
.\scripts\build-mobile-release.ps1
```

### 스크립트가 실행하는 단계:
1. `scripts/dev-env.ps1`을 통한 도구 환경 정규화 및 초기화
2. Flutter 종속성 최신화 (`flutter pub get`)
3. Flutter 코드 정적 분석 및 오류 검사 (`flutter analyze`)
4. 개발 환경(`dev`) 및 프로덕션 환경(`prod`) 릴리즈 APK 빌드
   - `flutter build apk --flavor dev` (개발용, 패키지 ID: `com.honeytong.app.dev`, 앱 이름: `허니통 (개발)`, API 주소: `http://10.0.2.2:8080`)
   - `flutter build apk --flavor prod` (배포용, 패키지 ID: `com.honeytong.app`, 앱 이름: `허니통`, API 주소: `https://api.honeytong.com`)
5. 구글 플레이 스토어 업로드용 App Bundle 빌드 (`flutter build appbundle --flavor prod`)

---

## 📦 4. 빌드 결과물 (Artifacts) 위치

빌드가 정상 종료되면 다음 경로에서 생성된 파일들을 확인할 수 있습니다:

- **개발자 테스트 및 수동 설치용 APK**:
  `mobile/build/app/outputs/flutter-apk/app-dev-release.apk`
- **프로덕션 릴리즈 및 수동 설치용 APK**:
  `mobile/build/app/outputs/flutter-apk/app-prod-release.apk`
- **릴리즈 AAB 파일 (구글 플레이 업로드용)**:
  `mobile/build/app/outputs/bundle/prodRelease/app-prod-release.aab`

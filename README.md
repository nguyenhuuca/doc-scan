# DocScanner — Android Document Scanner

Ứng dụng quét tài liệu offline cho Android, hỗ trợ ML Kit Document Scanner (GMS) và chế độ crop thủ công (non-GMS). Xuất PDF, chỉnh sửa ảnh, sắp xếp lại trang.

---

## Yêu cầu

| Công cụ | Phiên bản |
|---------|-----------|
| Android Studio (IntelliJ-based) | Hedgehog 2023.1.1+ hoặc mới hơn |
| JDK | 11+ (đi kèm Android Studio) |
| Android SDK | API 35 (Android 15) |
| Build Tools | 35.0.0 |
| Thiết bị / Máy ảo | Android 8.0+ (API 26+) |

> **Lưu ý:** Dự án dùng Gradle 8.9 + AGP 8.5.2 + Kotlin 2.0.21. Android Studio Hedgehog trở lên hỗ trợ tốt nhất.

---

## Mở dự án trong Android Studio

### Bước 1 — Tải Android Studio

Nếu chưa cài: tải tại [developer.android.com/studio](https://developer.android.com/studio)

Cài đặt bình thường, Android Studio sẽ tự cài kèm JDK và Android SDK.

### Bước 2 — Mở project

1. Khởi động Android Studio
2. Chọn **File → Open** (hoặc **Open** ở màn hình Welcome)
3. Trỏ đến thư mục gốc của dự án: `D:\DO\android`
4. Nhấn **OK**

Android Studio sẽ tự nhận file `settings.gradle.kts` và sync Gradle.

### Bước 3 — Cấu hình SDK (nếu cần)

Nếu Android Studio báo **"SDK location not found"**:

1. Mở **File → Project Structure** (hoặc `Ctrl+Alt+Shift+S`)
2. Chọn tab **SDK Location**
3. Mục **Android SDK location** → nhập đường dẫn SDK, ví dụ:
   ```
   C:\Users\<tên_máy>\AppData\Local\Android\Sdk
   ```
4. Nhấn **Apply → OK**

File `local.properties` ở thư mục gốc sẽ được cập nhật tự động.

### Bước 4 — Sync Gradle

Sau khi mở project, Android Studio sẽ tự sync. Nếu không:

- Nhấn nút **Sync Now** trên thanh thông báo vàng ở đầu màn hình, hoặc
- Chọn **File → Sync Project with Gradle Files**

Gradle sẽ tải toàn bộ dependencies (~200 MB lần đầu).

---

## Chạy ứng dụng

### Chạy trên thiết bị thật

1. Bật **USB Debugging** trên điện thoại:
   - **Cài đặt → Giới thiệu về điện thoại** → nhấn **Số phiên bản** 7 lần
   - Vào **Tuỳ chọn nhà phát triển** → bật **Gỡ lỗi qua USB**
2. Kết nối điện thoại qua USB
3. Chọn thiết bị trong dropdown ở thanh công cụ
4. Nhấn nút **Run** (▶) hoặc `Shift+F10`

### Chạy trên máy ảo (AVD)

1. Mở **Device Manager**: `View → Tool Windows → Device Manager`
2. Nhấn **Create Device**
3. Chọn thiết bị (ví dụ: Pixel 6) → chọn API 26+
4. Nhấn **Finish** → khởi động máy ảo
5. Nhấn **Run** (▶)

> **ML Kit Document Scanner** yêu cầu Google Play Services — dùng thiết bị thật hoặc AVD có Google APIs để tính năng quét tự động hoạt động. Nếu không có GMS, ứng dụng tự chuyển sang chế độ crop thủ công.

---

## Build APK

### Debug APK (dùng để test)

Trong Android Studio:
- **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- APK xuất ra tại: `app/build/outputs/apk/debug/app-debug.apk`

Hoặc dùng terminal tích hợp (`Alt+F12`):
```bash
./gradlew assembleDebug
```

### Release APK (dùng để phát hành)

```bash
./gradlew assembleRelease
```

> Release build yêu cầu keystore để ký APK. Xem [Ký APK](#ký-apk) bên dưới.

---

## Ký APK

Để tạo release APK có thể cài đặt:

1. **Build → Generate Signed Bundle / APK**
2. Chọn **APK** → Next
3. Tạo keystore mới hoặc chọn keystore đã có
4. Điền thông tin → Next → chọn **release** → Finish

APK đã ký xuất ra tại: `app/build/outputs/apk/release/app-release.apk`

---

## Chạy Tests

### Unit tests
```bash
./gradlew test
```
Hoặc trong Android Studio: click chuột phải vào thư mục `test/` → **Run Tests**

### Instrumented tests (cần thiết bị/máy ảo)
```bash
./gradlew connectedAndroidTest
```

---

## Cấu trúc dự án

```
android/
├── app/
│   └── src/main/java/com/docscanner/
│       ├── MyApplication.kt          # Application class, khởi tạo DI
│       ├── MainActivity.kt           # Entry point, setup Compose
│       ├── di/
│       │   └── AppContainer.kt       # Manual DI — wires tất cả dependencies
│       ├── domain/
│       │   ├── model/                # Document, Page data classes
│       │   └── usecase/              # Business logic (Save, Get, Export, Delete, Rename)
│       ├── data/
│       │   ├── local/db/             # Room database, DAO
│       │   ├── local/entity/         # Room entities
│       │   ├── local/filesystem/     # ImageStorage, ThumbnailGenerator
│       │   ├── pdf/                  # PdfGenerator (android.graphics.pdf)
│       │   └── repository/           # DocumentRepository interface + impl
│       ├── common/exceptions/        # Domain exceptions
│       └── ui/
│           ├── AppNavGraph.kt        # Navigation graph
│           ├── documentlist/         # Màn hình danh sách tài liệu
│           ├── scanner/              # Màn hình quét (ML Kit + manual crop)
│           ├── edit/                 # Chỉnh sửa trang (rotate, brightness, contrast)
│           ├── viewer/               # Xem tài liệu, reorder, export PDF
│           ├── settings/             # Màn hình cài đặt
│           ├── common/               # Shared UI utilities
│           └── theme/                # Material3 theme
├── gradle/
│   ├── libs.versions.toml            # Version catalog (tất cả dependencies)
│   └── wrapper/                      # Gradle wrapper 8.9
├── local.properties                  # Đường dẫn Android SDK (không commit)
├── build.gradle.kts                  # Root build config
├── settings.gradle.kts               # Project settings
└── gradle.properties                 # JVM args, Kotlin config
```

---

## Tính năng

| Tính năng | Mô tả |
|-----------|-------|
| Quét tài liệu (GMS) | ML Kit Document Scanner — tự động phát hiện góc, crop, edge detection |
| Quét thủ công | Camera + crop 4 góc bằng tay (fallback khi không có Google Play Services) |
| Chỉnh sửa trang | Xoay, điều chỉnh độ sáng/tương phản, chuyển grayscale, undo |
| Sắp xếp lại trang | Kéo thả để đổi thứ tự trang trong tài liệu |
| Xuất PDF | Tạo file PDF từ tất cả trang, chia sẻ qua FileProvider |
| Lưu trữ riêng tư | Tài liệu lưu trong `filesDir` — không cần quyền storage, không lộ ra ngoài |
| Giới hạn | Tối đa 100 tài liệu, 50 trang/tài liệu, cảnh báo khi dung lượng < 100 MB |

---

## Tech Stack

| Thành phần | Lựa chọn |
|-----------|----------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose 2.8.3 |
| Architecture | MVVM + Repository + Use Cases |
| Database | Room 2.6.1 (KSP) |
| Camera | CameraX 1.4.0 |
| Scanning | ML Kit Document Scanner 16.0.0-beta1 |
| Images | Coil 2.7.0 |
| Reorder | sh.calvin.reorderable 2.4.0 |
| DI | Manual (AppContainer) — không dùng Hilt/Koin |
| PDF | `android.graphics.pdf.PdfDocument` (built-in) |
| Build | Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21, KSP |

---

## Lưu ý

- **Tài liệu bị xóa khi gỡ cài đặt** — đây là thiết kế có chủ đích (lưu trữ riêng tư, không cần quyền storage). Ứng dụng hiển thị cảnh báo trong màn hình Settings.
- `local.properties` chứa đường dẫn SDK cục bộ — **không commit** file này lên Git.
- Debug APK được ký bằng debug key tự động, không cần cấu hình thêm.

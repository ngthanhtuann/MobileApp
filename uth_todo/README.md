# UTH Todo (Kotlin + Jetpack Compose + Firebase)

Một app gộp các bài tập về nhà (đề trong thư mục `pic/`, giao diện theo bộ thiết kế của thầy trong `UTH-Mobile/`):

| Đề | Nội dung trong app |
|---|---|
| Tuần 5 – BT01 | Firebase Authentication: Email/Mật khẩu, Google, Khách. Lỗi → hộp thoại báo lỗi. Thành công → Toast + vào Trang chủ. Nhấn avatar → Hồ sơ |
| Tuần 5 – BT02 | Storage, Cloud Messaging, Remote Config, Analytics, Crashlytics |
| Tuần 4 – Flow **Trang chủ** (UI + API) | Tiến độ hôm nay, thống kê Học tập / Cuộc sống, việc hôm nay |
| Tuần 4 – Flow **Danh sách** (UI + API) | Tìm kiếm, lọc theo nhóm, chia "Cần làm / Đã xong", nhãn "Gấp" |
| Tuần 4 – Flow **Chi tiết** (UI + API) | Hạn chót, nhắc nhở, ghi chú, việc nhỏ (thêm/sửa/xóa/tick), Đánh dấu hoàn thành, Sửa, Xóa công việc |
| Nâng cao 01 | Màn xin quyền Vị trí → Thông báo → Camera (Bật / Để sau), hiện 1 lần khi mở app lần đầu |
| Nâng cao 02 | Cài đặt › Giá crypto realtime: Retrofit + Coroutines, gọi song song 2 API CoinGecko, tự cập nhật 30 giây |

Theo bộ thiết kế `UTH-Mobile/`: Khởi động → Onboarding 3 bước (lần đầu) → Đăng nhập → Trang chủ / Danh sách / Chi tiết,
màn **Việc mới** (chọn ngày, giờ, nhắc, mức ưu tiên; dùng chung để Sửa), **Cài đặt** (Sáng / Tối / Tự động, màu chủ đạo, cỡ chữ, công tắc thông báo),
**Hồ sơ** (việc đã xong, chuỗi ngày liên tiếp, % đúng hạn, biểu đồ tuần, đồng bộ, quyền riêng tư, đăng xuất).

## Cấu trúc code (`app/src/main/java/com/example/bt2firebaseall/`)

Mỗi màn hình một file, đặt tên theo màn.

| File | Nội dung |
|---|---|
| `MainActivity.kt` | Activity, giao diện chung (`AppTheme`), các route và điều hướng giữa các màn |
| `Theme.kt` | Màu sắc, chế độ Sáng/Tối/Tự động, màu chủ đạo, cỡ chữ (đổi ở Cài đặt) |
| `Models.kt` | Nhóm (Học tập/Cuộc sống), mức ưu tiên, cách hiển thị ngày giờ và tên người dùng |
| `Components.kt` | Thành phần dùng lại: thanh trên cùng, thanh tab dưới, dòng công việc, avatar, nhãn... |
| `Api.kt` | Model `Task` (giống JSON của API trong đề), Retrofit `MockApi` (API của thầy), `CryptoApi` |
| `TaskRepository.kt` | Gọi API công việc: GET / GET theo id / POST / PATCH / DELETE |
| `FirebaseServices.kt` | Authentication, Storage, Remote Config, Analytics + Crashlytics, Cloud Messaging |
| `ProfileStats.kt` | Tính số liệu màn Hồ sơ (có unit test `StatsTest`) |
| `SplashScreen.kt`, `OnboardingScreen.kt`, `LoginScreen.kt` | Khởi động, Onboarding 3 bước, Đăng nhập |
| `HomeScreen.kt`, `ListScreen.kt`, `DetailScreen.kt`, `TaskEditScreen.kt` | Trang chủ, Danh sách, Chi tiết, Việc mới / Sửa |
| `NotificationsScreen.kt`, `SettingsScreen.kt`, `ProfileScreen.kt` | Thông báo (FCM), Cài đặt, Hồ sơ |
| `PermissionOnboarding.kt` | Nâng cao 01: xin quyền Vị trí → Thông báo → Camera |
| `CryptoScreen.kt` | Nâng cao 02: giá crypto realtime |

> Package / applicationId vẫn là `com.example.bt2firebaseall` vì app đã đăng ký với tên này trên Firebase (đổi tên phải đăng ký lại app và tải lại `google-services.json`).

## API công việc

Ba flow Trang chủ / Danh sách / Chi tiết gọi thẳng **mock API của thầy**
(`https://share.tryapps.store/mock/teach/`, mọi request kèm header `Authorization: Bearer TuanTruong-uth-student`):

| Lệnh | Đường dẫn | Dùng khi |
|---|---|---|
| GET | `/mock/teach/tasks` | Đăng nhập xong / mở Trang chủ, Danh sách |
| GET | `/mock/teach/tasks/{id}` | Mở Chi tiết, và tải lại sau mỗi lần sửa |
| POST | `/mock/teach/tasks` | Thêm việc (nút +) |
| PATCH | `/mock/teach/tasks/{id}` | Tick xong, sửa, việc nhỏ, nhắc nhở |
| DELETE | `/mock/teach/tasks/{id}` | Xóa công việc |

Mock luôn trả cùng một bộ mẫu (thêm/sửa/xóa không được lưu trên server), nên app giữ thay đổi trong bộ nhớ và phủ lên dữ liệu server.
Tắt app hoặc Cài đặt › Đặt lại dữ liệu mẫu là quay về dữ liệu mẫu, giống prototype của thầy.
Ngày trong mock cố định (20/10) nên được dời để ngày đầu tiên = hôm nay.

## Chạy

1. Android Studio › **Open** › chọn thư mục `uth_todo` › Sync Gradle.
2. Run trên máy ảo **có Google Play** (để đăng nhập Google) hoặc điện thoại thật.
3. File `app/google-services.json` đã có sẵn (project Firebase `uth-todo-week5`).

## Kiểm tra trên Firebase Console

| Dịch vụ | Trong app | Cách kiểm tra |
|---|---|---|
| Authentication | Màn đăng nhập | Console › Authentication › Users |
| Storage | Hồ sơ › nhấn avatar để chọn ảnh (`avatars/{uid}.jpg`) | Console › Storage (cần gói Blaze) |
| Cloud Messaging | Tab Thông báo › FCM token; app tự đăng ký chủ đề `uth_todo` | Console › Messaging › gửi tới token hoặc topic `uth_todo` |
| Remote Config | Dòng chữ nhỏ trong thẻ "Tiến độ hôm nay" | Console › Remote Config › key `home_message`, Publish, mở lại app |
| Analytics | Sự kiện `login`, `login_error`, `add_task` | Console › Analytics › DebugView (`adb shell setprop debug.firebase.analytics.app com.example.bt2firebaseall`) |
| Crashlytics | Hồ sơ › "Gửi crash thử" | App crash → mở lại → Console › Crashlytics |

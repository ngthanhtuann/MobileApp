# UTH Todo (Kotlin + Jetpack Compose + Firebase)

Một app gộp các bài tập về nhà (đề trong thư mục `pic/`):

| Đề | Nội dung trong app |
|---|---|
| Tuần 5 – BT01 | Firebase Authentication: Email/Mật khẩu, Google, Khách. Lỗi → hộp thoại báo lỗi. Thành công → Toast + vào Trang chủ. Nhấn avatar → Hồ sơ |
| Tuần 5 – BT02 | Realtime Database, Storage, Cloud Messaging, Remote Config, Analytics, Crashlytics |
| Tuần 4 – Flow **Trang chủ** (UI + API) | Tiến độ hôm nay, thống kê Học tập / Cuộc sống, việc hôm nay |
| Tuần 4 – Flow **Danh sách** (UI + API) | Tìm kiếm, lọc theo nhóm, chia "Cần làm / Đã xong", nhãn "Gấp" |
| Tuần 4 – Flow **Chi tiết** (UI + API) | Hạn chót, nhắc nhở, ghi chú, việc nhỏ (thêm/sửa/xóa/tick), Đánh dấu hoàn thành, Sửa, Xóa công việc |
| Nâng cao 01 | Màn xin quyền Vị trí → Thông báo → Camera (Bật / Để sau), hiện 1 lần khi mở app lần đầu |
| Nâng cao 02 | Cài đặt › Giá crypto realtime: Retrofit + Coroutines, gọi song song 2 API CoinGecko, tự cập nhật 30 giây |

Thêm: thanh điều hướng dưới (Trang chủ, Danh sách, +, Thông báo, Cài đặt), giao diện tối, đặt lại dữ liệu mẫu.

## Cấu trúc code (`app/src/main/java/com/example/bt2firebaseall/`)

| File | Nội dung |
|---|---|
| `MainActivity.kt` | Điều hướng + các màn: Đăng nhập, Trang chủ, Danh sách, Chi tiết, Hồ sơ, Thông báo, Cài đặt |
| `Api.kt` | Model `Task` (giống JSON của API trong đề), Retrofit `MockApi` (API của thầy), `TaskApi`, `CryptoApi` |
| `FirebaseServices.kt` | Auth, TaskRepository (gọi API + nghe realtime), Storage, Remote Config, Analytics, Crashlytics, FCM |
| `Advanced.kt` | Nâng cao 01 (màn xin quyền) và Nâng cao 02 (màn Crypto) |

> Package / applicationId vẫn là `com.example.bt2firebaseall` vì app đã đăng ký với tên này trên Firebase (đổi tên phải đăng ký lại app và tải lại `google-services.json`).

## API công việc

**Mock API của thầy** (`https://share.tryapps.store/mock/teach/tasks`, header `Authorization: Bearer TuanTruong-uth-student`) là nguồn dữ liệu mẫu:
tài khoản mới (hoặc Cài đặt › Đặt lại dữ liệu mẫu) sẽ gọi `GET /mock/teach/tasks` rồi lưu vào Firebase. Ngày trong mock cố định (20/10) nên được dời để ngày đầu tiên = hôm nay.

Mock luôn trả cùng một bộ mẫu (PATCH/POST/DELETE không lưu lại), nên các thao tác thêm/sửa/xóa dùng **REST API của Firebase Realtime Database** với cùng các lệnh:

| Lệnh | Đường dẫn | Dùng khi |
|---|---|---|
| GET | `/users/{uid}/tasks.json` | Mở Trang chủ / Danh sách |
| GET | `/users/{uid}/tasks/{id}.json` | Mở Chi tiết, và tải lại sau mỗi lần sửa |
| POST | `/users/{uid}/tasks.json` | Thêm việc (nút +) |
| PATCH | `/users/{uid}/tasks/{id}.json` | Tick xong, sửa, việc nhỏ, nhắc nhở |
| DELETE | `/users/{uid}/tasks/{id}.json` | Xóa công việc |

App cũng nghe realtime: sửa dữ liệu trên Firebase Console là app đổi ngay.

## Chạy

1. Android Studio › **Open** › chọn thư mục `uth_todo` › Sync Gradle.
2. Run trên máy ảo **có Google Play** (để đăng nhập Google) hoặc điện thoại thật.
3. File `app/google-services.json` đã có sẵn (project Firebase `uth-todo-week5`).

## Kiểm tra trên Firebase Console

| Dịch vụ | Trong app | Cách kiểm tra |
|---|---|---|
| Authentication | Màn đăng nhập | Console › Authentication › Users |
| Realtime Database | Việc lưu ở `users/{uid}/tasks` | Console › Realtime Database, sửa dữ liệu thấy app đổi ngay |
| Storage | Hồ sơ › nhấn avatar để chọn ảnh (`avatars/{uid}.jpg`) | Console › Storage (cần gói Blaze) |
| Cloud Messaging | Tab Thông báo › FCM token; app tự đăng ký chủ đề `uth_todo` | Console › Messaging › gửi tới token hoặc topic `uth_todo` |
| Remote Config | Dòng chữ nhỏ trong thẻ "Tiến độ hôm nay" | Console › Remote Config › key `home_message`, Publish, mở lại app |
| Analytics | Sự kiện `login`, `login_error`, `add_task` | Console › Analytics › DebugView (`adb shell setprop debug.firebase.analytics.app com.example.bt2firebaseall`) |
| Crashlytics | Hồ sơ › "Gửi crash thử" | App crash → mở lại → Console › Crashlytics |

### Rules Realtime Database

```json
{
  "rules": {
    "users": { "$uid": { ".read": "auth != null && auth.uid === $uid", ".write": "auth != null && auth.uid === $uid" } }
  }
}
```

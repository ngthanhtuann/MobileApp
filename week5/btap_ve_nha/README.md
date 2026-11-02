# Week 5 – Bài tập về nhà Firebase (Kotlin + Jetpack Compose)

| Thư mục | Đề | Nội dung |
|---|---|---|
| `bt1_firebase_auth` | BT về nhà 01 | Firebase Authentication: Email/Mật khẩu, Google, Khách (ẩn danh). Lỗi → hộp thoại báo lỗi. Thành công → Toast + vào Trang chủ. Nhấn avatar/tên ở Trang chủ → Hồ sơ cá nhân, có Đăng xuất. |
| `bt2_firebase_all` | BT về nhà 02 | Tất cả của BT1 + Realtime Database, Storage, Cloud Messaging, Remote Config, Analytics, Crashlytics. |

> Cả 2 project **cần file `google-services.json` của bạn** thì mới build được (Firebase bắt buộc).

## 1. Tạo project Firebase (làm 1 lần, dùng chung cho cả 2 bài)

1. Vào https://console.firebase.google.com → **Add project**.
2. Thêm **2 ứng dụng Android** vào cùng project:
   - Package `com.example.bt1firebaseauth`
   - Package `com.example.bt2firebaseall`
3. Thêm **SHA-1** cho cả 2 app (bắt buộc cho Google Sign-In). Lấy SHA-1 bằng lệnh (trong thư mục project):
   ```
   gradlew signingReport
   ```
   hoặc trong Android Studio: Gradle › app › Tasks › android › signingReport. Copy dòng `SHA1` của biến thể `debug`.
4. **Authentication › Sign-in method**: bật **Email/Password**, **Google**, **Anonymous**.
5. (BT2) **Realtime Database › Create database** (chọn test mode, hoặc dùng rules ở dưới).
6. (BT2) **Storage › Get started**. *Lưu ý: project mới cần gói Blaze mới dùng được Storage; nếu không bật, app vẫn chạy, chỉ báo lỗi khi đổi avatar.*
7. **Sau khi làm xong các bước trên**, tải `google-services.json` (Project settings › Your apps) và chép vào:
   - `bt1_firebase_auth/app/google-services.json`
   - `bt2_firebase_all/app/google-services.json`

   (1 file chứa cả 2 app, chép giống nhau vào 2 chỗ. Nếu bật Google/Database sau khi tải thì **tải lại** file.)

## 2. Chạy

Android Studio › **Open** › chọn `bt1_firebase_auth` (hoặc `bt2_firebase_all`) › Sync Gradle › Run trên máy ảo **có Google Play** (để đăng nhập Google) hoặc điện thoại thật.

## 3. Thử BT1

| Thao tác | Kết quả |
|---|---|
| Bấm "Đăng nhập bằng Email" khi bỏ trống / sai mật khẩu | Hộp thoại **Đăng nhập thất bại** + lý do |
| Nhập email + mật khẩu ≥ 6 ký tự → "Đăng ký bằng email này" | Toast **Đăng nhập thành công** → Trang chủ |
| "Tiếp tục với Google" | Chọn tài khoản → Trang chủ (hủy → báo lỗi) |
| "Dùng thử không cần tài khoản" | Vào Trang chủ với tên "Khách" |
| Nhấn avatar/tên trên Trang chủ | Màn **Hồ sơ**: tên, email, kiểu tài khoản, thống kê, Đăng xuất |

## 4. Thử BT2 – từng dịch vụ Firebase

| Dịch vụ | Trong app | Cách kiểm tra |
|---|---|---|
| Authentication | Màn đăng nhập (giống BT1) | Console › Authentication › Users |
| Realtime Database | Việc lưu ở `users/{uid}/tasks`, tick/thêm (nút **+**) đồng bộ realtime | Console › Realtime Database, sửa dữ liệu thấy app đổi ngay |
| Storage | Hồ sơ › nhấn avatar để chọn ảnh, lưu ở `avatars/{uid}.jpg` | Console › Storage |
| Cloud Messaging | Chuông ở Trang chủ › xem FCM token; app tự đăng ký chủ đề `uth_todo` | Console › Messaging › New campaign › gửi tới token hoặc topic `uth_todo` |
| Remote Config | Dòng chữ nhỏ trong thẻ "Tiến độ hôm nay" | Console › Remote Config › thêm key `home_message`, Publish, mở lại app (cache 60s) |
| Analytics | Sự kiện `login`, `login_error`, `add_task` | Console › Analytics › DebugView (chạy `adb shell setprop debug.firebase.analytics.app com.example.bt2firebaseall`) |
| Crashlytics | Hồ sơ › "Gửi crash thử (Crashlytics)" | App crash → mở lại app → Console › Crashlytics (vài phút) |

Không làm: Hosting (dành cho web), Test Lab (công cụ trên Console), App Indexing / Dynamic Links / Invites (Google đã ngừng), AdWords / AdMob (cần tài khoản quảng cáo riêng).

### Rules gợi ý

Realtime Database:
```json
{
  "rules": {
    "users": { "$uid": { ".read": "auth.uid === $uid", ".write": "auth.uid === $uid" } }
  }
}
```

Storage:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /avatars/{file} { allow read, write: if request.auth != null && file == request.auth.uid + '.jpg'; }
  }
}
```

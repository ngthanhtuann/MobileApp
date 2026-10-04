# Bài tập 1 — Giao diện hồ sơ sinh viên

## Mô tả bài tập

Xây dựng giao diện hồ sơ sinh viên trên ứng dụng Flutter, gồm ảnh đại diện, họ tên, mã số sinh viên và các nút quay lại/chỉnh sửa.

## Mục tiêu

- Làm quen với cách tổ chức giao diện bằng widget Flutter.
- Sử dụng `Scaffold`, `Card`, `Column`, `Row`, `CircleAvatar` và `Text` để bố trí nội dung.
- Tạo bố cục đơn giản, dễ nhìn và phù hợp với màn hình điện thoại.

## Kết quả đạt được

Ứng dụng hiển thị thẻ hồ sơ với avatar, tên **Nguyễn Thanh Tuấn**, MSSV **070206001860** và các biểu tượng điều hướng/chỉnh sửa. Dự án có thể chạy trên Android và Web bằng Flutter.

## Giải thích các hàm chính

- `main()`: điểm bắt đầu của ứng dụng, gọi `runApp` để hiển thị widget gốc.
- `BaiTapApp.build()`: tạo `MaterialApp`, cấu hình giao diện và chọn `StudentProfileScreen` làm màn hình đầu tiên.
- `StudentProfileScreen.build()`: dựng màn hình hồ sơ, căn giữa thẻ thông tin và sắp xếp avatar, tên, MSSV cùng các nút bằng widget Flutter.

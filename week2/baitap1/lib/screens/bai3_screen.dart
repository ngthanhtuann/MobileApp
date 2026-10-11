import 'package:flutter/material.dart';

// Bài 2 (BTVN): Câu lệnh if / else

// Bước 1: biến điểm có giá trị 8.5
double diem = 8.5;

// Các điểm dùng cho phần "Kiểm tra thêm"
List<double> diemKiemTra = [9, 7, 5.5, 3, 11];

// Bước 2: hàm nhận vào điểm và trả về xếp loại
String xepLoai(double diem) {
  if (diem < 0 || diem > 10) {
    return 'Không hợp lệ';
  } else if (diem >= 8.0) {
    return 'Giỏi';
  } else if (diem >= 6.5) {
    return 'Khá';
  } else if (diem >= 5.0) {
    return 'Trung bình';
  } else {
    return 'Yếu';
  }
}

// Bước 3: màu chữ của xếp loại cũng chọn bằng if
Color mauXepLoai(String loai) {
  if (loai == 'Giỏi') {
    return Colors.green.shade700;
  } else if (loai == 'Khá') {
    return Colors.blue.shade800;
  } else if (loai == 'Trung bình') {
    return Colors.orange.shade800;
  } else if (loai == 'Yếu') {
    return Colors.red.shade700;
  } else {
    return Colors.grey.shade600;
  }
}

// Đổi điểm thành chữ: 9.0 -> "9", 5.5 -> "5.5"
String hienDiem(double diem) {
  if (diem == diem.toInt()) {
    return diem.toInt().toString();
  }
  return diem.toString();
}

class Bai3Screen extends StatelessWidget {
  const Bai3Screen({super.key});

  // Tạo 1 dòng trong phần "Kiểm tra thêm", ví dụ: "Điểm 9 ... Giỏi"
  Widget taoDong(double diem) {
    String loai = xepLoai(diem);
    Color mau = mauXepLoai(loai);

    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text('Điểm ${hienDiem(diem)}'),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: mau.withAlpha(30),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              loai,
              style: TextStyle(
                fontSize: 12,
                color: mau,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Xếp loại và màu của điểm chính (8.5)
    String loai = xepLoai(diem);
    Color mau = mauXepLoai(loai);

    // Bước 4: gọi hàm với các điểm 9, 7, 5.5, 3 và 11
    List<Widget> cacDong = [];
    for (int i = 0; i < diemKiemTra.length; i++) {
      cacDong.add(taoDong(diemKiemTra[i]));

      // Đường kẻ ngăn cách giữa các dòng (dòng cuối không cần)
      if (i < diemKiemTra.length - 1) {
        cacDong.add(Container(height: 1, color: Colors.grey.shade200));
      }
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF3F4F6),
      appBar: AppBar(
        title: const Text('Xếp loại học lực'),
        backgroundColor: const Color(0xFF2D4373),
        foregroundColor: Colors.white,
      ),
      body: Container(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Thẻ điểm trung bình
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Column(
                children: [
                  const Text(
                    'Điểm trung bình',
                    style: TextStyle(fontSize: 13, color: Colors.grey),
                  ),
                  Text(
                    hienDiem(diem),
                    style: const TextStyle(
                      fontSize: 48,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: mau.withAlpha(30),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      loai,
                      style: TextStyle(
                        fontSize: 20,
                        color: mau,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 20),
            const Text(
              'KIỂM TRA THÊM',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),

            // Thẻ chứa các dòng kiểm tra thêm
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Column(children: cacDong),
            ),
          ],
        ),
      ),
    );
  }
}

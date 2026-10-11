import 'package:flutter/material.dart';

// Dùng lại hàm xepLoai() và mauXepLoai() đã viết ở Bài 2
import 'bai3_screen.dart';

// Bài 3 (BTVN): Tổng hợp for + if + nullable

// Bước 1: class Student
// double? -> điểm có thể bị null (chưa có điểm)
class Student {
  String name;
  double? score;

  Student(this.name, this.score);
}

// Dữ liệu: Chi chưa có điểm nên score là null
List<Student> students = [
  Student('Nguyễn Văn An', 9.0),
  Student('Trần Thị Bình', 7.2),
  Student('Lê Minh Chi', null),
  Student('Phạm Quốc Dũng', 4.5),
  Student('Võ Thanh Em', 5.8),
];

const Color mauXanh = Color(0xFF2D4373);

class Bai4Screen extends StatelessWidget {
  const Bai4Screen({super.key});

  // Tạo 1 ô thống kê nhỏ, ví dụ: "Giỏi" ở trên, "1" ở dưới
  Widget taoThongKe(String nhan, String giaTri) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(nhan, style: const TextStyle(fontSize: 11, color: Colors.white70)),
        const SizedBox(height: 2),
        Text(
          giaTri,
          style: const TextStyle(
            fontSize: 16,
            color: Colors.white,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }

  // Tạo thẻ của 1 sinh viên: tên, xếp loại, điểm
  Widget taoThe(Student sv) {
    double? score = sv.score;

    String loai;
    Color mau;
    String chuDiem;

    // Bước 2: nếu score == null thì hiện "Chưa có điểm"
    if (score == null) {
      loai = 'Chưa có điểm';
      mau = Colors.grey;
      chuDiem = '—';
    } else {
      loai = xepLoai(score);
      mau = mauXepLoai(loai);
      chuDiem = score.toStringAsFixed(1);
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                sv.name,
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                loai,
                style: TextStyle(
                  fontSize: 12,
                  color: mau,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          Text(
            chuDiem,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Bước 3: dùng for + if để tính các số liệu thống kê
    double tongDiem = 0;
    int soCoDiem = 0;
    int soGioi = 0;
    int soChuaCoDiem = 0;
    double caoNhat = 0;

    for (int i = 0; i < students.length; i++) {
      double? score = students[i].score;

      if (score == null) {
        // Chưa có điểm: chỉ đếm, không cộng vào tổng
        soChuaCoDiem++;
      } else {
        tongDiem = tongDiem + score;
        soCoDiem++;

        if (xepLoai(score) == 'Giỏi') {
          soGioi++;
        }
        if (score > caoNhat) {
          caoNhat = score;
        }
      }
    }

    // Điểm trung bình = tổng điểm / số người có điểm
    double trungBinh = 0;
    if (soCoDiem > 0) {
      trungBinh = tongDiem / soCoDiem;
    }

    // Bước 2: dùng for để tạo các thẻ sinh viên
    List<Widget> danhSachThe = [];
    for (int i = 0; i < students.length; i++) {
      danhSachThe.add(taoThe(students[i]));
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF3F4F6),
      appBar: AppBar(
        title: const Text('Bảng điểm lớp'),
        backgroundColor: mauXanh,
        foregroundColor: Colors.white,
      ),
      body: Container(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Thẻ thống kê của cả lớp
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: mauXanh,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Điểm trung bình lớp',
                    style: TextStyle(fontSize: 12, color: Colors.white70),
                  ),
                  Text(
                    trungBinh.toStringAsFixed(2),
                    style: const TextStyle(
                      fontSize: 34,
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Container(height: 1, color: Colors.white24),
                  const SizedBox(height: 10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      taoThongKe('Giỏi', '$soGioi'),
                      taoThongKe('Chưa có điểm', '$soChuaCoDiem'),
                      taoThongKe('Cao nhất', caoNhat.toStringAsFixed(1)),
                    ],
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),
            Text(
              'DANH SÁCH · ${students.length} SINH VIÊN',
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),

            // Các thẻ sinh viên
            Column(children: danhSachThe),
          ],
        ),
      ),
    );
  }
}

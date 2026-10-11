import 'package:flutter/material.dart';

// Bài 1 (BTVN): Vòng lặp for và kiểu nullable

// Bước 1: class Student
// String  -> bắt buộc phải có giá trị
// String? -> có thể bị null (thiếu dữ liệu)
class Student {
  String name;
  String? email;
  String? phone;

  Student(this.name, this.email, this.phone);
}

// Bước 2: list 5 sinh viên, vài người thiếu email hoặc số điện thoại (null)
List<Student> students = [
  Student('Nguyễn Văn An', 'an@sv.edu.vn', '0901 234 567'),
  Student('Trần Thị Bình', null, '0912 888 999'),
  Student('Lê Minh Chi', 'chi@sv.edu.vn', null),
  Student('Phạm Quốc Dũng', null, null),
  Student('Võ Thanh Em', 'em@sv.edu.vn', '0933 111 222'),
];

// Màu dùng trong bài
const Color mauXanh = Color(0xFF2D4373);
const Color mauDo = Color(0xFFC0392B);
const Color mauXam = Color(0xFF6B7280);

class Bai2Screen extends StatelessWidget {
  const Bai2Screen({super.key});

  // Tạo 1 dòng thông tin, ví dụ: "Email: an@sv.edu.vn"
  Widget taoDong(String nhan, String? giaTri) {
    // Bước 4: nếu giaTri bị null thì lấy chữ "Chưa cập nhật"
    String noiDung = giaTri ?? 'Chưa cập nhật';

    // Dữ liệu bị thiếu thì hiện chữ đỏ, in nghiêng
    TextStyle kieuChu = const TextStyle(fontSize: 13, color: mauXam);
    if (giaTri == null) {
      kieuChu = const TextStyle(
        fontSize: 13,
        color: mauDo,
        fontStyle: FontStyle.italic,
      );
    }

    return Row(
      children: [
        Text('$nhan: ', style: const TextStyle(fontSize: 13, color: mauXam)),
        Text(noiDung, style: kieuChu),
      ],
    );
  }

  // Tạo thẻ của 1 sinh viên
  Widget taoThe(Student sv) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            sv.name,
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 6),
          taoDong('Email', sv.email),
          const SizedBox(height: 2),
          taoDong('SĐT', sv.phone),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Bước 3: dùng for để tạo các thẻ sinh viên
    List<Widget> danhSachThe = [];
    for (int i = 0; i < students.length; i++) {
      danhSachThe.add(taoThe(students[i]));
    }

    // Bước 5: dùng for để đếm số sinh viên thiếu email
    int thieuEmail = 0;
    for (int i = 0; i < students.length; i++) {
      if (students[i].email == null) {
        thieuEmail++;
      }
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF3F4F6),
      appBar: AppBar(
        title: const Text('Danh sách sinh viên'),
        backgroundColor: mauXanh,
        foregroundColor: Colors.white,
      ),
      body: Container(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            // Các thẻ sinh viên
            Column(children: danhSachThe),

            // Dòng tổng kết ở cuối danh sách
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFE5EAF2),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      const Text('Tổng: '),
                      Text(
                        '${students.length} sinh viên',
                        style: const TextStyle(
                          color: mauXanh,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  Row(
                    children: [
                      const Text('Thiếu email: '),
                      Text(
                        '$thieuEmail',
                        style: const TextStyle(
                          color: mauDo,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

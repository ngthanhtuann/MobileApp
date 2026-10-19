import 'package:flutter/material.dart';

const Color mauXanh = Color(0xFF2196F3);
const String logoUrl =
    'https://giaothongvantaitphcm.edu.vn/wp-content/uploads/2025/01/Logo-GTVT.png';

// Khung chung cho các màn hình chi tiết: tiêu đề xanh + nút back
Widget khungChiTiet(String tieuDe, Widget body) {
  return Scaffold(
    backgroundColor: Colors.white,
    appBar: AppBar(
      title: Text(tieuDe, style: const TextStyle(color: mauXanh)),
      centerTitle: true,
      backgroundColor: Colors.white,
      foregroundColor: mauXanh,
    ),
    body: body,
  );
}

// Text Detail: 1 câu với nhiều kiểu chữ khác nhau
class TextDetailScreen extends StatelessWidget {
  const TextDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Text Detail',
      Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: RichText(
            text: const TextSpan(
              style: TextStyle(fontSize: 28, color: Colors.black, height: 1.6),
              children: [
                TextSpan(text: 'The '),
                TextSpan(
                  text: 'quick',
                  style: TextStyle(decoration: TextDecoration.lineThrough),
                ),
                TextSpan(
                  text: ' Brown\n',
                  style: TextStyle(fontSize: 36, color: Color(0xFFB8860B)),
                ),
                TextSpan(text: 'fox '),
                TextSpan(text: 'jumps', style: TextStyle(letterSpacing: 8)),
                TextSpan(
                  text: ' over\n',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontStyle: FontStyle.italic,
                  ),
                ),
                TextSpan(
                  text: 'the',
                  style: TextStyle(decoration: TextDecoration.underline),
                ),
                TextSpan(
                  text: ' lazy',
                  style: TextStyle(
                    fontFamily: 'serif',
                    fontStyle: FontStyle.italic,
                  ),
                ),
                TextSpan(text: ' dog.'),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// Images: 1 ảnh từ mạng (link) + 1 ảnh trong app (assets)
class ImageDetailScreen extends StatelessWidget {
  const ImageDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Images',
      ListView(
        padding: const EdgeInsets.all(24),
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: Image.network(
              logoUrl,
              errorBuilder: (_, _, _) =>
                  const Icon(Icons.broken_image, size: 80),
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            logoUrl,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 12,
              decoration: TextDecoration.underline,
            ),
          ),
          const SizedBox(height: 24),
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: Image.asset('assets/uth.jpg', fit: BoxFit.cover),
          ),
          const SizedBox(height: 8),
          const Text('In app', textAlign: TextAlign.center),
        ],
      ),
    );
  }
}

// TextField: gõ tới đâu, dòng chữ đỏ bên dưới cập nhật tới đó
class TextFieldDetailScreen extends StatefulWidget {
  const TextFieldDetailScreen({super.key});

  @override
  State<TextFieldDetailScreen> createState() => _TextFieldDetailScreenState();
}

class _TextFieldDetailScreenState extends State<TextFieldDetailScreen> {
  String noiDung = '';

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'TextField',
      Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                decoration: const InputDecoration(
                  hintText: 'Thông tin nhập',
                  border: OutlineInputBorder(),
                ),
                onChanged: (v) => setState(() => noiDung = v),
              ),
              const SizedBox(height: 16),
              Text(
                noiDung.isEmpty
                    ? 'Tự động cập nhật dữ liệu theo textfield'
                    : noiDung,
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 8),
              Text(
                'Số ký tự: ${noiDung.length}',
                style: const TextStyle(color: Colors.grey),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// PasswordField: ẩn mật khẩu, bấm icon con mắt để hiện/ẩn
class PasswordDetailScreen extends StatefulWidget {
  const PasswordDetailScreen({super.key});

  @override
  State<PasswordDetailScreen> createState() => _PasswordDetailScreenState();
}

class _PasswordDetailScreenState extends State<PasswordDetailScreen> {
  bool an = true;

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'PasswordField',
      Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: TextField(
            obscureText: an,
            decoration: InputDecoration(
              hintText: 'Mật khẩu',
              border: const OutlineInputBorder(),
              suffixIcon: IconButton(
                icon: Icon(an ? Icons.visibility : Icons.visibility_off),
                onPressed: () => setState(() => an = !an),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// Ô vuông màu xanh dùng cho Column / Row
Widget oVuong(Color mau) {
  return Container(
    width: 70,
    height: 50,
    margin: const EdgeInsets.all(6),
    decoration: BoxDecoration(
      color: mau,
      borderRadius: BorderRadius.circular(10),
    ),
  );
}

const Color xanhNhat = Color(0xFF9DB9F5);
const Color xanhDam = Color(0xFF4F86EC);

// Column Layout: các phần tử xếp theo chiều dọc
class ColumnDetailScreen extends StatelessWidget {
  const ColumnDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Column Layout',
      Center(
        child: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: const Color(0xFFF2F4F7),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [oVuong(xanhNhat), oVuong(xanhDam), oVuong(xanhNhat)],
          ),
        ),
      ),
    );
  }
}

// Row Layout: 4 hàng, mỗi hàng 3 ô xếp theo chiều ngang
class RowDetailScreen extends StatelessWidget {
  const RowDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Row Layout',
      Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            for (int i = 0; i < 4; i++)
              Container(
                margin: const EdgeInsets.only(bottom: 12),
                decoration: BoxDecoration(
                  color: const Color(0xFFF2F4F7),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    oVuong(xanhNhat),
                    oVuong(xanhDam),
                    oVuong(xanhNhat),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

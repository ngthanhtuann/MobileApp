import 'package:flutter/material.dart';

import 'bai2_screen.dart';
import 'bai3_screen.dart';
import 'bai4_screen.dart';
import 'btap_trenlop.dart';
import 'thuc_hanh_03.dart';

// Màn hình chính: chọn 1 trong 5 bài để mở
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Bài tập Week 2'),
      ),

      body: ListView(
        padding: const EdgeInsets.all(16),
        children: const [
          _BaiTapTile(title: 'Bài tập trên lớp', screen: Bai1Screen()),
          _BaiTapTile(title: 'Thực hành 03', screen: ThucHanh03Screen()),
          _BaiTapTile(title: 'Bài 2 - Danh sách sinh viên', screen: Bai2Screen()),
          _BaiTapTile(title: 'Bài 3 - Xếp loại học lực', screen: Bai3Screen()),
          _BaiTapTile(title: 'Bài 4 - Bảng điểm lớp', screen: Bai4Screen()),
        ],
      ),
    );
  }
}

// Một dòng trong danh sách, bấm vào để mở màn hình của bài tương ứng
class _BaiTapTile extends StatelessWidget {
  const _BaiTapTile({required this.title, required this.screen});

  final String title;
  final Widget screen;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        title: Text(title),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => screen),
          );
        },
      ),
    );
  }
}

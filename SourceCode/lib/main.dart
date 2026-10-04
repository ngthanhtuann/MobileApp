import 'package:flutter/material.dart';

void main() => runApp(const BaiTapApp());

class BaiTapApp extends StatelessWidget {
  const BaiTapApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'Hồ sơ sinh viên',
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFB84B45)),
          scaffoldBackgroundColor: Colors.white,
          fontFamily: 'Roboto',
        ),
        home: const StudentProfileScreen(),
      );
}

class StudentProfileScreen extends StatelessWidget {
  const StudentProfileScreen({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: const Color(0xFFF3F6FA),
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 420),
                child: Card(
                  elevation: 3,
                  color: Colors.white,
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(24)),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Row(
                          children: [
                            IconButton(
                              onPressed: () {},
                              icon: const Icon(Icons.arrow_back),
                              tooltip: 'Quay lại',
                            ),
                            const Spacer(),
                            IconButton(
                              onPressed: () {},
                              icon: const Icon(Icons.edit_outlined),
                              tooltip: 'Chỉnh sửa hồ sơ',
                            ),
                          ],
                        ),
                        const SizedBox(height: 34),
                        const CircleAvatar(
                          radius: 66,
                          backgroundColor: Color(0xFFDCE8F4),
                          child: Icon(Icons.person,
                              size: 92, color: Color(0xFF62758A)),
                        ),
                        const SizedBox(height: 24),
                        const Text(
                          'Nguyễn Thanh Tuấn',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                              fontSize: 23, fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 8),
                        const Text(
                          '070206001860',
                          style: TextStyle(fontSize: 17, color: Colors.black54),
                        ),
                        const SizedBox(height: 34),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      );
}

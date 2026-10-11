import 'package:flutter/material.dart';

// Bài 1
class Bai1Screen extends StatelessWidget {
  const Bai1Screen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor:Colors.white,

      body: SafeArea(
        child: Center(
          child: Column(
            children: [

              // =================================
              // 1. KHOẢNG TRỐNG PHÍA TRÊN
              // =================================

              const SizedBox(
                height: 165,
              ),

              // =================================
              // 2. KHUNG AVATAR
              // =================================

              SizedBox(
                width: 140,
                height: 140,



                child: Padding(
                  padding: const EdgeInsets.all(12),

                  child: Image.asset(
                    'assets/avatar.png',
                    fit: BoxFit.cover,
                  ),
                ),
              ),

              // =================================
              // 3. KHOẢNG CÁCH
              // =================================

              const SizedBox(
                height: 8,
              ),

              // =================================
              // 4. KHUNG THÔNG TIN
              // =================================

              SizedBox(
                width: 275,




                child: Column(
                  children: [

                    // -----------------------------
                    // TÊN
                    // -----------------------------

                    SizedBox(
                      height: 40,

                      child: Center(
                        child: Text(
                          'Nguyễn Thanh Tuấn - 070206001860',

                          style: const TextStyle(
                            fontSize: 14,
                            color: Colors.black,
                          ),
                        ),
                      ),
                    ),

                    // -----------------------------
                    // CÂU HỎI
                    // -----------------------------

                    SizedBox(
                      height: 68,

                      child: Center(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                          ),

                          child: Text(
                            'Mong muốn và định hướng của Bạn là gì\n'
                            'sau khi học xong môn học là gì?',

                            textAlign: TextAlign.center,

                            style: const TextStyle(
                              fontSize: 14,
                              color: Colors.grey,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';

class ThucHanh03Screen extends StatefulWidget {
  const ThucHanh03Screen({super.key});

  @override
  State<ThucHanh03Screen> createState() => _ThucHanh03ScreenState();
}

class _ThucHanh03ScreenState extends State<ThucHanh03Screen> {
  // Biến quản lý nhập liệu và trạng thái
  final TextEditingController _num1Controller = TextEditingController();
  final TextEditingController _num2Controller = TextEditingController();
  String? _selectedOp;
  String _result = '';

  // Hàm tính toán tự động
  void _calculate() {
    double? num1 = double.tryParse(_num1Controller.text);
    double? num2 = double.tryParse(_num2Controller.text);

    if (num1 == null || num2 == null || _selectedOp == null) {
      setState(() => _result = '');
      return;
    }

    double res = 0;
    if (_selectedOp == '+') res = num1 + num2;
    if (_selectedOp == '-') res = num1 - num2;
    if (_selectedOp == '*') res = num1 * num2;
    if (_selectedOp == '/') {
      if (num2 == 0) {
        setState(() => _result = 'Không thể chia cho 0');
        return;
      }
      res = num1 / num2;
    }

    setState(() {
      _result = res % 1 == 0 ? res.toInt().toString() : res.toString();
    });
  }

  // Widget tạo nút phép toán
  Widget _buildOpButton(String op, Color color) {
    bool isSelected = _selectedOp == op;
    return InkWell(
      onTap: () {
        _selectedOp = op;
        _calculate();
      },
      child: Container(
        width: 50,
        height: 50,
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(8),
          border: isSelected ? Border.all(color: Colors.black, width: 3) : null,
        ),
        child: Center(
          child: Text(
            op,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 22,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _num1Controller.dispose();
    _num2Controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Center(
                child: Text(
                  'Thực hành 03',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ),
              const SizedBox(height: 30),

              // Ô nhập số thứ 1
              TextField(
                controller: _num1Controller,
                keyboardType: TextInputType.number,
                onChanged: (_) => _calculate(),
                decoration: InputDecoration(
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
              const SizedBox(height: 15),

              // 4 nút chọn phép toán
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _buildOpButton('+', Colors.redAccent),
                  _buildOpButton('-', Colors.amber[700]!),
                  _buildOpButton('*', Colors.indigoAccent),
                  _buildOpButton('/', Colors.black87),
                ],
              ),
              const SizedBox(height: 15),

              // Ô nhập số thứ 2
              TextField(
                controller: _num2Controller,
                keyboardType: TextInputType.number,
                onChanged: (_) => _calculate(),
                decoration: InputDecoration(
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
              const SizedBox(height: 25),

              // Hiển thị kết quả
              Text(
                'Kết quả: $_result',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
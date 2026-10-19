import 'package:flutter/material.dart';

import 'detail_screens.dart';
import 'more_screens.dart';

// Màn hình 2: danh sách các thành phần UI, bấm vào để xem chi tiết
class ComponentsScreen extends StatelessWidget {
  const ComponentsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(
          'UI Components List',
          style: TextStyle(color: mauXanh),
        ),
        centerTitle: true,
        backgroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: const [
          _Nhom('Display'),
          _Muc('Text', 'Displays text', TextDetailScreen()),
          _Muc('Image', 'Displays an image', ImageDetailScreen()),
          _Nhom('Input'),
          _Muc('TextField', 'Input field for text', TextFieldDetailScreen()),
          _Muc(
            'PasswordField',
            'Input field for passwords',
            PasswordDetailScreen(),
          ),
          _Muc('Toggle', 'On/off switch', ToggleDetailScreen()),
          _Muc('Slider', 'Selects a value from a range', SliderDetailScreen()),
          _Muc(
            'DatePicker',
            'Selects a date and time',
            DatePickerDetailScreen(),
          ),
          _Nhom('Layout'),
          _Muc('Column', 'Arranges elements vertically', ColumnDetailScreen()),
          _Muc('Row', 'Arranges elements horizontally', RowDetailScreen()),
          _Nhom('Selection'),
          _Muc(
            'Button',
            'Triggers an action when tapped',
            ButtonDetailScreen(),
          ),
          _Muc(
            'Checkbox',
            'Selects one or multiple options',
            CheckboxDetailScreen(),
          ),
          _Muc('RadioButton', 'Selects a single option', RadioDetailScreen()),
          _Muc('Picker', 'Selects a value from a list', PickerDetailScreen()),
          _Nhom('Navigation'),
          _Muc(
            'NavigationView',
            'Navigation container with title and back button',
            NavigationDetailScreen(),
          ),
          _Muc(
            'TabBar',
            'Switches between main sections',
            TabBarDetailScreen(),
          ),
          _Muc(
            'Menu',
            'Shows a menu with multiple actions',
            MenuDetailScreen(),
          ),
          _Nhom('Content'),
          _Muc('List', 'Displays a list of items', ListDetailScreen()),
          _Muc('Card', 'Container with content and style', CardDetailScreen()),
          _Muc(
            'Divider',
            'Separates content into sections',
            DividerDetailScreen(),
          ),
        ],
      ),
    );
  }
}

// Tiêu đề nhóm: Display, Input, Layout, Selection, ...
class _Nhom extends StatelessWidget {
  const _Nhom(this.ten);
  final String ten;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 12, bottom: 6),
      child: Text(ten, style: const TextStyle(fontWeight: FontWeight.w600)),
    );
  }
}

// Một thẻ thành phần, bấm vào thì chuyển sang màn hình chi tiết
class _Muc extends StatelessWidget {
  const _Muc(this.ten, this.moTa, this.screen);

  final String ten;
  final String moTa;
  final Widget screen;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: const Color(0xFFBBDEFB),
      elevation: 0,
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        title: Text(ten, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(moTa),
        onTap: () =>
            Navigator.push(context, MaterialPageRoute(builder: (_) => screen)),
      ),
    );
  }
}

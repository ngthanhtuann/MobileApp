import 'package:flutter/material.dart';

import 'detail_screens.dart';

// Các thành phần UI thêm (lấy từ phần "Tự tìm hiểu" trong đề)

// ===================== INPUT =====================

// Toggle: công tắc bật / tắt
class ToggleDetailScreen extends StatefulWidget {
  const ToggleDetailScreen({super.key});

  @override
  State<ToggleDetailScreen> createState() => _ToggleDetailScreenState();
}

class _ToggleDetailScreenState extends State<ToggleDetailScreen> {
  bool wifi = true;
  bool denPin = false;

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Toggle',
      ListView(
        padding: const EdgeInsets.all(24),
        children: [
          SwitchListTile(
            title: const Text('Wi-Fi'),
            secondary: const Icon(Icons.wifi),
            value: wifi,
            onChanged: (v) => setState(() => wifi = v),
          ),
          SwitchListTile(
            title: const Text('Đèn pin'),
            secondary: const Icon(Icons.flashlight_on),
            value: denPin,
            onChanged: (v) => setState(() => denPin = v),
          ),
          const SizedBox(height: 24),
          // Bật đèn pin thì bóng đèn sáng lên
          Icon(
            Icons.lightbulb,
            size: 100,
            color: denPin ? Colors.amber : Colors.grey.shade300,
          ),
          Text(
            'Wi-Fi: ${wifi ? 'Bật' : 'Tắt'}  |  Đèn pin: ${denPin ? 'Bật' : 'Tắt'}',
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.red),
          ),
        ],
      ),
    );
  }
}

// Slider: kéo để chọn giá trị trong 1 khoảng
class SliderDetailScreen extends StatefulWidget {
  const SliderDetailScreen({super.key});

  @override
  State<SliderDetailScreen> createState() => _SliderDetailScreenState();
}

class _SliderDetailScreenState extends State<SliderDetailScreen> {
  double coChu = 24;

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Slider',
      Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Text('Cỡ chữ: ${coChu.round()}'),
            Slider(
              value: coChu,
              min: 10,
              max: 60,
              divisions: 50,
              label: coChu.round().toString(),
              onChanged: (v) => setState(() => coChu = v),
            ),
            const SizedBox(height: 24),
            // Kéo slider thì chữ to / nhỏ theo
            Text(
              'Xin chào UTH',
              style: TextStyle(fontSize: coChu, color: mauXanh),
            ),
          ],
        ),
      ),
    );
  }
}

// DatePicker: chọn ngày và giờ
class DatePickerDetailScreen extends StatefulWidget {
  const DatePickerDetailScreen({super.key});

  @override
  State<DatePickerDetailScreen> createState() => _DatePickerDetailScreenState();
}

class _DatePickerDetailScreenState extends State<DatePickerDetailScreen> {
  DateTime? ngay;
  TimeOfDay? gio;

  Future<void> chonNgay() async {
    final d = await showDatePicker(
      context: context,
      initialDate: ngay ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (d != null) setState(() => ngay = d);
  }

  Future<void> chonGio() async {
    final g = await showTimePicker(
      context: context,
      initialTime: gio ?? TimeOfDay.now(),
    );
    if (g != null) setState(() => gio = g);
  }

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'DatePicker',
      Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            OutlinedButton.icon(
              icon: const Icon(Icons.calendar_today),
              label: const Text('Chọn ngày'),
              onPressed: chonNgay,
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              icon: const Icon(Icons.access_time),
              label: const Text('Chọn giờ'),
              onPressed: chonGio,
            ),
            const SizedBox(height: 24),
            Text(
              'Ngày: ${ngay == null ? '--' : '${ngay!.day}/${ngay!.month}/${ngay!.year}'}'
              '   Giờ: ${gio == null ? '--' : gio!.format(context)}',
              style: const TextStyle(color: Colors.red, fontSize: 16),
            ),
          ],
        ),
      ),
    );
  }
}

// ===================== SELECTION =====================

// Button: bấm để thực hiện hành động (đếm số lần bấm)
class ButtonDetailScreen extends StatefulWidget {
  const ButtonDetailScreen({super.key});

  @override
  State<ButtonDetailScreen> createState() => _ButtonDetailScreenState();
}

class _ButtonDetailScreenState extends State<ButtonDetailScreen> {
  int dem = 0;

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Button',
      Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('Đã bấm: $dem lần', style: const TextStyle(fontSize: 22)),
            const SizedBox(height: 24),
            // 4 kiểu nút thường gặp
            ElevatedButton(
              onPressed: () => setState(() => dem++),
              child: const Text('ElevatedButton +1'),
            ),
            FilledButton(
              onPressed: () => setState(() => dem++),
              child: const Text('FilledButton +1'),
            ),
            OutlinedButton(
              onPressed: () => setState(() => dem = 0),
              child: const Text('OutlinedButton: Reset'),
            ),
            TextButton(
              onPressed: () => setState(() => dem--),
              child: const Text('TextButton -1'),
            ),
            IconButton(
              icon: const Icon(Icons.favorite, color: Colors.red),
              onPressed: () => setState(() => dem++),
            ),
          ],
        ),
      ),
    );
  }
}

// Checkbox: chọn được nhiều mục
class CheckboxDetailScreen extends StatefulWidget {
  const CheckboxDetailScreen({super.key});

  @override
  State<CheckboxDetailScreen> createState() => _CheckboxDetailScreenState();
}

class _CheckboxDetailScreenState extends State<CheckboxDetailScreen> {
  final List<String> soThich = ['Đọc sách', 'Bóng đá', 'Nghe nhạc', 'Du lịch'];
  final Set<String> daChon = {};

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Checkbox',
      ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('Sở thích của bạn (chọn nhiều):'),
          for (final s in soThich)
            CheckboxListTile(
              title: Text(s),
              value: daChon.contains(s),
              onChanged: (v) =>
                  setState(() => v! ? daChon.add(s) : daChon.remove(s)),
            ),
          const SizedBox(height: 16),
          Text(
            daChon.isEmpty ? 'Chưa chọn gì' : 'Đã chọn: ${daChon.join(', ')}',
            style: const TextStyle(color: Colors.red),
          ),
        ],
      ),
    );
  }
}

// RadioButton: chỉ chọn được 1 mục
class RadioDetailScreen extends StatefulWidget {
  const RadioDetailScreen({super.key});

  @override
  State<RadioDetailScreen> createState() => _RadioDetailScreenState();
}

class _RadioDetailScreenState extends State<RadioDetailScreen> {
  String gioiTinh = 'Nam';

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'RadioButton',
      Padding(
        padding: const EdgeInsets.all(24),
        child: RadioGroup<String>(
          groupValue: gioiTinh,
          onChanged: (v) => setState(() => gioiTinh = v!),
          child: Column(
            children: [
              const Text('Giới tính (chỉ chọn 1):'),
              for (final g in ['Nam', 'Nữ', 'Khác'])
                RadioListTile(title: Text(g), value: g),
              const SizedBox(height: 16),
              Text(
                'Bạn chọn: $gioiTinh',
                style: const TextStyle(color: Colors.red),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// Picker: chọn 1 giá trị từ danh sách xổ xuống
class PickerDetailScreen extends StatefulWidget {
  const PickerDetailScreen({super.key});

  @override
  State<PickerDetailScreen> createState() => _PickerDetailScreenState();
}

class _PickerDetailScreenState extends State<PickerDetailScreen> {
  String thanhPho = 'TP. Hồ Chí Minh';

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Picker',
      Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            DropdownButton<String>(
              value: thanhPho,
              isExpanded: true,
              items: const [
                'TP. Hồ Chí Minh',
                'Hà Nội',
                'Đà Nẵng',
                'Cần Thơ',
              ].map((t) => DropdownMenuItem(value: t, child: Text(t))).toList(),
              onChanged: (v) => setState(() => thanhPho = v!),
            ),
            const SizedBox(height: 16),
            Text(
              'Thành phố: $thanhPho',
              style: const TextStyle(color: Colors.red),
            ),
          ],
        ),
      ),
    );
  }
}

// ===================== NAVIGATION =====================

// NavigationView: AppBar có tiêu đề + nút back, bấm nút để sang trang con
class NavigationDetailScreen extends StatelessWidget {
  const NavigationDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'NavigationView',
      Center(
        child: ElevatedButton(
          child: const Text('Sang trang con'),
          onPressed: () => Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => khungChiTiet(
                'Trang con',
                const Center(child: Text('Bấm nút ← trên AppBar để quay lại')),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// TabBar: chuyển giữa các phần chính
class TabBarDetailScreen extends StatelessWidget {
  const TabBarDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: Colors.white,
        appBar: AppBar(
          title: const Text('TabBar', style: TextStyle(color: mauXanh)),
          centerTitle: true,
          backgroundColor: Colors.white,
          foregroundColor: mauXanh,
          bottom: const TabBar(
            tabs: [
              Tab(icon: Icon(Icons.home), text: 'Trang chủ'),
              Tab(icon: Icon(Icons.search), text: 'Tìm kiếm'),
              Tab(icon: Icon(Icons.person), text: 'Cá nhân'),
            ],
          ),
        ),
        // Mỗi tab một nội dung, có thể vuốt ngang để chuyển
        body: const TabBarView(
          children: [
            Center(child: Text('Nội dung Trang chủ')),
            Center(child: Text('Nội dung Tìm kiếm')),
            Center(child: Text('Nội dung Cá nhân')),
          ],
        ),
      ),
    );
  }
}

// Menu: bấm để hiện danh sách nhiều hành động
class MenuDetailScreen extends StatefulWidget {
  const MenuDetailScreen({super.key});

  @override
  State<MenuDetailScreen> createState() => _MenuDetailScreenState();
}

class _MenuDetailScreenState extends State<MenuDetailScreen> {
  String hanhDong = 'Chưa chọn';

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Menu',
      Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            PopupMenuButton<String>(
              onSelected: (v) => setState(() => hanhDong = v),
              itemBuilder: (_) => const [
                PopupMenuItem(
                  value: 'Sửa',
                  child: ListTile(
                    leading: Icon(Icons.edit),
                    title: Text('Sửa'),
                  ),
                ),
                PopupMenuItem(
                  value: 'Chia sẻ',
                  child: ListTile(
                    leading: Icon(Icons.share),
                    title: Text('Chia sẻ'),
                  ),
                ),
                PopupMenuItem(
                  value: 'Xoá',
                  child: ListTile(
                    leading: Icon(Icons.delete),
                    title: Text('Xoá'),
                  ),
                ),
              ],
              child: const Chip(
                avatar: Icon(Icons.more_vert),
                label: Text('Bấm để mở menu'),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Bạn chọn: $hanhDong',
              style: const TextStyle(color: Colors.red),
            ),
          ],
        ),
      ),
    );
  }
}

// ===================== CONTENT =====================

// List: danh sách nhiều phần tử, cuộn được
class ListDetailScreen extends StatelessWidget {
  const ListDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'List',
      // ListView.builder: chỉ vẽ những dòng đang hiện trên màn hình
      ListView.builder(
        itemCount: 30,
        itemBuilder: (_, i) => ListTile(
          leading: CircleAvatar(child: Text('${i + 1}')),
          title: Text('Sinh viên ${i + 1}'),
          subtitle: Text('MSSV: 2251${(i + 1).toString().padLeft(4, '0')}'),
        ),
      ),
    );
  }
}

// Card: khung chứa nội dung có bo góc, đổ bóng
class CardDetailScreen extends StatelessWidget {
  const CardDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Card',
      ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            elevation: 4,
            clipBehavior: Clip.antiAlias,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Image.asset(
                  'assets/uth.jpg',
                  height: 150,
                  width: double.infinity,
                  fit: BoxFit.cover,
                ),
                const ListTile(
                  title: Text('Trường ĐH Giao thông vận tải TP.HCM'),
                  subtitle: Text('Card gồm ảnh + chữ + bóng đổ'),
                ),
              ],
            ),
          ),
          const Card(
            color: Color(0xFFBBDEFB),
            child: ListTile(
              leading: Icon(Icons.info),
              title: Text('Card màu xanh'),
              subtitle: Text('Đổi màu nền bằng thuộc tính color'),
            ),
          ),
        ],
      ),
    );
  }
}

// Divider: đường kẻ chia các phần
class DividerDetailScreen extends StatelessWidget {
  const DividerDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return khungChiTiet(
      'Divider',
      const Padding(
        padding: EdgeInsets.all(24),
        child: Column(
          children: [
            Text('Phần 1'),
            Divider(), // mặc định: mảnh, màu xám
            Text('Phần 2'),
            Divider(thickness: 3, color: mauXanh), // dày, màu xanh
            Text('Phần 3'),
            Divider(indent: 40, endIndent: 40, color: Colors.red), // thụt 2 đầu
            Text('Phần 4'),
          ],
        ),
      ),
    );
  }
}

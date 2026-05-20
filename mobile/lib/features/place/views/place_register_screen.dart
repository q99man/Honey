import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';
import '../../../core/api/api_client.dart';
import '../services/place_service.dart';

class PlaceRegisterScreen extends StatefulWidget {
  const PlaceRegisterScreen({super.key});

  @override
  State<PlaceRegisterScreen> createState() => _PlaceRegisterScreenState();
}

class _PlaceRegisterScreenState extends State<PlaceRegisterScreen> {
  late PlaceService _placeService;
  final _formKey = GlobalKey<FormState>();

  final _nameController = TextEditingController();
  final _menuController = TextEditingController();
  final _reasonController = TextEditingController();
  final _roadAddressController = TextEditingController();
  final _jibunAddressController = TextEditingController();
  final _latController = TextEditingController();
  final _lngController = TextEditingController();
  final _featureController = TextEditingController();

  String _selectedCategory = 'KOREAN';
  bool _isFranchise = false;
  bool _isLoading = false;
  bool _isLocating = false;

  Map<String, dynamic>? _myRegion;
  bool _isLoadingRegion = true;

  final Map<String, String> _categoryMap = {
    'KOREAN': '한식',
    'CHINESE': '중식',
    'JAPANESE': '일식',
    'WESTERN': '양식',
    'SNACK': '분식',
    'CAFE': '카페',
  };

  @override
  void initState() {
    super.initState();
    _placeService = PlaceService(Provider.of<ApiClient>(context, listen: false));
    _loadUserRegion();
    _autoDetectLocation();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _menuController.dispose();
    _reasonController.dispose();
    _roadAddressController.dispose();
    _jibunAddressController.dispose();
    _latController.dispose();
    _lngController.dispose();
    _featureController.dispose();
    super.dispose();
  }

  Future<void> _loadUserRegion() async {
    setState(() => _isLoadingRegion = true);
    final region = await _placeService.getMyRegion();
    setState(() {
      _myRegion = region;
      _isLoadingRegion = false;
    });
  }

  Future<void> _autoDetectLocation() async {
    setState(() => _isLocating = true);
    try {
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      setState(() {
        _latController.text = position.latitude.toStringAsFixed(6);
        _lngController.text = position.longitude.toStringAsFixed(6);
        _isLocating = false;
      });
    } catch (e) {
      debugPrint('Location error: $e');
      setState(() {
        // Fallback default coordinates
        _latController.text = '37.556456';
        _lngController.text = '126.924456';
        _isLocating = false;
      });
    }
  }

  void _submit() async {
    if (!_formKey.currentState!.validate()) return;

    if (_myRegion == null || _myRegion!['dongId'] == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('활동 지역 설정 및 인증이 완료되어야 맛집 등록이 가능합니다.'),
          backgroundColor: Colors.redAccent,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    final placeData = {
      'name': _nameController.text.trim(),
      'categoryCode': _selectedCategory,
      'dongId': _myRegion!['dongId'],
      'addressRoad': _roadAddressController.text.trim().isNotEmpty 
          ? _roadAddressController.text.trim() 
          : '주소 미입력',
      'addressJibun': _jibunAddressController.text.trim().isNotEmpty
          ? _jibunAddressController.text.trim()
          : '주소 미입력',
      'latitude': double.parse(_latController.text),
      'longitude': double.parse(_lngController.text),
      'priceRangeCode': 'MID',
      'recommendedMenu': _menuController.text.trim(),
      'shortRecommendation': _reasonController.text.trim(),
      'featureText': _featureController.text.trim().isNotEmpty 
          ? _featureController.text.trim() 
          : '특징 없음',
      'franchise': _isFranchise,
      'imageUrls': <String>[],
    };

    final result = await _placeService.createPlace(placeData);

    setState(() => _isLoading = false);

    if (result['success'] == true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('꽃(맛집)이 성공적으로 피어났습니다! 🌸'),
          backgroundColor: Colors.green,
        ),
      );
      Navigator.of(context).pop(true); // Return true to indicate reload needed
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result['message']),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      appBar: AppBar(
        title: const Text(
          '신규 맛집 등록',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        elevation: 0.5,
        iconTheme: const IconThemeData(color: Colors.black87),
      ),
      body: _isLoadingRegion
          ? const Center(child: CircularProgressIndicator(valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300))))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16.0),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Region Info Card
                    _buildRegionInfoCard(),
                    const SizedBox(height: 20),

                    // Inputs Section
                    _buildSectionTitle('기본 정보'),
                    const SizedBox(height: 8),
                    _buildTextField(
                      controller: _nameController,
                      label: '맛집(식당) 이름',
                      hint: '예: 허니통 마포점',
                      icon: Icons.store,
                      validator: (v) => v!.isEmpty ? '이름을 입력해주세요.' : null,
                    ),
                    const SizedBox(height: 12),
                    _buildCategoryDropdown(),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _menuController,
                      label: '대표 추천 메뉴',
                      hint: '예: 벌꿀 와플, 크림 파스타',
                      icon: Icons.restaurant_menu,
                      validator: (v) => v!.isEmpty ? '대표 추천 메뉴를 입력해주세요.' : null,
                    ),

                    const SizedBox(height: 20),
                    _buildSectionTitle('추천 정보'),
                    const SizedBox(height: 8),
                    _buildTextField(
                      controller: _reasonController,
                      label: '이 장소를 추천하는 한 줄 평',
                      hint: '예: 부드러운 와플 크림과 아늑한 조명이 최고인 곳입니다.',
                      icon: Icons.thumb_up,
                      maxLines: 2,
                      validator: (v) => v!.isEmpty ? '추천사/한 줄 평을 입력해주세요.' : null,
                    ),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _featureController,
                      label: '추가 특징 (선택)',
                      hint: '예: 주차 가능, 반려동물 동반 가능',
                      icon: Icons.info_outline,
                    ),

                    const SizedBox(height: 20),
                    _buildSectionTitle('위치 정보'),
                    const SizedBox(height: 8),
                    _buildLocationPicker(),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _roadAddressController,
                      label: '도로명 주소',
                      hint: '예: 서울특별시 마포구 와우산로 23길 9',
                      icon: Icons.map,
                      validator: (v) => v!.isEmpty ? '도로명 주소를 입력해주세요.' : null,
                    ),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _jibunAddressController,
                      label: '지번 주소',
                      hint: '예: 서울특별시 마포구 서교동 345-12',
                      icon: Icons.location_city,
                      validator: (v) => v!.isEmpty ? '지번 주소를 입력해주세요.' : null,
                    ),

                    const SizedBox(height: 20),
                    _buildFranchiseSwitch(),

                    const SizedBox(height: 32),
                    _buildSubmitButton(),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.bold,
        color: Colors.black87,
      ),
    );
  }

  Widget _buildRegionInfoCard() {
    final bool hasRegion = _myRegion != null && _myRegion!['dongId'] != null;
    final bool isVerified = _myRegion != null && _myRegion!['verified'] == true;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: hasRegion ? const Color(0xFFFFFDE7) : const Color(0xFFFFEBEE),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: hasRegion ? const Color(0xFFFFF59D) : const Color(0xFFFFCDD2),
        ),
      ),
      child: Row(
        children: [
          Icon(
            hasRegion ? Icons.verified_user : Icons.warning_amber_rounded,
            color: hasRegion ? const Color(0xFFFF8F00) : Colors.red,
            size: 28,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  hasRegion ? '등록 가능 지역 정보' : '활동 지역 미인증',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: hasRegion ? const Color(0xFFE65100) : Colors.red[900],
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  hasRegion
                      ? '인증된 활동 지역: ${_myRegion!['cityName']} ${_myRegion!['districtName']} ${_myRegion!['dongName']}'
                      : '맛집 등록을 위해 마이페이지에서 본인인증과 동네인증을 먼저 완료해주세요.',
                  style: TextStyle(
                    fontSize: 12,
                    color: hasRegion ? Colors.black87 : Colors.red[800],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required String hint,
    required IconData icon,
    int maxLines = 1,
    String? Function(String?)? validator,
  }) {
    return TextFormField(
      controller: controller,
      maxLines: maxLines,
      validator: validator,
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(color: Colors.black54, fontSize: 13),
        hintText: hint,
        hintStyle: const TextStyle(color: Colors.black26, fontSize: 13),
        prefixIcon: Icon(icon, color: const Color(0xFFFFB300), size: 20),
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Colors.black12),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Colors.black12),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFFFB300), width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Colors.redAccent),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Colors.redAccent, width: 1.5),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      ),
    );
  }

  Widget _buildCategoryDropdown() {
    return DropdownButtonFormField<String>(
      value: _selectedCategory,
      decoration: InputDecoration(
        labelText: '카테고리',
        labelStyle: const TextStyle(color: Colors.black54, fontSize: 13),
        prefixIcon: const Icon(Icons.category, color: Color(0xFFFFB300), size: 20),
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Colors.black12),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Colors.black12),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFFFB300), width: 1.5),
        ),
      ),
      items: _categoryMap.entries.map((entry) {
        return DropdownMenuItem<String>(
          value: entry.key,
          child: Text(entry.value),
        );
      }).toList(),
      onChanged: (val) {
        if (val != null) {
          setState(() => _selectedCategory = val);
        }
      },
    );
  }

  Widget _buildLocationPicker() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.black12),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '위도/경도 좌표 수집',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ),
              IconButton(
                onPressed: _isLocating ? null : _autoDetectLocation,
                icon: _isLocating
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2, valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300))),
                      )
                    : const Icon(Icons.gps_fixed, color: Color(0xFFFFB300)),
                tooltip: '현재 위치로 좌표 갱신',
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  controller: _latController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  validator: (v) => v!.isEmpty ? '필수' : null,
                  decoration: const InputDecoration(
                    labelText: '위도 (Latitude)',
                    labelStyle: TextStyle(fontSize: 11),
                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  controller: _lngController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  validator: (v) => v!.isEmpty ? '필수' : null,
                  decoration: const InputDecoration(
                    labelText: '경도 (Longitude)',
                    labelStyle: TextStyle(fontSize: 11),
                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFranchiseSwitch() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.black12),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: const [
              Text(
                '프랜차이즈 여부',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
              ),
              SizedBox(height: 2),
              Text(
                '대형 체인점/프랜차이즈 식당인 경우 체크해주세요.',
                style: TextStyle(fontSize: 11, color: Colors.black45),
              ),
            ],
          ),
          Switch(
            value: _isFranchise,
            activeColor: const Color(0xFFFFB300),
            onChanged: (val) {
              setState(() => _isFranchise = val);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildSubmitButton() {
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: ElevatedButton(
        onPressed: _isLoading ? null : _submit,
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFFFFB300),
          foregroundColor: Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
        child: _isLoading
            ? const CircularProgressIndicator(valueColor: AlwaysStoppedAnimation(Colors.white))
            : const Text(
                '맛집 등록하기',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
      ),
    );
  }
}

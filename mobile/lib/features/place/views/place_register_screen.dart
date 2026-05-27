import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../../../core/api/api_client.dart';
import '../../../core/services/image_upload_service.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_section_title.dart';
import '../../../core/widgets/app_surface_card.dart';
import '../../auth/providers/auth_provider.dart';
import '../models/place_registration_eligibility.dart';
import '../services/place_service.dart';
import '../widgets/place_image_url_editor.dart';

class PlaceRegisterScreen extends StatefulWidget {
  const PlaceRegisterScreen({super.key, this.placeId});

  final int? placeId;

  @override
  State<PlaceRegisterScreen> createState() => _PlaceRegisterScreenState();
}

class _PlaceRegisterScreenState extends State<PlaceRegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _menuController = TextEditingController();
  final _reasonController = TextEditingController();
  final _roadAddressController = TextEditingController();
  final _jibunAddressController = TextEditingController();
  final _latController = TextEditingController();
  final _lngController = TextEditingController();
  final _featureController = TextEditingController();
  final _imageUrlController = TextEditingController();

  late final PlaceService _placeService;
  late final ImageUploadService _imageUploadService;
  final ImagePicker _imagePicker = ImagePicker();

  String _selectedCategory = 'KOREAN';
  bool _isFranchise = false;
  bool _isLoading = false;
  bool _isLocating = false;
  bool _isUploadingImage = false;
  bool _isLoadingRegion = true;
  bool _isLoadingPlace = false;
  Map<String, dynamic>? _myRegion;
  Map<String, dynamic>? _editingPlace;
  final List<String> _imageUrls = [];

  bool get _isEditMode => widget.placeId != null;

  final Map<String, String> _categoryMap = const {
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
    _placeService =
        PlaceService(Provider.of<ApiClient>(context, listen: false));
    _imageUploadService =
        ImageUploadService(Provider.of<ApiClient>(context, listen: false));
    _loadUserRegion();
    if (_isEditMode) {
      _loadPlaceForEdit();
    } else {
      _autoDetectLocation();
    }
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
    _imageUrlController.dispose();
    super.dispose();
  }

  Future<void> _loadUserRegion() async {
    setState(() => _isLoadingRegion = true);
    final region = await _placeService.getMyRegion();
    if (!mounted) return;
    setState(() {
      _myRegion = region;
      _isLoadingRegion = false;
    });
  }

  Future<void> _autoDetectLocation() async {
    setState(() => _isLocating = true);
    try {
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      if (!mounted) return;
      setState(() {
        _latController.text = position.latitude.toStringAsFixed(6);
        _lngController.text = position.longitude.toStringAsFixed(6);
      });
    } catch (e) {
      debugPrint('Location error: $e');
    } finally {
      if (mounted) {
        setState(() => _isLocating = false);
      }
    }
  }

  Future<void> _loadPlaceForEdit() async {
    final placeId = widget.placeId;
    if (placeId == null) return;

    setState(() => _isLoadingPlace = true);
    final details = await _placeService.getPlace(placeId);
    if (!mounted) return;

    if (details == null) {
      setState(() => _isLoadingPlace = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('수정할 맛집 정보를 불러오지 못했습니다.'),
          backgroundColor: AppColors.berry,
        ),
      );
      return;
    }

    _editingPlace = details;
    _nameController.text = (details['name'] ?? '').toString();
    _menuController.text = (details['recommendedMenu'] ?? '').toString();
    _reasonController.text = (details['shortRecommendation'] ?? '').toString();
    _roadAddressController.text = (details['addressRoad'] ?? '').toString();
    _jibunAddressController.text = (details['addressJibun'] ?? '').toString();
    _featureController.text = (details['featureText'] ?? '').toString();
    _latController.text = (details['latitude'] ?? '').toString();
    _lngController.text = (details['longitude'] ?? '').toString();
    _imageUrls
      ..clear()
      ..addAll(_extractImageUrls(details['imageUrls']));

    final categoryCode = details['categoryCode']?.toString();
    setState(() {
      if (categoryCode != null && _categoryMap.containsKey(categoryCode)) {
        _selectedCategory = categoryCode;
      }
      _isFranchise = details['franchise'] == true;
      _isLoadingPlace = false;
    });
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final authProvider = context.read<AuthProvider>();
    final phoneVerified = authProvider.userProfile?.phoneVerified == true;
    final hasEditableDong = _isEditMode && _editingPlace?['dongId'] != null;
    final hasVerifiedDong = _myRegion != null && _myRegion!['dongId'] != null;
    final eligibility = PlaceRegistrationEligibility.evaluate(
      phoneVerified: phoneVerified,
      hasVerifiedRegion: hasEditableDong || hasVerifiedDong,
    );

    if (!eligibility.allowed) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(eligibility.message!),
          backgroundColor: AppColors.berry,
        ),
      );
      return;
    }

    final latitude = double.tryParse(_latController.text.trim());
    final longitude = double.tryParse(_lngController.text.trim());
    if (latitude == null || longitude == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('현재 위치를 다시 잡거나 위도/경도를 숫자로 입력해주세요.'),
          backgroundColor: AppColors.berry,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    final dongId = _isEditMode
        ? (_editingPlace?['dongId'] ?? _myRegion!['dongId'])
        : _myRegion!['dongId'];

    final placeData = {
      'name': _nameController.text.trim(),
      'categoryCode': _selectedCategory,
      'dongId': dongId,
      'addressRoad': _nullableText(_roadAddressController),
      'addressJibun': _nullableText(_jibunAddressController),
      'latitude': latitude,
      'longitude': longitude,
      'priceRangeCode': 'MID',
      'recommendedMenu': _menuController.text.trim(),
      'shortRecommendation': _reasonController.text.trim(),
      'featureText': _nullableText(_featureController),
      'franchise': _isFranchise,
      'imageUrls': List<String>.from(_imageUrls),
    };

    final result = _isEditMode
        ? await _placeService.updatePlace(widget.placeId!, placeData)
        : await _placeService.createPlace(placeData);
    if (!mounted) return;

    setState(() => _isLoading = false);

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(result['message'] ?? '처리 결과를 확인할 수 없습니다.'),
        backgroundColor:
            result['success'] == true ? AppColors.leaf : AppColors.berry,
      ),
    );

    if (result['success'] == true) {
      Navigator.of(context).pop(true);
    }
  }

  String? _nullableText(TextEditingController controller) {
    final value = controller.text.trim();
    return value.isEmpty ? null : value;
  }

  List<String> _extractImageUrls(dynamic value) {
    if (value is! List) return [];
    return value
        .whereType<String>()
        .map((url) => url.trim())
        .where((url) => url.isNotEmpty)
        .toList();
  }

  void _addImageUrl() {
    final value = _imageUrlController.text.trim();
    if (value.isEmpty) return;

    final uri = Uri.tryParse(value);
    final isHttpImageUrl = uri != null &&
        uri.hasScheme &&
        (uri.scheme == 'http' || uri.scheme == 'https') &&
        uri.host.isNotEmpty;
    if (!isHttpImageUrl) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('http 또는 https로 시작하는 이미지 URL을 입력해주세요.'),
          backgroundColor: AppColors.berry,
        ),
      );
      return;
    }

    if (_imageUrls.contains(value)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('이미 추가한 사진 URL입니다.'),
          backgroundColor: AppColors.berry,
        ),
      );
      return;
    }

    setState(() {
      _imageUrls.add(value);
      _imageUrlController.clear();
    });
  }

  void _removeImageUrl(int index) {
    if (index < 0 || index >= _imageUrls.length) return;
    setState(() => _imageUrls.removeAt(index));
  }

  void _moveImageUrl(int index, int offset) {
    final newIndex = index + offset;
    if (index < 0 || index >= _imageUrls.length) return;
    if (newIndex < 0 || newIndex >= _imageUrls.length) return;
    setState(() {
      final url = _imageUrls.removeAt(index);
      _imageUrls.insert(newIndex, url);
    });
  }

  Future<void> _pickAndUploadPlaceImage() async {
    final image = await _imagePicker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
      maxWidth: 1600,
    );
    if (image == null) return;

    setState(() => _isUploadingImage = true);
    final imageUrl = await _imageUploadService.uploadImageFile(
      path: image.path,
      filename: image.name,
      target: ImageUploadTarget.place,
    );
    if (!mounted) return;

    setState(() => _isUploadingImage = false);

    if (imageUrl == null || imageUrl.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('사진 업로드에 실패했습니다. 다시 시도해주세요.'),
          backgroundColor: AppColors.berry,
        ),
      );
      return;
    }

    if (_imageUrls.contains(imageUrl)) return;
    setState(() => _imageUrls.add(imageUrl));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(
          _isEditMode ? '맛집 수정' : '새 맛집 등록',
          style: const TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      body: _isLoadingRegion || _isLoadingPlace
          ? const Center(
              child: CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation(AppColors.honey),
              ),
            )
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildRegionInfoCard(),
                    const SizedBox(height: 20),
                    const AppSectionTitle('기본 정보'),
                    const SizedBox(height: 8),
                    _buildTextField(
                      controller: _nameController,
                      label: '맛집 이름',
                      hint: '예: 언니분식',
                      icon: Icons.store,
                      validator: _required('맛집 이름을 입력해주세요.'),
                    ),
                    const SizedBox(height: 12),
                    _buildCategoryDropdown(),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _menuController,
                      label: '대표 추천 메뉴',
                      hint: '예: 김치찌개',
                      icon: Icons.restaurant_menu,
                      validator: _required('대표 추천 메뉴를 입력해주세요.'),
                    ),
                    const SizedBox(height: 20),
                    const AppSectionTitle('추천 정보'),
                    const SizedBox(height: 8),
                    _buildTextField(
                      controller: _reasonController,
                      label: '추천하는 이유',
                      hint: '동네 사람들에게 추천하고 싶은 이유를 적어주세요.',
                      icon: Icons.thumb_up,
                      maxLines: 2,
                      validator: _required('추천 이유를 입력해주세요.'),
                    ),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _featureController,
                      label: '추가 특징',
                      hint: '예: 혼밥 가능, 주차 가능',
                      icon: Icons.info_outline,
                    ),
                    const SizedBox(height: 20),
                    const AppSectionTitle('사진'),
                    const SizedBox(height: 8),
                    PlaceImageUrlEditor(
                      imageUrls: _imageUrls,
                      controller: _imageUrlController,
                      onAdd: _addImageUrl,
                      onRemove: _removeImageUrl,
                      onMoveUp: (index) => _moveImageUrl(index, -1),
                      onMoveDown: (index) => _moveImageUrl(index, 1),
                      onPickImage: _pickAndUploadPlaceImage,
                      isPickingImage: _isUploadingImage,
                    ),
                    const SizedBox(height: 20),
                    const AppSectionTitle('위치 정보'),
                    const SizedBox(height: 8),
                    _buildLocationPicker(),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _roadAddressController,
                      label: '도로명 주소',
                      hint: '예: 서울특별시 마포구 와우산로 1',
                      icon: Icons.map,
                      validator: (_) => _addressValidationMessage(),
                    ),
                    const SizedBox(height: 12),
                    _buildTextField(
                      controller: _jibunAddressController,
                      label: '지번 주소',
                      hint: '예: 서울특별시 마포구 서교동 1-1',
                      icon: Icons.location_city,
                      validator: (_) => _addressValidationMessage(),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      '도로명 주소와 지번 주소 중 하나만 입력해도 됩니다.',
                      style: TextStyle(fontSize: 12, color: AppColors.muted),
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

  String? _addressValidationMessage() {
    if (_roadAddressController.text.trim().isNotEmpty ||
        _jibunAddressController.text.trim().isNotEmpty) {
      return null;
    }
    return '도로명 주소와 지번 주소 중 하나를 입력해주세요.';
  }

  Widget _buildRegionInfoCard() {
    final authProvider = context.watch<AuthProvider>();
    final phoneVerified = authProvider.userProfile?.phoneVerified == true;
    final hasRegion = _myRegion != null && _myRegion!['dongId'] != null;
    final ready = phoneVerified && hasRegion;

    String title;
    String description;
    if (ready) {
      title = '등록 가능 지역';
      description =
          '${_myRegion!['cityName']} ${_myRegion!['districtName']} ${_myRegion!['dongName']}';
    } else if (!phoneVerified) {
      title = '휴대폰 인증 필요';
      description = '마이페이지에서 휴대폰 인증을 먼저 완료해주세요.';
    } else {
      title = '동네 인증 필요';
      description = '마이페이지에서 동네 인증을 먼저 완료해주세요.';
    }

    return AppSurfaceCard(
      child: Row(
        children: [
          Icon(
            ready ? Icons.verified_user : Icons.warning_amber_rounded,
            color: ready ? AppColors.nectar : AppColors.berry,
            size: 28,
          ),
          const SizedBox(width: AppSpacing.sm),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: ready ? AppColors.ink : AppColors.berry,
                  ),
                ),
                const SizedBox(height: AppSpacing.xxs),
                Text(
                  description,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.muted,
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
      onChanged: (_) {
        if (controller == _roadAddressController ||
            controller == _jibunAddressController) {
          _formKey.currentState?.validate();
        }
      },
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(color: AppColors.muted, fontSize: 13),
        hintText: hint,
        hintStyle: const TextStyle(color: AppColors.muted, fontSize: 13),
        prefixIcon: Icon(icon, color: AppColors.honey, size: 20),
      ),
    );
  }

  Widget _buildCategoryDropdown() {
    return DropdownButtonFormField<String>(
      initialValue: _selectedCategory,
      decoration: const InputDecoration(
        labelText: '카테고리',
        prefixIcon: Icon(Icons.category, color: AppColors.honey, size: 20),
      ),
      items: _categoryMap.entries.map((entry) {
        return DropdownMenuItem<String>(
          value: entry.key,
          child: Text(entry.value),
        );
      }).toList(),
      onChanged: (value) {
        if (value != null) {
          setState(() => _selectedCategory = value);
        }
      },
    );
  }

  Widget _buildLocationPicker() {
    return AppSurfaceCard(
      padding: const EdgeInsets.all(AppSpacing.sm),
      child: Column(
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  '위도/경도 좌표',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: AppColors.ink,
                  ),
                ),
              ),
              IconButton(
                onPressed: _isLocating ? null : _autoDetectLocation,
                tooltip: '현재 위치로 좌표 갱신',
                icon: _isLocating
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          valueColor: AlwaysStoppedAnimation(AppColors.honey),
                        ),
                      )
                    : const Icon(Icons.gps_fixed, color: AppColors.honey),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  controller: _latController,
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  validator: _required('위도는 필수입니다.'),
                  decoration: const InputDecoration(
                    labelText: '위도',
                    labelStyle: TextStyle(fontSize: 11),
                    contentPadding:
                        EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  controller: _lngController,
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  validator: _required('경도는 필수입니다.'),
                  decoration: const InputDecoration(
                    labelText: '경도',
                    labelStyle: TextStyle(fontSize: 11),
                    contentPadding:
                        EdgeInsets.symmetric(horizontal: 12, vertical: 8),
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
    return AppSurfaceCard(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.xs,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '프랜차이즈 여부',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                ),
                SizedBox(height: 2),
                Text(
                  '대형 체인 또는 프랜차이즈 매장이라면 체크해주세요.',
                  style: TextStyle(fontSize: 11, color: AppColors.muted),
                ),
              ],
            ),
          ),
          Switch(
            value: _isFranchise,
            activeThumbColor: AppColors.honey,
            onChanged: (value) => setState(() => _isFranchise = value),
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
        style: FilledButton.styleFrom(),
        child: _isLoading
            ? const CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation(Colors.white),
              )
            : Text(
                _isEditMode ? '맛집 수정하기' : '맛집 등록하기',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
      ),
    );
  }

  String? Function(String?) _required(String message) {
    return (value) {
      if (value == null || value.trim().isEmpty) {
        return message;
      }
      return null;
    };
  }
}

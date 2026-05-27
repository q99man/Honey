import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../../../core/api/api_client.dart';
import '../../../core/services/image_upload_service.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/utils/image_url_resolver.dart';
import '../../../core/widgets/app_section_title.dart';
import '../../../core/widgets/app_surface_card.dart';
import '../../../models/user.dart';
import '../../auth/providers/auth_provider.dart';

class ProfileEditScreen extends StatefulWidget {
  const ProfileEditScreen({super.key, required this.profile});

  final UserProfile profile;

  @override
  State<ProfileEditScreen> createState() => _ProfileEditScreenState();
}

class _ProfileEditScreenState extends State<ProfileEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nicknameController = TextEditingController();
  final _birthYearController = TextEditingController();
  final _nationalityController = TextEditingController();
  final ImagePicker _imagePicker = ImagePicker();

  late final ImageUploadService _imageUploadService;

  String _languagePreference = 'ko';
  String? _gender;
  String? _profileImageUrl;
  bool _isSaving = false;
  bool _isUploadingImage = false;

  @override
  void initState() {
    super.initState();
    _imageUploadService =
        ImageUploadService(Provider.of<ApiClient>(context, listen: false));
    _nicknameController.text = widget.profile.nickname;
    _birthYearController.text = widget.profile.birthYear?.toString() ?? '';
    _nationalityController.text = widget.profile.nationalityCode ?? '';
    _languagePreference = widget.profile.languagePreference ?? 'ko';
    _gender = widget.profile.gender;
    _profileImageUrl = widget.profile.profileImageUrl;
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _birthYearController.dispose();
    _nationalityController.dispose();
    super.dispose();
  }

  Future<void> _pickProfileImage() async {
    final image = await _imagePicker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
      maxWidth: 1200,
    );
    if (image == null) return;

    setState(() => _isUploadingImage = true);
    final imageUrl = await _imageUploadService.uploadImageFile(
      path: image.path,
      filename: image.name,
      target: ImageUploadTarget.profile,
    );
    if (!mounted) return;
    setState(() => _isUploadingImage = false);

    if (imageUrl == null || imageUrl.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('프로필 사진 업로드에 실패했습니다.'),
          backgroundColor: AppColors.berry,
        ),
      );
      return;
    }

    setState(() => _profileImageUrl = imageUrl);
  }

  Future<void> _saveProfile() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSaving = true);
    final success = await context.read<AuthProvider>().updateMyProfile(
          nickname: _nicknameController.text.trim(),
          languagePreference: _languagePreference,
          birthYear: _nullableBirthYear(),
          gender: _gender,
          nationalityCode: _nullableText(_nationalityController.text),
          profileImageUrl: _profileImageUrl,
        );
    if (!mounted) return;
    setState(() => _isSaving = false);

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(success ? '프로필이 수정되었습니다.' : '프로필 수정에 실패했습니다.'),
        backgroundColor: success ? AppColors.leaf : AppColors.berry,
      ),
    );

    if (success) {
      Navigator.of(context).pop(true);
    }
  }

  int? _nullableBirthYear() {
    final value = _birthYearController.text.trim();
    if (value.isEmpty) return null;
    return int.tryParse(value);
  }

  String? _nullableText(String value) {
    final trimmed = value.trim();
    return trimmed.isEmpty ? null : trimmed;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text(
          '내 정보 수정',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              AppSurfaceCard(
                child: Column(
                  children: [
                    _buildAvatar(),
                    const SizedBox(height: AppSpacing.md),
                    OutlinedButton.icon(
                      onPressed: _isUploadingImage ? null : _pickProfileImage,
                      icon: _isUploadingImage
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.photo_camera_back_outlined),
                      label: Text(_isUploadingImage ? '사진 업로드 중' : '프로필 사진 선택'),
                    ),
                    if (_profileImageUrl != null &&
                        _profileImageUrl!.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.xs),
                      TextButton.icon(
                        onPressed: () =>
                            setState(() => _profileImageUrl = null),
                        icon: const Icon(Icons.delete_outline),
                        label: const Text('프로필 사진 제거'),
                        style: TextButton.styleFrom(
                          foregroundColor: AppColors.berry,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.lg),
              const AppSectionTitle('기본 정보'),
              const SizedBox(height: AppSpacing.sm),
              TextFormField(
                controller: _nicknameController,
                decoration: const InputDecoration(
                  labelText: '닉네임',
                  prefixIcon: Icon(Icons.badge_outlined),
                ),
                validator: (value) {
                  final trimmed = value?.trim() ?? '';
                  if (trimmed.length < 2 || trimmed.length > 50) {
                    return '닉네임은 2자 이상 50자 이하로 입력해주세요.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: AppSpacing.sm),
              DropdownButtonFormField<String>(
                initialValue: _languagePreference,
                decoration: const InputDecoration(
                  labelText: '언어',
                  prefixIcon: Icon(Icons.language),
                ),
                items: const [
                  DropdownMenuItem(value: 'ko', child: Text('한국어')),
                  DropdownMenuItem(value: 'en', child: Text('English')),
                  DropdownMenuItem(value: 'ja', child: Text('日本語')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    setState(() => _languagePreference = value);
                  }
                },
              ),
              const SizedBox(height: AppSpacing.lg),
              const AppSectionTitle('선택 정보'),
              const SizedBox(height: AppSpacing.sm),
              TextFormField(
                controller: _birthYearController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: '출생년도',
                  hintText: '예: 1995',
                  prefixIcon: Icon(Icons.cake_outlined),
                ),
                validator: (value) {
                  final trimmed = value?.trim() ?? '';
                  if (trimmed.isEmpty) return null;
                  final year = int.tryParse(trimmed);
                  if (year == null || year < 1900 || year > 2100) {
                    return '출생년도를 올바르게 입력해주세요.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: AppSpacing.sm),
              DropdownButtonFormField<String?>(
                initialValue: _gender,
                decoration: const InputDecoration(
                  labelText: '성별',
                  prefixIcon: Icon(Icons.person_outline),
                ),
                items: const [
                  DropdownMenuItem(value: null, child: Text('선택 안 함')),
                  DropdownMenuItem(value: 'MALE', child: Text('남성')),
                  DropdownMenuItem(value: 'FEMALE', child: Text('여성')),
                  DropdownMenuItem(value: 'OTHER', child: Text('기타')),
                ],
                onChanged: (value) => setState(() => _gender = value),
              ),
              const SizedBox(height: AppSpacing.sm),
              TextFormField(
                controller: _nationalityController,
                textCapitalization: TextCapitalization.characters,
                decoration: const InputDecoration(
                  labelText: '국가 코드',
                  hintText: '예: KR',
                  prefixIcon: Icon(Icons.flag_outlined),
                ),
                validator: (value) {
                  final trimmed = value?.trim() ?? '';
                  if (trimmed.length > 10) {
                    return '국가코드는 10자 이하로 입력해주세요.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: AppSpacing.xl),
              SizedBox(
                height: 52,
                child: FilledButton(
                  onPressed:
                      _isSaving || _isUploadingImage ? null : _saveProfile,
                  child: _isSaving
                      ? const CircularProgressIndicator(
                          valueColor: AlwaysStoppedAnimation(Colors.white),
                        )
                      : const Text('저장하기'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAvatar() {
    final imageUrl = _profileImageUrl;
    return CircleAvatar(
      radius: 48,
      backgroundColor: AppColors.surfaceWarm,
      backgroundImage: imageUrl == null || imageUrl.isEmpty
          ? null
          : NetworkImage(ImageUrlResolver.resolve(imageUrl)),
      child: imageUrl == null || imageUrl.isEmpty
          ? const Icon(
              Icons.emoji_nature,
              color: AppColors.honey,
              size: 44,
            )
          : null,
    );
  }
}

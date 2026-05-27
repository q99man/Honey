import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/utils/image_url_resolver.dart';
import '../../../core/widgets/app_login_required_state.dart';
import '../../../core/widgets/app_surface_card.dart';
import '../../../utils/localization.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../../place/views/my_places_screen.dart';
import 'profile_edit_screen.dart';

class MyPageScreen extends StatefulWidget {
  const MyPageScreen({super.key});

  @override
  State<MyPageScreen> createState() => _MyPageScreenState();
}

class _MyPageScreenState extends State<MyPageScreen> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();
  bool _codeSent = false;
  bool _isVerifyingPhone = false;
  bool _isVerifyingRegion = false;

  @override
  void dispose() {
    _phoneController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _verifyCurrentRegion(BuildContext context) async {
    final messenger = ScaffoldMessenger.of(context);
    final authProvider = context.read<AuthProvider>();

    setState(() => _isVerifyingRegion = true);
    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        messenger.showSnackBar(
          const SnackBar(content: Text('휴대폰 위치 서비스를 켜주세요.')),
        );
        return;
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied) {
        messenger.showSnackBar(
          const SnackBar(content: Text('동네 인증을 위해 위치 권한이 필요합니다.')),
        );
        return;
      }
      if (permission == LocationPermission.deniedForever) {
        messenger.showSnackBar(
          const SnackBar(content: Text('앱 설정에서 위치 권한을 허용해주세요.')),
        );
        return;
      }

      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      final success = await authProvider.verifyRegion(
        latitude: position.latitude,
        longitude: position.longitude,
      );

      if (!mounted) return;
      messenger.showSnackBar(
        SnackBar(
          content: Text(
            success
                ? '동네 인증이 완료되었습니다.'
                : authProvider.errorMessage ?? '동네 인증에 실패했습니다.',
          ),
          backgroundColor: success ? AppColors.leaf : AppColors.berry,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isVerifyingRegion = false);
      }
    }
  }

  void _showPhoneVerificationDialog(BuildContext context) {
    final scaffoldMessenger = ScaffoldMessenger.of(context);
    _phoneController.clear();
    _codeController.clear();
    _codeSent = false;
    _isVerifyingPhone = false;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setStateInDialog) {
            final authProvider =
                Provider.of<AuthProvider>(context, listen: false);

            Future<void> sendCode() async {
              final phone = _phoneController.text.trim();
              if (!RegExp(r'^01[0-9]{8,9}$').hasMatch(phone)) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('휴대폰 번호를 숫자만 입력해주세요.')),
                );
                return;
              }

              setStateInDialog(() => _isVerifyingPhone = true);
              final success = await authProvider.sendPhoneVerification(phone);
              if (!mounted || !dialogContext.mounted) return;

              setStateInDialog(() {
                _isVerifyingPhone = false;
                _codeSent = success;
              });

              scaffoldMessenger.showSnackBar(
                SnackBar(
                  content: Text(
                    success
                        ? '인증번호를 전송했습니다.'
                        : authProvider.errorMessage ?? '인증번호 전송에 실패했습니다.',
                  ),
                  backgroundColor: success ? AppColors.leaf : AppColors.berry,
                ),
              );
            }

            Future<void> verifyCode() async {
              final phone = _phoneController.text.trim();
              final code = _codeController.text.trim();
              if (!RegExp(r'^[0-9]+$').hasMatch(code)) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('문자로 받은 인증번호를 입력해주세요.')),
                );
                return;
              }

              setStateInDialog(() => _isVerifyingPhone = true);
              final success = await authProvider.verifyPhoneCode(phone, code);
              if (!mounted || !dialogContext.mounted) return;
              setStateInDialog(() => _isVerifyingPhone = false);

              if (success) {
                Navigator.of(dialogContext).pop();
                scaffoldMessenger.showSnackBar(
                  const SnackBar(
                    content: Text('휴대폰 본인 인증이 완료되었습니다.'),
                    backgroundColor: AppColors.leaf,
                  ),
                );
              } else {
                scaffoldMessenger.showSnackBar(
                  SnackBar(
                    content: Text(
                      authProvider.errorMessage ?? '인증번호 확인에 실패했습니다.',
                    ),
                    backgroundColor: AppColors.berry,
                  ),
                );
              }
            }

            return AlertDialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              title: const Row(
                children: [
                  Icon(Icons.verified_user_outlined, color: AppColors.honey),
                  SizedBox(width: 8),
                  Text(
                    '휴대폰 본인 인증',
                    style: TextStyle(fontWeight: FontWeight.w800),
                  ),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '맛집 등록, 평가 작성, 추천 활동을 위해 본인 인증이 필요합니다.',
                    style: TextStyle(fontSize: 13, color: AppColors.muted),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    enabled: !_codeSent && !_isVerifyingPhone,
                    decoration: InputDecoration(
                      labelText: '휴대폰 번호',
                      hintText: '01012345678',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      suffixIcon: _codeSent
                          ? const Icon(Icons.check_circle,
                              color: AppColors.leaf)
                          : null,
                    ),
                  ),
                  if (_codeSent) ...[
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _codeController,
                      keyboardType: TextInputType.number,
                      maxLength: 10,
                      enabled: !_isVerifyingPhone,
                      decoration: InputDecoration(
                        labelText: '인증번호',
                        hintText: '문자로 받은 숫자',
                        counterText: '',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ],
                ],
              ),
              actions: [
                TextButton(
                  onPressed: _isVerifyingPhone
                      ? null
                      : () => Navigator.of(dialogContext).pop(),
                  child: const Text(
                    '취소',
                    style: TextStyle(color: AppColors.muted),
                  ),
                ),
                FilledButton(
                  onPressed: _isVerifyingPhone
                      ? null
                      : (_codeSent ? verifyCode : sendCode),
                  child: _isVerifyingPhone
                      ? const SizedBox(
                          height: 16,
                          width: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor:
                                AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : Text(_codeSent ? '인증 완료' : '인증번호 전송'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final user = authProvider.userProfile;
    final status = authProvider.userStatus;
    final summary = authProvider.userSummary;

    if (!authProvider.isAuthenticated || user == null) {
      return AppLoginRequiredState(
        icon: Icons.account_circle_outlined,
        title: 'auth.loginRequired'.tr,
        description: 'auth.phoneVerificationRequired'.tr,
        actionLabel: '로그인 / 회원가입',
        eyebrow: '허니통 활동 여권',
        previewItems: const [
          AppLoginPreviewItem(
            icon: Icons.verified_rounded,
            title: '전화 인증과 동네 인증',
            description: '신뢰 기반 참여에 필요한 인증을 관리합니다.',
          ),
          AppLoginPreviewItem(
            icon: Icons.emoji_events_rounded,
            title: '레벨과 활동 기록',
            description: '추천, 방문, 평가 기록을 한곳에서 확인합니다.',
          ),
        ],
        onActionPressed: () {
          Navigator.of(context).push(
            MaterialPageRoute(builder: (context) => const LoginScreen()),
          );
        },
      );
    }

    final regionVerified = status?.regionVerified ?? false;
    final expPercent = status != null && status.nextLevelExp > 0
        ? (status.exp / status.nextLevelExp).clamp(0.0, 1.0)
        : 0.0;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: RefreshIndicator(
        onRefresh: () => authProvider.refreshUserProfile(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              AppSurfaceCard(
                child: Padding(
                  padding: EdgeInsets.zero,
                  child: Row(
                    children: [
                      CircleAvatar(
                        radius: 30,
                        backgroundColor: AppColors.surfaceWarm,
                        backgroundImage: user.profileImageUrl == null ||
                                user.profileImageUrl!.isEmpty
                            ? null
                            : NetworkImage(
                                ImageUrlResolver.resolve(user.profileImageUrl!),
                              ),
                        child: user.profileImageUrl == null ||
                                user.profileImageUrl!.isEmpty
                            ? const Icon(
                                Icons.emoji_nature,
                                color: AppColors.honey,
                                size: 32,
                              )
                            : null,
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              user.nickname,
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: AppColors.ink,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              user.displayName,
                              style: const TextStyle(
                                fontSize: 12,
                                color: AppColors.muted,
                              ),
                            ),
                          ],
                        ),
                      ),
                      _buildStatusChip(
                        label: user.phoneVerified ? '본인 인증' : '본인 미인증',
                        verified: user.phoneVerified,
                      ),
                      IconButton(
                        tooltip: '내 정보 수정',
                        icon: const Icon(Icons.edit_outlined),
                        onPressed: () {
                          Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (context) =>
                                  ProfileEditScreen(profile: user),
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              _buildGrowthCard(status, expPercent),
              const SizedBox(height: 12),
              GridView.count(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 1.5,
                children: [
                  _buildStatCard(
                      '맛집 추천', '${summary?.recommendationCount ?? 0}개'),
                  _buildStatCard('방문 인증', '${summary?.visitCount ?? 0}개'),
                  _buildStatCard('작성 평가', '${summary?.commentCount ?? 0}개'),
                  _buildStatCard('등록 맛집', '${summary?.placeCount ?? 0}곳'),
                ],
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => const MyPlacesScreen(),
                    ),
                  );
                },
                icon: const Icon(Icons.storefront_outlined),
                label: const Text('내 등록 맛집 관리'),
              ),
              const SizedBox(height: 16),
              if (!user.phoneVerified)
                _buildActionBanner(
                  icon: Icons.phone_android,
                  title: '휴대폰 인증이 필요합니다',
                  description: '본인 인증을 완료해야 맛집 등록과 참여 활동을 할 수 있습니다.',
                  isLoading: _isVerifyingPhone,
                  onTap: () => _showPhoneVerificationDialog(context),
                ),
              if (!regionVerified) ...[
                if (!user.phoneVerified) const SizedBox(height: 12),
                _buildActionBanner(
                  icon: Icons.my_location,
                  title: '동네 인증이 필요합니다',
                  description: '현재 위치로 활동할 동네를 인증해주세요.',
                  isLoading: _isVerifyingRegion,
                  onTap: _isVerifyingRegion
                      ? null
                      : () => _verifyCurrentRegion(context),
                ),
              ],
              const SizedBox(height: 24),
              _buildLanguageCard(),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: () => authProvider.logout(),
                icon: const Icon(Icons.logout),
                label: const Text('로그아웃'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.berry,
                  side: const BorderSide(color: AppColors.berry, width: 1.2),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildGrowthCard(dynamic status, double expPercent) {
    return AppSurfaceCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'mypage.level'.tr,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: AppColors.ink,
                ),
              ),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.surfaceWarm,
                  borderRadius: BorderRadius.circular(AppRadius.pill),
                ),
                child: Text(
                  '${'mypage.trust'.tr}: ${status?.trustGrade ?? 'Seed Bee'}',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.nectar,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Level ${status?.level ?? 1}',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                  color: AppColors.honey,
                ),
              ),
              Text(
                '${status?.exp ?? 0} / ${status?.nextLevelExp ?? 0} EXP',
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.muted,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(10),
            child: LinearProgressIndicator(
              value: expPercent,
              minHeight: 10,
              backgroundColor: AppColors.outline,
              valueColor: const AlwaysStoppedAnimation<Color>(AppColors.honey),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            '추천 반영 가중치: x${(status?.recommendWeight ?? 1.0).toStringAsFixed(2)}',
            style: const TextStyle(
              fontSize: 12,
              color: AppColors.muted,
              fontStyle: FontStyle.italic,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusChip({
    required String label,
    required bool verified,
  }) {
    return Chip(
      labelPadding: const EdgeInsets.symmetric(horizontal: 4),
      avatar: Icon(
        verified ? Icons.verified : Icons.error_outline,
        size: 14,
        color: verified ? AppColors.leaf : AppColors.nectar,
      ),
      backgroundColor: verified
          ? AppColors.leaf.withValues(alpha: 0.1)
          : AppColors.surfaceWarm,
      label: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          color: verified ? AppColors.leaf : AppColors.nectar,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }

  Widget _buildStatCard(String title, String count) {
    return AppSurfaceCard(
      child: Padding(
        padding: EdgeInsets.zero,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontSize: 12, color: AppColors.muted),
            ),
            const SizedBox(height: 8),
            Text(
              count,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w800,
                color: AppColors.ink,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionBanner({
    required IconData icon,
    required String title,
    required String description,
    required bool isLoading,
    required VoidCallback? onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        decoration: BoxDecoration(
          color: AppColors.surfaceWarm,
          borderRadius: BorderRadius.circular(AppRadius.md),
          border: Border.all(color: AppColors.outline),
        ),
        child: Row(
          children: [
            Icon(icon, color: AppColors.nectar),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 14,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    description,
                    style:
                        const TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                ],
              ),
            ),
            if (isLoading)
              const SizedBox(
                height: 18,
                width: 18,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            else
              const Icon(Icons.chevron_right, color: AppColors.nectar),
          ],
        ),
      ),
    );
  }

  Widget _buildLanguageCard() {
    return AppSurfaceCard(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                const Icon(Icons.language, color: AppColors.honey),
                const SizedBox(width: 12),
                Text(
                  'mypage.language'.tr,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: AppColors.ink,
                  ),
                ),
              ],
            ),
            Consumer<Localization>(
              builder: (context, localization, _) {
                return DropdownButton<String>(
                  value: localization.locale,
                  underline: const SizedBox(),
                  items: const [
                    DropdownMenuItem(value: 'ko', child: Text('한국어')),
                    DropdownMenuItem(value: 'en', child: Text('English')),
                    DropdownMenuItem(value: 'ja', child: Text('日本語')),
                  ],
                  onChanged: (value) {
                    if (value != null) {
                      localization.changeLocale(value);
                    }
                  },
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

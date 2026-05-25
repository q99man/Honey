import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';

import '../../../utils/localization.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';
import '../../place/views/my_places_screen.dart';

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
          backgroundColor: success ? Colors.green : Colors.redAccent,
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
                  backgroundColor: success ? Colors.green : Colors.redAccent,
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
                    backgroundColor: Colors.green,
                  ),
                );
              } else {
                scaffoldMessenger.showSnackBar(
                  SnackBar(
                    content: Text(
                      authProvider.errorMessage ?? '인증번호 확인에 실패했습니다.',
                    ),
                    backgroundColor: Colors.redAccent,
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
                  Icon(Icons.verified_user_outlined, color: Color(0xFFFFB300)),
                  SizedBox(width: 8),
                  Text(
                    '휴대폰 본인 인증',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '맛집 등록, 평가 작성, 추천 활동을 위해 본인 인증이 필요합니다.',
                    style: TextStyle(fontSize: 13, color: Colors.black54),
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
                          ? const Icon(Icons.check_circle, color: Colors.green)
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
                    style: TextStyle(color: Colors.black54),
                  ),
                ),
                ElevatedButton(
                  onPressed: _isVerifyingPhone
                      ? null
                      : (_codeSent ? verifyCode : sendCode),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFFB300),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
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
      return Container(
        color: const Color(0xFFFAFAFA),
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Icon(
              Icons.account_circle_outlined,
              size: 80,
              color: Colors.black12,
            ),
            const SizedBox(height: 24),
            Text(
              'auth.loginRequired'.tr,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'auth.phoneVerificationRequired'.tr,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 14,
                color: Colors.black45,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 40),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(builder: (context) => const LoginScreen()),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFFB300),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                elevation: 0,
              ),
              child: const Text(
                '로그인 / 회원가입',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
      );
    }

    final regionVerified = status?.regionVerified ?? false;
    final expPercent = status != null && status.nextLevelExp > 0
        ? (status.exp / status.nextLevelExp).clamp(0.0, 1.0)
        : 0.0;

    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      body: RefreshIndicator(
        onRefresh: () => authProvider.refreshUserProfile(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                elevation: 0,
                color: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: const BorderSide(color: Colors.black12),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      CircleAvatar(
                        radius: 30,
                        backgroundColor:
                            const Color(0xFFFFB300).withValues(alpha: 0.1),
                        child: const Icon(
                          Icons.emoji_nature,
                          color: Color(0xFFFFB300),
                          size: 32,
                        ),
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
                                fontWeight: FontWeight.bold,
                                color: Colors.black87,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              user.displayName,
                              style: const TextStyle(
                                fontSize: 12,
                                color: Colors.black45,
                              ),
                            ),
                          ],
                        ),
                      ),
                      _buildStatusChip(
                        label: user.phoneVerified ? '본인 인증' : '본인 미인증',
                        verified: user.phoneVerified,
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
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFFFF8F00),
                  side: const BorderSide(color: Color(0xFFFFB300), width: 1.2),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
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
                  foregroundColor: Colors.redAccent,
                  side: const BorderSide(color: Colors.redAccent, width: 1.2),
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
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: Colors.black12),
      ),
      child: Padding(
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
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFF8F00).withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    '${'mypage.trust'.tr}: ${status?.trustGrade ?? 'Seed Bee'}',
                    style: const TextStyle(
                      fontSize: 12,
                      color: Color(0xFFFF8F00),
                      fontWeight: FontWeight.bold,
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
                    color: Color(0xFFFFB300),
                  ),
                ),
                Text(
                  '${status?.exp ?? 0} / ${status?.nextLevelExp ?? 0} EXP',
                  style: const TextStyle(
                    fontSize: 13,
                    color: Colors.black54,
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
                backgroundColor: Colors.black12,
                valueColor:
                    const AlwaysStoppedAnimation<Color>(Color(0xFFFFB300)),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              '추천 반영 가중치: x${(status?.recommendWeight ?? 1.0).toStringAsFixed(2)}',
              style: const TextStyle(
                fontSize: 12,
                color: Colors.black54,
                fontStyle: FontStyle.italic,
              ),
            ),
          ],
        ),
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
        color: verified ? Colors.green : Colors.orange,
      ),
      backgroundColor: verified
          ? Colors.green.withValues(alpha: 0.1)
          : Colors.orange.withValues(alpha: 0.1),
      label: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          color: verified ? Colors.green : Colors.orange,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildStatCard(String title, String count) {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: Colors.black12),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontSize: 12, color: Colors.black54),
            ),
            const SizedBox(height: 8),
            Text(
              count,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
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
          color: Colors.orange.withValues(alpha: 0.08),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.orange.withValues(alpha: 0.3)),
        ),
        child: Row(
          children: [
            Icon(icon, color: Colors.orange),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                      color: Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    description,
                    style: const TextStyle(fontSize: 12, color: Colors.black54),
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
              const Icon(Icons.chevron_right, color: Colors.orange),
          ],
        ),
      ),
    );
  }

  Widget _buildLanguageCard() {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: Colors.black12),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                const Icon(Icons.language, color: Color(0xFFFFB300)),
                const SizedBox(width: 12),
                Text(
                  'mypage.language'.tr,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
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

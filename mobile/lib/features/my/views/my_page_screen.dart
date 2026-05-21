import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/views/login_screen.dart';

class MyPageScreen extends StatefulWidget {
  const MyPageScreen({super.key});

  @override
  State<MyPageScreen> createState() => _MyPageScreenState();
}

class _MyPageScreenState extends State<MyPageScreen> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();
  bool _codeSent = false;
  bool _isVerifying = false;

  @override
  void dispose() {
    _phoneController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  void _showPhoneVerificationDialog(BuildContext context) {
    final scaffoldMessenger = ScaffoldMessenger.of(context);
    setState(() {
      _phoneController.clear();
      _codeController.clear();
      _codeSent = false;
      _isVerifying = false;
    });

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setStateInDialog) {
            final authProvider = Provider.of<AuthProvider>(context, listen: false);

            void sendCode() async {
              final phone = _phoneController.text.trim();
              if (phone.isEmpty || !RegExp(r'^010[0-9]{7,8}$').hasMatch(phone)) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('올바른 휴대폰 번호를 입력해주세요.')),
                );
                return;
              }

              setStateInDialog(() => _isVerifying = true);
              final success = await authProvider.sendPhoneVerification(phone);
              if (!mounted || !dialogContext.mounted) return;
              setStateInDialog(() {
                _isVerifying = false;
                if (success) {
                  _codeSent = true;
                  scaffoldMessenger.showSnackBar(
                    const SnackBar(content: Text('인증코드가 발송되었습니다. (인증코드: 123456)')),
                  );
                }
              });
            }

            void verifyCode() async {
              final phone = _phoneController.text.trim();
              final code = _codeController.text.trim();
              if (code.length != 6) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('6자리 인증번호를 입력해주세요.')),
                );
                return;
              }

              setStateInDialog(() => _isVerifying = true);
              final success = await authProvider.verifyPhoneCode(phone, code);
              if (!mounted || !dialogContext.mounted) return;
              setStateInDialog(() => _isVerifying = false);

              if (success) {
                Navigator.of(dialogContext).pop();
                scaffoldMessenger.showSnackBar(
                  const SnackBar(
                    content: Text('휴대폰 본인 인증이 완료되었습니다! 🎉'),
                    backgroundColor: Colors.green,
                  ),
                );
              } else {
                scaffoldMessenger.showSnackBar(
                  SnackBar(
                    content: Text(authProvider.errorMessage ?? '인증코드 확인에 실패했습니다.'),
                    backgroundColor: Colors.redAccent,
                  ),
                );
              }
            }

            return AlertDialog(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              title: const Row(
                children: [
                  Icon(Icons.verified_user_outlined, color: Color(0xFFFFB300)),
                  SizedBox(width: 8),
                  Text('휴대폰 본인 인증', style: TextStyle(fontWeight: FontWeight.bold)),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '맛집 등록, 댓글 작성, 추천 활동을 위해 본인 인증이 필요합니다.',
                    style: TextStyle(fontSize: 13, color: Colors.black54),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    enabled: !_codeSent && !_isVerifying,
                    decoration: InputDecoration(
                      labelText: '휴대폰 번호 (- 없이)',
                      hintText: '01012345678',
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
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
                      maxLength: 6,
                      enabled: !_isVerifying,
                      decoration: InputDecoration(
                        labelText: '인증 번호 (6자리)',
                        hintText: '123456',
                        counterText: '',
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                    ),
                  ],
                ],
              ),
              actions: [
                TextButton(
                  onPressed: _isVerifying ? null : () => Navigator.of(dialogContext).pop(),
                  child: const Text('취소', style: TextStyle(color: Colors.black54)),
                ),
                ElevatedButton(
                  onPressed: _isVerifying
                      ? null
                      : (_codeSent ? verifyCode : sendCode),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFFB300),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  child: _isVerifying
                      ? const SizedBox(
                          height: 16,
                          width: 16,
                          child: CircularProgressIndicator(strokeWidth: 2, valueColor: AlwaysStoppedAnimation(Colors.white)),
                        )
                      : Text(_codeSent ? '인증 완료' : '인증코드 발송'),
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

    // 1. Guest View
    if (!authProvider.isAuthenticated || user == null) {
      return Container(
        color: const Color(0xFFFAFAFA),
        padding: const EdgeInsets.symmetric(horizontal: 24.0),
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
            const Text(
              '더 풍성한 꿀벌 활동을 해보세요!',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              '로그인하시면 나만의 활동 기록, 맛집 추천,\n실시간 성장 지표를 확인하실 수 있습니다.',
              textAlign: TextAlign.center,
              style: TextStyle(
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

    // 2. Authenticated Profile View
    final expPercent = status != null && status.nextLevelExp > 0
        ? (status.exp / status.nextLevelExp).clamp(0.0, 1.0)
        : 0.0;

    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      body: RefreshIndicator(
        onRefresh: () => authProvider.refreshUserProfile(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Profile Card
              Card(
                elevation: 0,
                color: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                  side: const BorderSide(color: Colors.black12),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    children: [
                      CircleAvatar(
                        radius: 30,
                        backgroundColor: const Color(0xFFFFB300).withValues(alpha: 0.1),
                        child: const Icon(Icons.emoji_nature, color: Color(0xFFFFB300), size: 32),
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
                              user.username,
                              style: const TextStyle(
                                fontSize: 12,
                                color: Colors.black45,
                              ),
                            ),
                          ],
                        ),
                      ),
                      // Verification chip
                      Chip(
                        labelPadding: const EdgeInsets.symmetric(horizontal: 4),
                        avatar: Icon(
                          user.phoneVerified ? Icons.verified : Icons.error_outline,
                          size: 14,
                          color: user.phoneVerified ? Colors.green : Colors.orange,
                        ),
                        backgroundColor: user.phoneVerified
                            ? Colors.green.withValues(alpha: 0.1)
                            : Colors.orange.withValues(alpha: 0.1),
                        label: Text(
                          user.phoneVerified ? '인증됨' : '미인증',
                          style: TextStyle(
                            fontSize: 11,
                            color: user.phoneVerified ? Colors.green : Colors.orange,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),

              // Growth status card
              Card(
                elevation: 0,
                color: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                  side: const BorderSide(color: Colors.black12),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text(
                            '꿀벌 성장 상태',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                              color: Colors.black87,
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFF8F00).withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Text(
                              '등급: ${status?.trustGrade ?? 'Seed Bee'}',
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
                          valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFFFFB300)),
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        '💡 추천 반영 가중치: x${(status?.recommendWeight ?? 1.0).toStringAsFixed(2)}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.black54,
                          fontStyle: FontStyle.italic,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),

              // Activity stats grid
              GridView.count(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 1.5,
                children: [
                  _buildStatCard('👍 맛집 추천', '${summary?.recommendationCount ?? 0}회'),
                  _buildStatCard('📍 방문 인증', '${summary?.visitCount ?? 0}회'),
                  _buildStatCard('💬 작성 댓글', '${summary?.commentCount ?? 0}개'),
                  _buildStatCard('🌸 등록 맛집', '${summary?.placeCount ?? 0}곳'),
                ],
              ),
              const SizedBox(height: 16),

              // Action card (Verification / Account setup)
              if (!user.phoneVerified)
                InkWell(
                  onTap: () => _showPhoneVerificationDialog(context),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                    decoration: BoxDecoration(
                      color: Colors.orange.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.orange.withValues(alpha: 0.3)),
                    ),
                    child: const Row(
                      children: [
                        Icon(Icons.error_outline, color: Colors.orange),
                        SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '휴대폰 인증이 필요합니다',
                                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: Colors.black87),
                              ),
                              SizedBox(height: 2),
                              Text(
                                '본인 인증을 완료해야 꽃 등록 및 탐험이 가능합니다.',
                                style: TextStyle(fontSize: 12, color: Colors.black54),
                              ),
                            ],
                          ),
                        ),
                        Icon(Icons.chevron_right, color: Colors.orange),
                      ],
                    ),
                  ),
                ),
              const SizedBox(height: 24),

              // Logout Button
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

  Widget _buildStatCard(String title, String count) {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: Colors.black12),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
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
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.black87),
            ),
          ],
        ),
      ),
    );
  }
}

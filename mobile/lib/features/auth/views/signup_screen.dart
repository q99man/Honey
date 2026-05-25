import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key});

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _nicknameController = TextEditingController();
  final _phoneController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _nicknameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submitSignup() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final success = await authProvider.signup(
      _emailController.text.trim(),
      _passwordController.text,
      _nicknameController.text.trim(),
      _phoneController.text.trim(),
    );

    if (!mounted) {
      return;
    }

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('회원가입이 완료되었습니다. 로그인해주세요.'),
          backgroundColor: Colors.green,
        ),
      );
      Navigator.of(context).pop();
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(authProvider.errorMessage ?? '회원가입에 실패했습니다.'),
        backgroundColor: Colors.redAccent,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isLoading = context.watch<AuthProvider>().isLoading;

    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      appBar: AppBar(
        title: const Text(
          '회원가입',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.white,
        elevation: 0.5,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.black87),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  '허니통 회원 정보 입력',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  '서비스 이용을 위해 필요한 정보를 입력해주세요.',
                  style: TextStyle(fontSize: 14, color: Colors.black54),
                ),
                const SizedBox(height: 24),
                _buildEmailField(isLoading),
                const SizedBox(height: 16),
                _buildNicknameField(isLoading),
                const SizedBox(height: 16),
                _buildPhoneField(isLoading),
                const SizedBox(height: 16),
                _buildPasswordField(isLoading),
                const SizedBox(height: 16),
                _buildConfirmPasswordField(isLoading),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: isLoading ? null : _submitSignup,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFFB300),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    elevation: 0,
                  ),
                  child: isLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor:
                                AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : const Text(
                          '회원가입 완료',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildEmailField(bool isLoading) {
    return TextFormField(
      controller: _emailController,
      keyboardType: TextInputType.emailAddress,
      enabled: !isLoading,
      decoration: _inputDecoration('이메일 주소', Icons.email_outlined),
      validator: (value) {
        final text = value?.trim() ?? '';
        if (text.isEmpty) {
          return '이메일을 입력해주세요.';
        }
        if (!RegExp(r'^[\w-.]+@([\w-]+\.)+[\w-]{2,}$').hasMatch(text)) {
          return '올바른 이메일 형식이 아닙니다.';
        }
        return null;
      },
    );
  }

  Widget _buildNicknameField(bool isLoading) {
    return TextFormField(
      controller: _nicknameController,
      enabled: !isLoading,
      decoration: _inputDecoration('닉네임', Icons.person_outline),
      validator: (value) {
        final text = value?.trim() ?? '';
        if (text.isEmpty) {
          return '닉네임을 입력해주세요.';
        }
        if (text.length < 2) {
          return '닉네임은 2자 이상이어야 합니다.';
        }
        return null;
      },
    );
  }

  Widget _buildPhoneField(bool isLoading) {
    return TextFormField(
      controller: _phoneController,
      keyboardType: TextInputType.phone,
      enabled: !isLoading,
      decoration: _inputDecoration(
        '휴대폰 번호',
        Icons.phone_iphone_outlined,
        hintText: '01012345678',
      ),
      validator: (value) {
        final text = value?.trim() ?? '';
        if (text.isEmpty) {
          return '휴대폰 번호를 입력해주세요.';
        }
        if (!RegExp(r'^010[0-9]{7,8}$').hasMatch(text)) {
          return '올바른 휴대폰 번호 형식이 아닙니다. 예: 01012345678';
        }
        return null;
      },
    );
  }

  Widget _buildPasswordField(bool isLoading) {
    return TextFormField(
      controller: _passwordController,
      obscureText: true,
      enabled: !isLoading,
      decoration: _inputDecoration('비밀번호', Icons.lock_outline),
      validator: (value) {
        final text = value ?? '';
        if (text.isEmpty) {
          return '비밀번호를 입력해주세요.';
        }
        if (text.length < 8) {
          return '비밀번호는 8자 이상이어야 합니다.';
        }
        return null;
      },
    );
  }

  Widget _buildConfirmPasswordField(bool isLoading) {
    return TextFormField(
      controller: _confirmPasswordController,
      obscureText: true,
      enabled: !isLoading,
      decoration: _inputDecoration('비밀번호 확인', Icons.lock_outline),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '비밀번호 확인을 입력해주세요.';
        }
        if (value != _passwordController.text) {
          return '비밀번호가 일치하지 않습니다.';
        }
        return null;
      },
    );
  }

  InputDecoration _inputDecoration(
    String label,
    IconData icon, {
    String? hintText,
  }) {
    return InputDecoration(
      labelText: label,
      hintText: hintText,
      prefixIcon: Icon(icon),
      filled: true,
      fillColor: Colors.white,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
    );
  }
}

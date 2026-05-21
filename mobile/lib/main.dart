import 'package:flutter/material.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart' as kakao;
import 'package:provider/provider.dart';
import 'core/api/api_client.dart';
import 'features/auth/providers/auth_provider.dart';
import 'features/auth/services/auth_service.dart';
import 'features/auth/views/login_screen.dart';
import 'features/home/views/home_map_screen.dart';
import 'features/my/views/my_page_screen.dart';
import 'features/place/services/place_service.dart';
import 'features/ranking/services/ranking_service.dart';
import 'features/ranking/views/ranking_screen.dart';
import 'features/wishlist/services/wishlist_service.dart';
import 'features/wishlist/views/wishlist_screen.dart';
import 'features/community/services/community_service.dart';
import 'features/community/views/community_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Kakao SDK.
  // Using try-catch to avoid crashes on startup if native app keys are blank or unconfigured in Android/iOS builds
  try {
    kakao.KakaoSdk.init(nativeAppKey: 'a123bc456def789ghi012jkl345mno67'); // Placeholder native app key
  } catch (e) {
    debugPrint("Kakao SDK Initialization failed (harmless in local dev/emulators): $e");
  }

  runApp(const HoneytongApp());
}

class HoneytongApp extends StatelessWidget {
  const HoneytongApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        // Inject ApiClient
        Provider<ApiClient>(
          create: (_) => ApiClient(),
        ),
        // Inject AuthService
        ProxyProvider<ApiClient, AuthService>(
          update: (_, apiClient, __) => AuthService(apiClient),
        ),
        // Inject PlaceService
        ProxyProvider<ApiClient, PlaceService>(
          update: (_, apiClient, __) => PlaceService(apiClient),
        ),
        // Inject RankingService
        ProxyProvider<ApiClient, RankingService>(
          update: (_, apiClient, __) => RankingService(apiClient),
        ),
        // Inject WishlistService
        ProxyProvider<ApiClient, WishlistService>(
          update: (_, apiClient, __) => WishlistService(apiClient),
        ),
        // Inject CommunityService
        ProxyProvider<ApiClient, CommunityService>(
          update: (_, apiClient, __) => CommunityService(apiClient),
        ),
        // Inject AuthProvider (ChangeNotifier)
        ChangeNotifierProvider<AuthProvider>(
          create: (context) => AuthProvider(
            Provider.of<AuthService>(context, listen: false),
          ),
        ),
      ],
      child: MaterialApp(
        title: '허니통 (Honeytong)',
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFFFFB300), // Honey/Amber brand color
            primary: const Color(0xFFFFB300),
            secondary: const Color(0xFFFF8F00),
            background: const Color(0xFFFAFAFA),
          ),
          fontFamily: 'NanumSquare',
        ),
        home: const MainNavigationScreen(),
        debugShowCheckedModeBanner: false,
      ),
    );
  }
}

class MainNavigationScreen extends StatefulWidget {
  const MainNavigationScreen({super.key});

  @override
  State<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends State<MainNavigationScreen> {
  int _currentIndex = 0;

  // Screens list matching the BottomNavigationBar tabs
  late final List<Widget> _screens;

  @override
  void initState() {
    super.initState();
    _screens = [
      const HomeMapScreen(), // 홈 (지도로 맛집 탐험)
      const RankingScreen(), // 실시간 랭킹 화면
      WishlistScreen(
        onExploreTap: () {
          setState(() {
            _currentIndex = 0;
          });
        },
      ),
      CommunityScreen(
        onTabSwitch: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
      ),
      const MyPageScreen(), // 마이페이지 (성장 지표 및 휴대폰 인증)
    ];
  }

  void _onTabTapped(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  void _showLoginRequiredPrompt() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('로그인 필요', style: TextStyle(fontWeight: FontWeight.bold)),
        content: const Text('위시리스트 기능은 로그인 후 이용하실 수 있습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소', style: TextStyle(color: Colors.black54)),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context); // Close dialog
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const LoginScreen()),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFFFB300),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            child: const Text('로그인하기'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: IndexedStack(
          index: _currentIndex,
          children: _screens,
        ),
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _onTabTapped,
        type: BottomNavigationBarType.fixed,
        selectedItemColor: Theme.of(context).colorScheme.secondary,
        unselectedItemColor: Colors.grey,
        backgroundColor: Colors.white,
        elevation: 8,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.map_outlined),
            activeIcon: Icon(Icons.map),
            label: '홈',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.leaderboard_outlined),
            activeIcon: Icon(Icons.leaderboard),
            label: '랭킹',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.bookmark_outline),
            activeIcon: Icon(Icons.bookmark),
            label: '저장',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.forum_outlined),
            activeIcon: Icon(Icons.forum),
            label: '커뮤니티',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.person_outline),
            activeIcon: Icon(Icons.person),
            label: '마이',
          ),
        ],
      ),
    );
  }
}

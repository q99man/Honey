import 'package:flutter/material.dart';
import 'package:kakao_maps_flutter/kakao_maps_flutter.dart' as kakao_map;
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart' as kakao;
import 'package:provider/provider.dart';
import 'core/api/api_client.dart';
import 'core/config/app_config.dart';
import 'features/auth/providers/auth_provider.dart';
import 'features/auth/services/auth_service.dart';
import 'features/home/views/home_map_screen.dart';
import 'features/my/views/my_page_screen.dart';
import 'features/place/services/place_service.dart';
import 'features/ranking/services/ranking_service.dart';
import 'features/ranking/views/ranking_screen.dart';
import 'features/wishlist/services/wishlist_service.dart';
import 'features/wishlist/views/wishlist_screen.dart';
import 'features/community/services/community_service.dart';
import 'features/community/views/community_screen.dart';

import 'utils/localization.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Localization singleton.
  final localization = Localization();
  await localization.init();

  if (AppConfig.kakaoNativeAppKey.isNotEmpty) {
    try {
      await kakao_map.KakaoMapsFlutter.init(AppConfig.kakaoNativeAppKey);
    } catch (e) {
      debugPrint('카카오맵 SDK 초기화 실패: $e');
    }

    try {
      kakao.KakaoSdk.init(nativeAppKey: AppConfig.kakaoNativeAppKey);
    } catch (e) {
      debugPrint('카카오 로그인 SDK 초기화 실패: $e');
    }
  } else {
    debugPrint('KAKAO_NATIVE_APP_KEY가 없어 카카오 SDK 초기화를 건너뜁니다.');
  }

  runApp(const HoneytongApp());
}

class HoneytongApp extends StatelessWidget {
  const HoneytongApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        // Inject Localization
        ChangeNotifierProvider<Localization>.value(
          value: Localization(),
        ),
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
      child: Consumer<Localization>(
        builder: (context, localization, child) {
          return MaterialApp(
            title: '허니통 (Honeytong)',
            theme: ThemeData(
              useMaterial3: true,
              colorScheme: ColorScheme.fromSeed(
                seedColor: const Color(0xFFFFB300), // Honey/Amber brand color
                primary: const Color(0xFFFFB300),
                secondary: const Color(0xFFFF8F00),
                surface: const Color(0xFFFAFAFA),
              ),
              fontFamily: 'NanumSquare',
            ),
            home: const MainNavigationScreen(),
            debugShowCheckedModeBanner: false,
          );
        },
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
        items: [
          BottomNavigationBarItem(
            icon: const Icon(Icons.map_outlined),
            activeIcon: const Icon(Icons.map),
            label: 'nav.home'.tr,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.leaderboard_outlined),
            activeIcon: const Icon(Icons.leaderboard),
            label: 'nav.ranking'.tr,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.bookmark_outline),
            activeIcon: const Icon(Icons.bookmark),
            label: 'nav.save'.tr,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.forum_outlined),
            activeIcon: const Icon(Icons.forum),
            label: 'nav.community'.tr,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.person_outline),
            activeIcon: const Icon(Icons.person),
            label: 'nav.my'.tr,
          ),
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/api/api_client.dart';
import '../../../models/place.dart';
import '../services/place_service.dart';
import 'place_detail_screen.dart';
import 'place_register_screen.dart';

class MyPlacesScreen extends StatefulWidget {
  const MyPlacesScreen({super.key});

  @override
  State<MyPlacesScreen> createState() => _MyPlacesScreenState();
}

class _MyPlacesScreenState extends State<MyPlacesScreen> {
  late final PlaceService _placeService;
  late Future<List<Place>> _placesFuture;
  bool _isDeleting = false;

  @override
  void initState() {
    super.initState();
    _placeService = PlaceService(context.read<ApiClient>());
    _placesFuture = _placeService.getMyRegisteredPlaces();
  }

  Future<void> _refresh() async {
    setState(() {
      _placesFuture = _placeService.getMyRegisteredPlaces();
    });
    await _placesFuture;
  }

  Future<void> _openDetail(Place place) async {
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (context) => PlaceDetailScreen(placeId: place.id),
      ),
    );
    if (changed == true) {
      await _refresh();
    }
  }

  Future<void> _openEdit(Place place) async {
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (context) => PlaceRegisterScreen(placeId: place.id),
      ),
    );
    if (changed == true) {
      await _refresh();
    }
  }

  Future<void> _deletePlace(Place place) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('맛집 삭제'),
        content: Text('${place.name}을(를) 삭제할까요? 삭제한 맛집은 목록과 지도에서 숨겨집니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.of(context).pop(true),
            icon: const Icon(Icons.delete_outline),
            label: const Text('삭제'),
            style: FilledButton.styleFrom(backgroundColor: Colors.redAccent),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    setState(() => _isDeleting = true);
    final result = await _placeService.deletePlace(place.id);
    if (!mounted) return;
    setState(() => _isDeleting = false);

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(result['message'] ?? '처리 결과를 확인할 수 없습니다.'),
        backgroundColor:
            result['success'] == true ? Colors.green : Colors.redAccent,
      ),
    );
    if (result['success'] == true) {
      await _refresh();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      appBar: AppBar(
        title: const Text(
          '내 등록 맛집',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        elevation: 0.5,
        iconTheme: const IconThemeData(color: Colors.black87),
      ),
      body: Stack(
        children: [
          FutureBuilder<List<Place>>(
            future: _placesFuture,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(
                  child: CircularProgressIndicator(
                    valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300)),
                  ),
                );
              }

              final places = snapshot.data ?? [];
              if (places.isEmpty) {
                return RefreshIndicator(
                  onRefresh: _refresh,
                  child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(24),
                    children: const [
                      SizedBox(height: 120),
                      Icon(Icons.storefront_outlined,
                          size: 56, color: Colors.black26),
                      SizedBox(height: 16),
                      Text(
                        '아직 등록한 맛집이 없습니다.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.black54,
                        ),
                      ),
                    ],
                  ),
                );
              }

              return RefreshIndicator(
                onRefresh: _refresh,
                child: ListView.separated(
                  padding: const EdgeInsets.all(16),
                  itemBuilder: (context, index) => _PlaceManageTile(
                    place: places[index],
                    onTap: () => _openDetail(places[index]),
                    onEdit: () => _openEdit(places[index]),
                    onDelete: () => _deletePlace(places[index]),
                  ),
                  separatorBuilder: (context, index) =>
                      const SizedBox(height: 10),
                  itemCount: places.length,
                ),
              );
            },
          ),
          if (_isDeleting)
            Container(
              color: Colors.black26,
              child: const Center(
                child: CircularProgressIndicator(
                  valueColor: AlwaysStoppedAnimation(Color(0xFFFFB300)),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _PlaceManageTile extends StatelessWidget {
  const _PlaceManageTile({
    required this.place,
    required this.onTap,
    required this.onEdit,
    required this.onDelete,
  });

  final Place place;
  final VoidCallback onTap;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final address = place.addressRoad.isNotEmpty
        ? place.addressRoad
        : place.addressJibun;

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.black12),
          ),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFB300).withValues(alpha: 0.14),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.store, color: Color(0xFFFFB300)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      place.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      address.isEmpty ? '주소 정보 없음' : address,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12,
                        color: Colors.black54,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                tooltip: '수정',
                onPressed: onEdit,
                icon: const Icon(Icons.edit_outlined),
                color: Colors.black54,
              ),
              IconButton(
                tooltip: '삭제',
                onPressed: onDelete,
                icon: const Icon(Icons.delete_outline),
                color: Colors.redAccent,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

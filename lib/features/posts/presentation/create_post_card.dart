import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';

import '../data/post_repository.dart';

class CreatePostCard extends StatefulWidget {
  const CreatePostCard({super.key, required this.onCreated});
  final Future<void> Function() onCreated;

  @override
  State<CreatePostCard> createState() => _CreatePostCardState();
}

class _CreatePostCardState extends State<CreatePostCard> {
  final _controller = TextEditingController();
  final _picker = ImagePicker();
  final _files = <File>[];
  double? _latitude;
  double? _longitude;
  String? _locationName;
  bool _loading = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _addImages() async {
    final images = await _picker.pickMultiImage();
    setState(() => _files.addAll(images.map((e) => File(e.path))));
  }

  Future<void> _addFile() async {
    final result = await FilePicker.platform.pickFiles(allowMultiple: true);
    if (result == null) return;
    setState(() => _files.addAll(result.files.where((f) => f.path != null).map((f) => File(f.path!))));
  }

  Future<void> _useLocation() async {
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) permission = await Geolocator.requestPermission();
    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) return;
    final position = await Geolocator.getCurrentPosition();
    setState(() {
      _latitude = position.latitude;
      _longitude = position.longitude;
      _locationName = '${position.latitude.toStringAsFixed(5)}, ${position.longitude.toStringAsFixed(5)}';
    });
  }

  Future<void> _submit() async {
    setState(() => _loading = true);
    try {
      await PostRepository(SupabaseHolder.client).createPost(PostDraft(
        content: _controller.text,
        latitude: _latitude,
        longitude: _longitude,
        locationName: _locationName,
        files: List.unmodifiable(_files),
      ));
      _controller.clear();
      _files.clear();
      _latitude = null;
      _longitude = null;
      _locationName = null;
      await widget.onCreated();
    } on PostValidationException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Could not create post: $e')));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              TextField(controller: _controller, maxLines: 4, minLines: 2, decoration: const InputDecoration(hintText: 'What do you want to share?', border: InputBorder.none)),
              if (_files.isNotEmpty)
                Align(alignment: Alignment.centerLeft, child: Wrap(spacing: 8, children: [for (final file in _files) Chip(label: Text(file.path.split('/').last), onDeleted: () => setState(() => _files.remove(file))) ])),
              if (_locationName != null) Align(alignment: Alignment.centerLeft, child: Text('Location: $_locationName')),
              const Divider(),
              Row(
                children: [
                  IconButton(tooltip: 'Photos', onPressed: _loading ? null : _addImages, icon: const Icon(Icons.photo_library_outlined)),
                  IconButton(tooltip: 'File', onPressed: _loading ? null : _addFile, icon: const Icon(Icons.attach_file)),
                  IconButton(tooltip: 'Location', onPressed: _loading ? null : _useLocation, icon: const Icon(Icons.location_on_outlined)),
                  const Spacer(),
                  FilledButton(onPressed: _loading ? null : _submit, child: const Text('Post')),
                ],
              ),
            ],
          ),
        ),
      );
}

// Kept as a tiny adapter so feature widgets do not import global configuration.
class SupabaseHolder {
  static final client = __supabaseClient;
}

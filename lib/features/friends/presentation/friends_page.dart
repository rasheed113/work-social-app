import 'package:flutter/material.dart';

import '../../../core/design/app_tokens.dart';

class FriendsPage extends StatelessWidget {
  const FriendsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 110),
      children: [
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            borderRadius: AppTokens.radiusLg,
            gradient: AppTokens.heroGradient,
            boxShadow: AppTokens.heroShadow,
          ),
          child: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Friends',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 28,
                  fontWeight: FontWeight.w900,
                ),
              ),
              SizedBox(height: 6),
              Text(
                'Find people, manage requests, and stay connected.',
                style: TextStyle(color: Colors.white70, height: 1.4),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          decoration: InputDecoration(
            prefixIcon: const Icon(Icons.search),
            hintText: 'Search people',
            suffixIcon: IconButton(
              tooltip: 'Clear search',
              onPressed: null,
              icon: const Icon(Icons.close),
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Text(
              'Your friends and pending requests will appear here.',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppTokens.textSecondary,
                  ),
            ),
          ),
        ),
      ],
    );
  }
}

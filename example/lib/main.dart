import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:kiosk_lock_task/kiosk_lock_task.dart';

void main() {
  runApp(const KioskExampleApp());
}

class KioskExampleApp extends StatelessWidget {
  const KioskExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Kiosk Lock Task Example',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const KioskHomePage(),
    );
  }
}

class KioskHomePage extends StatefulWidget {
  const KioskHomePage({super.key});

  @override
  State<KioskHomePage> createState() => _KioskHomePageState();
}

class _KioskHomePageState extends State<KioskHomePage> {
  final _kioskLockTaskPlugin = KioskLockTask();
  bool _isKioskEnabled = false;
  bool _isDeviceOwner = false;
  bool _launchOnBoot = false;
  bool _loading = false;
  String _message = '상태를 확인해 주세요.';

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    await _run(() async {
      final isEnabled = await _kioskLockTaskPlugin.isEnabled();
      final isDeviceOwner = await _kioskLockTaskPlugin.isDeviceOwner();
      final launchOnBoot = await _kioskLockTaskPlugin.isLaunchOnBootEnabled();
      setState(() {
        _isKioskEnabled = isEnabled;
        _isDeviceOwner = isDeviceOwner;
        _launchOnBoot = launchOnBoot;
        _message = isEnabled ? '키오스크 모드가 켜져 있습니다.' : '키오스크 모드가 꺼져 있습니다.';
      });
    });
  }

  Future<void> _enable() async {
    await _run(() async {
      final isEnabled = await _kioskLockTaskPlugin.enable();
      setState(() {
        _isKioskEnabled = isEnabled;
        _message = isEnabled ? '키오스크 모드를 켰습니다.' : 'Android 화면 고정 확인 후 다시 확인해 주세요.';
      });
    });
  }

  Future<void> _disable() async {
    await _run(() async {
      final isEnabled = await _kioskLockTaskPlugin.disable();
      setState(() {
        _isKioskEnabled = isEnabled;
        _message = isEnabled ? '키오스크 해제 요청을 보냈습니다.' : '키오스크 모드를 껐습니다.';
      });
    });
  }

  Future<void> _setLaunchOnBoot(bool enabled) async {
    await _run(() async {
      await _kioskLockTaskPlugin.setLaunchOnBootEnabled(enabled);
      final launchOnBoot = await _kioskLockTaskPlugin.isLaunchOnBootEnabled();
      setState(() {
        _launchOnBoot = launchOnBoot;
        _message = launchOnBoot ? '부팅 후 자동 실행을 켰습니다.' : '부팅 후 자동 실행을 껐습니다.';
      });
    });
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() {
      _loading = true;
    });

    try {
      await action();
    } on PlatformException catch (error) {
      setState(() {
        _message = error.message ?? '키오스크 요청에 실패했습니다.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Kiosk Lock Task'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _refresh,
            icon: const Icon(Icons.refresh),
            tooltip: '상태 새로고침',
          ),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 520),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Icon(
                    _isKioskEnabled ? Icons.lock : Icons.lock_open,
                    size: 88,
                    color: _isKioskEnabled ? Colors.green : Colors.redAccent,
                  ),
                  const SizedBox(height: 24),
                  Text(
                    _isKioskEnabled ? '키오스크 모드 ON' : '키오스크 모드 OFF',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  const SizedBox(height: 12),
                  Text(_message, textAlign: TextAlign.center),
                  const SizedBox(height: 20),
                  Text(
                    'Device Owner: ${_isDeviceOwner ? "YES" : "NO"}',
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 28),
                  FilledButton.icon(
                    onPressed: _loading || _isKioskEnabled ? null : _enable,
                    icon: const Icon(Icons.lock),
                    label: const Text('키오스크 모드 켜기'),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _loading ? null : _disable,
                    icon: const Icon(Icons.lock_open),
                    label: const Text('키오스크 모드 끄기'),
                  ),
                  const SizedBox(height: 12),
                  SwitchListTile(
                    value: _launchOnBoot,
                    onChanged: _loading ? null : _setLaunchOnBoot,
                    title: const Text('부팅 후 앱 자동 실행'),
                  ),
                  if (_loading) ...[
                    const SizedBox(height: 20),
                    const Center(child: CircularProgressIndicator()),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

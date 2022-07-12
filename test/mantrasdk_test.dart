import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mantrasdk/mantrasdk.dart';

void main() {
  const MethodChannel channel = MethodChannel('mantrasdk');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await Mantrasdk.platformVersion, '42');
  });
}

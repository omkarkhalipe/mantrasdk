import 'dart:async';

import 'package:flutter/services.dart';

class Mantrasdk {
  static const MethodChannel _channel = MethodChannel('mantrasdk');

  static Future<String?> get platformVersion async {
    await _channel.invokeMethod('invokeCapture');
    final String? version = "iPhone";
    return version;
  }

  static Future<String?> get getFinger async {
    final String? version = await _channel.invokeMethod('getFinger');
    print("return value "+version!);
    return version;
  }
}

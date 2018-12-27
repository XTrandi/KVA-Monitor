cd C:\Users\z00352um\AppData\Local\Android\Sdk\platform-tools
adb forward tcp:4444 localabstract:/adb-hub
adb connect 127.0.0.1:4444
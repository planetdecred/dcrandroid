# $ANDROID_HOME/tools/bin/sdkmanager 'system-images;android-28;google_apis;x86'
$ANDROID_HOME/tools/bin/sdkmanager --licenses

$ANDROID_HOME/tools/bin/avdmanager list

echo no | $ANDROID_HOME/tools/bin/avdmanager create avd -n testemu -k 'system-images;android-28;google_apis;x86'
$ANDROID_HOME/tools/bin/avdmanager list avd
$ANDROID_HOME/tools/emulator -avd testemu -no-window -no-boot-anim -no-audio -verbose &

$ANDROID_HOME/platform-tools/adb wait-for-device

# emulator isn't ready yet, wait 1 min more
# prevents APK installation error
sleep 60

./gradlew connectedTstnetDebugAndroidTest
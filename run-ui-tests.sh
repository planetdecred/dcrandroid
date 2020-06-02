echo no | $ANDROID_HOME/tools/bin/avdmanager create avd --force -n testemu --abi default/386 --package 'system-images;android-28;google_apis;x86_64'
$ANDROID_HOME/tools/bin/avdmanager list avd
$ANDROID_HOME/tools/emulator -avd testemu -no-window -no-boot-anim -no-audio -verbose &


# emulator isn't ready yet, wait 1 min more
# prevents APK installation error
sleep 60

./gradlew connectedTstnetDebugAndroidTest
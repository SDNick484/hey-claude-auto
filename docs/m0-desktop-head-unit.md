# Milestone 0: trying the spike on the Desktop Head Unit

Milestone 0 answers one question: can a turn be started from the car screen
when no message is waiting? It builds two throwaway apps that can be installed
side by side:

| App on the car screen | Package | What it is |
|---|---|---|
| **Claude T** (teal icon) | `io.github.sdnick484.heyclaude.spike.t` | Templated messaging: a Car App Library screen listing one conversation, plus notifications. |
| **Claude N** (orange icon) | `io.github.sdnick484.heyclaude.spike.n` | Messaging notifications only. No Car App Library code. |

Both echo back whatever is dictated, three seconds later, as a new message
from Claude. Neither touches the network.

These steps were checked on Gentoo with Android Studio installed from Portage
(`dev-util/android-studio`), its SDK in `~/Android/Sdk`, and `adb` from
`dev-util/android-tools`. Commands marked `#` run as root, `$` as you.

## 1. One-time setup on the computer

**Libraries.** DHU 2.0 links against `libc++.so.1` and `libc++abi.so.1`, which
a default Gentoo install does not have. It also needs glibc 2.32 or newer.

```
# emerge --ask llvm-runtimes/libcxx llvm-runtimes/libcxxabi
```

**SDK command-line tools.** There is no system JDK on this machine, so point
`JAVA_HOME` at the one bundled with Android Studio.

```
$ export ANDROID_HOME="$HOME/Android/Sdk"
$ export JAVA_HOME=/opt/android-studio/jbr
$ cd /tmp
$ curl -fLO https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip
$ echo "4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583  commandlinetools-linux-15859902_latest.zip" | sha256sum -c
$ mkdir -p "$ANDROID_HOME/cmdline-tools"
$ unzip -q commandlinetools-linux-15859902_latest.zip -d "$ANDROID_HOME/cmdline-tools"
$ mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
```

The checksum is the one published on the Android Studio download page. To
keep the two variables for later shells, add the two `export` lines with
`vim ~/.bashrc`.

**The Desktop Head Unit.**

```
$ "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" "extras;google;auto"
$ ls "$ANDROID_HOME/extras/google/auto/desktop-head-unit"
```

`sdkmanager` prints a notice that it is deprecated in favour of `android sdk`.
It still works. If you would rather click: Android Studio, Settings,
Languages & Frameworks, Android SDK, SDK Tools tab, "Android Auto Desktop Head
Unit Emulator".

## 2. One-time setup on the phone

1. Turn on Developer options and USB debugging (Settings, About phone, tap
   Build number seven times; then Settings, System, Developer options, USB
   debugging).
2. Update the Android Auto app from the Play Store.
3. Open Android Auto's settings: Settings, Apps, See all apps, Android Auto,
   Additional settings in the app. On recent Pixels the same screen is under
   Settings, Connected devices, Connection preferences, Android Auto.
4. Scroll to the bottom and tap **Version** to reveal **Version and permission
   info**. Tap that ten times and accept "Allow development settings?".
5. Open the three-dot menu, choose **Developer settings**, and turn on
   **Unknown sources**. This is what lets a sideloaded app's messaging
   notifications reach the car.
6. Back on the settings screen, under **Previously connected cars**, check
   that **Add new cars to Android Auto** is on.

## 3. Install the two APKs from CI

CI attaches both APKs to every run as one artifact. Replace `RUN_ID` with the
run you want (`gh run list --repo SDNick484/hey-claude-auto` shows them).

```
$ gh run download RUN_ID --repo SDNick484/hey-claude-auto --name hey-claude-m0-debug-apks --dir ~/m0-apks
$ adb devices
$ adb install -r ~/m0-apks/templated/debug/spike-templated-debug.apk
$ adb install -r ~/m0-apks/notif/debug/spike-notif-debug.apk
$ adb shell pm grant io.github.sdnick484.heyclaude.spike.t android.permission.POST_NOTIFICATIONS
$ adb shell pm grant io.github.sdnick484.heyclaude.spike.n android.permission.POST_NOTIFICATIONS
```

`adb devices` must list the phone as `device`. If it says `unauthorized`,
accept the prompt on the phone. The two `pm grant` lines save you tapping
"Allow" on the notification prompt in each app.

## 4. Start the head unit

On the phone, in Android Auto's settings, open the three-dot menu and choose
**Start head unit server**. Keep the phone unlocked. Then:

```
$ adb forward tcp:5277 tcp:5277
$ cd "$ANDROID_HOME/extras/google/auto"
$ ./desktop-head-unit
```

Accept anything the phone asks the first time. If the window stays blank,
close it, stop and start the head unit server on the phone, and run it again.
If it will not connect at all, the docs recommend accessory mode for DHU 2.x:
`./desktop-head-unit --usb`.

**Speaking to it.** Click the DHU window and press `M`, or type `mic begin` in
the terminal the DHU is running in. That is the steering-wheel voice button,
and it listens on the computer's microphone. With no working microphone,
record a clip and play it in instead:

```
$ arecord -f S16_LE -r 16000 -c 1 -d 4 /tmp/hello.wav
```

then type `mic play /tmp/hello.wav` in the DHU terminal.

## 5. What to try, and what to tell me

Each app's phone screen has an event log that records exactly what Android
Auto asked it to do. The same lines go to logcat:

```
$ adb logcat -d -s HeyClaudeSpike
```

**Claude T, the clean way**

1. Is **Claude T** in the car's app launcher? Open it. Is there one
   conversation called "Claude"?
2. Tap the conversation. What does the car do: read "What do you need?" aloud,
   offer a reply, both, neither? Dictate a few words.
3. About three seconds later, is "You said: ..." announced? Is it read aloud
   by itself, or only after you tap something?
4. With nothing new waiting, open the conversation again and dictate a second
   turn. Does that work?

**Claude N, the fallback**

5. Is **Claude N** in the car's launcher at all? If it is, what does tapping
   it do before any message has been posted?
6. On the phone, open Claude N and tap **Post "What do you need?"
   (heads-up)**. Does the car show it? Tap it, listen, reply by voice. Does
   the echo come back?
7. Once the echo has been heard, can you get back to the conversation and
   reply again without a new message arriving, from the launcher icon or the
   car's notification centre?
8. On the phone, tap **Post "What do you need?" (quiet)**. No banner is
   expected. Can you still find the message in the car and reply to it?

**Phone only**

9. Pull down the notification shade and reply to Claude N by typing. The echo
   should arrive three seconds later.

For each step, tell me what happened, and send the output of the `adb logcat`
line above.

**In the real car, if you get the chance (parked)**

10. With both apps installed and Unknown sources on, plug in. Which of
    Claude T and Claude N appear in the launcher? Does step 6 work?

The Android docs say Unknown sources covers messaging notifications but not
apps built with the Car App Library, so Claude T is not expected to appear in
a real car when sideloaded. The Desktop Head Unit is more lenient, which is
why step 10 matters.

## Cleaning up

```
$ adb uninstall io.github.sdnick484.heyclaude.spike.t
$ adb uninstall io.github.sdnick484.heyclaude.spike.n
```

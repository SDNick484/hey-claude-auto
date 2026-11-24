# hey-claude-auto

A small Android app that puts "Claude" in Android Auto as a messaging
conversation: tap it, dictate, and the answer is read aloud.

The app is deliberately thin. It turns what the driver says into one HTTP
request to a separate relay service, and turns the reply into a message that
Android Auto reads out. It is sideloaded on one phone and is not published
to the Play Store.

## Status

Milestone 0: a throwaway spike in `spike/` that answers one question, whether
a turn can be started from the car screen when no message is waiting. See
[docs/m0-desktop-head-unit.md](docs/m0-desktop-head-unit.md) for how to try it
on the Desktop Head Unit. The real app has not been started.

## Building

CI tests, lints and builds debug APKs on every pull request and attaches them
to the workflow run as an artifact. To build locally you need a JDK (17 or
newer) and the Android SDK, with `ANDROID_HOME` pointing at it:

```sh
./gradlew :spike:assembleDebug
```

## This repo is public

No hostnames, tokens, or keystores are committed here. The relay URL and its
access credentials are entered on the phone.

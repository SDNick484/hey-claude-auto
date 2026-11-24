# hey-claude-auto

A small Android app that puts "Claude" in Android Auto as a messaging
conversation: tap it, dictate, and the answer is read aloud.

The app is deliberately thin. It turns what the driver says into one HTTP
request to a separate relay service, and turns the reply into a message that
Android Auto reads out. It is sideloaded on one phone and is not published
to the Play Store.

## This repo is public

No hostnames, tokens, or keystores are committed here. The relay URL and its
access credentials are entered on the phone.

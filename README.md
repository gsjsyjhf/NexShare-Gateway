# NexShare

Share this phone’s internet — including an already-running VPN — through a local HTTP + SOCKS5 proxy. No root.

## Build on GitHub Actions (recommended)

1. Create a **new public or private repository** on your GitHub account (empty, no README).
2. Upload this project (or push it with git).
3. Open the **Actions** tab → run **Build NexShare APK**.
4. Download the artifact `NexShare-APK`.

The workflow file is `.github/workflows/build-apk.yml`.

## Local build

JDK 17 + Android SDK 34:

```
./gradlew :app:assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

## How to use

1. Connect mobile data and your VPN app.
2. Enable the system hotspot or USB tethering.
3. Tap **Start sharing** in NexShare.
4. On the second device set HTTP proxy to the address shown (port 8080).

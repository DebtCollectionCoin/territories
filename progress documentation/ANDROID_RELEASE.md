# Android release & Play Console internal track

This guide covers producing a signed `app-bundle` (`.aab`) and uploading it
to the Play Console internal-testing track.

## 1. Create a release keystore (one-time)

```bash
keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias territories
```

Store `release.jks` somewhere safe **outside** the repo (or at the repo
root — it's gitignored via `*.jks`). Losing this keystore means you can
never publish an update to the same Play listing.

Back it up in at least two locations (encrypted USB, password manager
attachment, etc.).

## 2. Wire up `keystore.properties`

Copy the sample and fill in your real credentials:

```bash
cp keystore.properties.sample keystore.properties
$EDITOR keystore.properties
```

`keystore.properties` is gitignored. The `app-android/build.gradle.kts`
file detects it automatically and applies the signing config to the
release variant. Without this file, `:app-android:bundleRelease` still
works but produces an **unsigned** bundle — Play Console will reject it.

## 3. Build the release App Bundle

```bash
./gradlew :app-android:bundleRelease
```

Output: `app-android/build/outputs/bundle/release/app-android-release.aab`.

To produce a quick install-test APK instead:

```bash
./gradlew :app-android:assembleRelease
```

Output: `app-android/build/outputs/apk/release/app-android-release.apk`.

## 4. Verify the signature

```bash
./gradlew :app-android:bundleRelease
keytool -list -printcert -jarfile \
  app-android/build/outputs/bundle/release/app-android-release.aab
```

You should see your alias and the SHA-256 fingerprint of the cert.

## 5. Upload to Play Console (internal testing)

1. Open <https://play.google.com/console> and create the app listing
   (one-time): App name = "Territories", default language = English (UK),
   app type = Game, free, declarations.
2. **Setup → App content**: complete privacy policy, ads declaration,
   target audience, content rating, data-safety form, news-app status.
3. **Release → Testing → Internal testing → Create new release**.
4. Drop the `.aab` file into the upload area. Add release notes.
5. **Testers → Add email list** of internal testers, save, then
   **Roll out** the release.
6. Share the opt-in URL with testers (visible on the same page once the
   release is live; usually under a minute after submission).

## 6. Subsequent releases

Bump `versionCode` (must monotonically increase) and `versionName` in
`app-android/build.gradle.kts → defaultConfig`. Re-run step 3, then
"Create new release" under the same internal track.

## CI signing (optional, future)

For GitHub Actions release builds, store the keystore as a base64
secret and decode it at build time:

```yaml
- name: Decode keystore
  run: echo "${{ secrets.RELEASE_KEYSTORE_B64 }}" | base64 -d > release.jks
- name: Write keystore.properties
  run: |
    cat > keystore.properties <<EOF
    storeFile=release.jks
    storePassword=${{ secrets.RELEASE_STORE_PASSWORD }}
    keyAlias=${{ secrets.RELEASE_KEY_ALIAS }}
    keyPassword=${{ secrets.RELEASE_KEY_PASSWORD }}
    EOF
- run: ./gradlew :app-android:bundleRelease
```

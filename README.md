# Hisab Book (Android)

Daily hisab app: business accounts, clients with commission rates, daily Plus / Minus,
payments, net balance, share as picture. All data stays on the phone.

## Get the APK
1. Put these files in a **private** GitHub repository.
2. Open the repository's **Actions** tab. "Build APK" runs by itself (about 5 minutes).
   You can also press "Run workflow".
3. When it finishes, open **Releases** (right side of the repository page) and download `HisabBook.apk`.
4. On the phone: open the APK, allow "Install unknown apps" for the browser or file app, and install.

Keep the repository private: `hisab-release.keystore` (the key that signs the app) is inside.
New builds are signed with the same key, so they install over the old app and keep your data.

## Layout
- `www/index.html` the whole app (screens, calculations, storage)
- `app/` the small Android wrapper (WebView, share sheet, back button, icon)
- `.github/workflows/build-apk.yml` builds the APK on GitHub

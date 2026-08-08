# InkShelf — Android

Your offline notebook studio (GoodNotes-style shelf, RichMediaJournal log widgets, UUID page links, one-file HTML export), packaged two ways:

- **A real Android APK**, built for you automatically by GitHub Actions (no Android Studio needed)
- **An installable web app (PWA)** via GitHub Pages, as a zero-build fallback

Everything is stored on-device (IndexedDB). No servers, no accounts.

---

## Option A — Build the APK with GitHub Actions (recommended)

1. **Create a new repository** on github.com (e.g. `InkShelf`). Public or private both work.
2. **Upload this entire folder** to the repo, keeping the structure intact:
   - On the repo page: *Add file → Upload files* → drag everything in → *Commit changes*.
   - Make sure `.github/workflows/build-apk.yml` made it in (GitHub's uploader handles dot-folders fine if you drag the whole folder from your file manager; if it gets skipped, create the file manually with *Add file → Create new file* named `.github/workflows/build-apk.yml` and paste its contents).
3. The push triggers the build automatically. Open the **Actions** tab → click the running **Build Android APK** workflow → wait ~3–5 minutes for the green check.
4. On the finished run's page, scroll to **Artifacts** and download **InkShelf-apk** (a zip containing `InkShelf.apk`).
5. Copy `InkShelf.apk` to your phone (or download the artifact directly on the phone), tap it, and allow **Install unknown apps** for your browser/file manager when prompted. That prompt appears because this is a debug-signed personal build, not a Play Store release — it's expected.

Optional: pushing a tag like `v1.0` also attaches the APK to a GitHub **Release** for a permanent download link.

### What the app wrapper does

- Serves the bundled `inkshelf.html` from a secure origin (`WebViewAssetLoader`) so IndexedDB storage is fully reliable and persistent
- Hooks the image/font/sticker **file pickers** into Android's system picker
- **Export HTML** saves straight into your **Downloads** folder (via the `AndroidBridge` the HTML already knows about)
- Link cards open in your real browser; Back button behaves sensibly

### Updating the app later

Replace `app/src/main/assets/inkshelf.html` with a newer version, bump `versionCode`/`versionName` in `app/build.gradle`, push, and grab the new APK from Actions. Your notebooks survive updates (they live in app storage, not in the HTML file) — but uninstalling the app deletes them, so export anything precious first.

---

## Option B — Installable web app via GitHub Pages (2 minutes, no build)

1. In the same repo: **Settings → Pages → Source: Deploy from a branch → Branch: `main`, folder: `/docs` → Save**.
2. Wait a minute, then open the published URL (`https://<you>.github.io/<repo>/`) on your phone.
3. Chrome menu → **Add to Home screen** → **Install**. You get an icon, fullscreen launch, and offline support via the service worker.

Note: the PWA and the APK keep **separate** storage (different origins), and neither shares data with plain browser tabs. Use **Export HTML** to move a notebook anywhere.

---

## Repo layout

```
app/                      Android app module (WebView host, Java, no extra deps beyond androidx.webkit)
  src/main/assets/inkshelf.html   ← the entire InkShelf app
docs/                     GitHub Pages PWA (index.html + manifest + service worker + icons)
.github/workflows/        CI that builds the APK on every push
build.gradle, settings.gradle, gradle.properties
```

Built with AGP 8.5 / Gradle 8.7 / Java 17 / minSdk 24 (Android 7.0+). The S23 FE is very comfortably in range.

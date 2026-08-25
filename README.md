# 🧩 legendsciber's Patches

Morphe Patches repository by legendsciber.

## ❓ About

This is a collection of [Morphe](https://github.com/MorpheApp) patches maintained by legendsciber.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.3.1](https://github.com/legendsciber/morphe-patches/releases/tag/v1.3.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;7 patches total
<details open>
<summary>📦 Toolbox for Minecraft PE&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 5.4.58 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Bypass 64-bit Architecture Gate](#bypass-64-bit-architecture-gate) | 64-bit mimari kontrolunu devre disi birakir: "Unsupported 64-bit" uyarisi ve otomatik 64-bit yeniden baslatma olmaz. Uyumsuz mimaride crash riski var — varsayilan kapali. |  |
| [Bypass Install Location Check](#bypass-install-location-check) | Minecraft kurulum konumu kontrolu hep basarili olur: paket sorgusu uygulamanin kendisine yonlendirilir ("not installed" hatasi asla cikmaz). |  |
| [Spoof Google Play Installer](#spoof-google-play-installer) | Minecraft Play Store'dan kurulmus gibi gosterilir; kurulum kaynagina bagli kontroller gecer. |  |
| [Unlock All MCPE Versions](#unlock-all-mcpe-versions) | Desteklenen surum kontrolu her zaman gecer: liste disi Minecraft surumlerinde "not supported" hatasi cikmaz. |  |

</details>

<details open>
<summary>📦 Hill Climb Racing&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 1.70.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Hill Climb Racing Ad Removal](#hill-climb-racing-ad-removal) | Completely removes ads: banners and interstitials can never be displayed (CFirebaseAds.showBanners/showInterstitial become no-ops) and ad-free is granted once per app start — loadStore() seeds mAdFree = 1, the native engine's poll grants it and the store's own reset (inappPurchasesProcessed) zeroes the field, so no repeated purchase popups. |  |
| [Hill Climb Racing Free Store](#hill-climb-racing-free-store) | Every store item is granted instantly and free: coins, gems, paints, ad-skips, ad-free and bundles, without launching Google Play billing. |  |
| [Hill Climb Racing Instant Rewarded Video Rewards](#hill-climb-racing-instant-rewarded-video-rewards) | Rewarded video ads grant their reward instantly without playing the ad: the native engine receives onVideoStartedSuccess + onVideoCompletedSuccess on the GL thread, exactly as if the video had been watched and completed. |  |

</details>

<!-- PATCHES_END -->

#### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=legendsciber/morphe-patches

Or manually add this repository url as a patch source in Morphe: https://github.com/legendsciber/morphe-patches

### 🛠️ Building

To build legendsciber's Morphe Patches,
you can follow the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation).

## 📜 License

legendsciber's Patches are licensed under the [GNU General Public License v3.0](LICENSE)

# APK update architecture

The production application identity is fixed at `com.rasheed113.worksocial`.

Initial release values:

- `versionCode = 1`
- `versionName = 1.0.0`

Normal Android upgrades require the new APK to keep the same application ID and signing certificate while increasing `versionCode`.

Release signing is supplied externally through:

- `WORK_SOCIAL_KEYSTORE_FILE`
- `WORK_SOCIAL_KEYSTORE_PASSWORD`
- `WORK_SOCIAL_KEY_ALIAS`
- `WORK_SOCIAL_KEY_PASSWORD`

The repository intentionally contains no release keystore and no fake update server.

Future distribution flow:

`Work Social website -> hosted APK release -> in-app update notice -> Android package installer -> signed upgrade`

The website-side release metadata/download endpoint will be added in a later phase. Until that contract exists, the Android app does not pretend that automatic update delivery is implemented.

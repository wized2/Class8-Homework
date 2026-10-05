# Class 8 Homework (Android)

Simple, beautiful student app for **Class 8** daily homework.

- API: `GET https://class-8.pages.dev/api/read`
- Admin UI: [wized2/Homework](https://github.com/wized2/Homework)

## Features

- Material 3 UI, pull-to-refresh
- All Class 8 subjects with friendly empty state
- Caches last successful response
- Signed CI releases on `main`

## Build

```bash
gradle :app:assembleRelease
```

CI signs with repository secrets: `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

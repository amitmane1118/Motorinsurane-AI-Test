# Motor Insurance Renewals — On-device Preview

An Android 16-compatible private renewal overview. It imports a CSV exported by the owner from their private Google Sheet, parses renewal dates as **DD-MM-YYYY**, groups repeat vehicle records by mobile number, and shows upcoming/overdue counts and customer cards. Imported data stays in memory for the current app session; there is no server connection or public customer data.

## Current boundaries

- This build is a customer-list and renewal overview. It does not place calls, speak with customers, record/transcribe audio, or update the source Google Sheet.
- It does not access Google sign-in or Sheets directly. Export the `Sheet1` tab as CSV and select that file with **Import private Sheet CSV**.
- NCB shown from the customer's previous-year claim response is an estimate only. Confirm claim-free history, the applicable insurer wording, add-ons and the policy schedule before quoting.
- The dashboard is inside the Android app. No customer dashboard tab is required in the Google Sheet.
- No Android call, microphone, contacts, call-log or Internet permissions are requested.

## Use

In Google Sheets, open the customer tab and download/export it as CSV. In the app, tap **Import private Sheet CSV** and choose the downloaded file. Renewal dates such as `12-10-2026` mean 12 October 2026. The app groups rows sharing a mobile number and shows only one customer card for that number.

To refresh after Sheet edits, export and import the CSV again. The app discards the imported list when it closes. The source Google Sheet remains the record of truth.

## Build

Android SDK API 36, Java 17, AGP 8.10.1 and Gradle 8.11.1. GitHub Actions builds the debug APK and saves it as an artifact; no laptop Android installation is required. CI success verifies compilation and lint only. It does not enable or certify AI calling, insurance solicitation, or insurer-approved scripts.

## Privacy

This public repository contains source code only. Do not upload customer CSVs, policy schedules, real phone numbers, credentials, or API keys to it. The app blocks screenshots, has backup disabled, requests no network permission, and keeps imported customer values in app memory only.

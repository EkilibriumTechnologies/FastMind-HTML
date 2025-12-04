# Android App Signing Setup

## Security Warning
**NEVER commit keystore files or signing credentials to version control.**

## Setting Up Signing Credentials

You have two options to provide signing credentials for release builds:

### Option 1: Environment Variables (Recommended)

Set the following environment variables before building:

**Windows (PowerShell):**
```powershell
$env:KEYSTORE_PASSWORD="your_keystore_password"
$env:KEY_ALIAS="your_key_alias"
$env:KEY_PASSWORD="your_key_password"
```

**Windows (Command Prompt):**
```cmd
set KEYSTORE_PASSWORD=your_keystore_password
set KEY_ALIAS=your_key_alias
set KEY_PASSWORD=your_key_password
```

**macOS/Linux:**
```bash
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"
```

### Option 2: Local gradle.properties File

1. Copy the example file:
   ```bash
   cp gradle.properties.example gradle.properties
   ```

2. Edit `gradle.properties` and add your credentials:
   ```
   KEYSTORE_PASSWORD=your_keystore_password
   KEY_ALIAS=your_key_alias
   KEY_PASSWORD=your_key_password
   ```

3. The `gradle.properties` file is already ignored by Git, so your credentials won't be committed.

## Building a Release

After setting up credentials (via environment variables or local gradle.properties), build the release:

```bash
cd android
./gradlew clean
./gradlew bundleRelease
```

The signed AAB file will be generated at:
`android/app/build/outputs/bundle/release/app-release.aab`

## Important Notes

- The keystore file (`fastmind-release-key.keystore`) must be located at `android/app/`
- Keep your keystore file secure and backed up - losing it means you cannot update your app on Google Play
- Never share your keystore password or keystore file
- The `build.gradle` is configured to read credentials from either environment variables or `gradle.properties` in that order


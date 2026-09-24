#!/usr/bin/env bash
set -euo pipefail

repo="${GH_REPO:-MACCY00013/CAREEROS-ULTIMATE}"
keystore="${1:-careeros-release.jks}"

if [[ -e "$keystore" ]]; then
  printf 'Using existing keystore: %s\n' "$keystore"
  read -r -p 'Key alias: ' alias
  read -r -s -p 'Keystore password: ' password
  printf '\n'
else
  read -r -p 'Key alias: ' alias
  read -r -s -p 'Keystore/key password: ' password
  printf '\n'
  keytool -genkeypair -v \
    -keystore "$keystore" \
    -alias "$alias" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$password" \
    -keypass "$password" \
    -dname 'CN=CareerOS, O=Maccy Creations, C=IN'
fi
read -r -s -p 'Confirm password: ' confirmation
printf '\n'
[[ "$password" == "$confirmation" ]] || { echo 'Passwords do not match.' >&2; exit 1; }

base64 -w 0 "$keystore" | gh secret set ANDROID_KEYSTORE_BASE64 --repo "$repo"
printf '%s' "$password" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$repo"
printf '%s' "$alias" | gh secret set ANDROID_KEY_ALIAS --repo "$repo"
printf '%s' "$password" | gh secret set ANDROID_KEY_PASSWORD --repo "$repo"

echo "Signing secrets configured for $repo. Keep $keystore backed up securely."
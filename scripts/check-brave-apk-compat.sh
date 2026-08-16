#!/usr/bin/env bash

set -euo pipefail

if (($# != 1)); then
    echo "usage: $0 /path/to/brave.apk" >&2
    exit 2
fi

BRAVE_APK=$1
ANDROID_SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
APK_ANALYZER=${APK_ANALYZER:-}
FAILURES=0

if [[ ! -f "$BRAVE_APK" ]]; then
    echo "error: Brave APK not found at $BRAVE_APK" >&2
    exit 2
fi

if [[ -z "$APK_ANALYZER" ]]; then
    if [[ -z "$ANDROID_SDK" ]]; then
        echo "error: set ANDROID_HOME, ANDROID_SDK_ROOT, or APK_ANALYZER" >&2
        exit 2
    fi
    APK_ANALYZER="$ANDROID_SDK/cmdline-tools/latest/bin/apkanalyzer"
fi

if [[ ! -x "$APK_ANALYZER" ]]; then
    echo "error: apkanalyzer not executable at $APK_ANALYZER" >&2
    exit 2
fi

DEX_REPORT=$(mktemp)
PREFERENCE_CODE=$(mktemp)
PREFERENCE_SCREEN_CODE=$(mktemp)
PREFERENCE_GROUP_CODE=$(mktemp)
MANIFEST_REPORT=$(mktemp)
trap 'rm -f "$DEX_REPORT" "$PREFERENCE_CODE" "$PREFERENCE_SCREEN_CODE" \
    "$PREFERENCE_GROUP_CODE" "$MANIFEST_REPORT"' EXIT
"$APK_ANALYZER" dex packages --defined-only "$BRAVE_APK" >"$DEX_REPORT"
"$APK_ANALYZER" manifest print "$BRAVE_APK" >"$MANIFEST_REPORT"
"$APK_ANALYZER" dex code --class androidx.preference.Preference \
    "$BRAVE_APK" >"$PREFERENCE_CODE"
"$APK_ANALYZER" dex code --class androidx.preference.PreferenceScreen \
    "$BRAVE_APK" >"$PREFERENCE_SCREEN_CODE"
PREFERENCE_GROUP_CLASS=$(sed -n 's/^\.super L\(.*\);$/\1/p' \
    "$PREFERENCE_SCREEN_CODE" | tr '/' '.')
if [[ -n "$PREFERENCE_GROUP_CLASS" ]]; then
    "$APK_ANALYZER" dex code --class "$PREFERENCE_GROUP_CLASS" \
        "$BRAVE_APK" >"$PREFERENCE_GROUP_CODE"
fi

require_regex() {
    local description=$1
    local pattern=$2

    if rg --quiet --regexp "$pattern" "$DEX_REPORT"; then
        echo "ok: $description"
    else
        echo "missing: $description" >&2
        FAILURES=$((FAILURES + 1))
    fi
}

require_code_regex() {
    local description=$1
    local pattern=$2
    local report=$3

    if rg --quiet --regexp "$pattern" "$report"; then
        echo "ok: $description"
    else
        echo "missing: $description" >&2
        FAILURES=$((FAILURES + 1))
    fi
}

require_regex "PrefService class" \
    'C d .*org\.chromium\.components\.prefs\.PrefService$'
require_regex "Chromium local-state bridge" \
    'C d .*org\.chromium\.chrome\.browser\.prefs\.LocalStatePrefs$'
require_regex "R8-compatible local-state PrefService getter" \
    'M d .*LocalStatePrefs org\.chromium\.components\.prefs\.PrefService [^(]+\(\)$'
require_regex "R8-compatible PrefService native factory" \
    'M d .*PrefService org\.chromium\.components\.prefs\.PrefService [^(]+\(long\)$'
require_regex "R8-compatible boolean preference setter" \
    'M d .*PrefService void [^(]+\((java\.lang\.String,boolean|boolean,java\.lang\.String)\)$'
require_regex "R8-compatible integer preference query" \
    'M d .*PrefService int [^(]+\(java\.lang\.String\)$'
require_regex "R8-compatible integer preference setter" \
    'M d .*PrefService void [^(]+\((java\.lang\.String,int|int,java\.lang\.String)\)$'
require_regex "R8-compatible string preference setter" \
    'M d .*PrefService void [^(]+\(java\.lang\.String,java\.lang\.String\)$'
require_regex "CommandLine array initializer" \
    'M d .*org\.chromium\.base\.CommandLine void [^(]+\(java\.lang\.String\[\]\)$'
require_regex "CommandLine switch reader" \
    'M d .*org\.chromium\.base\.CommandLine java\.lang\.String [^(]+\(java\.lang\.String\)$'
require_regex "CommandLine switch writer" \
    'M d .*org\.chromium\.base\.CommandLine void [^(]+\(java\.lang\.String,java\.lang\.String\)$'
require_regex "isolated chrome split bootstrap" \
    'M d .*SplitChromeApplication android\.content\.Context createContextForSplit\(java\.lang\.String\)$'
require_regex "regular profile structural getter" \
    'M d .*ProfileManager org\.chromium\.chrome\.browser\.profiles\.Profile [^(]+\(\)$'
require_regex "browser resume lifecycle" \
    'M d .*ChromeTabbedActivity void onResume\(\)$'
require_regex "privacy settings class" \
    'C d .*org\.chromium\.chrome\.browser\.privacy\.settings\.BravePrivacySettings$'
require_regex "R8-compatible privacy settings creation" \
    'M d .*BravePrivacySettings void [^(]+\((android\.os\.Bundle,java\.lang\.String|java\.lang\.String,android\.os\.Bundle)\)$'
require_regex "structural Shields default setter" \
    'M d .*BraveShieldsContentSettings void [^(]+\(org\.chromium\.chrome\.browser\.profiles\.Profile,java\.lang\.String,java\.lang\.String,java\.lang\.String(,boolean)?\)$'
require_regex "Wallet provider delegate helper" \
    'C d .*org\.chromium\.chrome\.browser\.crypto_wallet\.BraveWalletProviderDelegateImplHelper$'
require_regex "Wallet Web3 notification gate" \
    'M d .*BraveWalletProviderDelegateImplHelper boolean isWeb3NotificationAllowed\(\)$'
require_regex "Wallet provider panel callback" \
    'M d .*BraveWalletProviderDelegateImplHelper void showPanel\('
require_regex "Wallet provider unlock callback" \
    'M d .*BraveWalletProviderDelegateImplHelper void unlockWallet\('
require_regex "Wallet provider backup callback" \
    'M d .*BraveWalletProviderDelegateImplHelper void showWalletBackup\('
require_regex "Wallet provider onboarding callback" \
    'M d .*BraveWalletProviderDelegateImplHelper void showWalletOnboarding\('
require_regex "Wallet provider account callback" \
    'M d .*BraveWalletProviderDelegateImplHelper void showAccountCreation\('
require_regex "Wallet provider interaction callback" \
    'M d .*BraveWalletProviderDelegateImplHelper void walletInteractionDetected\('
require_regex "injectable Chrome switch preference" \
    'C d .*org\.chromium\.components\.browser_ui\.settings\.ChromeSwitchPreference$'
require_code_regex "R8-compatible preference-group add operation" \
    '^\.method .*\(Landroidx/preference/Preference;\)(V|Z)$' \
    "$PREFERENCE_GROUP_CODE"
require_code_regex "R8-compatible preference change callback" \
    'invoke-interface .*\(Landroidx/preference/Preference;Ljava/lang/Object;\)Z' \
    "$PREFERENCE_CODE"
require_code_regex "manifest-stable Brave VPN service" \
    'org\.chromium\.chrome\.browser\.vpn\.wireguard\.WireguardService' \
    "$MANIFEST_REPORT"
if ((FAILURES > 0)); then
    echo "APK compatibility check failed: $FAILURES required structures changed" >&2
    exit 1
fi

echo "APK compatibility check passed"

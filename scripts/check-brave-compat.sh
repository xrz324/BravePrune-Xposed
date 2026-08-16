#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BRAVE_CORE=${BRAVE_CORE:-"$PROJECT_ROOT/brave-core"}
FAILURES=0

if ! command -v rg >/dev/null 2>&1; then
    echo "error: ripgrep (rg) is required" >&2
    exit 2
fi

if [[ ! -d "$BRAVE_CORE" ]]; then
    echo "error: Brave source not found at $BRAVE_CORE" >&2
    exit 2
fi

require_pattern() {
    local description=$1
    local pattern=$2
    local path=$3

    if rg --quiet --fixed-strings -- "$pattern" "$BRAVE_CORE/$path"; then
        echo "ok: $description"
    else
        echo "missing: $description ($path: $pattern)" >&2
        FAILURES=$((FAILURES + 1))
    fi
}

require_project_pattern() {
    local description=$1
    local pattern=$2
    local path=$3

    if rg --quiet --fixed-strings -- "$pattern" "$PROJECT_ROOT/$path"; then
        echo "ok: $description"
    else
        echo "missing: $description ($path: $pattern)" >&2
        FAILURES=$((FAILURES + 1))
    fi
}

reject_project_pattern() {
    local description=$1
    local pattern=$2
    local path=$3

    if rg --quiet --fixed-strings -- "$pattern" "$PROJECT_ROOT/$path"; then
        echo "unexpected: $description ($path: $pattern)" >&2
        FAILURES=$((FAILURES + 1))
    else
        echo "ok: $description"
    fi
}

require_pattern "early Brave command-line hook" \
    "initCommandLine" \
    "android/java/org/chromium/base/BraveCommandLineInitUtil.java"
require_pattern "hidden filter-list feature" \
    "kBraveAdblockShowHiddenComponents" \
    "components/brave_shields/core/common/features.cc"
require_pattern "strict fingerprinting feature" \
    "kBraveShowStrictFingerprintingMode" \
    "components/brave_shields/core/common/features.cc"
require_pattern "first-party network blocking feature" \
    "kBraveAdblockDefault1pBlocking" \
    "components/brave_shields/core/common/features.cc"
require_pattern "timestamp rounding feature" \
    "kBraveRoundTimeStamps" \
    "chromium_src/third_party/blink/common/features.cc"
require_pattern "HTTPS-by-default feature name" \
    '"HttpsByDefault"' \
    "chromium_src/net/base/features.cc"
require_pattern "strict fingerprinting preference adapter" \
    "setFingerprintingPref" \
    "android/java/org/chromium/chrome/browser/preferences/website/BraveShieldsContentSettings.java"
require_pattern "strict HTTPS preference adapter" \
    "setHttpsUpgradePref" \
    "android/java/org/chromium/chrome/browser/preferences/website/BraveShieldsContentSettings.java"
require_pattern "aggressive Shields preference adapter" \
    "setTrackersPref" \
    "android/java/org/chromium/chrome/browser/preferences/website/BraveShieldsContentSettings.java"
require_pattern "WebRTC preference adapter" \
    "prefs::kWebRTCIPHandlingPolicy" \
    "browser/android/preferences/brave_pref_service_bridge.cc"
require_pattern "WebRTC profile preference key" \
    'pref-key="webrtc.ip_handling_policy"' \
    "browser/resources/settings/brave_privacy_page/brave_personalization_options.html"
require_pattern "privacy settings injection point" \
    "onCreatePreferences" \
    "android/java/org/chromium/chrome/browser/privacy/settings/BravePrivacySettings.java"
require_pattern "Android Web Discovery managed policy" \
    "android:138-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveWebDiscoveryEnabled.yaml"
require_pattern "Android AI Chat managed policy" \
    "android:148-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveAIChatEnabled.yaml"
require_pattern "Android P3A managed policy" \
    "android:138-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveP3AEnabled.yaml"
require_pattern "Android stats ping managed policy" \
    "android:138-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveStatsPingEnabled.yaml"
require_pattern "Android Rewards managed policy" \
    "android:142-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveRewardsDisabled.yaml"
require_pattern "Android News managed policy" \
    "android:142-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveNewsDisabled.yaml"
require_pattern "Android Wallet managed policy" \
    "android:142-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveWalletDisabled.yaml"
require_pattern "native Wallet managed-pref gate" \
    "IsManagedPreference(kBraveWalletDisabledByPolicy)" \
    "components/brave_wallet/browser/brave_wallet_utils.cc"
require_pattern "Android Wallet service factory" \
    "getBraveWalletService" \
    "android/java/org/chromium/chrome/browser/crypto_wallet/BraveWalletServiceFactory.java"
require_pattern "Wallet Web3 notification callback" \
    "isWeb3NotificationAllowed" \
    "android/java/org/chromium/chrome/browser/crypto_wallet/BraveWalletProviderDelegateImplHelper.java"
require_pattern "Wallet provider panel callback" \
    "showPanel" \
    "android/java/org/chromium/chrome/browser/crypto_wallet/BraveWalletProviderDelegateImplHelper.java"
require_pattern "Wallet provider none value" \
    "None = 1" \
    "components/brave_wallet/common/brave_wallet.mojom"
require_pattern "decentralized DNS disabled value" \
    "DISABLED" \
    "components/decentralized_dns/core/constants.h"
require_pattern "Web3 usage metric" \
    "Brave.Web3.DappVisited" \
    "components/misc_metrics/web3_metrics.h"
require_pattern "Android VPN managed policy" \
    "android:148-" \
    "components/policy/resources/templates/policy_definitions/BraveSoftware/BraveVPNDisabled.yaml"
require_pattern "native VPN feature" \
    "kBraveVPN" \
    "components/brave_vpn/common/features.cc"
require_pattern "Android VPN managed-pref gate" \
    "isManagedPreference(BravePref.MANAGED_BRAVE_VPN_DISABLED)" \
    "android/java/org/chromium/chrome/browser/vpn/BraveVpnPolicy.java"
require_pattern "Android VPN service component" \
    "org.chromium.chrome.browser.vpn.wireguard.WireguardService" \
    "android/java/AndroidManifest.xml"
require_project_pattern "Web Discovery policy override" \
    'Map.entry("BraveWebDiscoveryEnabled", false)' \
    "app/src/main/java/com/brave/enhance/policy/ManagedPolicyOverrides.java"
reject_project_pattern "no obsolete filter-list selection override" \
    "brave.ad_block.checked_all_default_regions" \
    "app/src/main"
reject_project_pattern "no permanent WebRTC command-line override" \
    "force-webrtc-ip-handling-policy" \
    "app/src/main"
reject_project_pattern "no ineffective Brave Ads startup-delay override" \
    "InProcessBraveAdsService" \
    "app/src/main"
WALLET_FEATURES_FILE="$BRAVE_CORE/components/brave_wallet/common/features.cc"
MODULE_FEATURES_FILE="$PROJECT_ROOT/app/src/main/java/com/brave/enhance/feature/FeatureOverrides.java"
while IFS= read -r feature; do
    if ! rg --quiet --fixed-strings -- "\"$feature\"" "$MODULE_FEATURES_FILE"; then
        echo "missing: Wallet feature override ($feature)" >&2
        FAILURES=$((FAILURES + 1))
    fi
done < <(
    awk '
        /BASE_FEATURE\(kBraveWallet/ {
            block = $0
            while (block !~ /\);/ && getline line) block = block " " line
            if (match(block, /"BraveWallet[A-Za-z0-9_]+"/)) {
                print substr(block, RSTART + 1, RLENGTH - 2)
            } else {
                sub(/^.*BASE_FEATURE\(k/, "", block)
                sub(/,.*/, "", block)
                print block
            }
        }
    ' "$WALLET_FEATURES_FILE" | sort -u
)

if ((FAILURES > 0)); then
    echo "compatibility check failed: $FAILURES required symbols changed" >&2
    exit 1
fi

echo "compatibility check passed"

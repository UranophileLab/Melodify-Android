#!/usr/bin/env bash
set -euo pipefail

# Local F-Droid pipeline verification script for Melodify-Android.
# Simulates the checks and build steps run by fdroid/fdroiddata GitLab CI:
#   1. Static F-Droid compatibility & metadata formatting (rewritemeta) checks
#   2. Clean-tree release build (either in F-Droid's Docker image or locally with JDK 21)
#   3. Reproducible-build byte-for-byte APK content diff against a reference release APK

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="dev.melodify.uranophilelab"
EXPECTED_SIGNING_KEY="b43870a0524e81a6532a81cfe2770f58099b154eae14236fb833445502a2fa70"

MODE="auto"          # auto | local | docker
CHECK_ONLY=0
SKIP_BUILD=0
METADATA_FILE=""
REFERENCE_APK=""
DOWNLOAD_TAG=""
BUILT_APK=""

usage() {
  cat <<'EOF'
Usage: ./scripts/fdroid-local-test.sh [options]

Options:
  --check-only              Run static & metadata checks only (skip Gradle build)
  --local                   Force local clean-copy Gradle build (no Docker)
  --docker                  Run full fdroid lint/rewritemeta/build inside F-Droid Docker image
  --metadata <path>         Path to fdroiddata metadata/dev.melodify.uranophilelab.yml to validate
  --reference-apk <path>    Compare built APK against a local reference release APK
  --download-release [tag]  Download GitHub release APK (e.g. v5.4.4, defaults to v<versionName>)
                            and compare for F-Droid reproducibility
  --built-apk <path>        Use an already-built APK instead of running Gradle
  -h, --help                Show this help

Examples:
  # Fast pre-flight check + metadata check:
  ./scripts/fdroid-local-test.sh --check-only --metadata ../fdroiddata/metadata/dev.melodify.uranophilelab.yml

  # Local clean-tree build + compare with GitHub release v5.4.4:
  ./scripts/fdroid-local-test.sh --local --download-release v5.4.4

  # Full F-Droid CI simulation using Docker (recommended on host):
  ./scripts/fdroid-local-test.sh --docker --metadata ../fdroiddata/metadata/dev.melodify.uranophilelab.yml
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --check-only) CHECK_ONLY=1; shift ;;
    --local) MODE="local"; shift ;;
    --docker) MODE="docker"; shift ;;
    --metadata) METADATA_FILE="${2:?missing path for --metadata}"; shift 2 ;;
    --reference-apk) REFERENCE_APK="${2:?missing path for --reference-apk}"; shift 2 ;;
    --download-release)
      if [[ $# -gt 1 && ! "$2" =~ ^- ]]; then
        DOWNLOAD_TAG="$2"; shift 2
      else
        DOWNLOAD_TAG="AUTO"; shift
      fi
      ;;
    --built-apk) BUILT_APK="${2:?missing path for --built-apk}"; SKIP_BUILD=1; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  esac
done

log()  { printf '\033[1;34m[fdroid-test]\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[PASS]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; exit 1; }

# Extract versionName & versionCode from app/build.gradle
APP_GRADLE="$ROOT_DIR/app/build.gradle"
VERSION_NAME="$(sed -nE "s/^[[:space:]]*versionName[[:space:]]+['\"]([^'\"]+)['\"].*/\1/p" "$APP_GRADLE" | head -n1)"
VERSION_CODE="$(sed -nE "s/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\1/p" "$APP_GRADLE" | head -n1)"
[[ -n "$VERSION_NAME" && -n "$VERSION_CODE" ]] || fail "Could not parse versionName/versionCode from $APP_GRADLE"

log "Project version: $VERSION_NAME (versionCode=$VERSION_CODE)"

# ---------------------------------------------------------------------------
# Step 1: Static checks (F-Droid & reproducible-build readiness)
# ---------------------------------------------------------------------------
log "Running static F-Droid readiness checks..."

if [[ -f "$ROOT_DIR/gradle/gradle-daemon-jvm.properties" ]]; then
  fail "gradle/gradle-daemon-jvm.properties exists! F-Droid deletes it during build, causing GitHub Actions and F-Droid to use different JDKs."
else
  ok "gradle/gradle-daemon-jvm.properties is absent."
fi

if grep -qE 'includeInApk\s*=\s*false' "$APP_GRADLE"; then
  ok "dependenciesInfo { includeInApk = false } is present in app/build.gradle."
else
  fail "app/build.gradle is missing dependenciesInfo { includeInApk = false }."
fi

if grep -qE 'java-version:\s*["'\'']21["'\'']' "$ROOT_DIR/.github/workflows/release-build.yml"; then
  ok ".github/workflows/release-build.yml targets JDK 21 (matches F-Droid buildserver)."
else
  warn ".github/workflows/release-build.yml does not specify java-version: \"21\"."
fi

if [[ -n "$METADATA_FILE" ]]; then
  [[ -f "$METADATA_FILE" ]] || fail "Metadata file not found: $METADATA_FILE"
  log "Validating F-Droid metadata file: $METADATA_FILE"
  python3 - "$METADATA_FILE" "$VERSION_NAME" "$VERSION_CODE" "$EXPECTED_SIGNING_KEY" <<'PY'
import pathlib, re, sys

meta_path = pathlib.Path(sys.argv[1])
ver_name = sys.argv[2]
ver_code = sys.argv[3]
expected_key = sys.argv[4]
text = meta_path.read_text(encoding="utf-8")
lines = text.splitlines()

errors = []
warnings = []

# Check rewritemeta rules observed in fdroid/fdroiddata CI:
# 1) No blank line between Repo: and Binaries:
for i, line in enumerate(lines):
    if line.startswith("Repo:") and i + 2 < len(lines) and lines[i + 1].strip() == "" and lines[i + 2].startswith("Binaries:"):
        errors.append("Remove the blank line between 'Repo:' and 'Binaries:' (fdroid rewritemeta fails on it).")
    if line.startswith("Binaries:") and line[len("Binaries:"):].strip():
        errors.append("Wrap 'Binaries:' URL onto an indented next line ('Binaries: \\n  https://...') as required by fdroid rewritemeta.")

if f"versionName: {ver_name}" not in text:
    warnings.append(f"Metadata does not list versionName: {ver_name}.")
if f"versionCode: {ver_code}" not in text:
    warnings.append(f"Metadata does not list versionCode: {ver_code}.")
if f"AllowedAPKSigningKeys: {expected_key}" not in text:
    warnings.append(f"AllowedAPKSigningKeys does not match {expected_key}.")

# Check standard F-Droid categories
standard_cats = {
    "Connectivity", "Development", "Games", "Graphics", "Internet", "Money",
    "Multimedia", "Navigation", "Phone & SMS", "Reading", "Science & Education",
    "Security", "Sports & Health", "System", "Theming", "Time", "Writing", "AI"
}
cat_match = re.search(r"(?m)^Categories:\n((?:  - .+\n)+)", text)
if cat_match:
    for raw in cat_match.group(1).splitlines():
        cat = raw.strip()[2:].strip()
        if cat not in standard_cats:
            warnings.append(f"Category '{cat}' is not a standard F-Droid category (e.g., 'Multimedia' or 'Internet'); fdroid lint may reject it.")

for w in warnings:
    print(f"[WARN] {w}")
if errors:
    for e in errors:
        print(f"[FAIL] {e}", file=sys.stderr)
    sys.exit(1)
print("[PASS] Metadata formatting checks passed.")
PY
fi

if [[ "$CHECK_ONLY" -eq 1 ]]; then
  ok "Static checks complete (--check-only)."
  exit 0
fi

# ---------------------------------------------------------------------------
# Decide Docker vs Local mode
# ---------------------------------------------------------------------------
CONTAINER_CLI=""
if command -v docker >/dev/null 2>&1; then
  CONTAINER_CLI="docker"
elif command -v podman >/dev/null 2>&1; then
  CONTAINER_CLI="podman"
fi

if [[ "$MODE" == "auto" ]]; then
  if [[ -n "$CONTAINER_CLI" ]]; then
    MODE="docker"
  else
    MODE="local"
  fi
fi

# ---------------------------------------------------------------------------
# Docker F-Droid Buildserver mode (exact CI simulation)
# ---------------------------------------------------------------------------
if [[ "$MODE" == "docker" && "$SKIP_BUILD" -eq 0 ]]; then
  [[ -n "$CONTAINER_CLI" ]] || fail "--docker requested, but neither docker nor podman is installed."
  [[ -n "$METADATA_FILE" ]] || fail "--docker mode requires --metadata <path/to/dev.melodify.uranophilelab.yml>."
  [[ -f "$METADATA_FILE" ]] || fail "Metadata file not found: $METADATA_FILE"

  IMAGE="registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie"
  WORKDIR_TMP="$(mktemp -d /tmp/fdroid-docker-XXXXXX)"
  trap 'rm -rf "$WORKDIR_TMP"' EXIT
  mkdir -p "$WORKDIR_TMP/metadata" "$WORKDIR_TMP/build" "$WORKDIR_TMP/unsigned" "$WORKDIR_TMP/tmp"
  cp "$METADATA_FILE" "$WORKDIR_TMP/metadata/${APP_ID}.yml"

  log "Running full F-Droid CI simulation in $IMAGE via $CONTAINER_CLI..."
  $CONTAINER_CLI run --rm -t \
    -v "$WORKDIR_TMP:/work" \
    -w /work \
    "$IMAGE" bash -lc "
      set -euo pipefail
      git clone --depth=1 https://gitlab.com/fdroid/fdroidserver.git /tmp/fdroidserver
      export PATH=/tmp/fdroidserver:\$PATH
      export PYTHONPATH=/tmp/fdroidserver:/tmp/fdroidserver/examples
      fdroid rewritemeta --verbose $APP_ID
      fdroid lint $APP_ID
      fdroid build --verbose --test --on-server --no-tarball ${APP_ID}:${VERSION_CODE}
    "
  ok "F-Droid Docker pipeline succeeded!"
  exit 0
fi

# ---------------------------------------------------------------------------
# Local clean-tree F-Droid-style build
# ---------------------------------------------------------------------------
TMP_ROOT="$(mktemp -d /tmp/fdroid-local-XXXXXX)"
trap 'rm -rf "$TMP_ROOT"' EXIT

find_jdk21() {
  local candidates=()
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    candidates+=("$JAVA_HOME")
  fi
  for p in /usr/lib/jvm/java-21* /usr/lib/jvm/temurin-21* "$HOME/.sdkman/candidates/java/21"* "/app/extra/jbr"; do
    [[ -x "$p/bin/java" ]] && candidates+=("$p")
  done
  for c in "${candidates[@]}"; do
    local ver
    ver="$("$c/bin/java" -version 2>&1 | head -n1)"
    if [[ "$ver" =~ \"21\. ]]; then
      echo "$c"
      return 0
    fi
  done
  return 1
}

find_any_jdk() {
  for p in "${JAVA_HOME:-}" /usr/lib/jvm/* "$HOME/.sdkman/candidates/java/current" /app/extra/jbr; do
    if [[ -n "$p" && -x "$p/bin/java" ]]; then
      echo "$p"
      return 0
    fi
  done
  return 1
}

find_sdk_dir() {
  if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
    echo "$ANDROID_HOME"; return 0
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -d "$ANDROID_SDK_ROOT" ]]; then
    echo "$ANDROID_SDK_ROOT"; return 0
  fi
  if [[ -f "$ROOT_DIR/local.properties" ]]; then
    local prop_sdk
    prop_sdk="$(sed -nE 's/^sdk\.dir=(.*)$/\1/p' "$ROOT_DIR/local.properties" | head -n1)"
    if [[ -n "$prop_sdk" && -d "$prop_sdk" ]]; then
      echo "$prop_sdk"; return 0
    fi
  fi
  for p in "$HOME/Android/Sdk" "$HOME/flutter"; do
    [[ -d "$p/build-tools" ]] && { echo "$p"; return 0; }
  done
  return 1
}

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  JDK_HOME=""
  if JDK_HOME="$(find_jdk21)"; then
    ok "Using JDK 21 at: $JDK_HOME"
  else
    JDK_HOME="$(find_any_jdk)" || fail "No JDK found. Set JAVA_HOME to JDK 21."
    warn "JDK 21 not found; falling back to $JDK_HOME ($("$JDK_HOME/bin/java" -version 2>&1 | head -n1))."
    warn "Note: Exact byte-for-byte classes.dex matching with F-Droid requires JDK 21!"
  fi

  SDK_DIR="$(find_sdk_dir)" || fail "Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties."
  ok "Using Android SDK at: $SDK_DIR"

  CLEAN_SRC="$TMP_ROOT/src"
  mkdir -p "$CLEAN_SRC"
  log "Copying clean source tree to $CLEAN_SRC (excluding .git, build/, .gradle/, local.properties)..."
  tar -C "$ROOT_DIR" \
    --exclude='.git' \
    --exclude='.gradle' \
    --exclude='build' \
    --exclude='app/build' \
    --exclude='lrclib/build' \
    --exclude='.kotlin' \
    --exclude='local.properties' \
    -cf - . | tar -C "$CLEAN_SRC" -xf -

  # Mirror fdroidserver cleanup steps
  rm -f "$CLEAN_SRC/gradle/gradle-daemon-jvm.properties"
  printf 'sdk.dir=%s\n' "$SDK_DIR" > "$CLEAN_SRC/local.properties"

  log "Running ./gradlew :app:assembleRelease --no-daemon in clean source tree..."
  (
    cd "$CLEAN_SRC"
    export JAVA_HOME="$JDK_HOME"
    export PATH="$JDK_HOME/bin:$PATH"
    chmod +x ./gradlew
    ./gradlew :app:assembleRelease --no-daemon --warning-mode none
  )

  BUILT_APK="$CLEAN_SRC/app/build/outputs/apk/release/Melodify-${VERSION_NAME}-release.apk"
  [[ -f "$BUILT_APK" ]] || fail "Expected APK not found at $BUILT_APK"
  ok "Clean release APK built: $BUILT_APK"
fi

# ---------------------------------------------------------------------------
# Step 3: Reproducible-build APK diff against reference binary
# ---------------------------------------------------------------------------
if [[ -n "$DOWNLOAD_TAG" ]]; then
  [[ "$DOWNLOAD_TAG" == "AUTO" ]] && DOWNLOAD_TAG="v${VERSION_NAME}"
  REF_URL="https://github.com/UranophileLab/Melodify-Android/releases/download/${DOWNLOAD_TAG}/Melodify-${VERSION_NAME}-release.apk"
  REFERENCE_APK="$TMP_ROOT/reference-${VERSION_NAME}.apk"
  log "Downloading reference APK from $REF_URL..."
  python3 - "$REF_URL" "$REFERENCE_APK" <<'PY'
import sys, urllib.request
url, dst = sys.argv[1], sys.argv[2]
req = urllib.request.Request(url, headers={"User-Agent": "fdroid-local-test"})
with urllib.request.urlopen(req, timeout=60) as r, open(dst, "wb") as f:
    f.write(r.read())
PY
  ok "Downloaded reference APK ($(wc -c < "$REFERENCE_APK") bytes)"
fi

if [[ -n "$REFERENCE_APK" ]]; then
  [[ -f "$REFERENCE_APK" ]] || fail "Reference APK not found: $REFERENCE_APK"
  [[ -f "$BUILT_APK" ]] || fail "Built APK not found: $BUILT_APK"

  # Check signing key fingerprint if apksigner is available
  SDK_DIR_FOR_TOOLS="$(find_sdk_dir 2>/dev/null || true)"
  APKSIGNER_BIN=""
  if [[ -n "$SDK_DIR_FOR_TOOLS" ]]; then
    APKSIGNER_BIN="$(find "$SDK_DIR_FOR_TOOLS/build-tools" -maxdepth 2 -name apksigner 2>/dev/null | sort -V | tail -n1 || true)"
  fi
  if [[ -n "$APKSIGNER_BIN" && -n "${JDK_HOME:-}" ]]; then
    log "Verifying reference APK signing certificate..."
    CERT_SHA="$(JAVA_HOME="$JDK_HOME" PATH="$JDK_HOME/bin:$PATH" "$APKSIGNER_BIN" verify --print-certs "$REFERENCE_APK" 2>&1 | sed -nE 's/.*SHA-256 digest: ([0-9a-f]+).*/\1/p' | head -n1 || true)"
    if [[ -n "$CERT_SHA" ]]; then
      if [[ "$CERT_SHA" == "$EXPECTED_SIGNING_KEY" ]]; then
        ok "Reference APK signing key matches AllowedAPKSigningKeys ($CERT_SHA)"
      else
        fail "Signing key mismatch! Reference=$CERT_SHA, Expected=$EXPECTED_SIGNING_KEY"
      fi
    fi
  fi

  log "Comparing APK contents (F-Droid reproducibility check, ignoring v1 META-INF signatures)..."
  python3 - "$REFERENCE_APK" "$BUILT_APK" <<'PY'
import hashlib, re, sys, zipfile

ref_path, built_path = sys.argv[1], sys.argv[2]
SIG_RE = re.compile(r"^META-INF/(MANIFEST\.MF|.*\.(SF|RSA|DSA|EC))$")

def read_entries(path):
    entries = {}
    with zipfile.ZipFile(path, "r") as zf:
        for info in zf.infolist():
            if info.is_dir() or SIG_RE.match(info.filename):
                continue
            entries[info.filename] = hashlib.sha256(zf.read(info.filename)).hexdigest()
    return entries

ref = read_entries(ref_path)
built = read_entries(built_path)

only_ref = sorted(set(ref) - set(built))
only_built = sorted(set(built) - set(ref))
differ = sorted(k for k in (set(ref) & set(built)) if ref[k] != built[k])

if only_ref or only_built or differ:
    print("[FAIL] APKs are NOT reproducible:", file=sys.stderr)
    for k in only_ref:
        print(f"  Only in reference: {k}", file=sys.stderr)
    for k in only_built:
        print(f"  Only in built:     {k}", file=sys.stderr)
    for k in differ:
        print(f"  Content differs:   {k}", file=sys.stderr)
    sys.exit(1)

print(f"[PASS] All {len(ref)} APK entries match byte-for-byte! Build is reproducible.")
PY
fi

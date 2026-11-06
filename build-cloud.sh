#!/usr/bin/env bash
# Build in the managed /workspace environment using its proxy and trusted CAs.
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/workspace/.jdk-21}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/.gradle}"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/.android-sdk}"

# Java does not automatically honor HTTPS_PROXY. Keep requests on the provided proxy.
if [[ -n "${HTTPS_PROXY:-${https_proxy:-}}" ]]; then
    read -r proxy_host proxy_port < <(python3 - <<'PY'
import os
from urllib.parse import urlsplit
p = urlsplit(os.environ.get('HTTPS_PROXY') or os.environ['https_proxy'])
if p.username or p.password or not p.hostname:
    raise SystemExit('Expected the managed environment HTTP proxy without inline credentials')
print(p.hostname, p.port or 80)
PY
    )
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dhttp.proxyHost=$proxy_host -Dhttp.proxyPort=$proxy_port -Dhttps.proxyHost=$proxy_host -Dhttps.proxyPort=$proxy_port"
fi
if [[ -f /etc/ssl/certs/java/cacerts ]]; then
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
exec ./gradlew "-Dorg.gradle.java.installations.paths=$JAVA_HOME" :app:assembleDebug --no-daemon "$@"

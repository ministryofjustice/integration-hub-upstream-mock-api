#!/usr/bin/env bash

# IntelliJ Shell Script configurations can invoke this through zsh and ignore
# the shebang, so re-exec with Bash before using Bash-specific syntax.
if [[ -z "${BASH_VERSION:-}" ]]; then
  exec /usr/bin/env bash "$0" "$@"
fi

set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
config_file="${project_root}/.integration-hub-demo.env"

# Prefer Homebrew's Java 25 so the IntelliJ shell configuration needs no JVM setup.
if command -v brew >/dev/null 2>&1; then
  brew_java_home="$(brew --prefix openjdk@25 2>/dev/null || true)/libexec/openjdk.jdk/Contents/Home"
  if [[ -x "${brew_java_home}/bin/java" ]]; then
    export JAVA_HOME="${brew_java_home}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi

if ! java -version 2>&1 | grep -q 'version "25\.'; then
  echo "Java 25 is required. Install it with: brew install openjdk@25" >&2
  exit 1
fi

if [[ ! -f "${config_file}" ]]; then
  echo "Missing ${config_file}. Copy .integration-hub-demo.env.example and add the environment values." >&2
  exit 1
fi

set -a
# The local file is Git-ignored and supplies the demo target and credentials.
source "${config_file}"
set +a

required_variables=(
  INTEGRATION_HUB_API_BASE_URL
  INTEGRATION_HUB_API_USERNAME
  INTEGRATION_HUB_API_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required environment variable: ${variable_name}" >&2
    exit 1
  fi
done

export INTEGRATION_HUB_DEMO_ENABLED=true
export INTEGRATION_HUB_DEMO_CORRELATION_ID="${INTEGRATION_HUB_DEMO_CORRELATION_ID:-integration-hub-demo-$(date +%Y%m%d-%H%M%S)}"

cd "${project_root}"
exec ./gradlew bootRun --args='--spring.profiles.active=dev'

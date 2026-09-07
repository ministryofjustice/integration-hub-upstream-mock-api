#!/usr/bin/env bash

set -euo pipefail

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

exec ./gradlew bootRun --args='--spring.profiles.active=dev'

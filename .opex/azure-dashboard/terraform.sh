#!/usr/bin/env bash

set -Eeuo pipefail
trap cleanup SIGINT SIGTERM ERR EXIT

root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd -P)

usage() {
  cat <<EOF
Usage: $(basename "${BASH_SOURCE[0]}") [-h] [-v] ACTION ENV [TF_ARGS]

A Terraform wrapper to ease operations.

ACTION is a standard Terraform action. ENV is the target environment
to point to in the 'env' folder. TF_ARGS are optional Terraform
argument proxied to ACTION.

Options:
    -h, --help          Print help information
    -v, --verbose       Set debug output
EOF
  exit 0
}

cleanup() {
  trap - SIGINT SIGTERM ERR EXIT
  # no needs, just in case
}

die() {
  local msg=$1
  local code=${2-1} # default exit status 1
  msg "$msg"
  exit "$code"
}

msg() {
  echo >&2 -e "${1-}"
}

parse_params() {
  while :; do
    case "${1-}" in
    -h | --help) usage ;;
    -v | --verbose) set -x ;;
    -?*) die "[ERROR] Unknown option: $1" ;;
    *) break ;;
    esac
    shift
  done

  arg_action=${1-}
  [[ -z "${arg_action-}" ]] && die "[ERROR] Missing ACTION"
  shift

  arg_env=${1-}
  [[ -z "${arg_env-}" ]] && die "[ERROR] Missing ENV"
  shift

  arg_tf="$@"

  return 0
}

set_subscription() {
  subscription=""
  source "${root_dir}/env/${arg_env}/backend.ini"
  az account set -s "${subscription}"
}

terraform_proxy() {
  backend_config_path="${root_dir}/env/${arg_env}/backend.tfvars"

  if echo "init plan apply refresh import output state taint destroy" | grep -w "${arg_action}" > /dev/null; then
    if [ "${arg_action}" = "init" ]; then
      msg "[INFO] init tf on env: ${arg_env}"
      terraform -chdir="${root_dir}" "${arg_action}" -backend-config="${backend_config_path}" ${arg_tf}
    elif [ "${arg_action}" = "output" ] || [ "${arg_action}" = "state" ] || [ "${arg_action}" = "taint" ]; then
      terraform -chdir="${root_dir}" init -reconfigure -backend-config="${backend_config_path}"
      terraform -chdir="${root_dir}" "${arg_action}" ${arg_tf}
    else
      msg "[INFO] init tf on env: ${arg_env}"
      terraform -chdir="${root_dir}" init -reconfigure -backend-config="${backend_config_path}"

      msg "[INFO] run tf with: ${arg_action} on env: ${arg_env} and other: >${arg_tf}<"
      terraform -chdir="${root_dir}" "${arg_action}" -var-file="${root_dir}/env/${arg_env}/terraform.tfvars" -compact-warnings ${arg_tf}
    fi
  else
    die "[ERROR] action not allowed."
  fi
}

main() {
  parse_params "$@"

  # if using cygwin, we have to transcode the WORKDIR
  if [[ ${WORKDIR-} == /cygdrive/* ]]; then
    WORKDIR=$(cygpath -w $WORKDIR)
  fi

  set_subscription
  terraform_proxy
}

main "$@"

#!/usr/bin/env bash
# Golem Core GitHub release -dev jars -> ~/.m2 as dev.golem:golemcore-<loader>-<mc>:<ver>+<mc> (compileOnly deps.golem).
#   install-golem-core.sh <version>
set -euo pipefail

VERSION="${1:?version, e.g. 1.0.4}"
NODES=(forge-1.20.1 neoforge-1.21.1 neoforge-26.1)
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for node in "${NODES[@]}"; do
    loader="${node%-*}" mc="${node##*-}"
    gav="golemcore-$loader-$mc"
    ver="$VERSION+$mc"
    gh release download "v$VERSION" -R Warfactory-Official/Golem-Core -p "golemcore-$loader-$ver-dev.jar" -D "$TMP"
    dir="$HOME/.m2/repository/dev/golem/$gav/$ver"
    mkdir -p "$dir"
    cp "$TMP/golemcore-$loader-$ver-dev.jar" "$dir/$gav-$ver.jar"
    cat > "$dir/$gav-$ver.pom" <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>dev.golem</groupId>
  <artifactId>$gav</artifactId>
  <version>$ver</version>
</project>
POM
    echo "installed dev.golem:$gav:$ver"
done

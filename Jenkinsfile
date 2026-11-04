@Library('wf-ci') _

def golem = '.github/scripts/install-golem-core.sh "$(grep -m1 -oP \'golemcore-[a-z]+-[0-9.]+:\\K[0-9.]+\' stonecutter.properties.toml)"'

wfBuild(
    repo: 'WarForge-Remaintained',
    jdk: '25',
    ci: false,
    branches: ['multi-version'],
    prepare: golem,
    build: 'buildAll --no-daemon --no-parallel --stacktrace',
    publish: 'publishAll --no-daemon --no-parallel --stacktrace',
    archive: 'versions/*/build/libs/*.jar',
    modRelease: [
        versionCmd: '''awk -F'"' '/^\\[mod\\]/{m=1;next} /^\\[/{m=0} m && /^version *=/{print $2; exit}' stonecutter.properties.toml''',
        build: 'buildAll --no-daemon --no-parallel --stacktrace',
        publish: 'publishAllMods --no-daemon --no-parallel --stacktrace',
        name: 'WarForge @VERSION@',
        collect: '''
mkdir -p dist
for node in versions/*/; do
  node=$(basename "$node")
  source="versions/$node/build/libs/warforge-remaintained-$node-$RELEASE_VERSION.jar"
  [ -f "$source" ] || { echo "Missing release jar: $source" >&2; exit 1; }
  cp "$source" "dist/warforge-$RELEASE_VERSION+$node.jar"
done
ls dist
''',
    ],
)

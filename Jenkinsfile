@Library('nstut-ci') _

// Trusted-main-only verification. Keep untrusted PR checks and release publishing
// on their existing isolated paths until equivalent controls have been proven.
nstutGradle(
    verify: 'python3 -m unittest discover -s tools -p "test_*.py" -v',
    targets: [
        [name: 'All Forge and NeoForge JVM tests and production builds',
         java: 21, minutes: 60,
         command: './gradlew testAllTargets buildAllTargets --stacktrace --no-daemon --max-workers=1']
    ],
    tests: '**/build/test-results/test/*.xml',
    artifacts: '**/build/libs/*.jar',
    diagnostics: '**/build/reports/tests/**,**/run/logs/**,**/run/crash-reports/**'
)

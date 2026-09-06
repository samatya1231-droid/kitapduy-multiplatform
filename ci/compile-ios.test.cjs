// Run with node --test ci/compile-ios.test.cjs (set BASH_EXE on Windows).
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

for (const exitCode of [0, 1, 42]) {
  test(`compiler exit ${exitCode} is preserved and output is saved`, () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'kitapduy-ci-test-'));
    fs.mkdirSync(path.join(root, 'ci'));
    fs.copyFileSync(path.join(__dirname, 'compile-ios.sh'), path.join(root, 'ci', 'compile-ios.sh'));
    fs.writeFileSync(path.join(root, 'gradlew'),
      '#!/usr/bin/env bash\nprintf "%s\\n" "$@"\nprintf "compiler stdout\\n"\nprintf "e: fixture.kt:1: compiler stderr\\n" >&2\nexit ' + exitCode + '\n',
      { mode: 0o755 });
    const result = spawnSync(process.env.BASH_EXE || 'bash',
      ['ci/compile-ios.sh', ':composeApp:linkReleaseFrameworkIosArm64'], {
        cwd: root, encoding: 'utf8',
        env: { ...process.env, KOTLIN_BUILD_LOG: 'compiler.log' }
      });
    assert.ifError(result.error);
    assert.equal(result.status, exitCode, result.stdout + result.stderr);
    const log = fs.readFileSync(path.join(root, 'compiler.log'), 'utf8');
    for (const expected of [':composeApp:linkReleaseFrameworkIosArm64', '--no-daemon',
      '--console=plain', '--stacktrace', 'compiler stdout', 'e: fixture.kt:1: compiler stderr']) {
      assert.ok(log.includes(expected), `Missing log content: ${expected}`);
    }
    // Retain the tiny fixture and log in the OS temp directory for troubleshooting.
  });
}

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

test('low-memory runner caps heap instead of forcing -Xmx6g and preserves custom GRADLE_OPTS', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'kitapduy-ci-mem-'));
  fs.mkdirSync(path.join(root, 'ci'));
  fs.copyFileSync(path.join(__dirname, 'compile-ios.sh'), path.join(root, 'ci', 'compile-ios.sh'));
  fs.writeFileSync(path.join(root, 'gradlew'),
    '#!/usr/bin/env bash\nprintf "GRADLE_OPTS=%s\\n" "${GRADLE_OPTS:-}"\nfor arg in "$@"; do\n  printf "ARG=%s\\n" "$arg"\ndone\n',
    { mode: 0o755 });

  const lowMem = spawnSync(process.env.BASH_EXE || 'bash',
    ['ci/compile-ios.sh', ':composeApp:linkReleaseFrameworkIosArm64'], {
      cwd: root, encoding: 'utf8',
      env: {
        ...process.env,
        GRADLE_OPTS: '',
        KITAPDUY_CI_MEM_BYTES: '7516192768',
        KOTLIN_BUILD_LOG: 'logs/compiler.log'
      }
    });
  assert.ifError(lowMem.error);
  assert.equal(lowMem.status, 0, lowMem.stdout + lowMem.stderr);
  assert.ok(lowMem.stdout.includes('"-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8 -XX:+UseG1GC"'), lowMem.stdout);
  assert.ok(lowMem.stdout.includes('ARG=--no-parallel'), lowMem.stdout);
  assert.ok(lowMem.stdout.includes('ARG=--max-workers=1'), lowMem.stdout);
  assert.ok(lowMem.stdout.includes('ARG=-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8 -XX:+UseG1GC'), lowMem.stdout);
  assert.ok(lowMem.stdout.includes('ARG=-Pkotlin.daemon.jvmargs=-Xmx1536m -XX:+UseG1GC'), lowMem.stdout);
  assert.ok(lowMem.stdout.includes('ARG=-Pkotlin.native.jvmArgs=-Xmx3584m -XX:+UseG1GC'), lowMem.stdout);
  assert.ok(!lowMem.stdout.includes('-Xmx6g'), lowMem.stdout);

  const customOpts = spawnSync(process.env.BASH_EXE || 'bash',
    ['ci/compile-ios.sh', ':composeApp:linkReleaseFrameworkIosArm64'], {
      cwd: root, encoding: 'utf8',
      env: {
        ...process.env,
        GRADLE_OPTS: '-Xmx2g -Dkotlin.native.jvmArgs=-Xmx3g',
        KITAPDUY_CI_MEM_BYTES: '17179869184',
        KOTLIN_BUILD_LOG: 'logs/custom.log'
      }
    });
  assert.ifError(customOpts.error);
  assert.equal(customOpts.status, 0, customOpts.stdout + customOpts.stderr);
  assert.ok(!customOpts.stdout.includes('-Xmx6g'), customOpts.stdout);
  assert.ok(customOpts.stdout.includes('GRADLE_OPTS=-Xmx2g -Dkotlin.native.jvmArgs=-Xmx3g'), customOpts.stdout);
});

test('configure-ci-gradle.sh writes idempotent 7 GB runner settings without erasing user properties', () => {
  const gradleHome = fs.mkdtempSync(path.join(os.tmpdir(), 'kitapduy-gradle-home-'));
  const propsPath = path.join(gradleHome, 'gradle.properties');
  fs.writeFileSync(propsPath, 'custom.user.prop=keep-me\n');

  const scriptPath = path.join(__dirname, 'configure-ci-gradle.sh');
  for (let i = 0; i < 2; i += 1) {
    const res = spawnSync(process.env.BASH_EXE || 'bash', [scriptPath], {
      encoding: 'utf8',
      env: {
        ...process.env,
        GRADLE_USER_HOME: gradleHome,
        JAVA_HOME: '/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home',
        KITAPDUY_CI_MEM_BYTES: '7516192768'
      }
    });
    assert.ifError(res.error);
    assert.equal(res.status, 0, res.stdout + res.stderr);
  }

  const content = fs.readFileSync(propsPath, 'utf8');
  assert.ok(content.includes('custom.user.prop=keep-me'), content);
  assert.ok(content.includes('org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home'), content);
  assert.ok(content.includes('org.gradle.jvmargs=-Xmx1536m'), content);
  assert.ok(content.includes('kotlin.native.jvmArgs=-Xmx3584m'), content);
  assert.ok(content.includes('org.gradle.workers.max=1'), content);
  assert.equal((content.match(/# BEGIN KITAPDUY CI GRADLE/g) || []).length, 1, content);

  const winGradleHome = fs.mkdtempSync(path.join(os.tmpdir(), 'kitapduy-gradle-win-'));
  const winRes = spawnSync(process.env.BASH_EXE || 'bash', [scriptPath], {
    encoding: 'utf8',
    env: {
      ...process.env,
      GRADLE_USER_HOME: winGradleHome,
      JAVA_HOME: 'C:\\Java\\jdk-17',
      KITAPDUY_CI_MEM_BYTES: '7516192768'
    }
  });
  assert.ifError(winRes.error);
  assert.equal(winRes.status, 0, winRes.stdout + winRes.stderr);
  const winContent = fs.readFileSync(path.join(winGradleHome, 'gradle.properties'), 'utf8');
  assert.ok(winContent.includes('org.gradle.java.home=C:/Java/jdk-17'), winContent);
});

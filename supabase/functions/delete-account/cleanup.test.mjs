import { test } from 'node:test'
import assert from 'node:assert/strict'
import { deleteVerifiedAccount, cleanupTargets } from './cleanup.ts'

function fixture(failAt = '', throwAt = '') {
  const calls = []
  const client = {
    from(table) { return { delete() { return { async eq(column, uid) {
      calls.push({ table, column, uid })
      if (table === throwAt) throw new Error('network failure')
      return { error: table === failAt ? { message: 'private database details' } : null }
    } } } } },
    auth: { admin: { async deleteUser(uid) {
      calls.push({ table: 'auth', uid })
      return { error: failAt === 'auth' ? { message: 'rejected' } : null }
    } } },
  }
  return { client, calls }
}

test('each database failure prevents Auth deletion and does not expose database details', async () => {
  for (const [table] of cleanupTargets) {
    const { client, calls } = fixture(table)
    await assert.rejects(deleteVerifiedAccount(client, 'verified-uid'), /^Error: Application data cleanup failed$/)
    assert.equal(calls.some(c => c.table === 'auth'), false)
    assert.equal(calls.at(-1).table, table)
  }
})

test('network exceptions stop the sequence before Auth deletion', async () => {
  const { client, calls } = fixture('', 'posts')
  await assert.rejects(deleteVerifiedAccount(client, 'verified-uid'))
  assert.equal(calls.some(c => c.table === 'auth'), false)
})

test('successful cleanup scopes every operation to the verified UID and deletes Auth last', async () => {
  const { client, calls } = fixture()
  await deleteVerifiedAccount(client, 'verified-uid')
  assert.equal(calls.length, cleanupTargets.length + 1)
  assert.equal(calls.at(-1).table, 'auth')
  assert.ok(calls.every(c => c.uid === 'verified-uid'))
})

test('Auth deletion failure is not reported as success', async () => {
  const { client } = fixture('auth')
  await assert.rejects(deleteVerifiedAccount(client, 'verified-uid'), /Auth deletion failed/)
})

test('empty identity cannot initiate cleanup', async () => {
  const { client, calls } = fixture()
  await assert.rejects(deleteVerifiedAccount(client, ' '), /Verified user required/)
  assert.deepEqual(calls, [])
})

test('a retry can finish after a previous partial failure', async () => {
  const failed = fixture('profiles')
  await assert.rejects(deleteVerifiedAccount(failed.client, 'verified-uid'))
  const retry = fixture()
  await deleteVerifiedAccount(retry.client, 'verified-uid')
  assert.equal(retry.calls.at(-1).table, 'auth')
})

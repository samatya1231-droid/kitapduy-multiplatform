// The caller must derive uid from auth.getUser(token), never from a request body.
// Every listed operation is required. Missing tables require a schema review, not silent success.
type DeleteResult = { error: unknown }
type CleanupClient = {
  from(table: string): { delete(): { eq(column: string, uid: string): PromiseLike<DeleteResult> } }
  auth: { admin: { deleteUser(uid: string): PromiseLike<DeleteResult> } }
}

export const cleanupTargets: ReadonlyArray<readonly [string, string]> = [
  ['followers', 'follower_id'],
  ['followers', 'following_id'],
  ['user_clubs', 'user_id'],
  ['reading_clubs', 'ownerid'],
  ['posts', 'user_id'],
  ['feed_comments', 'user_id'],
  ['comments', 'user_id'],
  ['poems', 'user_id'],
  ['user_bookshelves', 'user_id'],
  ['global_chat', 'user_id'],
  ['notifications', 'receiver_id'],
  ['fcm_tokens', 'user_id'],
  ['profiles', 'id'],
  ['users', 'uid'],
  ['quiz_leaderboard', 'user_id'],
]

export async function deleteVerifiedAccount(client: CleanupClient, uid: string): Promise<void> {
  if (!uid.trim()) throw new Error('Verified user required')
  for (const [table, column] of cleanupTargets) {
    const result = await client.from(table).delete().eq(column, uid)
    if (result.error) throw new Error('Application data cleanup failed')
  }
  // Keep Auth available for a retry until all application-data operations have succeeded.
  const result = await client.auth.admin.deleteUser(uid)
  if (result.error) throw new Error('Auth deletion failed')
}

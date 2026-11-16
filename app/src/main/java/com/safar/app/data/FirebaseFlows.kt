package com.safar.app.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Emits the query's snapshot on every change; removes the listener when collection stops. */
fun Query.snapshots(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot)
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

class TransactionAbortedException(message: String) : Exception(message)

/**
 * Runs a transaction. [update] returns the new value or throws
 * [TransactionAbortedException] to abort with a user-facing reason.
 */
suspend fun DatabaseReference.transact(update: (MutableData) -> Unit) {
    var abortReason: String? = null
    suspendCancellableCoroutine { cont ->
        runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result = try {
                update(current)
                Transaction.success(current)
            } catch (e: TransactionAbortedException) {
                abortReason = e.message
                Transaction.abort()
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                when {
                    error != null -> cont.resumeWithException(error.toException())
                    !committed -> cont.resumeWithException(TransactionAbortedException(abortReason ?: "Please try again."))
                    else -> cont.resume(Unit)
                }
            }
        })
    }
}

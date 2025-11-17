package com.veigar.questtracker.data

import android.annotation.SuppressLint
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.veigar.questtracker.model.ChildWishListItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object WishlistRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    private fun getChildWishListCollection(childId: String) =
        db.collection("wishlists").document(childId).collection("wishlist")


    fun addChildWishListItem(childId: String, item: ChildWishListItem): Task<Void> {
        val documentReference = getChildWishListCollection(childId).document()
        return documentReference.set(item.copy(
            wishlistId = documentReference.id
        ))
    }

    fun getChildWishListItem(childId: String, itemId: String): Task<ChildWishListItem> {
        return getChildWishListCollection(childId).document(itemId).get().continueWith { task ->
            task.result?.toObject(ChildWishListItem::class.java)
        }
    }

    fun updateChildWishListItem(childId: String, item: ChildWishListItem): Task<Void> {
        return getChildWishListCollection(childId).document(item.wishlistId).set(item)
    }

    fun deleteChildWishListItem(childId: String, itemId: String): Task<Void> {
        return getChildWishListCollection(childId).document(itemId).delete()
    }

    fun getWishListUpdates(childId: String): Flow<List<ChildWishListItem>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = getChildWishListCollection(childId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // An error occurred
                    close(e) // Close the flow with an exception
                    return@addSnapshotListener
                }

                val wishListItems = mutableListOf<ChildWishListItem>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        doc.toObject(ChildWishListItem::class.java).let { wishListItems.add(it) }
                    }
                }
                trySend(wishListItems).isSuccess // Offer the latest list to the flow
            }
        // When the flow is cancelled, remove the listener
        awaitClose { listenerRegistration.remove() }
    }
}
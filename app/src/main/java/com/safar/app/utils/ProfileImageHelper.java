package com.safar.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.safar.app.firebase.FirebaseManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * SAFAR Profile Image Helper
 * 
 * Handles profile picture upload, compression, and validation.
 * Max size: 5MB
 * Compression: JPEG quality 85%
 * Max dimensions: 1024x1024
 */
public class ProfileImageHelper {

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
        void onProgress(int progress);
    }

    private static final int MAX_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 85;

    /**
     * Upload profile picture with compression and validation.
     * 
     * @param context Android context
     * @param imageUri URI of the selected image
     * @param callback Upload result callback
     */
    public static void uploadProfilePicture(Context context, Uri imageUri, UploadCallback callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        try {
            // Check file size first
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onError("Unable to read image");
                return;
            }
            
            int fileSize = inputStream.available();
            inputStream.close();
            
            if (fileSize > AppConstants.MAX_PROFILE_PICTURE_SIZE) {
                callback.onError("Image too large. Maximum size is 5MB");
                return;
            }

            // Validate file type
            String mimeType = context.getContentResolver().getType(imageUri);
            if (mimeType == null || !mimeType.startsWith("image/")) {
                callback.onError("Please select a valid image file");
                return;
            }

            // Compress and resize
            byte[] compressedData = compressImage(context, imageUri);
            if (compressedData == null) {
                callback.onError("Failed to process image");
                return;
            }

            // Upload to Firebase Storage
            StorageReference storageRef = FirebaseManager.getInstance()
                .getUserProfilePictureRef(uid);
            
            UploadTask uploadTask = storageRef.putBytes(compressedData);
            
            uploadTask.addOnProgressListener(snapshot -> {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                callback.onProgress((int) progress);
            });
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    
                    // Update user profile in database
                    FirebaseManager.getInstance().getUserRef(uid)
                        .child("photoUrl").setValue(downloadUrl)
                        .addOnSuccessListener(unused -> callback.onSuccess(downloadUrl))
                        .addOnFailureListener(e -> callback.onError("Failed to update profile: " + e.getMessage()));
                });
            });
            
            uploadTask.addOnFailureListener(e -> 
                callback.onError("Upload failed: " + e.getMessage())
            );
            
        } catch (Exception e) {
            callback.onError("Error processing image: " + e.getMessage());
        }
    }

    /**
     * Compress and resize image to reduce upload size.
     */
    private static byte[] compressImage(Context context, Uri imageUri) {
        try {
            // Decode with inJustDecodeBounds to get dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            InputStream is = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();
            
            // Calculate sample size for downscaling
            int width = options.outWidth;
            int height = options.outHeight;
            int sampleSize = 1;
            
            while (width / sampleSize > MAX_DIMENSION || height / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2;
            }
            
            // Decode with sample size
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            
            is = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();
            
            if (bitmap == null) return null;
            
            // Scale to exact max dimension if still larger
            float scale = Math.min(
                (float) MAX_DIMENSION / bitmap.getWidth(),
                (float) MAX_DIMENSION / bitmap.getHeight()
            );
            
            if (scale < 1) {
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
            }
            
            // Compress to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            bitmap.recycle();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete user's profile picture from storage.
     */
    public static void deleteProfilePicture(String uid, Runnable onSuccess, Runnable onError) {
        StorageReference storageRef = FirebaseManager.getInstance()
            .getUserProfilePictureRef(uid);
        
        storageRef.delete()
            .addOnSuccessListener(unused -> {
                // Clear photoUrl in database
                FirebaseManager.getInstance().getUserRef(uid)
                    .child("photoUrl").removeValue()
                    .addOnSuccessListener(u -> { if (onSuccess != null) onSuccess.run(); })
                    .addOnFailureListener(e -> { if (onError != null) onError.run(); });
            })
            .addOnFailureListener(e -> {
                if (onError != null) onError.run();
            });
    }
}
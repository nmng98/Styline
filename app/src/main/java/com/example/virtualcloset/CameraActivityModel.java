package com.example.virtualcloset;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CameraActivityModel {
    // Labels
    private static Set<String> bottom_labels = new HashSet<>(Arrays.asList("trousers", "pants",
            "skirt", "jeans", "sweatpants", "trunks", "khaki", "joggers", "shorts",
            "skinny jeans", "mini skirt", "chinos", "cargo pants", "cargo shorts"));
    private static Set<String> top_labels = new HashSet<>(Arrays.asList("t-shirt", "shirt",
            "polo shirt", "polo", "dress shirt", "blouse",
            "sleeveless shirt", "jacket", "sherpa", "sweatshirt", "windbreaker",
            "sweater", "hoodie", "Blazer"));

    // Constants
    private static final int NUM_ITERATIONS = 1;

    public static void labelAndUpload(final Context mContext, final Bitmap bitmap,
                               final DrawableImageView imageView, final ProgressBar progressBar,
                               final StorageReference storageRef, final String UID) {
        progressBar.setVisibility(View.VISIBLE);

        // Label image
        FirebaseVisionImage imageToLabel = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getCloudImageLabeler();
        labeler.processImage(imageToLabel)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        String type = "";
                        String metaType = "";
                        for (FirebaseVisionImageLabel label : labels) {
                            metaType = label.getText();
                            if (top_labels.contains(metaType.toLowerCase())) {
                                type = "top";
                                break;
                            } else if (bottom_labels.contains(metaType.toLowerCase())) {
                                type = "bottom";
                                break;
                            }
                        }
                        if (type.length() == 0) {
                            Toast.makeText(mContext, "Unrecognizable!", Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }
                        Toast.makeText(mContext, "Type: " + type +
                                "\nMeta: " + metaType, Toast.LENGTH_LONG).show();

                        // Get masked from the drawing we made
                        Mat mask = imageView.mask;

                        // Create ROI
                        Rect bounding_box = new Rect(10, 10, mask.cols()-10,
                                mask.rows()-10);

                        // Get original image and convert from RGBA to RGB
                        Mat original_image = new Mat();
                        Utils.bitmapToMat(imageView.original_bitmap, original_image);
                        Imgproc.cvtColor(original_image, original_image, Imgproc.COLOR_RGBA2RGB);

                        // Do extraction
                        Imgproc.grabCut(original_image, mask, bounding_box, new Mat(), new Mat(),
                                NUM_ITERATIONS, Imgproc.GC_INIT_WITH_MASK);

                        // New mask to hold ONLY what was marked GC_PR_FGD by grabcut on our mask.
                        Mat probable_fgd_mask = new Mat();
                        Core.compare(mask, new Scalar(Imgproc.GC_PR_FGD), probable_fgd_mask, Core.CMP_EQ);

                        // Reusing mask variable, store into mask only what was marked as GC_FGD
                        // inside of mask.
                        Core.compare(mask, new Scalar(Imgproc.GC_FGD), mask, Core.CMP_EQ);

                        // Combine both masks so that we have GC_FGD + GC_PR_FGD
                        Core.add(probable_fgd_mask, mask, mask);

                        // We will store the foreground into an all-black Mat,
                        Mat foreground = new Mat(original_image.size(), CvType.CV_8UC3,
                                new Scalar(255, 255, 255, 255));
                        Imgproc.cvtColor(original_image, original_image, Imgproc.COLOR_RGB2RGBA);
                        Imgproc.cvtColor(original_image, original_image, Imgproc.COLOR_RGBA2mRGBA);
                        original_image.copyTo(foreground, mask);
                        Bitmap output_image = Bitmap.createBitmap(mask.cols(), mask.rows(),
                                Bitmap.Config.ARGB_8888);
                        Imgproc.cvtColor(foreground, foreground, Imgproc.COLOR_RGBA2mRGBA);
                        Utils.matToBitmap(foreground, output_image);
                        imageView.setImageBitmap(output_image);
                        upload(mContext, output_image, type, UID, storageRef);
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext,
                                "Failed to label & upload!",
                                Toast.LENGTH_LONG).show();
                        Log.e("MYAPP", "exception", e);
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(
                maxImageSize / realImage.getWidth(),
                maxImageSize / realImage.getHeight());

        realImage = createTrimmedBitmap(realImage);

//        if (ratio >= 1) return realImage;

        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());

        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    public static void upload(final Context mContext, Bitmap bitmap, String type, String UserUID,
                       StorageReference storageRef) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
        byte[] data = baos.toByteArray();

        // Define where we will save the image
        String uniqueImageName = UUID.randomUUID().toString();
        String savePath = "users/" + UserUID + "/clothes/" + type + "/" + uniqueImageName;

        // Start uploading, and set listeners to treat a successful/failed upload.
        StorageReference uploadRef = storageRef.child(savePath);
        UploadTask uploadTask = uploadRef.putBytes(data);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(mContext, "Failed to upload image.",
                        Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(mContext, "Upload Successful!",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(mContext, MainActivity.class);
                mContext.startActivity(intent);
                ((Activity)mContext).finish();
            }
        });
    }

    public static Bitmap createTrimmedBitmap(Bitmap bmp) {
        int imgHeight = bmp.getHeight();
        int imgWidth  = bmp.getWidth();

        //TRIM WIDTH - LEFT
        int startWidth = 0;
        for(int x = 0; x < imgWidth; x++) {
            if (startWidth == 0) {
                for (int y = 0; y < imgHeight; y++) {
                    if (bmp.getPixel(x, y) != Color.TRANSPARENT) {
                        startWidth = x;
                        break;
                    }
                }
            } else break;
        }
        //TRIM WIDTH - RIGHT
        int endWidth  = 0;
        for(int x = imgWidth - 1; x >= 0; x--) {
            if (endWidth == 0) {
                for (int y = 0; y < imgHeight; y++) {
                    if (bmp.getPixel(x, y) != Color.TRANSPARENT) {
                        endWidth = x;
                        break;
                    }
                }
            } else break;
        }
        //TRIM HEIGHT - TOP
        int startHeight = 0;
        for(int y = 0; y < imgHeight; y++) {
            if (startHeight == 0) {
                for (int x = 0; x < imgWidth; x++) {
                    if (bmp.getPixel(x, y) != Color.TRANSPARENT) {
                        startHeight = y;
                        break;
                    }
                }
            } else break;
        }
        //TRIM HEIGHT - BOTTOM
        int endHeight = 0;
        for(int y = imgHeight - 1; y >= 0; y--) {
            if (endHeight == 0 ) {
                for (int x = 0; x < imgWidth; x++) {
                    if (bmp.getPixel(x, y) != Color.TRANSPARENT) {
                        endHeight = y;
                        break;
                    }
                }
            } else break;
        }
        return Bitmap.createBitmap(
                bmp,
                startWidth,
                startHeight,
                endWidth - startWidth,
                endHeight - startHeight
        );

    }
}

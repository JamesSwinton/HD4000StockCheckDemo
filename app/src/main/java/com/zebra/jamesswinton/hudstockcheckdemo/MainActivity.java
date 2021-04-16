package com.zebra.jamesswinton.hudstockcheckdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.symbol.zebrahud.ZebraHud;
import com.zebra.jamesswinton.hudstockcheckdemo.databinding.ActivityMainBinding;
import com.zebra.jamesswinton.hudstockcheckdemo.databinding.LayoutHudViewBinding;

import java.util.List;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity implements ZebraHud.EventListener,
        OnCompleteListener<List<Barcode>>, OnSuccessListener<List<Barcode>>, OnFailureListener,
        // OnCompleteListener<Text>, OnSuccessListener<Text>, OnFailureListener,
        AdapterView.OnItemSelectedListener {

    // Debugging
    private static final String TAG = "MainActivity";

    // HUD
    private ZebraHud mZebraHud = new ZebraHud();

    // UI
    private ActivityMainBinding mDataBinding;

    // MLKit Text Recognizer & BarcodeScanner
    private TextRecognizer mTextRecognizer;
    private BarcodeScanner mBarcodeScanner;

    // Current Frame Holder
    private Bitmap mCurrentFrame;
    private boolean mReadyForNextFrame = false;

    // Hud Settings
    private List<ZebraHud.HudCameraResolution> mCameraResolutions;

    // Mode Holder
    private boolean mBarcodeMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        // Init TextRecognizer
        mTextRecognizer = TextRecognition.getClient();

        // Init BarcodeScanner
        mBarcodeScanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_CODE_128, Barcode.FORMAT_QR_CODE)
                .build()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Init HUD
        mZebraHud.attachToContext(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mZebraHud.detachFromContext(this, true);
    }

    /**
     * HUD Callbacks
     */

    @Override
    public void onConnected(@NonNull Boolean connected) {
        // Log Connection
        Snackbar.make(mDataBinding.getRoot(), "HUD " + (connected ? "connected" : "disconnected"), LENGTH_LONG).show();

        // Start / Stop Camera Capture
        if (connected) {
            // Populate & Enabled Spinners
            initSettings();

            // Enable Camera Processing
            mReadyForNextFrame = true;
            mZebraHud.startCameraCapture();
        } else {
            // Disable Spinner
            disableSettings();

            // Disable Camera Processing
            mReadyForNextFrame = false;
            mZebraHud.stopCameraCapture();
        }

    }

    @Override
    public void onImageUpdated(@NonNull byte[] bytes) {
        // Mirror HUD display to ImageView
        mDataBinding.rawFrame.setImageBitmap(mCurrentFrame);
    }

    @Override
    public void onCameraImage(@NonNull Bitmap frame) {
        // Process Image
        if (mReadyForNextFrame) {
            mCurrentFrame = frame;
            mReadyForNextFrame = false;

            mBarcodeScanner.process(InputImage.fromBitmap(frame,0))
                    .addOnCompleteListener(this)
                    .addOnSuccessListener(this)
                    .addOnFailureListener(this);

//            mTextRecognizer.process(InputImage.fromBitmap(frame, 0))
//                    .addOnCompleteListener(this)
//                    .addOnSuccessListener(this)
//                    .addOnFailureListener(this);
        }
    }

    @Override
    public void onCameraImageRawJpeg(@NonNull byte[] bytes) {

    }

    private void initSettings() {
        // Enable Spinners
        mDataBinding.cameraResolution.setEnabled(true);

        // Init Settings
        mDataBinding.cameraResolution.setOnItemSelectedListener(this);

        // Set Camera Resolution Adapter
        mCameraResolutions = mZebraHud.getCameraAllResolutions();
        if (!mCameraResolutions.isEmpty()) {
            // update resolution spinner
            String[] listRes = new String[mCameraResolutions.size()];
            for (int ix = 0; ix < mCameraResolutions.size(); ix++) {
                ZebraHud.HudCameraResolution res = mCameraResolutions.get(ix);
                listRes[ix] = getString(R.string.camera_resolution_entry, res.width, res.height, res.maxZoom);
            }
            ArrayAdapter<String> adapterRes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listRes);
            mDataBinding.cameraResolution.setAdapter(adapterRes);
        }
    }

    private void disableSettings() {
        mDataBinding.cameraResolution.setEnabled(false);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.camera_resolution: {
                mZebraHud.setCameraCurrentResolution(mCameraResolutions.get(position));

                // If 720p, set max FrameRate
                if (mCameraResolutions.get(position).width == 720) {
                    mZebraHud.setCamera720pFramerate(ZebraHud.ListCamera720pFramerates[
                            ZebraHud.ListCamera720pFramerates.length -1]);
                }
                break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * MLKit Callbacks
     */
    @Override
    public void onComplete(@NonNull Task<List<Barcode>> task) {
        // Verify Text Was Detected
        if (task.isSuccessful()) {
            List<Barcode> barcodes = task.getResult();
            if (barcodes != null && barcodes.size() > 0) {
                // Get Bounding around Barcode
                BitmapDrawable frameWithText = TextDrawer.drawBarcodeOnBitmap(this,
                        mCurrentFrame, barcodes.get(0));

                // Check Stock
                if (Constants.StockMap.get(barcodes.get(0).getRawValue()) != null) {
                    Product product = Constants.StockMap.get(barcodes.get(0).getRawValue());
                    mDataBinding.hudViewLayout.productText.setText(product.getName() + "\n(" + barcodes.get(0).getRawValue() + ")");
                    if (product.getStock() == 0) {
                        mDataBinding.hudViewLayout.baseLayout.setBackgroundColor(getColor(R.color.zebra_red));
                        mDataBinding.hudViewLayout.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_error));
                        mDataBinding.hudViewLayout.statusText.setText("No stock remaining!");
                    } else if (product.getStock() > 0 && product.getStock() < 5) {
                        mDataBinding.hudViewLayout.baseLayout.setBackgroundColor(getColor(R.color.zebra_yellow));
                        mDataBinding.hudViewLayout.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_warning));
                        mDataBinding.hudViewLayout.statusText.setText("Stock Low! \n" + product.getStock());
                    } else {
                        mDataBinding.hudViewLayout.baseLayout.setBackgroundColor(getColor(R.color.zebra_green));
                        mDataBinding.hudViewLayout.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_success));
                        mDataBinding.hudViewLayout.statusText.setText("Well stocked! \n" + product.getStock());
                    }
                } else {
                    mDataBinding.hudViewLayout.baseLayout.setBackgroundColor(getColor(R.color.zebra_red));
                    mDataBinding.hudViewLayout.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_error));
                    mDataBinding.hudViewLayout.statusText.setText("Invalid Barcode!");
                    mDataBinding.hudViewLayout.productText.setText("");
                }

                // Update Views
                mDataBinding.hudViewLayout.textFrame.setImageDrawable(frameWithText);
                mZebraHud.showHudView(mDataBinding.hudViewLayout.getRoot());
            } else {
                mDataBinding.hudViewLayout.baseLayout.setBackgroundColor(getColor(R.color.zebra_blue));
                mDataBinding.hudViewLayout.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_info));
                mDataBinding.hudViewLayout.statusText.setText("Waiting for barcode...");
                mDataBinding.hudViewLayout.productText.setText("");

                mDataBinding.hudViewLayout.textFrame.setImageBitmap(mCurrentFrame);
                mZebraHud.showHudView(mDataBinding.hudViewLayout.getRoot());
            }
        }
        // Update Holder
        mReadyForNextFrame = true;
    }

    @Override
    public void onSuccess(List<Barcode> barcodes) {
        Log.i(TAG, "Success: " + (barcodes.isEmpty() ? "No barcode" : barcodes.get(0).getRawValue()));
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        e.printStackTrace();
    }


    //    @Override
//    public void onComplete(@NonNull Task<Text> task) {
//
//    }
//
//    @Override
//    public void onSuccess(@Nullable Text text) {
//        // Verify Text Was Detected
//        if (text != null && text.getTextBlocks().size() > 0) {
//            BitmapDrawable frameWithText = TextDrawer.drawTextOnBitmap(this, mCurrentFrame, text);
//            mDataBinding.hudViewLayout.textFrame.setImageDrawable(frameWithText);
//            mZebraHud.showHudView(mDataBinding.hudViewLayout.getRoot());
//        } else {
//            Log.i(TAG, "No Text Detected");
//            mDataBinding.hudViewLayout.textFrame.setImageBitmap(mCurrentFrame);
//            mZebraHud.showHudView(mDataBinding.hudViewLayout.getRoot());
//        }
//
//        // Update Holder
//        mReadyForNextFrame = true;
//    }
//
//    @Override
//    public void onFailure(@NonNull Exception e) {
//        // Show Bitmap without Text
//        mDataBinding.hudViewLayout.textFrame.setImageBitmap(mCurrentFrame);
//        mZebraHud.showHudView(mDataBinding.hudViewLayout.getRoot());
//        mReadyForNextFrame = true;
//
//        // Log Error
//        e.printStackTrace();
//        Log.e(TAG, "Error: " + e.getMessage());
//        Toast.makeText(this, "Error: " + e.getMessage(),
//                Toast.LENGTH_LONG).show();
//    }

}
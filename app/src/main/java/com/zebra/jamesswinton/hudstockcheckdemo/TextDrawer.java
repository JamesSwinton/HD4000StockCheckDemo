package com.zebra.jamesswinton.hudstockcheckdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.text.Text;

import java.util.List;

public class TextDrawer {

    public static BitmapDrawable drawTextOnBitmap(@NonNull Context cx, @NonNull Bitmap originalImage,
                                                  @NonNull Text text) {
        // The Color of the Rectangle to Draw on top of Text
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.parseColor("WHITE"));
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4.0f);

        // The Color of the Rectangle to Draw on top of Text
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.parseColor("WHITE"));
        textPaint.setTextSize(35);

        // Create the Canvas object,
        // Which ever way you do image that is ScreenShot for example, you
        // need the views Height and Width to draw recatngles
        // because the API detects the position of Text on the View
        // So Dimesnions are important for Draw method to draw at that Text
        // Location
        Bitmap tempBitmap = Bitmap.createBitmap(originalImage.getWidth(), originalImage.getHeight(),
                originalImage.getConfig());
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(originalImage, 0, 0, null);

        // Loop through each `Block`
        for (Text.TextBlock textBlock : text.getTextBlocks()) {
            for (Text.Line line : textBlock.getLines()) {
                for (Text.Element element : line.getElements()) {
                    // Get the Rectangle / BoundingBox of the word
                    RectF rect = new RectF(element.getBoundingBox());
                    rectPaint.setColor(Color.parseColor("red"));
                    rectPaint.setStrokeWidth(4.0f);

                    // Finally Draw Rectangle/boundingBox around word
                    canvas.drawRect(rect, rectPaint);

                    // Draw Text
                    canvas.drawText(element.getText(),  element.getCornerPoints()[0].x, element.getCornerPoints()[0].y, textPaint);
                }
            }
        }

        return new BitmapDrawable(cx.getResources(), tempBitmap);
    }

    public static BitmapDrawable drawBarcodeOnBitmap(@NonNull Context cx, @NonNull Bitmap originalImage,
                                                  @NonNull Barcode barcode) {
        // The Color of the Rectangle to Draw on top of Text
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.parseColor("WHITE"));
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4.0f);

        // The Color of the Rectangle to Draw on top of Text
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.parseColor("WHITE"));
        textPaint.setTextSize(35);

        // Create the Canvas object,
        // Which ever way you do image that is ScreenShot for example, you
        // need the views Height and Width to draw recatngles
        // because the API detects the position of Text on the View
        // So Dimesnions are important for Draw method to draw at that Text
        // Location
        Bitmap tempBitmap = Bitmap.createBitmap(originalImage.getWidth(), originalImage.getHeight(),
                originalImage.getConfig());
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(originalImage, 0, 0, null);

        // Get the Rectangle / BoundingBox of the word
        Rect rect = barcode.getBoundingBox();
        rectPaint.setColor(Color.parseColor("red"));
        rectPaint.setStrokeWidth(4.0f);

        // Finally Draw Rectangle/boundingBox around word
        canvas.drawRect(rect, rectPaint);

        // Draw Text
        canvas.drawText(barcode.getRawValue(), barcode.getCornerPoints()[0].x,
                barcode.getCornerPoints()[0].y, textPaint);

        return new BitmapDrawable(cx.getResources(), tempBitmap);
    }
}

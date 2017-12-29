package com.decrediton.Util;

/**
 * Created by Macsleven on 11/23/2015.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.decrediton.R;


public class CircleImage {
    Context context;

    public CircleImage(Context context) {
        this.context = context;
    }
   /* public Bitmap transform(Bitmap source) {
        try {

            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap
                    .createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                // source.recycle();
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size,
                    squaredBitmap.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            // canvas.drawArc(rectf, -90, 360, false, lightRed);
            // squaredBitmap.recycle();
            return bitmap;
        } catch (Exception e) {
            // TODO: handle exception
        }

        return BitmapFactory.decodeResource(context.getResources(),
                R.drawable.debit);
    }*/

}

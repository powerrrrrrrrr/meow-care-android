package com.meowcare.app.appearance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Icon;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.AtomicFile;
import com.meowcare.app.MainActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/** Keeps a bounded local bitmap; no gallery permission or long-lived URI grant is needed. */
public final class CustomIcon {
    public static final String SHORTCUT_ID="meow-care-custom-icon";
    public static final int SIZE=512;
    private static final int MAX_DECODE_EDGE=2048;
    private final AtomicFile file;

    public CustomIcon(Context context) { this(new File(context.getFilesDir(),"custom-app-icon.png")); }
    public CustomIcon(File file) { this.file=new AtomicFile(file); }

    public Bitmap load() throws IOException {
        if (!file.getBaseFile().exists()) return null;
        try (InputStream input=file.openRead()) {
            Bitmap image=BitmapFactory.decodeStream(input);
            if (image==null) throw new IOException("图标文件无法读取，请重新选择图片");
            return image;
        }
    }

    public void save(Bitmap bitmap) throws IOException {
        FileOutputStream output=null;
        try {
            output=file.startWrite();
            if (!bitmap.compress(Bitmap.CompressFormat.PNG,100,output)) throw new IOException("图标保存失败");
            file.finishWrite(output);
        } catch (IOException | RuntimeException error) {
            if (output!=null) file.failWrite(output);
            throw error;
        }
    }

    public static Bitmap read(Context context, Uri uri) throws IOException {
        Bitmap decoded;
        if (Build.VERSION.SDK_INT>=28) {
            decoded=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),uri),(decoder,info,source)->{
                int width=info.getSize().getWidth(),height=info.getSize().getHeight();
                float scale=Math.min(1f,(float)MAX_DECODE_EDGE/Math.max(width,height));
                decoder.setTargetSize(Math.max(1,Math.round(width*scale)),Math.max(1,Math.round(height*scale)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        } else {
            decoded=readLegacy(context,uri);
        }
        try { return square(decoded); } finally { decoded.recycle(); }
    }

    static Bitmap readLegacy(Context context, Uri uri) throws IOException {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        try (InputStream input=context.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input,null,options); }
        if (options.outWidth<=0 || options.outHeight<=0) throw new IOException("请选择可读取的图片");
        options.inSampleSize=1;
        while (Math.max(options.outWidth,options.outHeight)/options.inSampleSize>MAX_DECODE_EDGE) options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        Bitmap decoded;
        try (InputStream input=context.getContentResolver().openInputStream(uri)) { decoded=BitmapFactory.decodeStream(input,null,options); }
        if (decoded==null) throw new IOException("请选择可读取的图片");
        if(!"image/jpeg".equals(options.outMimeType)) return decoded;
        try (InputStream input=context.getContentResolver().openInputStream(uri)) {
            ExifInterface exif=new ExifInterface(input);
            Matrix matrix=new Matrix();
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.setRotate(180);break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1,-1);break;
                case ExifInterface.ORIENTATION_TRANSPOSE: matrix.setRotate(90);matrix.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.setRotate(90);break;
                case ExifInterface.ORIENTATION_TRANSVERSE: matrix.setRotate(270);matrix.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.setRotate(270);break;
                default: break;
            }
            Bitmap oriented=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),matrix,true);
            if (oriented!=decoded) decoded.recycle();
            return oriented;
        } catch (IOException | RuntimeException error) {
            decoded.recycle();throw error;
        }
    }

    public static Bitmap square(Bitmap source) {
        int edge=Math.min(source.getWidth(),source.getHeight());
        int left=(source.getWidth()-edge)/2,top=(source.getHeight()-edge)/2;
        Bitmap output=Bitmap.createBitmap(SIZE,SIZE,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(output);canvas.drawColor(0xFFF7F8F3);
        canvas.drawBitmap(source,new Rect(left,top,left+edge,top+edge),new Rect(0,0,SIZE,SIZE),new Paint(Paint.FILTER_BITMAP_FLAG));
        return output;
    }

    public static Icon icon(Bitmap bitmap) {
        // Adaptive layers extend past the visible mask. Keep the entire photo in the viewport
        // and extend its edge pixels into the overscan, rather than adding a white frame.
        int padding=Math.round(bitmap.getWidth()*AdaptiveIconDrawable.getExtraInsetFraction());
        Bitmap layer=Bitmap.createBitmap(bitmap.getWidth()+2*padding,bitmap.getHeight()+2*padding,Bitmap.Config.ARGB_8888);
        BitmapShader shader=new BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
        Matrix translation=new Matrix();translation.setTranslate(padding,padding);shader.setLocalMatrix(translation);
        Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);paint.setShader(shader);
        new Canvas(layer).drawRect(0,0,layer.getWidth(),layer.getHeight(),paint);
        return Icon.createWithAdaptiveBitmap(layer);
    }

    /** Upgrade existing home-screen copies in place, without asking the launcher to add another. */
    public static void migratePinned(Context context) throws IOException {
        SharedPreferences preferences=context.getSharedPreferences("app-icon",Context.MODE_PRIVATE);
        if(preferences.getBoolean("adaptive-shortcut",false))return;
        Bitmap saved=new CustomIcon(context).load();
        if(saved==null)return;
        try {
            ShortcutManager manager=context.getSystemService(ShortcutManager.class);
            if(manager==null)return;
            boolean exists=manager.getPinnedShortcuts().stream().anyMatch(shortcut->SHORTCUT_ID.equals(shortcut.getId()));
            if(exists&&!manager.updateShortcuts(Collections.singletonList(shortcut(context,saved))))
                throw new IllegalStateException("Shortcut update rate limited");
            preferences.edit().putBoolean("adaptive-shortcut",true).apply();
        } finally {saved.recycle();}
    }

    public static ShortcutInfo shortcut(Context context, Bitmap bitmap) {
        return new ShortcutInfo.Builder(context,SHORTCUT_ID)
                .setActivity(AppIcons.component(context,AppIcons.current(context)))
                .setShortLabel("喵洁日历").setLongLabel("打开喵洁日历")
                .setIcon(icon(bitmap))
                .setIntent(new Intent(context,MainActivity.class).setAction(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP))
                .build();
    }
}

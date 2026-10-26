package com.meowcare.app.appearance;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.meowcare.app.MainActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppIconsTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test public void customPhotoFillsCircleSquareAndRoundedSquareWithoutAWhiteWrapper() {
        Bitmap photo=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
        Canvas source=new Canvas(photo);source.drawColor(Color.GREEN);
        Paint paint=new Paint();paint.setColor(Color.BLUE);source.drawRect(0,0,32,512,paint);
        paint.setColor(Color.RED);source.drawRect(480,0,512,512,paint);
        Drawable loaded=CustomIcon.icon(photo).loadDrawable(context);
        assertTrue("Custom photos must use adaptive icons, not legacy white wrappers",loaded instanceof AdaptiveIconDrawable);
        AdaptiveIconDrawable adaptive=(AdaptiveIconDrawable)loaded;
        adaptive.setBounds(0,0,256,256);
        for(int shape=0;shape<3;shape++) {
            Bitmap rendered=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(rendered);Path mask=new Path();
            if(shape==0)mask.addCircle(128,128,128,Path.Direction.CW);
            else if(shape==1)mask.addRect(new RectF(0,0,256,256),Path.Direction.CW);
            else mask.addRoundRect(new RectF(0,0,256,256),56,56,Path.Direction.CW);
            canvas.clipPath(mask);
            // Use the framework's actual overscan bounds with each representative launcher mask.
            adaptive.getForeground().draw(canvas);
            assertEquals("Left edge must retain the photo, not a wrapper or zoomed crop",Color.BLUE,rendered.getPixel(8,128));
            assertEquals("Right edge must retain the photo",Color.RED,rendered.getPixel(247,128));
            assertEquals("Top must be filled to the mask",Color.GREEN,rendered.getPixel(128,8));
            assertEquals("Bottom must be filled to the mask",Color.GREEN,rendered.getPixel(128,247));
            assertEquals(Color.GREEN,rendered.getPixel(128,128));
            assertEquals(shape==1?255:0,Color.alpha(rendered.getPixel(0,0)));
            rendered.recycle();
        }
        photo.recycle();
    }

    @Test public void switchingAndRestoringLeavesExactlyOneWorkingLauncher() {
        AppIcons.Choice original=AppIcons.current(context);
        try {
            for(AppIcons.Choice choice:AppIcons.Choice.values()) {
                AppIcons.select(context,choice);
                assertEquals(choice,AppIcons.current(context));
                Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.getPackageName());
                List<ResolveInfo> matches=context.getPackageManager().queryIntentActivities(launcher,0);
                assertEquals(1,matches.size());
                assertEquals(AppIcons.component(context,choice).getClassName(),matches.get(0).activityInfo.name);
                assertEquals(MainActivity.class.getName(),matches.get(0).activityInfo.targetActivity);
                assertNotNull(context.getPackageManager().resolveActivity(new Intent(context,MainActivity.class),0));
                for(android.content.pm.ShortcutInfo shortcut:context.getSystemService(android.content.pm.ShortcutManager.class).getPinnedShortcuts()) {
                    if(CustomIcon.SHORTCUT_ID.equals(shortcut.getId())) {
                        assertTrue("Custom shortcut must stay enabled after switching",shortcut.isEnabled());
                        assertEquals(AppIcons.component(context,choice),shortcut.getActivity());
                    }
                }
            }
        } finally { AppIcons.select(context,original); }
    }

    @Test public void photoIsCenterCroppedAndSurvivesStoreRecreation() throws Exception {
        File source=File.createTempFile("icon-source-",".png",context.getCacheDir());
        File saved=File.createTempFile("icon-saved-",".png",context.getCacheDir());
        Bitmap input=Bitmap.createBitmap(1600,800,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(input);canvas.drawColor(Color.RED);Paint paint=new Paint();paint.setColor(Color.GREEN);canvas.drawRect(400,0,1200,800,paint);
        try {
            try(FileOutputStream out=new FileOutputStream(source)){assertTrue(input.compress(Bitmap.CompressFormat.PNG,100,out));}
            Bitmap cropped=CustomIcon.read(context,Uri.fromFile(source));
            assertEquals(512,cropped.getWidth());assertEquals(512,cropped.getHeight());
            assertEquals(Color.GREEN,cropped.getPixel(10,256));assertEquals(Color.GREEN,cropped.getPixel(501,256));
            new CustomIcon(saved).save(cropped);
            Bitmap loaded=new CustomIcon(saved).load();
            assertNotNull(loaded);assertTrue(cropped.sameAs(loaded));
            cropped.recycle();loaded.recycle();
        } finally {input.recycle();assertTrue(source.delete());assertTrue(saved.delete());}
    }

    @Test public void malformedImageDoesNotReplaceTheSavedIcon() throws Exception {
        File bad=File.createTempFile("icon-bad-",".png",context.getCacheDir());
        File saved=File.createTempFile("icon-valid-",".png",context.getCacheDir());
        Bitmap image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);image.eraseColor(Color.BLUE);
        try {
            CustomIcon store=new CustomIcon(saved);store.save(image);
            try {store.save(CustomIcon.read(context,Uri.fromFile(bad)));fail("Must reject malformed image");}
            catch(IOException expected){assertNotNull(expected);}
            Bitmap loaded=store.load();assertTrue(image.sameAs(loaded));loaded.recycle();
        } finally {image.recycle();assertTrue(bad.delete());assertTrue(saved.delete());}
    }

    @Test public void legacyDecoderSamplesLargeImagesAndHonorsCameraRotation() throws Exception {
        File photo=File.createTempFile("icon-rotated-",".jpg",context.getCacheDir());
        Bitmap image=Bitmap.createBitmap(3200,1600,Bitmap.Config.ARGB_8888);image.eraseColor(Color.CYAN);
        try {
            try(FileOutputStream out=new FileOutputStream(photo)){assertTrue(image.compress(Bitmap.CompressFormat.JPEG,95,out));}
            ExifInterface exif=new ExifInterface(photo);exif.setAttribute(ExifInterface.TAG_ORIENTATION,String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));exif.saveAttributes();
            Bitmap decoded=CustomIcon.readLegacy(context,Uri.fromFile(photo));
            assertEquals(800,decoded.getWidth());assertEquals(1600,decoded.getHeight());decoded.recycle();
            Bitmap modern=CustomIcon.read(context,Uri.fromFile(photo));assertEquals(512,modern.getWidth());modern.recycle();
        } finally {image.recycle();assertTrue(photo.delete());}
    }

    @Test public void shortcutKeepsAStableIdentityAndDirectLaunchTargetAcrossIconChanges() {
        AppIcons.Choice original=AppIcons.current(context);
        Bitmap bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
        try {
            for(AppIcons.Choice choice:AppIcons.Choice.values()) {
                AppIcons.select(context,choice);
                android.content.pm.ShortcutInfo shortcut=CustomIcon.shortcut(context,bitmap);
                assertEquals(CustomIcon.SHORTCUT_ID,shortcut.getId());
                assertEquals(AppIcons.component(context,choice),shortcut.getActivity());
                assertEquals(MainActivity.class.getName(),shortcut.getIntent().getComponent().getClassName());
                assertNotNull(context.getPackageManager().resolveActivity(shortcut.getIntent(),PackageManager.MATCH_DEFAULT_ONLY));
            }
        } finally {bitmap.recycle();AppIcons.select(context,original);}
    }
}

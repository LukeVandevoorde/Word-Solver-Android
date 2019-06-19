package lukev.wordsolver;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class ScreenShotObserver extends ContentObserver {

    private static final String screenshotPath = Environment.getExternalStorageDirectory()
            + File.separator + Environment.DIRECTORY_PICTURES
            + File.separator + "Screenshots" + File.separator;

    public ScreenShotObserver(Handler handler) {
        super(handler);

        Log.i("Screenshot path", screenshotPath);
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Log.i("DETECTED SCREENSHOT", "#####################");
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        Log.i("DETECTED SCREENSHOT", "#### URI: " + uri);
    }
}

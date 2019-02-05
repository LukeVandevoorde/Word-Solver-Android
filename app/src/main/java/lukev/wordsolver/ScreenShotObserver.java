package lukev.wordsolver;

import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class ScreenShotObserver extends FileObserver {

    private static final String screenshotPath = Environment.getExternalStorageDirectory()
            + File.separator + Environment.DIRECTORY_PICTURES
            + File.separator + "Screenshots" + File.separator;

    public ScreenShotObserver() {
        super(screenshotPath);

        Log.d("Screenshot path", screenshotPath);
    }

    public void onEvent(int event, String path) {
        Log.d("SCREENSHOT FOUND?!", "################################################### Path: " + path);
    }
}

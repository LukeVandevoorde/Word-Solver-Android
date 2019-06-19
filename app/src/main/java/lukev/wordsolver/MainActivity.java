package lukev.wordsolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.abangfadli.shotwatch.ScreenshotData;
import com.abangfadli.shotwatch.ShotWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;

import client.WSClientUploader;
import solver.Board;
import solver.Letter;
import solver.StringTools;
import solver.Word;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;
    private Thread solveThread;
    private Thread requestThread;
    private Board boardToSolve;
    private final String SCREENSHOT_PATH = Environment.getExternalStorageDirectory()
            + File.separator + Environment.DIRECTORY_PICTURES
            + File.separator + "Screenshots" + File.separator;
    private final Uri SCREENSHOT_URI = new Uri.Builder().appendPath(SCREENSHOT_PATH).build();
    private ScreenShotObserver screenShotObserver;

    private ShotWatch shotWatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[] {READ_EXTERNAL_STORAGE}, 1);
        ShotWatch.Listener listener = new ShotWatch.Listener() {
            public void onScreenShotTaken(ScreenshotData data) {
                Log.i("WORDSOLVER", "##################$$$$$$$$$$$$$$$$$$$$$$$$$$$$%%%%%%%%%%%%%%%%%%%%%^^^^^^^^^^^^^^^^^^^^^^^^^^ SCREENSHOT DETECTED");
                parseUri(Uri.parse(data.getPath()));
            }
        };

        shotWatch = new ShotWatch(getContentResolver(), listener);
        shotWatch.register();

        new Thread(new Runnable() {
            @Override
            public void run() {
                StringTools.dictionaryWordsContain("foo");
                Board.solveThreaded(new Board(15, 15), "a", (progress)->{}, new Board.WordRank(5));
            }
        }).start();

        solveThread = null;
    }

    @Override
    public void onResume() {
        super.onResume();
//        getContentResolver().registerContentObserver(SCREENSHOT_URI, false, screenShotObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
//        getContentResolver().unregisterContentObserver(screenShotObserver);
    }

    public void solveWord(View v) {
        if (screenShotObserver == null) {
            Log.i("KILLED SC OBSERVER!!!", "###################################");
        }

        if (solveThread == null || !solveThread.isAlive() && boardToSolve != null) {
            final Button button = findViewById(R.id.find_word);
            final EditText letterEditText = findViewById(R.id.available_letters);
            final String letters = letterEditText.getText().toString().toLowerCase();
            final MainActivity thisActivity = this;

            button.setEnabled(false);

            displayWord("Solving");

            solveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final long startTime = System.currentTimeMillis();

                    Board.ProgressMonitor updater = (progress) -> {
                        thisActivity.displayProgress(progress);
                    };

                    Board.WordRank wr = new Board.WordRank(5);

                    final Word[] bestWords = Board.solveThreaded(boardToSolve, letters, updater, wr);
                    final long endTime = System.currentTimeMillis();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Word bestWord = bestWords[0];
                            displayWord(bestWord.word() + ", " + bestWord.score() + ", t=" + (endTime-startTime));
                            displayBoard(boardToSolve, bestWord);
                            button.setEnabled(true);
                        }
                    });
                }
            });

            solveThread.start();
        }
    }

    public void displayBoard(final Board board, final Word highlight) {
        String boardHR = board.readableString();

        for (Letter l: highlight.allLetters()) {
            int letterIndex = 32*l.getY() + 2*l.getX() + 1;
            boardHR = boardHR.substring(0, letterIndex) + l.getLetter() + boardHR.substring(letterIndex+1);
        }

        Spannable span = new SpannableString(boardHR);

        for (Letter l: highlight.allLetters()) {
            int letterIndex = 32*l.getY() + 2*l.getX() + 1;
            span.setSpan(new ForegroundColorSpan(Color.CYAN), letterIndex, letterIndex+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = findViewById(R.id.board_display);
                tv.setText(span);
            }
        });
    }

    public void displayWord(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = findViewById(R.id.word_display);
                tv.setText(text);
            }
        });
    }

    public void displayProgress (final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar pb = findViewById(R.id.progress_bar);
                pb.setProgress(progress);
            }
        });
    }

    // Image choosing
    public void parseUri(Uri uri) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.word_display);
                textView.setText("Parsing board");
            }
        });

        new BoardImageParser(this, this.getContentResolver()).parse(uri);
    }

    public void passParsedBoard(final Letter[][] letters) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardToSolve = new Board(15, 15, letters);
                final TextView tv = (TextView) findViewById(R.id.board_display);
                tv.setText(boardToSolve.readableString());
            }
        });
    }

    public void passLetters(final String availableLetters) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final EditText editText = (EditText) findViewById(R.id.available_letters);
                editText.setText(availableLetters);
            }
        });
    }

    public void chooseImage(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i("Parsed URI", uri.toString());
                parseUri(uri);
            }
        }
    }
}

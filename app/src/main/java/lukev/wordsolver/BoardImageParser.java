package lukev.wordsolver;

import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;

import solver.Board;
import solver.Letter;

public class BoardImageParser {
    private static final float LETTER_DISTANCE = 71.2f;
    private static final float LETTER_WIDTH = 69;
    private static final float INITIAL_X = 7;
    private static final float INITIAL_Y = 746; // 445
    private static final float SCREEN_WIDTH = 1080;
    private static final float SCREEN_HEIGHT = 2340;
    private static final float AVAILABLE_LETTER_Y = 1978;//1591;
    private static final float AVAILABLE_LETTER_X = 2;
    private static final float AVAILABLE_LETTER_WIDTH = 148;
    private static final float AVAILABLE_LETTER_DISTANCE = 154;

    private static final String CHAR_COMPARE_PRIORITY = "berpfqdgoctajkmnhsulvwzxyi";

    private static Bitmap[] letters;

    private final ContentResolver cr;
    private final MainActivity mainActivity;

    public BoardImageParser(MainActivity activity, ContentResolver cr) {
        this.cr = cr;
        this.mainActivity = activity;

        if (letters == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AssetManager assetManager = mainActivity.getAssets();

                    letters = new Bitmap[26];

                    try {
                        for (int i = 0; i < 26; i++) {
                            letters[i] = BitmapFactory.decodeStream(assetManager.open("letters/id_" + (char) ('a' + i) + ".png"));
                        }
                    } catch (IOException e) {
                        Log.i("WWF Solver", "Unable to open image masks");
                    }
                }
            }).start();

        }
    }

    private int screenIndex(int x, int y) {
        return (int)(SCREEN_WIDTH)*y + x;
    }

    private int maskIndex(int x, int y) {
        return (int)(LETTER_WIDTH)*y + x;
    }

    public void parse(final Uri uri) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Letter[][] boardGrid = new Letter[15][15];
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(cr, uri);

                    int[] allPixels = new int[(int)(SCREEN_WIDTH*SCREEN_HEIGHT)];
                    bitmap.getPixels(allPixels, 0, (int)SCREEN_WIDTH, 0, 0, (int)SCREEN_WIDTH, (int)SCREEN_HEIGHT);

                    int[] maskPixels = new int[(int)(LETTER_WIDTH*LETTER_WIDTH)];

                    float x = INITIAL_X;
                    float y = INITIAL_Y;

                    for (int letterY = 0; letterY < 15; letterY++) {
                        for (int letterX = 0; letterX < 15; letterX++) {

                            int xpos = (int) (x );
                            int ypos = (int) (y );

                            letterLoop: for(int charPriority = 0; charPriority < CHAR_COMPARE_PRIORITY.length(); charPriority++) {
                                char comp = CHAR_COMPARE_PRIORITY.charAt(charPriority);
                                int letter = (int) (comp - 'a');
                                letters[letter].getPixels(maskPixels, 0, (int)LETTER_WIDTH, 0, 0, (int)LETTER_WIDTH, (int)LETTER_WIDTH);

                                boolean matched = true;

                                pixelLoop: for (int i = 20; i < (int) LETTER_WIDTH - 10; i++) {
                                    for (int j = 15; j < (int) LETTER_WIDTH - 15; j++) {
                                        int pxColor = allPixels[screenIndex(xpos + j, ypos + i)];
                                        int maskPixelColor = maskPixels[maskIndex(j, i)];

                                        // If (blue pixel in letter mask) and not(a brown pixel or a white pixel as would be in the most recently played word)
                                        if (Color.blue(maskPixelColor) > 245 && !pixelIsLetterMaterial(pxColor)) {
                                            matched = false;
                                            break pixelLoop;
                                        }
                                    }
                                }

                                // Determine if the letter has the default point value, or not score if it is a wildcard tile

                                if (matched) {
//                                    boardGrid[letterY][letterX] = defaultScore ? new Letter(letterX, letterY, (char) ('a' + letter)) : new Letter(letterX, letterY, (char) ('a' + letter), 0);
                                    boardGrid[letterX][letterY] = new Letter(letterX, letterY, (char) ('a' + letter));
                                    break letterLoop;
                                }
                            }

                            boolean defaultScore = false;

                            scoreSearchLoop: for (int i = 10; i <= 15; i++) {
                                for (int j = 55; j < 60; j++) {
                                    if (isScoreDigitColor(allPixels[screenIndex(xpos+j, ypos+i)])) {
                                        defaultScore = true;
//                                            Log.i("Board Parse", "NOTICE ################# NOTICE #########NOTICE##### NOTICE ##### NOTICE Found defaultscore letter at (" + letterX + ", " + letterY + ")");
                                        break scoreSearchLoop;
                                    }
                                }
                            }
//
                            if (!defaultScore) {
                                Log.i("Board Parse", "NOTICE ################# NOTICE #########NOTICE##### NOTICE ##### NOTICE Found wildcard letter at (" + letterX + ", " + letterY + ")");
                            }

                            x += LETTER_DISTANCE;

                            int progress = (int) (100.0 * (letterY*15 + letterX)/(15.0*15.0));
                            mainActivity.displayProgress(progress);
                        }
                        x = INITIAL_X;
                        y += LETTER_DISTANCE;
                    }

                    String availableLetters = "";

                    x = AVAILABLE_LETTER_X;
                    y = AVAILABLE_LETTER_Y;
                    for (int availableLetter = 0; availableLetter < 7; availableLetter++) {
                        letterLoop: for(int charPriority = 0; charPriority < CHAR_COMPARE_PRIORITY.length(); charPriority++) {
                            char comp = CHAR_COMPARE_PRIORITY.charAt(charPriority);
                            int letter = (int) (comp - 'a');
                            letters[letter].getPixels(maskPixels, 0, (int)LETTER_WIDTH, 0, 0, (int)LETTER_WIDTH, (int)LETTER_WIDTH);

                            boolean matched = true;

                            pixelLoop: for (int i = 0; i < (int) LETTER_WIDTH; i++) {
                                for (int j = 0; j < (int) LETTER_WIDTH; j++) {
//                                    int pxColor = allPixels[screenIndex((int) (x + j*AVAILABLE_LETTER_WIDTH/LETTER_WIDTH + 0.5), (int)(y + i*AVAILABLE_LETTER_WIDTH/LETTER_WIDTH + 0.5))];
                                    int maskPixelColor = maskPixels[maskIndex(j, i)];

                                    if (Color.blue(maskPixelColor) > 245) {
                                        int xAdjustedIndex = (int) (x + j*AVAILABLE_LETTER_WIDTH/LETTER_WIDTH + 0.5);
                                        int yAdjustedIndex = (int) (y + i*AVAILABLE_LETTER_WIDTH/LETTER_WIDTH + 0.5);

                                        boolean dotMatch = false;

                                        int acceptableRange = 2;

                                        shiftLoop: for (int horizontalRangeI = -acceptableRange; horizontalRangeI <= acceptableRange; horizontalRangeI++) {
                                            for (int verticalRangeI = -acceptableRange; verticalRangeI <= acceptableRange; verticalRangeI ++) {
                                                int pxColor = allPixels[screenIndex(xAdjustedIndex + horizontalRangeI, yAdjustedIndex + horizontalRangeI)];

                                                if (pixelIsLetterMaterial(pxColor)) {
                                                    dotMatch = true;
                                                    break shiftLoop;
                                                }
                                            }
                                        }

                                        if (!dotMatch) {
                                            matched = false;
                                            break pixelLoop;
                                        }
                                    }
                                }
                            }

                            if (matched) {
                                availableLetters += comp;
                                break letterLoop;
                            }
                        }

                        x += AVAILABLE_LETTER_DISTANCE;
                    }

                    mainActivity.passLetters(availableLetters);
                    mainActivity.passParsedBoard(boardGrid);
                    mainActivity.displayProgress(100);
                    mainActivity.displayWord("Board Parsed");
                    Log.i("Board read:", new Board(15, 15, boardGrid).toString());
                } catch (IOException e) {
                    Log.i("WWF Solver", "Unable to open image bitmap\n" + Log.getStackTraceString(e));
                } catch (Exception e) {
                    Log.i("WordSolver", Log.getStackTraceString(e));
                }
            }
        }).start();
    }

    // If a ~brown pixel a white pixel (as would be in the most recently played word)
    private boolean pixelIsLetterMaterial (int pixel) {
        return Color.red(pixel) > 245 && Color.blue(pixel) > 245 || brownColorApproximateMatch(pixel);// || Color.red(pixel) == 69 && Color.green(pixel) == 31 && Color.blue(pixel) == 18;
    }

    private boolean brownColorApproximateMatch(int pixel) {
        return (Color.red(pixel) > 60 && Color.red(pixel) < 80 && Color.green(pixel) > 20 && Color.green(pixel) < 40 && Color.blue(pixel) >= 0 && Color.blue(pixel) < 25);
    }

    private boolean isScoreDigitColor(int color) {
        return Color.red(color) > 50 && Color.red(color) < 142 && Color.green(color) > 20 && Color.green(color) < 90 && Color.blue(color) >= 0 && Color.blue(color) < 45;
    }
}

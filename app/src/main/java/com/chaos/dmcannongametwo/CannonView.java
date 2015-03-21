package com.chaos.dmcannongametwo;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CannonView extends SurfaceView
        implements SurfaceHolder.Callback
{
    private CannonThread cannonThread; // controls the game loop
    private Activity activity; // to display Game Over dialog in GUI thread
    private boolean dialogIsDisplayed = false;

    // constants for game play
    public static final int TARGET_PIECES = 10; // sections in the target
    public static final int TARGET_PIECES2=10;
    public static final int TARGET_PIECES3=10;
    public static final int MISS_PENALTY = 2;
    public static final int HIT_REWARD = 0; // seconds added on a hit
    public static final int HIT_REWARD2 = 1;//seconds added for hitting a yellow brick
    public static final int HIT_REWARD3=1;//seconds added for hitting a red brick
    public float currentScore;//the current score

    // variables for the game loop and tracking statistics
    private boolean gameOver; // is the game over?
    private double timeLeft; // the amount of time left in seconds
    private int shotsFired; // the number of shots the user has fired
    private double totalElapsedTime; // the number of seconds elapsed

    // variables for the blocker and target
    private Line blocker; // start and end points of the blocker
    private int blockerDistance; // blocker distance from left
    private int blockerBeginning; // blocker distance from top
    private int blockerEnd; // blocker bottom edge distance from top
    private int initialBlockerVelocity; // initial blocker speed multiplier
    private float blockerVelocity; // blocker speed multiplier during game

    private Line target; // start and end points of the target
    private Line target2;//yellow
    private Line target3;//red
    private int targetDistance; // target distance from left
    private int targetDistance2;//yellow
    private int targetDistance3;//red
    private int targetBeginning; // target distance from top
    private double pieceLength; // length of a target piece
    private double pieceLength2;//yellow
    private double pieceLength3;//red
    private int targetEnd; // target bottom's distance from top
    private int lineWidth; // width of the target and blocker
    private boolean[] hitStates; // is each target piece hit?
    private boolean[] hitStates2;
    private int[] hitCount2;//count that tells how many times you shot a yellow brick
    private boolean[] hitStates3;
    private int[] hitCount3;//count that tells how many times you shot a red brick


    // variables for the cannon and cannonball
    private Point cannonball; // cannonball image's upper-left corner
    private int cannonballVelocityX; // cannonball's x velocity
    private int cannonballVelocityY; // cannonball's y velocity
    private boolean cannonballOnScreen; // is the cannonball on the screen
    private int cannonballRadius; // cannonball radius
    private int cannonballSpeed; // cannonball speed
    private int cannonBaseRadius; // cannon base radius
    private int cannonLength; // cannon barrel length
    private Point barrelEnd; // the endpoint of the cannon's barrel
    private int screenWidth; // width of the screen
    private int screenHeight; // height of the screen

    // constants and variables for managing sounds
    private static final int TARGET_SOUND_ID = 0;
    private static final int CANNON_SOUND_ID = 1;
    private static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // plays sound effects
    private Map<Integer, Integer> soundMap; // maps IDs to SoundPool

    // Paint variables used when drawing each item on the screen
    private Paint textPaint; // Paint used to draw text
    private Paint cannonballPaint; // Paint used to draw the cannonball
    private Paint cannonPaint; // Paint used to draw the cannon
    private Paint blockerPaint; // Paint used to draw the blocker
    private Paint targetPaint; // Paint used to draw the target
    private Paint targetPaint2;//yellow
    private Paint targetPaint3;//red
    private Paint backgroundPaint; // Paint used to clear the drawing area

    // public constructor
    public CannonView(Context context, AttributeSet attrs)
    {
        super(context, attrs); // call super's constructor
        activity = (Activity) context;
        // register SurfaceHolder.Callback listener
        getHolder().addCallback(this);
        // initialize Lines and points representing game items
        blocker = new Line(); // create the blocker as a Line
        target = new Line(); // create the target as a Line
        target2 = new Line();//yellow
        target3=new Line();//red
        cannonball = new Point(); // create the cannonball as a point

        // initialize hitStates as a boolean array
        hitStates = new boolean[TARGET_PIECES];
        hitStates2= new boolean[TARGET_PIECES2];
        hitCount2= new int[TARGET_PIECES2];//initialize how many hits a yellow brick has
        hitStates3=new boolean[TARGET_PIECES3];
        hitCount3= new int[TARGET_PIECES2];//initialize how many hits a red brick has

        // initialize SoundPool to play the app's three sound effects
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        // create Map of sounds and pre-load sounds
        soundMap = new HashMap<Integer, Integer>(); // create new HashMap
        soundMap.put(TARGET_SOUND_ID,
                soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID,
                soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID,
                soundPool.load(context, R.raw.blocker_hit, 1));

        // construct Paints for drawing text, cannonball, cannon,
        // blocker and target; these are configured in method onSizeChanged
        textPaint = new Paint(); // Paint for drawing text
        cannonPaint = new Paint(); // Paint for drawing the cannon
        cannonballPaint = new Paint(); // Paint for drawing a cannonball
        blockerPaint = new Paint(); // Paint for drawing the blocker
        targetPaint = new Paint(); // Paint for drawing the target
        targetPaint2 = new Paint();//yellow
        targetPaint3=new Paint();//red
        backgroundPaint = new Paint(); // Paint for drawing the target
    } // end CannonView constructor

    // called by surfaceChanged when the size of the SurfaceView changes,
    // such as when it's first added to the View hierarchy
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w; // store the width
        screenHeight = h; // store the height
        cannonBaseRadius = h / 18; // cannon base radius 1/18 screen height
        cannonLength = w / 8; // cannon length 1/8 screen width

        cannonballRadius = w / 36; // cannonball radius 1/36 screen width
        cannonballSpeed = w * 3 / 2; // cannonball speed multiplier

        lineWidth = w / 16; // target and blocker 1/24 screen width

        // configure instance variables related to the blocker
        blockerDistance = w * 7 / 8; // blocker 5/8 screen width from left
        blockerBeginning = h / 8; // distance from top 1/8 screen height
        blockerEnd = h * 2 / 8; // distance from top 3/8 screen height
        initialBlockerVelocity = h / 2; // initial blocker speed multiplier
        blocker.start = new Point(blockerDistance, blockerBeginning);
        blocker.end = new Point(blockerDistance, blockerEnd);

        // configure instance variables related to the target
        targetDistance = w * 4 / 8; // target 7/8 screen width from left
        targetDistance2 = w * 5 / 8; // yellow distance
        targetDistance3= w * 6 / 8;//red distance
        targetBeginning = 0; // distance from top 1/8 screen height
        targetEnd = h; // distance from top 7/8 screen height
        pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
        pieceLength2 = (targetEnd - targetBeginning) / TARGET_PIECES2;
        pieceLength3 = (targetEnd - targetBeginning) / TARGET_PIECES3;
        target.start = new Point(targetDistance, targetBeginning);
        target2.start = new Point(targetDistance2, targetBeginning);
        target.end = new Point(targetDistance, targetEnd);
        target2.end=new Point(targetDistance2, targetEnd);
        target3.start = new Point(targetDistance3, targetBeginning);
        target3.end=new Point(targetDistance3, targetEnd);

        // endpoint of the cannon's barrel initially points horizontally
        barrelEnd = new Point(cannonLength, h / 2);

        // configure Paint objects for drawing game elements
        textPaint.setTextSize(w / 20); // text size 1/20 of screen width
        textPaint.setAntiAlias(true); // smoothes the text
        cannonPaint.setStrokeWidth(lineWidth * 1.5f); // set line thickness
        blockerPaint.setStrokeWidth(lineWidth); // set line thickness
        blockerPaint.setColor(255);//set the color of the blocker to white to mask overlap from nick cage
        targetPaint.setStrokeWidth(lineWidth); // set line thickness
        targetPaint2.setStrokeWidth(lineWidth);
        targetPaint3.setStrokeWidth(lineWidth);
        backgroundPaint.setColor(Color.WHITE); // set background color

        newGame(); // set up and start a new game
    } // end method onSizeChanged

    // reset all the screen elements and start a new game
    public void newGame()
    {
        // set every element of hitStates to false--restores target pieces
        for (int i = 0; i < TARGET_PIECES; ++i)
            hitStates[i] = false;
        for (int i=0; i<TARGET_PIECES2; i++)
            hitStates2[i]=false;
        for (int i=0; i<TARGET_PIECES3; i++)
            hitStates3[i]=false;
        for (int i=0; i<TARGET_PIECES2; i++)
            hitCount2[i]=0;
        for (int i=0; i<TARGET_PIECES3; i++)
            hitCount3[i]=0;
        currentScore=0;//initialize score
        blockerVelocity = initialBlockerVelocity; // set initial velocity
        timeLeft = 10; // start the countdown at 10 seconds
        cannonballOnScreen = false; // the cannonball is not on the screen
        shotsFired = 0; // set the initial number of shots fired
        totalElapsedTime = 0.0; // set the time elapsed to zero
        blocker.start.set(blockerDistance, blockerBeginning);
        blocker.end.set(blockerDistance, blockerEnd);
        target.start.set(targetDistance, targetBeginning);
        target2.start.set(targetDistance2, targetBeginning);
        target.end.set(targetDistance, targetEnd);
        target2.end.set(targetDistance2, targetEnd);
        target3.start.set(targetDistance3, targetBeginning);
        target3.end.set(targetDistance3, targetEnd);
        if (gameOver)//end the game if you shoot nick cage lol
        {
            gameOver = false; // the game is not over
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        } // end if
    } // end method newGame

    // called repeatedly by the CannonThread to update game elements
    private void updatePositions(double elapsedTimeMS)
    {
        double interval = elapsedTimeMS / 1000.0; // convert to seconds

        if (cannonballOnScreen) // if there is currently a shot fired
        {
            // update cannonball position
            cannonball.x += interval * cannonballVelocityX;
            cannonball.y += interval * cannonballVelocityY;

            // check for collision with blocker
            if (cannonball.x + cannonballRadius > blockerDistance &&
                    cannonball.x - cannonballRadius < blockerDistance &&
                    cannonball.y + cannonballRadius > blocker.start.y &&
                    cannonball.y - cannonballRadius < blocker.end.y)
            {
                cannonballVelocityX *= -1; // reverse cannonball's direction
                timeLeft -= MISS_PENALTY; // penalize the user

                // play blocker sound
                soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
                cannonThread.setRunning(false);
                showGameOverDialog(R.string.win); // show winning dialog
                gameOver = true; // the game is over
            } // end if

            // check for collisions with left and right walls
            else if (cannonball.x + cannonballRadius > screenWidth ||
                    cannonball.x - cannonballRadius < 0)
                cannonballOnScreen = false; // remove cannonball from screen

                // check for collisions with top and bottom walls
            else if (cannonball.y + cannonballRadius > screenHeight ||
                    cannonball.y - cannonballRadius < 0)
                cannonballOnScreen = false; // make the cannonball disappear

                // check for cannonball collision with target
            else if (cannonball.x + cannonballRadius > targetDistance &&
                    cannonball.x - cannonballRadius < targetDistance &&
                    cannonball.y + cannonballRadius > target.start.y &&
                    cannonball.y - cannonballRadius < target.end.y) {
                // determine target section number (0 is the top)
                int section =
                        (int) ((cannonball.y - target.start.y) / pieceLength);

                // check if the piece hasn't been hit yet
                if ((section >= 0 && section < TARGET_PIECES) &&
                        !hitStates[section]) {
                    hitStates[section] = true; // section was hit
                    cannonballOnScreen = false; // remove cannonball
                    timeLeft += HIT_REWARD; // add reward to remaining time

                    // play target hit sound
                    soundPool.play(soundMap.get(TARGET_SOUND_ID), 1,
                            1, 1, 0, 1f);


                } // end if
            }
                else if (cannonball.x + cannonballRadius > targetDistance2 &&
                        cannonball.x - cannonballRadius < targetDistance2 &&
                        cannonball.y + cannonballRadius > target2.start.y &&
                        cannonball.y - cannonballRadius < target2.end.y)
            {
                // determine target section number (0 is the top)
                int section2 =
                        (int) ((cannonball.y - target2.start.y) / pieceLength2);

                // check if the piece hasn't been hit yet and how many hits
                if ((section2 >= 0 && section2 < TARGET_PIECES2) &&
                        hitCount2[section2]<=2) {
                    hitCount2[section2]++;
                    cannonballOnScreen = false; // remove cannonball
                    timeLeft += HIT_REWARD2; // add reward to remaining time
                    currentScore=currentScore+1;
                    // play target hit sound
                    soundPool.play(soundMap.get(TARGET_SOUND_ID), 1,
                            1, 1, 0, 1f);


                } // end if
            } // end else if
            else if (cannonball.x + cannonballRadius > targetDistance3 &&
                    cannonball.x - cannonballRadius < targetDistance3 &&
                    cannonball.y + cannonballRadius > target3.start.y &&
                    cannonball.y - cannonballRadius < target3.end.y) {
                // determine target section number (0 is the top)
                int section3 =
                        (int) ((cannonball.y - target3.start.y) / pieceLength3);

                // check if the piece hasn't been hit yet and how many hits
                if ((section3 >= 0 && section3 < TARGET_PIECES3) &&
                        hitCount3[section3]<=3) {
                    hitCount3[section3]++;

                    cannonballOnScreen = false; // remove cannonball
                    timeLeft += HIT_REWARD3; // add reward to remaining time
                    currentScore=currentScore+1;
                    // play target hit sound
                    soundPool.play(soundMap.get(TARGET_SOUND_ID), 1,
                            1, 1, 0, 1f);


                } // end if
            }
        } // end if

        // update the blocker's position
        double blockerUpdate = interval * blockerVelocity;
        blocker.start.y += blockerUpdate;
        blocker.end.y += blockerUpdate;

        // if the blocker hit the top or bottom, reverse direction
        if (blocker.start.y < 0 || blocker.end.y > screenHeight)
            blockerVelocity *= -1;
        if (blocker.start.y < 0 || blocker.end.y > screenHeight)
            soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);

        timeLeft -= interval; // subtract from time left

        // if the timer reached zero
        if (timeLeft <= 0.0)
        {
            timeLeft = 0.0;
            gameOver = true; // the game is over
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.lose); // show the losing dialog
        } // end if
    } // end method updatePositions

    // fires a cannonball
    public void fireCannonball(MotionEvent event)
    {
        if (cannonballOnScreen) // if a cannonball is already on the screen
            return; // do nothing

        double angle = alignCannon(event); // get the cannon barrel's angle

        // move the cannonball to be inside the cannon
        cannonball.x = cannonballRadius; // align x-coordinate with cannon
        cannonball.y = screenHeight / 2; // centers ball vertically

        // get the x component of the total velocity
        cannonballVelocityX = (int) (cannonballSpeed * Math.sin(angle));

        // get the y component of the total velocity
        cannonballVelocityY = (int) (-cannonballSpeed * Math.cos(angle));
        cannonballOnScreen = true; // the cannonball is on the screen
        ++shotsFired; // increment shotsFired

        // play cannon fired sound
        soundPool.play(soundMap.get(CANNON_SOUND_ID), 1, 1, 1, 0, 1f);
    } // end method fireCannonball

    // aligns the cannon in response to a user touch
    public double alignCannon(MotionEvent event)
    {
        // get the location of the touch in this view
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());

        // compute the touch's distance from center of the screen
        // on the y-axis
        double centerMinusY = (screenHeight / 2 - touchPoint.y);

        double angle = 0; // initialize angle to 0

        // calculate the angle the barrel makes with the horizontal
        if (centerMinusY != 0) // prevent division by 0
            angle = Math.atan((double) touchPoint.x / centerMinusY);

        // if the touch is on the lower half of the screen
        if (touchPoint.y > screenHeight / 2)
            angle += Math.PI; // adjust the angle

        // calculate the endpoint of the cannon barrel
        barrelEnd.x = (int) (cannonLength * Math.sin(angle));
        barrelEnd.y =
                (int) (-cannonLength * Math.cos(angle) + screenHeight / 2);

        return angle; // return the computed angle
    } // end method alignCannon

    // draws the game to the given Canvas
    public void drawGameElements(Canvas canvas)
    {
        // clear the background
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
                backgroundPaint);

        // display time remaining
        canvas.drawText(getResources().getString(
                R.string.time_remaining_format, timeLeft), 30, 50, textPaint);

        // if a cannonball is currently on the screen, draw it
        if (cannonballOnScreen)
            canvas.drawCircle(cannonball.x, cannonball.y, cannonballRadius,
                    cannonballPaint);

        // draw the cannon barrel
        canvas.drawLine(0, screenHeight / 2, barrelEnd.x, barrelEnd.y,
                cannonPaint);

        // draw the cannon base
        canvas.drawCircle(0, (int) screenHeight / 2,
                (int) cannonBaseRadius, cannonPaint);

        // draw the blocker
        canvas.drawLine(blocker.start.x, blocker.start.y, blocker.end.x,
                blocker.end.y, blockerPaint);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap foreground = BitmapFactory.decodeResource(getResources(), R.drawable.scalednickcage, options);
        Paint paint2=new Paint();
        canvas.drawBitmap(foreground, blocker.start.x-120, blocker.start.y, paint2);
        Point currentPoint = new Point(); // start of current target section
        Point currentPoint2=new Point();
        Point currentPoint3=new Point();
        // initialize curPoint to the starting point of the target
        currentPoint.x = target.start.x;
        currentPoint.y = target.start.y;
        currentPoint2.x=target2.start.x;
        currentPoint2.y=target2.start.y;
        currentPoint3.x=target3.start.x;
        currentPoint3.y=target3.start.y;

        // draw the target
        for (int i = 1; i <= TARGET_PIECES; ++i)
        {
            // if this target piece is not hit, draw it
            if (!hitStates[i - 1])
            {
                    targetPaint.setColor(Color.BLUE);

                canvas.drawLine(currentPoint.x, currentPoint.y, target.end.x,
                        (int) (currentPoint.y + pieceLength), targetPaint);
            } // end if

            // move curPoint to the start of the next piece
            currentPoint.y += pieceLength;
        } // end for

        for (int i = 1; i <= TARGET_PIECES2; ++i)
        {
            // if this target piece is not hit, draw it and the hits are less than three
            if (hitCount2[i-1]<=2)
            {
                    targetPaint2.setColor(Color.YELLOW);

                canvas.drawLine(currentPoint2.x, currentPoint2.y, target2.end.x,
                        (int) (currentPoint2.y + pieceLength2), targetPaint2);
            } // end if

            // move curPoint to the start of the next piece
            currentPoint2.y += pieceLength2;
        } // end for
        for (int i = 1; i <= TARGET_PIECES3; ++i)
        {
            // if this target piece is not hit, draw it and the hits are less than four
            if (hitCount3[i-1]<=3)
            {
                    targetPaint3.setColor(Color.RED);

                canvas.drawLine(currentPoint3.x, currentPoint3.y, target3.end.x,
                        (int) (currentPoint3.y + pieceLength3), targetPaint3);
            } // end if

            // move curPoint to the start of the next piece
            currentPoint3.y += pieceLength3;
        } // end for
    } // end method drawGameElements

    // display an AlertDialog when the game ends
    private void showGameOverDialog(int messageId)
    {
        Highscore highscore=new Highscore(activity);//get the highscore class and set it to the context of the activiti, IMPORTANT! this has to be set to the activities context

        currentScore= ((float) (currentScore/totalElapsedTime))*100;//calculate score, reduce for time and increase for hit bonuses
        if((highscore.inHighScore(currentScore))){//check if the score is in highscores
            highscore.addScore(currentScore);//if not, add it! there are three, but this game only cares about the top score
        }
        // create a dialog displaying the given String
        final AlertDialog.Builder dialogBuilder =
                new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle(getResources().getString(messageId));
        dialogBuilder.setCancelable(false);

        // display number of shots fired and total time elapsed
        dialogBuilder.setMessage(String.format("%s %d, %s %.2f, %s %.02f",//format dialog
                getResources().getString(R.string.shotsfired), shotsFired,
                getResources().getString(R.string.totalelapsedtime),
                totalElapsedTime,
                getResources().getString(R.string.highscore1),highscore.getScore(0)));
        dialogBuilder.setPositiveButton(R.string.reset_game,
                new DialogInterface.OnClickListener()
                {
                    // called when "Reset Game" Button is pressed
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialogIsDisplayed = false;
                        newGame(); // set up and start a new game
                    } // end method onClick
                } // end anonymous inner class
        ); // end call to setPositiveButton

        activity.runOnUiThread(
                new Runnable() {
                    public void run()
                    {
                        dialogIsDisplayed = true;
                        dialogBuilder.show(); // display the dialog
                    } // end method run
                } // end Runnable
        ); // end call to runOnUiThread
    } // end method showGameOverDialog

    // stops the game
    public void stopGame()
    {
        if (cannonThread != null)
            cannonThread.setRunning(false);
    } // end method stopGame

    // releases resources; called by CannonGame's onDestroy method
    public void releaseResources()
    {
        soundPool.release(); // release all resources used by the SoundPool
        soundPool = null;
    } // end method releaseResources

    // called when surface changes size
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format,
                               int width, int height)
    {
    } // end method surfaceChanged

    // called when surface is first created
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (!dialogIsDisplayed)
        {
            cannonThread = new CannonThread(holder);
            cannonThread.setRunning(true);
            cannonThread.start(); // start the game loop thread
        } // end if
    } // end method surfaceCreated

    // called when the surface is destroyed
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // ensure that thread terminates properly
        boolean retry = true;
        cannonThread.setRunning(false);

        while (retry)
        {
            try
            {
                cannonThread.join();
                retry = false;
            } // end try
            catch (InterruptedException e)
            {
            } // end catch
        } // end while
    } // end method surfaceDestroyed

    // Thread subclass to control the game loop
    private class CannonThread extends Thread
    {
        private SurfaceHolder surfaceHolder; // for manipulating canvas
        private boolean threadIsRunning = true; // running by default

        // initializes the surface holder
        public CannonThread(SurfaceHolder holder)
        {
            surfaceHolder = holder;
            setName("CannonThread");
        } // end constructor

        // changes running state
        public void setRunning(boolean running)
        {
            threadIsRunning = running;
        } // end method setRunning

        // controls the game loop
        @Override
        public void run()
        {
            Canvas canvas = null; // used for drawing
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning)
            {
                try
                {
                    canvas = surfaceHolder.lockCanvas(null);

                    // lock the surfaceHolder for drawing
                    synchronized(surfaceHolder)
                    {
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS / 1000.00;
                        updatePositions(elapsedTimeMS); // update game state
                        drawGameElements(canvas); // draw
                        previousFrameTime = currentTime; // update previous time
                    } // end synchronized block
                } // end try
                finally
                {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                } // end finally
            } // end while
        } // end method run
    } // end nested class CannonThread
} // end class CannonView


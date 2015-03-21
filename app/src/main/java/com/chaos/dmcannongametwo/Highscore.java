package com.chaos.dmcannongametwo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by daniel on 3/20/15.
 */
//created a custom class that uses shared preferences to save high scores. for more than simple things like highscores use sqllite.
public class Highscore {//instantiate
    private SharedPreferences preferences;//create new shared preferences file preferences
    private float score[];//create a new float array of your high scores

    public Highscore(Context context)//IMPORTANT! grab the context from your onCreate
    {
        preferences=context.getSharedPreferences("Highscore", 0);//initialize shared preferences
        score = new float[3];//set array bounds
        for (int x=0; x<3; x++){//populate shared preferences
            score[x]=preferences.getFloat("score"+x,0);
        }

    }
    public float getScore(int x)//get score method that returns the score at an array position x, remember we start from zero
    {
        return score[x];
    }
    public boolean inHighScore(float score)//check if the current score is within highscore and if it is greater than the previously saved scores
    {
        int position;
        for(position=0; position<3&&this.score[position]>score;
            position++);
        if(position==3) return false;
        return true;
    }
    public boolean addScore(float score)//add a score to the shared preferences
    {
        int position;
        for(position=0; position<2&&this.score[position]>score;
            position++);
        if(position==3) return false;
        for(int x=2; x>position; x--)
        {
            this.score[x]=this.score[x-1];
        }
        this.score[position]=score;
        SharedPreferences.Editor editor=preferences.edit();
        for(int x=0; x<3; x++)
        {
            editor.putFloat("score"+x, this.score[x]);
        }
        editor.commit();
        return true;
    }
}

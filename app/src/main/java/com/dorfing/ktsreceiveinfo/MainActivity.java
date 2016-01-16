package com.dorfing.ktsreceiveinfo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;

/*
    Code by Jordan Marx for KTS code assignment.

    This application automatically connects to a socket server when started.  Clicking
    the RECEIVE button will receive the name and date of birth strings from the server.
    The information will be displayed on the application and so will the calculated age
    based on the date of birth given.

 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Servers IP and port
    private static final String SERVER_IP = "10.0.0.4";
    private static final int SERVERPORT = 8880;

    // Stores the current date
    int currentYear;
    int currentMonth;
    int currentDay;

    // Stores users birth date variables
    int birthYear;
    int birthMonth;
    int birthDay;

    // Final calculated age of user
    int finalAge;

    // Stores users birth date into chars
    char[] stringToCharArray = null;

    // Text to show name, birth date, and final age of user
    TextView nameText;
    TextView birthDateText;
    TextView finalAgeText;

    // Socket for connecting to server
    Socket s = null;
    Boolean connectedToServer = false;

    // PrintWriter for sending msg
    PrintWriter out = null;

    // Buffered Reader for reading received msg
    BufferedReader in = null;

    // Strings to store name and birth date of user
    String name = null;
    String birthDate = null;

    // Async for reading msg from server
    ReadMsgFromServer readMsgFromServer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Acces the name TextView defined in content XML
        nameText = (TextView) findViewById(R.id.name_text);

        // Acces the birth date TextView defined in content XML
        birthDateText = (TextView) findViewById(R.id.birthdate_text);

        // Acces the final age TextView defined in content XML
        finalAgeText = (TextView) findViewById(R.id.finalage_text);

        // Access the receive button defined in content XML and set OnClick to this
        Button buttonReceive = (Button) findViewById(R.id.receive_button);
        buttonReceive.setOnClickListener(this);

        // Connect to the socket server
        Runnable connectSocket = new connectSocket();
        new Thread(connectSocket).start();

    }


    @Override
    public void onClick(View v) {
        /*
            RECEIVE button has been clicked!
         */


        // If connected to server then read msg
        if(connectedToServer)
        {
            readMsgFromServer = new ReadMsgFromServer();
            readMsgFromServer.execute();
        }
        else
        {
            // Displays pop-up msg
            Toast.makeText(this, "ERROR: Could not connect to server!", Toast.LENGTH_LONG).show();
        }

    }


    // ----------------------------------------------------------------------------//
    //////////////////////////////////// CLASSES ////////////////////////////////////


    // Connect to socket server
    class connectSocket implements Runnable {

        @Override
        public void run() {
            try {
                // Connect to the socket s
                s = new Socket(SERVER_IP, SERVERPORT);

                // You connected to server!
                connectedToServer = true;

                // Get the output stream from the socket s
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);

                // Get the input stream from the socket s
                in = new BufferedReader((new InputStreamReader((s.getInputStream()))));
            } catch (IOException e) {

                // You didn't connect to server :(
                connectedToServer = false;
                e.printStackTrace();
            }
        }
    }

    // Reads msg from server
    private class ReadMsgFromServer extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                /*
                    First string sent by other application(KTS Task Send Info) is
                    name and second is birth date.
                 */
                name = in.readLine();
                birthDate = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        // This function is executed when doInBackground has ended.
        // Change all editText and TextViews here.
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // Set the name TextView to the users name
            nameText.setText("NAME: " + name);

            // Set the birth date TextView to the users birth date
            birthDateText.setText("BIRTHDATE: " + birthDate);

            // Breaks up birthDate string into integer variables of day, month, and year
            getUserBirthDateVariables();

            // Calculates the age of the user
            calculateAge();

        }
    }


    //-------------------------------------------------------------------------------//
    ///////////////////////////////////// METHODS /////////////////////////////////////


    /*
        Calculates age of user by subtracting current year and users birth year.
        It then minuses 1 if their birth day has not yet occurred during the current year.
     */
    public void calculateAge() {

        // Get the current date
        Calendar currentDate = Calendar.getInstance();
        currentYear = currentDate.get(Calendar.YEAR);
        currentMonth = currentDate.get(Calendar.MONTH) + 1;
        currentDay = currentDate.get(Calendar.DAY_OF_MONTH);

        // Calculate final age only using years(assumes birth date has occurred)
        finalAge = currentYear - birthYear;

        // If your birth month hasn't occurred yet and you are not 0 years old,
        // then minus 1 from finalAge
        if((birthMonth > currentMonth) && (finalAge != 0))
        {
            // Birth date has not occurred so minus 1
            finalAge = finalAge - 1;
        }
        // Else if birthday is current month but more than current day,
        // and you are not 0 years old... then minus 1 from finalAge.
        else if((birthMonth == currentMonth) && (birthDay > currentDay) && (finalAge != 0))
        {
            // Birth date has not occurred so minus 1
            finalAge = finalAge - 1;
        }

        // If it's users birthday then wish them happy birthday!
        if(birthDay == currentDay && birthMonth == currentMonth)
        {
            // Displays pop-up msg
            Toast.makeText(this, "Happy Birthday!", Toast.LENGTH_LONG).show();
        }

        // Set final age to the finalAge TextView
        finalAgeText.setText("AGE: " + Integer.toString(finalAge));

    }


    /*
        Breaks down the birthDate string into character array.
        birthdate is sent in format of mm-dd-yyyy from the server.
        Therefore first characters are month, then when there is a '-' start storing
        the day... then when there is a '-' again start storing the year
    */
    public void getUserBirthDateVariables() {

        // Stores birthDate string into char array
        stringToCharArray = birthDate.toCharArray();

        // Index used for when to change month/day/year string
        int dateIndex = 0;

        // Strings used to store the date
        String stringBirthDay = "";
        String stringBirthMonth = "";
        String stringBirthYear = "";


        // Users birth date is stored in the form of mm-dd-yyyy
        for(int i = 0; i < stringToCharArray.length; i++)
        {
            // If you read a '-' then change the dateIndex
            if(stringToCharArray[i] == '-')
            {
                dateIndex++;
            }
            else {

                // Store month
                if (dateIndex == 0)
                {
                    stringBirthMonth = stringBirthMonth + stringToCharArray[i];
                }
                // Store day
                else if (dateIndex == 1)
                {
                    stringBirthDay = stringBirthDay + stringToCharArray[i];
                }
                // Store year
                else if (dateIndex == 2)
                {
                    stringBirthYear = stringBirthYear + stringToCharArray[i];
                }

            }
        }

        // Change the strings into integers and store them into these variables
        birthDay = Integer.parseInt(stringBirthDay);
        birthMonth = Integer.parseInt(stringBirthMonth);
        birthYear = Integer.parseInt(stringBirthYear);

    }


}

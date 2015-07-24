package com.example.hp.helloworld;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    private EditText mEdtSex, mEdtAge;
    private Button mBtnOK;
    private TextView mTxtR;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEdtSex = (EditText) findViewById(R.id.edtSex);
        mEdtAge = (EditText)findViewById(R.id.edtAge);
        mBtnOK = (Button) findViewById(R.id.btnOK);
        mTxtR = (TextView) findViewById(R.id.txtR);

        mBtnOK.setOnClickListener(btnOKOnClick);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private View.OnClickListener btnOKOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String strSex = mEdtSex.getText().toString();
            int iAge = Integer.parseInt(mEdtAge.getText().toString());

            String strSug = getString(R.string.result);
            if (strSex.equals(getString(R.string.sex_male)))
                if (iAge < 28)
                    strSug += getString(R.string.sug_not_hurry);
                else if (iAge > 33)
                    strSug += getString(R.string.sug_get_married);
                else
                    strSug += getString(R.string.sug_find_couple);
            else if (iAge < 25)
                strSug += getString(R.string.sug_not_hurry);
            else if (iAge > 30)
                strSug += getString(R.string.sug_get_married);
            else
                strSug += getString(R.string.sug_find_couple);

            mTxtR.setText(strSug);
        }
    };
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

package com.example.root.bugapp2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Button buttonSomar = (Button) findViewById(R.id.buttonSomar);
        buttonSomar.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                EditText x = (EditText) findViewById(R.id.x);
                EditText y = (EditText) findViewById(R.id.y);
                float a = Float.parseFloat(x.getText().toString());
                float b = Float.parseFloat(y.getText().toString());
                float res = Calculator.sum(a,b);
                TextView result = (TextView) findViewById(R.id.result);
                result.setText(Float.toString(res));
                TextView sign = (TextView) findViewById(R.id.sign);
                sign.setText("+");
            }
        });

        Button buttonSubtrair = (Button) findViewById(R.id.buttonSubtrair);
        buttonSubtrair.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                EditText x = (EditText) findViewById(R.id.x);
                EditText y = (EditText) findViewById(R.id.y);
                float a = Float.parseFloat(x.getText().toString());
                float b = Float.parseFloat(y.getText().toString());
                float res = Calculator.subtract(a,b);
                TextView result = (TextView) findViewById(R.id.result);
                result.setText(Float.toString(res));
                TextView sign = (TextView) findViewById(R.id.sign);
                sign.setText("-");
            }
        });

        Button buttonMultiplicar = (Button) findViewById(R.id.buttonMultiplicar);
        buttonMultiplicar.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                EditText x = (EditText) findViewById(R.id.x);
                EditText y = (EditText) findViewById(R.id.y);
                float a = Float.parseFloat(x.getText().toString());
                float b = Float.parseFloat(y.getText().toString());
                float res = Calculator.multiply(a,b);
                TextView result = (TextView) findViewById(R.id.result);
                result.setText(Float.toString(res));
                TextView sign = (TextView) findViewById(R.id.sign);
                sign.setText("x");
            }
        });

        Button buttonDividir = (Button) findViewById(R.id.buttonDividir);
        buttonDividir.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                EditText x = (EditText) findViewById(R.id.x);
                EditText y = (EditText) findViewById(R.id.y);
                float a = Float.parseFloat(x.getText().toString());
                float b = Float.parseFloat(y.getText().toString());
                float res = Calculator.divide(a,b);
                TextView result = (TextView) findViewById(R.id.result);
                result.setText(Float.toString(res));
                TextView sign = (TextView) findViewById(R.id.sign);
                sign.setText("/");
            }
        });
    }
}

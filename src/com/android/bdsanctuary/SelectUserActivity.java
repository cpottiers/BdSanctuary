package com.android.bdsanctuary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.android.bdsanctuary.datas.Global;

public class SelectUserActivity extends Activity implements OnClickListener {
    static final String IS_NOT_CANCELLABLE_EXTRA = "com.android.bdsanctuary.selectuseractivity.cancellableextra";
    private boolean     isNotCancellable         = false;

    public final static int RESULT_QUIT    = 100;
    
    private final static int MENU_FIRST    = 0;
    private final static int MENU_QUIT     = MENU_FIRST + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userdetails_layout);

        if (getIntent() != null)
            isNotCancellable = getIntent().getBooleanExtra(IS_NOT_CANCELLABLE_EXTRA, false);

        Button login = (Button) findViewById(R.id.ms_login);
        login.setOnClickListener(this);

        EditText ed = null;
        String username = Global.getUsername();
        String password = Global.getPassword();
        ed = (EditText) findViewById(R.id.ms_username);
        ed.setText(username);
        ed.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                checkForm();
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }
        });
        ed = (EditText) findViewById(R.id.ms_password);
        ed.setText(password);
        ed.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                checkForm();
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }
        });
        checkForm();
    }

    @Override
    public void onClick(View v) {
        if (v != null && v.getId() == R.id.ms_login) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if(imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.First_Sync_Alert);
            builder.setPositiveButton(R.string.Error_Alert_OK_Btn, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText ed = null;
                    ed = (EditText) findViewById(R.id.ms_username);
                    String username = ed.getText().toString();
                    ed = (EditText) findViewById(R.id.ms_password);
                    String password = ed.getText().toString();
                    Global.setUsername(username);
                    Global.setPassword(password);

                    setResult(RESULT_OK);
                    finish();
                }
            });
            builder.setNegativeButton(R.string.Error_Alert_Cancel_Btn, null);
            builder.show();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (isNotCancellable) return true;
            setResult(RESULT_CANCELED);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkForm() {
        Button login = (Button) findViewById(R.id.ms_login);

        EditText ed = null;
        ed = (EditText) findViewById(R.id.ms_username);
        String username = ed.getText().toString();
        ed = (EditText) findViewById(R.id.ms_password);
        String password = ed.getText().toString();
        if (username.length() > 0 && password.length() > 0)
            login.setEnabled(true);
        else
            login.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem qItem = menu.add(Menu.NONE, MENU_QUIT, MENU_QUIT, getResources().getString(R.string.Menu_Quit));
        qItem.setIcon(R.drawable.ic_menu_close_clear_cancel);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_QUIT:
                setResult(RESULT_QUIT);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

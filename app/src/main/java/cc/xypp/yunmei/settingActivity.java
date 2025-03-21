package cc.xypp.yunmei;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.xypp.yunmei.eneity.Lock;

public class settingActivity extends AppCompatActivity {
    private final List<Lock> locks = new ArrayList<>();
    private SharedPreferences sp, ssp;
    private int currentLock = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        sp = getSharedPreferences("storage", MODE_PRIVATE);
        System.out.println(((Switch) findViewById(R.id.quickConn)).isChecked());
        System.out.println(((Switch) findViewById(R.id.autoConn)).isChecked());
        System.out.println(((Switch) findViewById(R.id.autoExit)).isChecked());
        ((Switch) findViewById(R.id.quickConn)).setChecked(sp.getBoolean("quickCon", true));
        ((Switch) findViewById(R.id.autoConn)).setChecked(sp.getBoolean("autoCon", false));
        ((Switch) findViewById(R.id.autoExit)).setChecked(sp.getBoolean("autoExit", false));
        switch (sp.getString("sigLoc", "ask")) {
            case "ask":
                ((RadioButton) findViewById(R.id.sigLocOpt_ask)).setChecked(true);
                break;
            case "rel":
                ((RadioButton) findViewById(R.id.sigLocOpt_rel)).setChecked(true);
                break;
            case "lst":
                ((RadioButton) findViewById(R.id.sigLocOpt_lst)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.sigLocOpt_ask)).setChecked(true);
                break;
        }
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            ssp = EncryptedSharedPreferences.create(
                    "yunmei_secure",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        reloadLocks();
    }

    public void edit_quickCon(View view) {
        SharedPreferences.Editor a = sp.edit();
        a.putBoolean("quickCon", ((Switch) findViewById(R.id.quickConn)).isChecked());
        a.apply();
    }

    public void edit_autoCon(View view) {
        if (((Switch) findViewById(R.id.autoConn)).isChecked() && ((Switch) findViewById(R.id.autoExit)).isChecked()) {
            checkDanger();
            return;
        }
        SharedPreferences.Editor a = sp.edit();
        a.putBoolean("autoCon", ((Switch) findViewById(R.id.autoConn)).isChecked());
        a.apply();
    }

    public void edit_autoExit(View view) {
        if (((Switch) findViewById(R.id.autoConn)).isChecked() && ((Switch) findViewById(R.id.autoExit)).isChecked()) {
            checkDanger();
            return;
        }
        SharedPreferences.Editor a = sp.edit();
        a.putBoolean("autoExit", ((Switch) findViewById(R.id.autoExit)).isChecked());
        a.apply();
    }

    public void checkDanger() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("危险操作提醒");
        builder.setMessage("警告！您正在执行一项非常危险的操作！\n理论上同时打开自动开门和自动退出会导致以后无法再进行APP的其他操作。\n您只应该在确定了风险后执行此操作。\n确定要执行该操作吗？");
        builder.setPositiveButton("放弃", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ((Switch) findViewById(R.id.autoConn)).setChecked(sp.getBoolean("autoCon", false));
                ((Switch) findViewById(R.id.autoExit)).setChecked(sp.getBoolean("autoExit", false));
            }
        });
        builder.setNeutralButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                alertAgain();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void alertAgain() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("危险操作再次提醒");
        builder.setMessage("警告！您正在执行一项的操作确实是非常危险的！\n您不应该为了图方便而忽视风险\n您需要在确定不使用本APP其他功能时才执行此操作。\n再次确定，要执行该操作吗？");
        builder.setPositiveButton("放弃", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ((Switch) findViewById(R.id.autoConn)).setChecked(sp.getBoolean("autoCon", false));
                ((Switch) findViewById(R.id.autoExit)).setChecked(sp.getBoolean("autoExit", false));
            }
        });
        builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                alertLast();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void alertLast() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("最后提醒");
        builder.setMessage("确认操作后将同时打开这两项功能。请再次确认。\n您确定要打开吗");
        builder.setNeutralButton("放弃", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ((Switch) findViewById(R.id.autoConn)).setChecked(sp.getBoolean("autoCon", false));
                ((Switch) findViewById(R.id.autoExit)).setChecked(sp.getBoolean("autoExit", false));
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SharedPreferences.Editor a = sp.edit();
                a.putBoolean("autoCon", ((Switch) findViewById(R.id.autoConn)).isChecked());
                a.putBoolean("autoExit", ((Switch) findViewById(R.id.autoExit)).isChecked());
                a.apply();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void clickClearInfo(View view) {
        Boolean atco = sp.getBoolean("autoCon", false);
        Boolean atex = sp.getBoolean("autoExit", false);
        Boolean qkcn = sp.getBoolean("quickCon", true);
        SharedPreferences.Editor a = sp.edit();
        a.clear();
        a.apply();
    }

    public void ClickSigLoc(View view) {
        SharedPreferences.Editor a = sp.edit();
        if (view.getId() == R.id.sigLocOpt_ask)
            a.putString("sigLoc", "ask");
        else if (view.getId() == R.id.sigLocOpt_lst)
            a.putString("sigLoc", "lst");
        else if (view.getId() == R.id.sigLocOpt_rel)
            a.putString("sigLoc", "rel");
        a.apply();
    }

    public void clickCheckInfo(View view) {
        startActivity(new Intent(this, lockInfoActivity.class));
    }

    public void about(View view) {
        Uri uri = Uri.parse("https://yunmei.xypp.cc/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void reloadLocks() {
        Set<String> lockSet = ssp.getStringSet("locks", new HashSet<>());
        List<String> nameSet = new ArrayList<>();
        locks.clear();
        lockSet.forEach((v) -> {
            String[] tmp = v.split("\\|");
            if (tmp.length == 5) {
                locks.add(new Lock(tmp[0], tmp[1], tmp[2], tmp[3], tmp[4]));
                nameSet.add(tmp[0]);
            }
        });
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, com.google.android.material.R.layout.support_simple_spinner_dropdown_item, nameSet);
        ((Spinner) findViewById(R.id.lockSelector_del)).setAdapter(adapter);
        ((Spinner) findViewById(R.id.lockSelector_del)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currentLock = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                currentLock = -1;
            }
        });
    }

    public void deleteLock(View view) {
        locks.remove(currentLock);
        Set<String> lockSet = new HashSet<>();
        locks.forEach((v) -> {
            lockSet.add(v.toString());
        });
        ssp.edit().putStringSet("locks", lockSet).apply();
        reloadLocks();
    }

    public void scanQR(View view) {
        // 创建IntentIntegrator对象
        IntentIntegrator intentIntegrator = new IntentIntegrator(settingActivity.this);
        // 开始扫描
        intentIntegrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 获取解析结果
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "取消扫描", Toast.LENGTH_LONG).show();
            } else {
                String c = result.getContents();
                if (c.startsWith("yunmei://addlock/")) {
                    Intent i = new Intent(this, addLockActivity.class);
                    i.setData(Uri.parse(c));
                    startActivity(i);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
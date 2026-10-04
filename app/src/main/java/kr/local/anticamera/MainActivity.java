package kr.local.anticamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Button> buttons = new ArrayList<>();
    private PrivilegeBridge bridge;
    private Spinner modes;
    private TextView status;
    private SharedPreferences prefs;
    private int user;
    private boolean busy, destroyed;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        user = Process.myUid() / 100000;
        prefs = getSharedPreferences("camera_original_u" + user, MODE_PRIVATE);
        bridge = new Bridge(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20); panel.setPadding(pad, pad, pad, pad);
        scroll.addView(panel);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets area = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = area.left; top = area.top; right = area.right; bottom = area.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom); return insets;
        });
        setContentView(scroll);
        label(panel, "카메라 무음 설정", 26);
        label(panel, "Android " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT
                + "\n" + Build.MANUFACTURER + " " + Build.MODEL
                + "\n사용자 " + user + " · ABI " + Arrays.toString(Build.SUPPORTED_ABIS), 13);
        label(panel, "기본 카메라의 Samsung CSC 설정을 변경합니다. 사진을 촬영하는 앱이 아닙니다.\n"
                + "설정 값이 저장돼도 제조사·카메라 앱·펌웨어가 이를 무시할 수 있습니다. 실제 촬영으로 확인하세요.\n"
                + "키가 없거나 값이 0/1이 아니면 변경하지 않습니다. 미디어·벨소리 볼륨은 변경하지 않습니다.", 15);
        label(panel, "실행 권한", 18);
        modes = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bridge.modes());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modes.setAdapter(adapter);
        modes.setSelection(Math.min(prefs.getInt("mode", 0), bridge.modes().length - 1));
        panel.addView(modes);
        label(panel, "Root: Root 관리자의 su 권한을 승인하세요.\n"
                + "Shizuku: ADB/무선 디버깅으로 시작한 Shizuku를 사용합니다. Android 9·10은 PC ADB 시작이 필요합니다.\n"
                + "Root Shizuku: Root로 시작한 Shizuku(UID 0)가 필요합니다. 앱이 기기를 루팅하지는 않습니다.", 14);
        button(panel, "현재 설정 조회 / 권한 연결", () -> perform(0));
        button(panel, "무음 설정 (0)", () -> confirm(true));
        button(panel, "셔터음 설정 (1)", () -> confirm(false));
        button(panel, "처음 저장한 원래 값 복원", () -> perform(3));
        status = label(panel, "권한을 선택하고 조회하세요.\n저장된 원래 값: " + original(), 16);
        status.setTextIsSelectable(true);
    }
    private String original() { return prefs.getString("original", "없음"); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView label(LinearLayout panel, String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size);
        view.setPadding(0, dp(10), 0, dp(10)); panel.addView(view); return view;
    }
    private void button(LinearLayout panel, String text, Runnable click) {
        Button b = new Button(this); b.setText(text); b.setOnClickListener(v -> click.run());
        panel.addView(b); buttons.add(b);
    }
    private void confirm(boolean mute) {
        new AlertDialog.Builder(this).setTitle(mute ? "무음 설정" : "셔터음 설정")
            .setMessage("변경 전 원래 값을 저장합니다. 카메라가 이 키를 지원하는지는 실제 촬영으로 확인해야 합니다. 계속할까요?")
            .setNegativeButton("취소", null).setPositiveButton("적용", (d, which) -> perform(mute ? 1 : 2)).show();
    }
    private void setBusy(boolean value) {
        busy = value; modes.setEnabled(!value);
        for (Button b : buttons) b.setEnabled(!value);
    }
    private void perform(int action) {
        if (busy || destroyed) return;
        final int mode = modes.getSelectedItemPosition();
        prefs.edit().putInt("mode", mode).apply();
        setBusy(true); status.setText("권한 연결 및 실행 중…");
        bridge.prepare(mode, () -> worker.execute(() -> {
            try {
                SettingsLogic logic = new SettingsLogic(op -> bridge.run(mode, op, user).checked(), new SettingsLogic.Snapshot() {
                    public String load() { return prefs.getString("original", null); }
                    public boolean save(String value) { return prefs.edit().putString("original", value).commit(); }
                    public boolean clear() { return prefs.edit().remove("original").commit(); }
                });
                String value;
                switch (action) {
                    case 0: value = logic.read(); break;
                    case 1: value = logic.set(true); break;
                    case 2: value = logic.set(false); break;
                    case 3: value = logic.restore(); break;
                    default: throw new IllegalArgumentException();
                }
                String prefix = action == 0 ? "조회 완료" : action == 3 ? "원래 값 복원·재조회 완료" : "설정 저장·재조회 완료";
                finish(prefix + "\n현재 값: " + value + (value.equals("0") ? " (무음 요청)" : " (셔터음 요청)")
                        + "\n실제 무음 여부는 카메라를 완전히 종료하고 다시 열어 촬영으로 확인하세요.\n저장된 원래 값: " + original());
            } catch (Exception e) {
                finish("실패: " + e.getMessage() + "\n일부 변경이 적용됐을 수 있습니다. 조회 후 필요하면 원래 값 복원을 사용하세요.\n저장된 원래 값: " + original());
            }
        }));
    }
    private void finish(String message) {
        runOnUiThread(() -> {
            if (destroyed) return;
            setBusy(false); status.setText(message);
        });
    }
    public void preparationFailed(String message) { finish(message); }
    @Override public void onBackPressed() {
        if (busy) Toast.makeText(this, "명령 실행이 끝난 뒤 닫아주세요.", Toast.LENGTH_SHORT).show();
        else super.onBackPressed();
    }
    @Override public void onDestroy() {
        destroyed = true; worker.shutdown(); bridge.close(); super.onDestroy();
    }
}

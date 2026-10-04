package kr.local.anticamera;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import rikka.shizuku.Shizuku;

public final class Bridge implements PrivilegeBridge {
    private final MainActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Shizuku.UserServiceArgs args;
    private volatile ISettingsService service;
    private Runnable pending;
    private int requestedMode;
    private boolean bound, closed;
    private final Runnable timeout = () -> fail("Shizuku 연결 시간 초과: Shizuku를 시작하고 다시 시도하세요.");
    private final Shizuku.OnRequestPermissionResultListener permission = (code, result) -> {
        if (code != 711 || closed || pending == null) return;
        if (result == PackageManager.PERMISSION_GRANTED) connect();
        else fail("Shizuku 권한이 거부되었습니다. Shizuku 앱에서 권한을 허용하세요.");
    };
    private final Shizuku.OnBinderDeadListener died = () -> {
        service = null; bound = false;
        if (pending != null) fail("Shizuku 연결이 종료되었습니다.");
    };
    private final ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (closed) return;
            service = ISettingsService.Stub.asInterface(binder);
            finishPrepare();
        }
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            if (pending != null) fail("Shizuku UserService 연결이 종료되었습니다.");
        }
    };
    public Bridge(MainActivity activity) {
        this.activity = activity;
        args = new Shizuku.UserServiceArgs(new ComponentName(activity.getPackageName(), SettingsService.class.getName()))
                .daemon(false).processNameSuffix("camera_settings").debuggable(false).version(1);
        Shizuku.addRequestPermissionResultListener(permission);
        Shizuku.addBinderDeadListener(died);
    }
    public String[] modes() { return new String[] { "Root (su)", "Shizuku (ADB / Shell)", "Root Shizuku (UID 0)" }; }
    public void prepare(int mode, Runnable ready) {
        if (mode == 0) { ready.run(); return; }
        requestedMode = mode; pending = ready;
        try {
            if (!Shizuku.pingBinder()) throw new Exception("Shizuku가 실행되지 않았습니다. 먼저 Shizuku를 시작하세요.");
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 12) throw new Exception("Shizuku 12 이상이 필요합니다.");
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                if (Shizuku.shouldShowRequestPermissionRationale()) throw new Exception("Shizuku 앱에서 이 앱의 권한을 허용하세요.");
                handler.postDelayed(timeout, 60000);
                Shizuku.requestPermission(711);
            } else connect();
        } catch (Exception e) { fail(e.getMessage()); }
    }
    private void connect() {
        try {
            checkUid(Shizuku.getUid(), requestedMode);
            handler.removeCallbacks(timeout);
            handler.postDelayed(timeout, 15000);
            if (service != null && service.asBinder().isBinderAlive()) finishPrepare();
            else { bound = true; Shizuku.bindUserService(args, connection); }
        } catch (Exception e) { fail(e.getMessage()); }
    }
    private void finishPrepare() {
        if (pending == null) return;
        try {
            checkUid(service.uid(), requestedMode);
            handler.removeCallbacks(timeout);
            Runnable ready = pending; pending = null; ready.run();
        } catch (Exception e) { fail(e.getMessage()); }
    }
    private static void checkUid(int uid, int mode) throws Exception {
        int expected = mode == 2 ? 0 : 2000;
        if (uid != expected) throw new Exception("선택 모드는 UID " + expected + "이 필요합니다. 현재 UID=" + uid
                + ". 실행 중인 Shizuku 권한에 맞는 모드를 선택하세요.");
    }
    private void fail(String message) {
        pending = null; handler.removeCallbacks(timeout);
        activity.preparationFailed(message);
    }
    public CommandResult run(int mode, int operation, int user) throws Exception {
        if (mode == 0) return Shell.run(operation, user, true);
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
            throw new Exception("Shizuku 연결 또는 권한이 없어졌습니다.");
        ISettingsService remote = service;
        if (remote == null || !remote.asBinder().isBinderAlive()) throw new Exception("Shizuku 서비스를 다시 연결하세요.");
        checkUid(remote.uid(), mode);
        return CommandResult.decode(remote.execute(operation, user));
    }
    public void close() {
        closed = true; pending = null; handler.removeCallbacks(timeout);
        Shizuku.removeRequestPermissionResultListener(permission);
        Shizuku.removeBinderDeadListener(died);
        if (bound) try { Shizuku.unbindUserService(args, connection, true); } catch (Exception ignored) { }
        service = null;
    }
}

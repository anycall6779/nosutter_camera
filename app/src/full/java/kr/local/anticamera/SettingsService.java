package kr.local.anticamera;

import android.os.Process;
import android.os.RemoteException;

public final class SettingsService extends ISettingsService.Stub {
    public SettingsService() { }
    @Override public void destroy() { System.exit(0); }
    @Override public int uid() { return Process.myUid(); }
    @Override public synchronized String execute(int operation, int user) throws RemoteException {
        try {
            int uid = uid();
            if (uid != 0 && uid != 2000) throw new Exception("Root/Shell 권한이 아닙니다: " + uid);
            return Shell.run(operation, user, false).encode();
        } catch (Exception e) {
            throw new RemoteException(e.toString());
        }
    }
}

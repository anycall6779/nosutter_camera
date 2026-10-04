package kr.local.anticamera;

public final class Bridge implements PrivilegeBridge {
    public Bridge(MainActivity activity) { }
    public String[] modes() { return new String[] { "Root (su)" }; }
    public void prepare(int mode, Runnable ready) { ready.run(); }
    public CommandResult run(int mode, int operation, int user) throws Exception {
        if (mode != 0) throw new Exception("Root 전용 앱입니다.");
        return Shell.run(operation, user, true);
    }
    public void close() { }
}

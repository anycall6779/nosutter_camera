package kr.local.anticamera;

public final class LogicTest {
    static final class Fixture implements SettingsLogic.Transport, SettingsLogic.Snapshot {
        String current = "1", saved; boolean saveFails, writeFails, ignoredWrite;
        int writes;
        public String execute(int op) throws Exception {
            if (op == Shell.READ) return current;
            writes++;
            if (writeFails) throw new Exception("denied");
            if (!ignoredWrite) current = op == Shell.MUTE ? "0" : "1";
            return "";
        }
        public String load() { return saved; }
        public boolean save(String value) { if (saveFails) return false; saved = value; return true; }
        public boolean clear() { saved = null; return true; }
        SettingsLogic logic() { return new SettingsLogic(this, this); }
    }
    interface Checked { void run() throws Exception; }
    static int checks;
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    static void fails(Checked action) throws Exception {
        boolean failed = false;
        try { action.run(); } catch (Exception expected) { failed = true; }
        check(failed);
    }
    public static void main(String[] args) throws Exception {
        Fixture f = new Fixture();
        check(f.logic().set(true).equals("0")); check(f.saved.equals("1"));
        f.logic().set(true); check(f.saved.equals("1"));
        f.logic().set(false); check(f.saved.equals("1"));
        check(f.logic().restore().equals("1")); check(f.saved == null);
        fails(() -> f.logic().restore());
        for (String invalid : new String[] { "null", "", "2", "Error", "SecurityException" }) {
            Fixture a = new Fixture(); a.current = invalid;
            fails(() -> a.logic().set(true)); check(a.writes == 0 && a.saved == null);
        }
        Fixture a = new Fixture(); a.saveFails = true;
        fails(() -> a.logic().set(true)); check(a.writes == 0);
        Fixture b = new Fixture(); b.writeFails = true;
        fails(() -> b.logic().set(true)); check(b.saved.equals("1"));
        Fixture c = new Fixture(); c.ignoredWrite = true;
        fails(() -> c.logic().set(true)); check(c.saved.equals("1"));
        Fixture d = new Fixture(); d.current = "0";
        d.logic().set(false); check(d.saved.equals("0")); check(d.logic().restore().equals("0"));
        Fixture e = new Fixture(); e.saved = "garbage";
        fails(() -> e.logic().restore()); check(e.writes == 0);
        fails(() -> Shell.command(99, 0)); fails(() -> Shell.command(Shell.READ, -1));
        check(Shell.command(Shell.MUTE, 10).equals("/system/bin/settings --user 10 put system " + Shell.KEY + " 0"));
        check(Shell.command(Shell.SOUND, 0).endsWith(" 1"));
        fails(() -> new CommandResult(1, "", "denied").checked());
        fails(() -> new CommandResult(0, "", "SecurityException").checked());
        check(new CommandResult(0, "0\n", "").checked().equals("0"));
        System.out.println("PASS: " + checks + " checks");
    }
}

package kr.local.anticamera;

/** Saves the first known value before a write; failed writes retain the recovery snapshot. */
public final class SettingsLogic {
    public interface Transport { String execute(int operation) throws Exception; }
    public interface Snapshot {
        String load();
        boolean save(String value);
        boolean clear();
    }
    private final Transport transport;
    private final Snapshot snapshot;
    public SettingsLogic(Transport transport, Snapshot snapshot) {
        this.transport = transport; this.snapshot = snapshot;
    }
    public String read() throws Exception {
        String value = transport.execute(Shell.READ).trim();
        if (!value.equals("0") && !value.equals("1"))
            throw new Exception("지원 여부를 확인할 수 없습니다. 설정 키 값=" + value
                    + ". 이 앱은 기존 값이 0 또는 1인 기기에서만 변경합니다.");
        return value;
    }
    public String set(boolean mute) throws Exception {
        String before = read();
        if (snapshot.load() == null && !snapshot.save(before))
            throw new Exception("원래 값 저장 실패: 설정을 변경하지 않았습니다.");
        String expected = mute ? "0" : "1";
        transport.execute(mute ? Shell.MUTE : Shell.SOUND);
        String actual = read();
        if (!expected.equals(actual)) throw new Exception("재조회 검증 실패: 기대=" + expected + ", 실제=" + actual);
        return actual;
    }
    public String restore() throws Exception {
        String original = snapshot.load();
        if (original == null) throw new Exception("저장된 원래 값이 없습니다.");
        if (!original.equals("0") && !original.equals("1")) throw new Exception("저장된 원래 값이 잘못되었습니다.");
        transport.execute(original.equals("0") ? Shell.MUTE : Shell.SOUND);
        String actual = read();
        if (!original.equals(actual)) throw new Exception("원래 값 복원 검증 실패");
        if (!snapshot.clear()) throw new Exception("설정은 복원됐지만 백업 삭제에 실패했습니다. 다시 복원해도 안전합니다.");
        return actual;
    }
}

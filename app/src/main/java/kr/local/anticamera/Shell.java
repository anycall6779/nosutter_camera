package kr.local.anticamera;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.*;

/** Only fixed settings operations are accepted, never caller-supplied shell commands. */
public final class Shell {
    public static final String KEY = "csc_pref_camera_forced_shuttersound_key";
    public static final int READ = 0, MUTE = 1, SOUND = 2;
    public static String command(int operation, int user) {
        if (user < 0 || user > 21474) throw new IllegalArgumentException("잘못된 사용자 ID");
        String base = "/system/bin/settings --user " + user;
        switch (operation) {
            case READ: return base + " get system " + KEY;
            case MUTE: return base + " put system " + KEY + " 0";
            case SOUND: return base + " put system " + KEY + " 1";
            default: throw new IllegalArgumentException("허용되지 않은 명령");
        }
    }
    public static CommandResult run(int operation, int user, boolean root) throws Exception {
        String command = command(operation, user);
        ProcessBuilder builder = root
            ? new ProcessBuilder("su", "-c", "u=$(/system/bin/id -u); [ \"$u\" = 0 ] || { echo 'Root UID 확인 실패' >&2; exit 77; }; exec " + command)
            : new ProcessBuilder("/system/bin/sh", "-c", "exec " + command);
        Process process = builder.start();
        ExecutorService readers = Executors.newFixedThreadPool(2);
        Future<String> out = readers.submit(() -> read(process.getInputStream()));
        Future<String> err = readers.submit(() -> read(process.getErrorStream()));
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) throw new Exception("30초 시간 초과: Root 승인 또는 서비스 상태를 확인하세요.");
            return new CommandResult(process.exitValue(), out.get(3, TimeUnit.SECONDS), err.get(3, TimeUnit.SECONDS));
        } finally {
            process.destroyForcibly();
            process.getInputStream().close(); process.getErrorStream().close(); process.getOutputStream().close();
            readers.shutdownNow();
        }
    }
    private static String read(InputStream stream) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048]; int count;
        while ((count = stream.read(buffer)) != -1) {
            if (bytes.size() + count > 65536) throw new Exception("명령 출력 크기 초과");
            bytes.write(buffer, 0, count);
        }
        return bytes.toString("UTF-8");
    }
}

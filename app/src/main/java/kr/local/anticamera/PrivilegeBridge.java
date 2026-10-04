package kr.local.anticamera;

public interface PrivilegeBridge {
    String[] modes();
    void prepare(int mode, Runnable ready);
    CommandResult run(int mode, int operation, int user) throws Exception;
    void close();
}

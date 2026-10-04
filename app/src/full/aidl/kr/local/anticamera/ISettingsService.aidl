package kr.local.anticamera;
interface ISettingsService {
    void destroy() = 16777114;
    int uid() = 0;
    String execute(int operation, int user) = 1;
}

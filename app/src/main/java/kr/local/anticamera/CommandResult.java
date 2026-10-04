package kr.local.anticamera;

import org.json.JSONObject;

public final class CommandResult {
    public final int exit;
    public final String out, err;
    public CommandResult(int exit, String out, String err) {
        this.exit = exit; this.out = out; this.err = err;
    }
    public String encode() throws Exception {
        return new JSONObject().put("exit", exit).put("out", out).put("err", err).toString();
    }
    public static CommandResult decode(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        return new CommandResult(o.getInt("exit"), o.getString("out"), o.getString("err"));
    }
    public String checked() throws Exception {
        if (exit != 0 || !err.trim().isEmpty())
            throw new Exception("명령 실패 (" + exit + "): " + err + " " + out);
        return out.trim();
    }
}

package pl.tomaszko.s03e05.hub;

public record HubCallResult(boolean success, String body, String errorText) {

    public static HubCallResult ok(String body) {
        return new HubCallResult(true, body, null);
    }

    public static HubCallResult failed(String errorText) {
        return new HubCallResult(false, null, errorText);
    }

    public String textForModel() {
        if (success) {
            return body;
        }
        return errorText != null ? errorText : "Unreadable hub response";
    }
}

package pl.tomaszko.s03e05.hub;

import java.util.List;

public record VerifyRequest(String apikey, String task, List<String> answer) {
}

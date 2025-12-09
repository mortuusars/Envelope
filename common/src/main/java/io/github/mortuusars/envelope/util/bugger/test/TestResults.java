package io.github.mortuusars.envelope.util.bugger.test;

import java.util.List;

public record TestResults(List<String> passed, List<Result> failed) {
    public record Result(String name, String error) {}
}

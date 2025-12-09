package io.github.mortuusars.envelope.util.bugger.test;

import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BuggerTests {
    private final List<Test> tests = new ArrayList<>();

    public BuggerTests add(Test test) {
        tests.add(test);
        return this;
    }

    public BuggerTests add(String name, Supplier<DataResult<Boolean>> function) {
        return add(new Test(name, function));
    }

    public BuggerTests add(BuggerTests tests) {
        this.tests.addAll(tests.tests);
        return this;
    }

    // --

    public TestResults run(Consumer<Integer> testCount) {
        testCount.accept(tests.size());

        List<String> passed = new ArrayList<>();
        List<TestResults.Result> failed = new ArrayList<>();

        for (Test test : tests) {
            try {
                test.function().get().error().ifPresentOrElse(
                      err -> failed.add(new TestResults.Result(test.name(), err.message())),
                      () -> passed.add(test.name()));
            } catch (Exception e) {
                failed.add(new TestResults.Result(test.name(), e.getMessage()));
            }
        }

        return new TestResults(passed, failed);
    }
}

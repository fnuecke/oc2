/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.gametest.device.GuestTestChannel;
import li.cil.oc2.gametest.device.GuestTestPortDevice;
import net.minecraft.gametest.framework.GameTestAssertException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class GuestTests {
    private record Case(String name, boolean passed, List<String> detail) {
    }

    // --------------------------------------------------------------------- //

    public static GuestTests of(final VirtualMachine virtualMachine) {
        return new GuestTests((AbstractVirtualMachine) virtualMachine);
    }

    // --------------------------------------------------------------------- //

    private final AbstractVirtualMachine virtualMachine;
    private final List<Case> cases = new ArrayList<>();
    private final List<String> unexpected = new ArrayList<>();
    private List<String> detail = new ArrayList<>();
    private boolean ready;
    private boolean finished;
    private int failed;

    // --------------------------------------------------------------------- //

    public void requireReady() {
        poll();
        if (!ready) {
            throw new GameTestAssertException("guest test runner has not reported in yet");
        }
    }

    public void run(final String suite) {
        cases.clear();
        detail = new ArrayList<>();
        finished = false;
        failed = 0;
        requireChannel().send("run " + suite);
    }

    public void requireSuccess() {
        poll();
        if (!finished) {
            throw new GameTestAssertException("guest suite has not finished; " + describe());
        }

        if (failed > 0) {
            throw new GameTestAssertException(failed + " guest test(s) failed:\n" + describe());
        }

        if (cases.isEmpty()) {
            throw new GameTestAssertException("guest suite ran no tests at all");
        }
    }

    // --------------------------------------------------------------------- //

    private void poll() {
        final GuestTestChannel channel = channel();
        if (channel == null) {
            return;
        }

        for (final String line : channel.receive()) {
            accept(line);
        }
    }

    @Nullable
    private GuestTestChannel channel() {
        for (final Device device : virtualMachine.getBusController().getDevices()) {
            if (device instanceof final GuestTestPortDevice port) {
                return port.getChannel();
            }
        }

        return null;
    }

    private GuestTestChannel requireChannel() {
        final GuestTestChannel channel = channel();
        if (channel == null) {
            throw new GameTestAssertException("no guest test port on the bus; " +
                "the machine needs a " + GuestTestPortDevice.PORT_NAME + " item installed");
        }
        return channel;
    }

    private void accept(final String line) {
        final String[] parts = line.split(" ", 2);
        final String argument = parts.length > 1 ? parts[1] : "";
        switch (parts[0]) {
            case "ready" -> ready = true;
            case "case" -> {
                final String[] verdict = argument.split(" ", 2);
                detail = new ArrayList<>();
                cases.add(new Case(verdict.length > 1 ? verdict[1] : "?",
                    "PASS".equals(verdict[0]), detail));
            }
            case "detail" -> detail.add(argument);
            case "end" -> {
                final String[] counts = argument.split(" ");
                failed = counts.length > 1 ? parseCount(counts[1]) : 0;
                finished = true;
            }
            default -> unexpected.add(line);
        }
    }

    private String describe() {
        final StringBuilder text = new StringBuilder();
        for (final Case result : cases) {
            text.append(result.passed() ? "  PASS " : "  FAIL ").append(result.name()).append('\n');
            if (!result.passed()) {
                for (final String line : result.detail()) {
                    text.append("    ").append(line).append('\n');
                }
            }
        }

        if (cases.isEmpty()) {
            text.append("  (no results yet)\n");
        }

        for (final String line : unexpected) {
            text.append("  unparsed: ").append(line).append('\n');
        }

        return text.toString();
    }

    private static int parseCount(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return 1;
        }
    }

    // --------------------------------------------------------------------- //

    private GuestTests(final AbstractVirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }
}

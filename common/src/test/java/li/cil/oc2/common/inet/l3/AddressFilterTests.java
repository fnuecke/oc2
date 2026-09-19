/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l3;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.inet.util.AddressParseException;
import li.cil.oc2.common.inet.util.InternetUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AddressFilterTests {
    private static int ip(final String address) {
        try {
            return InternetUtils.parseIpv4Address(address);
        } catch (final AddressParseException e) {
            throw new AssertionError(e);
        }
    }

    private static AddressFilter denying(final String... rules) {
        return new AddressFilter(List.of(), List.of(rules), false);
    }

    private static AddressFilter allowing(final String... rules) {
        return new AddressFilter(List.of(rules), List.of(), false);
    }

    @Test
    public void loopbackMulticastAndBroadcastAreDeniedWithoutAnyRules() {
        final AddressFilter filter = AddressFilter.withBuiltInDenials(List.of("0.0.0.0/0"), List.of(), false);

        for (final String address : new String[]{"0.0.0.0", "0.1.2.3", "127.0.0.1", "127.255.255.255",
            "169.254.169.254", "224.0.0.1", "239.255.255.250", "240.0.0.1", "255.255.255.255"}) {
            assertFalse(filter.isAllowed(ip(address)), address + " must be denied even when everything is allowed");
        }
        assertTrue(filter.isAllowed(ip("8.8.8.8")));
        assertTrue(filter.isAllowed(ip("10.0.0.1")), "the built-in rules cover nothing beyond their own ranges");
        assertTrue(filter.isAllowed(ip("169.254.0.1")), "only the metadata address is built in, not all of link-local");
    }

    @Test
    public void shippedDefaultsBlockEverythingTheyClaimTo() {
        final AddressFilter filter =
            AddressFilter.withBuiltInDenials(Config.internetAllowedHosts, Config.internetDeniedHosts, false);

        final String[] mustBeBlocked = {
            "127.0.0.1",
            "127.1.2.3",
            "10.0.0.1",
            "172.16.0.1",
            "172.31.255.254",
            "192.168.1.1",
            "100.64.0.1",
            "169.254.169.254",
            "169.254.0.1",
            "0.0.0.0",
            "192.0.0.1",
            "198.18.0.1",
            "224.0.0.1",
            "239.255.255.250",
            "240.0.0.1",
            "255.255.255.255",
        };
        for (final String address : mustBeBlocked) {
            assertFalse(filter.isAllowed(ip(address)), address + " must not be reachable by default");
        }

        final String[] mustBeReachable = {"8.8.8.8", "1.1.1.1", "93.184.216.34", "172.32.0.1", "172.15.255.255"};
        for (final String address : mustBeReachable) {
            assertTrue(filter.isAllowed(ip(address)), address + " should be reachable by default");
        }
    }

    @Test
    public void shippedDefaultsNeedNoDnsLookup() {
        final AddressFilter filter =
            new AddressFilter(Config.internetAllowedHosts, Config.internetDeniedHosts, false);

        assertFalse(filter.needsPeriodicRefresh(), "shipped rules should be literal addresses");
    }

    @Test
    public void withNoRulesEverythingIsPermitted() {
        final AddressFilter filter = new AddressFilter(List.of(), List.of(), false);

        assertTrue(filter.isAllowed(ip("8.8.8.8")));
        assertTrue(filter.isAllowed(ip("127.0.0.1")));
    }

    @Test
    public void deniedSubnetsAreBlocked() {
        final AddressFilter filter = denying("10.0.0.0/8", "127.0.0.0/8");

        assertFalse(filter.isAllowed(ip("10.1.2.3")));
        assertFalse(filter.isAllowed(ip("127.0.0.1")));
        assertTrue(filter.isAllowed(ip("8.8.8.8")));
    }

    @Test
    public void cloudMetadataIsBlockedByTheDefaultRules() {
        final AddressFilter filter = denying("169.254.0.0/16");

        assertFalse(filter.isAllowed(ip("169.254.169.254")));
    }

    @Test
    public void allowListPermitsOnlyWhatItNames() {
        final AddressFilter filter = allowing("8.8.8.8", "1.1.1.0/24");

        assertTrue(filter.isAllowed(ip("8.8.8.8")));
        assertTrue(filter.isAllowed(ip("1.1.1.42")));
        assertFalse(filter.isAllowed(ip("8.8.4.4")));
        assertFalse(filter.isAllowed(ip("1.1.2.1")));
    }

    @Test
    public void denyRulesOverrideAllowRules() {
        final AddressFilter filter = new AddressFilter(
            List.of("10.0.0.0/8"), List.of("10.1.0.0/16"), false);

        assertTrue(filter.isAllowed(ip("10.2.3.4")));
        assertFalse(filter.isAllowed(ip("10.1.3.4")), "the deny rule must win over the allow rule");
        assertFalse(filter.isAllowed(ip("8.8.8.8")), "anything outside the allow list stays out");
    }

    @Test
    public void rangesAreUnderstood() {
        final AddressFilter filter = denying("1.2.3.10-1.2.3.20");

        assertFalse(filter.isAllowed(ip("1.2.3.15")));
        assertTrue(filter.isAllowed(ip("1.2.3.9")));
        assertTrue(filter.isAllowed(ip("1.2.3.21")));
    }

    @Test
    public void singleAddressesAreUnderstood() {
        final AddressFilter filter = denying("203.0.113.5");

        assertFalse(filter.isAllowed(ip("203.0.113.5")));
        assertTrue(filter.isAllowed(ip("203.0.113.6")));
    }

    @Test
    public void unparsableDenyRuleRefusesToStart() {
        assertThrows(IllegalArgumentException.class,
            () -> denying("10.0.0.0/8", "10.0.0.0\\8", "192.168.0.0/16"));
        assertThrows(IllegalArgumentException.class, () -> denying("10.0.0.0/33"));
        assertThrows(IllegalArgumentException.class, () -> denying("not an address at all"));
    }

    @Test
    public void unparsableAllowRuleIsSkippedNotFatal() {
        final AddressFilter filter = allowing("8.8.8.8", "10.0.0.0/33");

        assertTrue(filter.isAllowed(ip("8.8.8.8")));
        assertFalse(filter.isAllowed(ip("1.1.1.1")));
    }

    @Test
    public void blankAndCommentedRulesAreSkipped() {
        final AddressFilter filter = denying("", "   ", "# a comment", "10.0.0.0/8");

        assertFalse(filter.isAllowed(ip("10.0.0.1")));
        assertTrue(filter.isAllowed(ip("8.8.8.8")));
    }

    @Test
    public void ruleDenyingEverythingBlocksEverything() {
        final AddressFilter filter = denying("0.0.0.0/0");

        assertFalse(filter.isAllowed(ip("8.8.8.8")));
        assertFalse(filter.isAllowed(ip("255.255.255.255")));
        assertFalse(filter.isAllowed(ip("0.0.0.0")));
    }

    @Test
    public void hostNameContainingHyphensIsTreatedAsAHostName() {
        final AddressFilter filter = allowing("my-internal-host.example.invalid");

        assertTrue(filter.needsPeriodicRefresh(),
            "a hyphenated host name should be kept as a host name, not parsed as a range");
    }

    @Test
    public void rangeIsStillRecognisedAsARange() {
        final AddressFilter filter = denying("1.2.3.10-1.2.3.20");

        assertFalse(filter.needsPeriodicRefresh());
        assertFalse(filter.isAllowed(ip("1.2.3.15")));
    }

    @Test
    public void unparsableAllowListDeniesAll() {
        final AddressFilter filter = allowing("10.0.0.0/33", "not an address");

        assertFalse(filter.isAllowed(ip("8.8.8.8")));
        assertFalse(filter.isAllowed(ip("169.254.169.254")));
        assertFalse(filter.isAllowed(ip("10.0.0.1")));
    }

    @Test
    public void allowListOfOnlyBlanksAndCommentsIsNotAnAllowList() {
        final AddressFilter filter = allowing("", "  ", "# nothing here");

        assertTrue(filter.isAllowed(ip("8.8.8.8")));
    }

    @Test
    public void partlyValidAllowListStillAppliesTheValidEntries() {
        final AddressFilter filter = allowing("8.8.8.8", "garbage/99");

        assertTrue(filter.isAllowed(ip("8.8.8.8")));
        assertFalse(filter.isAllowed(ip("1.1.1.1")));
    }

    @Test
    public void hostNameRulesAreRecognisedAsNeedingRefresh() {
        final AddressFilter withNames = allowing("example.invalid");
        final AddressFilter withoutNames = denying("10.0.0.0/8");

        assertTrue(withNames.needsPeriodicRefresh());
        assertFalse(withoutNames.needsPeriodicRefresh());
    }

    @Test
    public void denyHostNameThatDoesNotResolveRefusesToStart() {
        assertThrows(IllegalArgumentException.class, () -> denying("this-name-does-not-exist.invalid"));
    }

    @Test
    public void allowHostNameThatDoesNotResolveMatchesNothing() {
        final AddressFilter filter = allowing("this-name-does-not-exist.invalid");

        assertFalse(filter.isAllowed(ip("8.8.8.8")));
    }
}

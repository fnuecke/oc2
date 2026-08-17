/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity.robot;

public record RobotActionCompletedEvent(int actionId, RobotActionResult result) {
    public static final String TYPE = "robotActionCompleted";
}

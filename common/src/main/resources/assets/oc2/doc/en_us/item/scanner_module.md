# Scanner Module
![That's inside the room!](item:oc2:scanner_module)

The scanner module lets [robots](robot.md) inspect what is around them. Where the robot's built-in `detect` only rudimentary data, the scanner can provide specifics about the block, the fluid and any creatures and objects.

It can also scan the wider area around the robot, measuring block hardness and detecting entities in a 7x7x7 cube around the robot. This has been successfully employed find cavities and ore deposits.

Scan results are relative to the robot and aligned to the built-in compass. Also see the "Position" section of the [robot device](../device/robot.md).

## API
Programs control the module through the `scanner` device. See the [scanner device](../device/scanner.md) reference for its methods and the layout of scan results.

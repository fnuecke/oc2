# Projector
![Shadow on the wall](block:oc2:projector)

The projector provides a framebuffer device to [computers](computer.md). They have a resolution of 640 by 480 pixels, with the color format r5g6b5: 5 bits for the red color component, 6 bits for the green color component and 5 bits for the blue color component.

Projectors need to be powered directly to function. The device bus cannot provide enough energy on its own. When insufficiently powered, this is indicated by a red glow in the projector's lens.

On OpenOS II and Linux, projectors appear as `/dev/fbX` devices. To send data to the framebuffer, write to these devices. For example, to clear a framebuffer, write 614400 zeros to it, two bytes for each pixel: `head -c 614400 /dev/zero > /dev/fb0`. CP/M does not support projectors.

On Linux, the first connected framebuffer shows a virtual terminal, and a [keyboard](keyboard.md) may be used to send input to it.

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.

# Sound Card
![Less sound of silence](item:oc2:sound_card)

The sound card provides a means for computers to emit arbitrary audio signals.

On a Linux system, sound cards will typically appear as `/dev/dsp` devices. To send data to the sounds card, it is possible to write to this device. The simplest way to play audio is to write raw samples to `/dev/dsp` like so: `cat sound.raw > /dev/dsp`

Opened like this, `/dev/dsp` expects mono audio at 8000 samples per second, one unsigned byte per sample. Audio in any other format has to be converted first, or the program playing it has to configure the device to match.

The card supports mono audio at up to 22050 samples per second.

Computers have to be shut down before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.

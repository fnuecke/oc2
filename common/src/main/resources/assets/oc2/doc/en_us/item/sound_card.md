# Sound Card
![Less sound of silence](item:oc2:sound_card)

The sound card provides a means for computers to emit arbitrary audio signals. The card supports mono audio at up to 22050 samples per second. CP/M does not support it.

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.

## OpenOS II
See the [OpenOS II guide](../systems/openos2.md) for general usage.

The first sound card provides three devices, all playing mono audio at 11025 samples per second:
- `/dev/dsp` takes one unsigned byte per sample.
- `/dev/dspW` takes signed 16-bit samples, little endian.
- `/dev/audio` takes one mu-law encoded byte per sample.

The format and rate cannot be changed. Audio must be 11025 samples per second. Raw samples can be written to the devices directly, e.g.:  
`cat sound.raw > /dev/dspW`

## Linux
See the [Linux guide](../systems/linux.md) for general usage.

Sound cards typically appear as `/dev/dsp` devices. The simplest way to play audio is to write raw samples to the device:  
`cat sound.raw > /dev/dsp`

Opened like this, `/dev/dsp` expects mono audio at 8000 samples per second, one unsigned byte per sample. Audio in any other format has to be converted first, or the program playing it has to configure the device to match.

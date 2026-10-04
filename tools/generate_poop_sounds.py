import math
import random
import struct
import wave
from pathlib import Path


SAMPLE_RATE = 22_050
DURATION_SECONDS = 2.35
FART_DURATION_SECONDS = 1.05


def smooth_noise(rng: random.Random, state: list[float], response: float) -> float:
    state[0] += (rng.uniform(-1.0, 1.0) - state[0]) * response
    return state[0]


def flush_sound(time: float, rng: random.Random, water_state: list[float]) -> float:
    attack = min(1.0, time / 0.045)
    tail = max(0.0, 1.0 - time / DURATION_SECONDS)
    rush_envelope = attack * tail**0.75

    water = smooth_noise(rng, water_state, 0.24)
    low_water = smooth_noise(rng, water_state, 0.035)
    swirl_rate = 7.5 + time * 3.2
    swirl = math.sin(2.0 * math.pi * swirl_rate * time) * 0.16
    rush = (water * 0.72 + low_water * 0.34 + swirl) * rush_envelope

    bowl_tone_frequency = 155.0 - time * 37.0
    bowl_tone = (
        math.sin(2.0 * math.pi * bowl_tone_frequency * time)
        * 0.16
        * attack
        * tail**1.5
    )

    gurgle = 0.0
    for index, start in enumerate((0.78, 1.09, 1.38, 1.68)):
        offset = time - start
        if 0.0 <= offset < 0.24:
            envelope = math.sin(math.pi * offset / 0.24) ** 2
            frequency = 74.0 + index * 9.0 + 22.0 * math.sin(offset * 24.0)
            gurgle += math.sin(2.0 * math.pi * frequency * offset) * envelope * 0.34

    drain_offset = time - 1.62
    drain = 0.0
    if drain_offset >= 0.0:
        drain_tail = max(0.0, 1.0 - drain_offset / 0.73)
        drain = (
            math.sin(2.0 * math.pi * (235.0 + drain_offset * 390.0) * drain_offset)
            * drain_tail**2
            * 0.12
        )

    return rush + bowl_tone + gurgle + drain


def fart_sound(time: float, rng: random.Random, state: list[float]) -> float:
    attack = min(1.0, time / 0.018)
    tail = max(0.0, 1.0 - time / FART_DURATION_SECONDS)
    sputter = 0.45 + 0.55 * max(0.0, math.sin(2.0 * math.pi * 13.5 * time))
    wobble = 1.0 + 0.19 * math.sin(2.0 * math.pi * 8.2 * time)
    frequency = (72.0 - 19.0 * time) * wobble
    body = math.sin(2.0 * math.pi * frequency * time)
    roughness = smooth_noise(rng, state, 0.31)
    pop = 0.0
    for start in (0.0, 0.19, 0.41, 0.67):
        offset = time - start
        if 0.0 <= offset < 0.13:
            pop_envelope = math.sin(math.pi * offset / 0.13) ** 2
            pop += math.sin(2.0 * math.pi * (58.0 + 30.0 * offset) * offset) * pop_envelope
    return attack * tail**1.35 * sputter * (body * 0.72 + roughness * 0.32 + pop * 0.26)


def render(path: Path, duration: float, generator, seed: int) -> None:
    rng = random.Random(seed)
    state = [0.0]
    samples = [
        generator(index / SAMPLE_RATE, rng, state)
        for index in range(int(duration * SAMPLE_RATE))
    ]
    peak = max(abs(value) for value in samples) or 1.0
    gain = 0.88 / peak
    pcm = b"".join(
        struct.pack("<h", int(max(-1.0, min(1.0, value * gain)) * 32_767))
        for value in samples
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(pcm)


def main() -> None:
    raw = Path("app/src/main/res/raw")
    render(raw / "toilet_flush.wav", DURATION_SECONDS, flush_sound, seed=4_242)
    render(raw / "poop_fart.wav", FART_DURATION_SECONDS, fart_sound, seed=8_008)


if __name__ == "__main__":
    main()

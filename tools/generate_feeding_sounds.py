import math
import random
import struct
import wave
from pathlib import Path


SAMPLE_RATE = 22_050


def envelope(time: float, start: float, duration: float, attack: float = 0.015) -> float:
    position = (time - start) / duration
    if position < 0 or position >= 1:
        return 0.0
    attack_part = min(1.0, position / max(attack / duration, 0.001))
    return attack_part * (1.0 - position) ** 2


def filtered_noise(rng: random.Random, state: list[float], smoothing: float) -> float:
    value = rng.uniform(-1.0, 1.0)
    state[0] += (value - state[0]) * smoothing
    return state[0]


def ham_sound(time: float, rng: random.Random, state: list[float]) -> float:
    sample = 0.0
    for index, start in enumerate((0.04, 0.32, 0.61, 0.91)):
        duration = 0.19 if index == 0 else 0.16
        env = envelope(time, start, duration)
        crunch = rng.uniform(-1.0, 1.0) * (0.9 if index == 0 else 0.55)
        chew = math.sin(2 * math.pi * (115 + index * 9) * time) * 0.22
        sample += env * (crunch + chew)
    return sample


def melon_sound(time: float, rng: random.Random, state: list[float]) -> float:
    sample = 0.0
    bite = envelope(time, 0.03, 0.24)
    watery_noise = filtered_noise(rng, state, 0.18)
    sample += bite * (watery_noise * 0.85 + math.sin(2 * math.pi * 185 * time) * 0.2)
    for index, start in enumerate((0.36, 0.69, 1.02)):
        env = envelope(time, start, 0.24, attack=0.03)
        squish = filtered_noise(rng, state, 0.055) * 0.55
        mouth = math.sin(2 * math.pi * (82 + index * 7) * time) * 0.28
        sample += env * (squish + mouth)
    slurp_env = envelope(time, 1.15, 0.25, attack=0.04)
    slurp_frequency = 210 + max(0.0, time - 1.15) * 420
    sample += slurp_env * math.sin(2 * math.pi * slurp_frequency * time) * 0.3
    return sample


def render(path: Path, duration: float, generator, seed: int) -> None:
    rng = random.Random(seed)
    state = [0.0]
    samples = [generator(index / SAMPLE_RATE, rng, state) for index in range(int(duration * SAMPLE_RATE))]
    peak = max(abs(value) for value in samples) or 1.0
    gain = 0.88 / peak
    pcm = b"".join(
        struct.pack("<h", int(max(-1.0, min(1.0, value * gain)) * 32767))
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
    render(raw / "eat_ham.wav", 1.25, ham_sound, seed=731)
    render(raw / "eat_melon.wav", 1.48, melon_sound, seed=947)


if __name__ == "__main__":
    main()

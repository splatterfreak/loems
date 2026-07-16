import math
import random
import struct
import wave
from pathlib import Path


RATE = 22_050


def env(t: float, duration: float, attack: float = 0.01) -> float:
    if t < 0 or t >= duration:
        return 0.0
    return min(1.0, t / attack) * (1.0 - t / duration) ** 2


def render(path: Path, duration: float, sample_fn, seed: int) -> None:
    rng = random.Random(seed)
    samples = [sample_fn(i / RATE, rng) for i in range(int(duration * RATE))]
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
        output.setframerate(RATE)
        output.writeframes(pcm)


def fire_shot(t: float, rng: random.Random) -> float:
    return env(t, 0.62) * (rng.uniform(-1, 1) * 0.65 + math.sin(2 * math.pi * (180 + 420 * t) * t) * 0.4)


def fire_hit(t: float, rng: random.Random) -> float:
    boom = math.sin(2 * math.pi * 72 * t) * env(t, 0.7, 0.004)
    crackle = rng.uniform(-1, 1) * env(t, 0.48, 0.002)
    return boom * 0.65 + crackle * 0.7


def water_shot(t: float, rng: random.Random) -> float:
    wobble = math.sin(2 * math.pi * (260 - 90 * t) * t)
    return env(t, 0.7, 0.025) * (wobble * 0.5 + rng.uniform(-1, 1) * 0.3)


def water_hit(t: float, rng: random.Random) -> float:
    splash = rng.uniform(-1, 1) * env(t, 0.75, 0.006)
    bubble = math.sin(2 * math.pi * (130 + 90 * t) * t) * env(t, 0.55, 0.02)
    return splash * 0.65 + bubble * 0.4


def wind_shot(t: float, rng: random.Random) -> float:
    whistle = math.sin(2 * math.pi * (430 + 520 * t) * t)
    return env(t, 0.72, 0.08) * (whistle * 0.55 + rng.uniform(-1, 1) * 0.18)


def wind_hit(t: float, rng: random.Random) -> float:
    gust = rng.uniform(-1, 1) * env(t, 0.68, 0.04)
    thump = math.sin(2 * math.pi * 95 * t) * env(t, 0.38, 0.005)
    return gust * 0.55 + thump * 0.5


def earth_shot(t: float, rng: random.Random) -> float:
    rumble = math.sin(2 * math.pi * (78 + 25 * t) * t)
    scrape = rng.uniform(-1, 1) * env(t, 0.5, 0.01)
    return env(t, 0.66, 0.008) * rumble * 0.7 + scrape * 0.32


def earth_hit(t: float, rng: random.Random) -> float:
    impact = math.sin(2 * math.pi * 62 * t) * env(t, 0.58, 0.003)
    debris = rng.uniform(-1, 1) * env(t - 0.045, 0.58, 0.005)
    return impact * 0.8 + debris * 0.42


def battle_victory(t: float, rng: random.Random) -> float:
    notes = (261.63, 329.63, 392.00, 523.25)
    note_index = min(int(t / 0.48), len(notes) - 1)
    note_t = t - note_index * 0.48
    tone = math.sin(2 * math.pi * notes[note_index] * note_t)
    shimmer = math.sin(2 * math.pi * notes[note_index] * 2 * note_t) * 0.25
    fanfare = env(note_t, 0.75, 0.018) * (tone + shimmer)
    final_chord = 0.0
    if t >= 1.55:
        chord_t = t - 1.55
        final_chord = env(chord_t, 1.45, 0.025) * sum(
            math.sin(2 * math.pi * frequency * chord_t)
            for frequency in (261.63, 329.63, 392.00)
        ) / 3
    sparkle = rng.uniform(-1, 1) * env(t - 2.05, 0.95, 0.01) * 0.06
    return fanfare * 0.65 + final_chord * 0.8 + sparkle


def battle_defeat(t: float, rng: random.Random) -> float:
    notes = (293.66, 246.94, 196.00, 146.83)
    note_index = min(int(t / 0.62), len(notes) - 1)
    note_t = t - note_index * 0.62
    frequency = notes[note_index] * (1.0 - 0.055 * note_t)
    tone = math.sin(2 * math.pi * frequency * note_t)
    hollow = math.sin(2 * math.pi * frequency * 0.5 * note_t) * 0.38
    phrase = env(note_t, 0.88, 0.025) * (tone + hollow)
    low_tail = math.sin(2 * math.pi * 73.42 * (t - 2.05)) * env(t - 2.05, 0.95, 0.03)
    noise = rng.uniform(-1, 1) * env(t - 2.1, 0.9, 0.02) * 0.035
    return phrase * 0.72 + low_tail * 0.45 + noise


def main() -> None:
    raw = Path("app/src/main/res/raw")
    sounds = {
        "battle_fire_shot.wav": (0.62, fire_shot),
        "battle_fire_hit.wav": (0.70, fire_hit),
        "battle_water_shot.wav": (0.70, water_shot),
        "battle_water_hit.wav": (0.75, water_hit),
        "battle_wind_shot.wav": (0.72, wind_shot),
        "battle_wind_hit.wav": (0.68, wind_hit),
        "battle_earth_shot.wav": (0.66, earth_shot),
        "battle_earth_hit.wav": (0.68, earth_hit),
        "battle_victory.wav": (3.00, battle_victory),
        "battle_defeat.wav": (3.00, battle_defeat),
    }
    for index, (name, (duration, generator)) in enumerate(sounds.items()):
        render(raw / name, duration, generator, seed=1_200 + index)


if __name__ == "__main__":
    main()

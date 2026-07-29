from pathlib import Path
from PIL import Image, ImageDraw

SCALE = 2048 / 108


def cubic(p0, p1, p2, p3, steps=32):
    points = []
    for index in range(1, steps + 1):
        t = index / steps
        u = 1 - t
        points.append(
            (
                u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0],
                u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1],
            )
        )
    return points


def scaled(points):
    return [(round(x * SCALE), round(y * SCALE)) for x, y in points]


def render():
    image = Image.new("RGB", (2048, 2048), "#F7F3EA")
    draw = ImageDraw.Draw(image)

    w = [(24, 40)]
    w += cubic(w[-1], (29, 50), (33, 62), (39, 74))
    w += cubic(w[-1], (42, 80), (48, 80), (51, 73))
    w += [(58, 57), (65, 73)]
    w += cubic(w[-1], (68, 80), (74, 80), (78, 73))
    w += cubic(w[-1], (84, 61), (88, 49), (91, 37))
    w += [(82, 34)]
    w += cubic(w[-1], (79, 45), (76, 55), (71, 65))
    w += [(62, 43)]
    w += cubic(w[-1], (60, 38), (55, 38), (53, 43))
    w += [(44, 65)]
    w += cubic(w[-1], (40, 55), (37, 46), (33, 36))
    draw.polygon(scaled(w), fill="#1C1B19")

    drop = [(73, 20)]
    drop += cubic(drop[-1], (73, 20), (65, 29), (65, 35))
    drop += cubic(drop[-1], (65, 40), (68.8, 44), (73.5, 44))
    drop += cubic(drop[-1], (78.2, 44), (82, 40), (82, 35))
    drop += cubic(drop[-1], (82, 29), (73, 20), (73, 20))
    draw.polygon(scaled(drop), fill="#B74432")

    output = image.resize((512, 512), Image.Resampling.LANCZOS)
    destination = Path(__file__).resolve().parents[1] / "branding" / "play-store-icon.png"
    output.save(destination, format="PNG", optimize=True)
    print(destination)


if __name__ == "__main__":
    render()


"""
워드마크가 한 획으로 써지는 Lottie 를 만든다.

입력은 앱의 워드마크 벡터(`core/ui/.../drawable/img_laimory_wordmark.xml`)이고, 출력은 Lottie JSON 이다.
로고 모양을 바꾸면 이 스크립트로 다시 만든다 — 손으로 JSON 을 고치지 않는다.

    python3 -m venv .venv && .venv/bin/pip install numpy scikit-image
    .venv/bin/python tools/lottie/wordmark_writing.py

만드는 방법:
1. 워드마크 경로(M · C · L · Z)를 래스터로 칠하고 골격선을 뽑아 획의 중심선을 얻는다.
2. 중심선을 손글씨 순서(ORDER)로 이어 열린 경로 하나로 만든다.
3. 그 경로를 굵은 선 + 트림으로 그리고, 원본 워드마크 채움의 알파 매트로 쓴다. 중심선이 조금 어긋나도
   보이는 윤곽은 원본 그대로다.

구간(마커): intro = 써지기 + i 점 자리잡기, shimmer = 빛 띠, erase = 지우기. 마지막 프레임은 빈 화면이다.
"""

import json
import math
import re
from pathlib import Path

import numpy as np
from skimage.draw import polygon as fill_polygon
from skimage.morphology import skeletonize

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'core/ui/src/main/res/drawable/img_laimory_wordmark.xml'
OUT = ROOT / 'core/ui/src/main/res/raw/lottie_wordmark_writing.json'

FPS = 60
T_DRAW0, T_DRAW1 = 0, 72
T_DOT0 = 62
T_DOT_SETTLED = T_DOT0 + 30
T_SHIM0, T_SHIM1 = 100, 150
T_DOTOUT0 = 160
T_ERASE0, T_ERASE1 = 166, 214
T_END = 230
# 펜 굵기. 로고 획(약 14.6)보다 조금 굵어야 끝이 덜 칠해지지 않고, 너무 굵으면 옆 획이 먼저 드러난다.
STROKE_W = 17
C0 = (0x87 / 255, 0x9B / 255, 0xD3 / 255)
C1 = (0xDC / 255, 0xE3 / 255, 0xF4 / 255)
DOT = (136.24, 42.98)
RASTER_SCALE = 10

# 골격선의 끝점 · 갈림점(워드마크 좌표). 쓰는 순서를 이 이름으로 적는다.
NODES = {
    'l_top': (56.0, 28.7),
    'a_join': (83.5, 88.0),
    'a_tail': (108.7, 92.3),
    'm_join1': (181.6, 67.3),
    'm_leg1': (181.6, 93.1),
    'm_join2': (217.6, 70.1),
    'm_leg2': (217.3, 91.9),
    'r_end': (244.9, 59.6),
}
# (시작, 끝, 되짚기). 되짚기는 이미 그린 자리를 되돌아가는 구간이라 빠르게 지나간다.
ORDER = [
    ('l_top', 'a_join', False),     # l: 위에서 아래로, 바닥에서 a 쪽으로
    ('a_join', 'a_tail', False),    # a 꼬리
    ('a_tail', 'a_join', True),     # 꼬리에서 되돌아옴
    ('a_join', 'm_join1', False),   # a 몸통 → i 기둥 → m 첫 봉우리
    ('m_join1', 'm_leg1', False),   # m 첫 다리 내려감
    ('m_leg1', 'm_join1', True),    # 다시 올라감
    ('m_join1', 'm_join2', False),  # m 둘째 봉우리
    ('m_join2', 'm_leg2', False),   # m 둘째 다리 내려감
    ('m_leg2', 'm_join2', True),    # 다시 올라감
    ('m_join2', 'r_end', False),    # r 팔
]
BACKTRACK_WEIGHT = 0.12


# ---- 워드마크 경로 ----

def read_paths():
    return re.findall(r'pathData="([^"]*)"', SRC.read_text())


def parse(d):
    """M · C · L · Z 절대 명령만 있는 경로 → 닫힌 윤곽 목록. 윤곽은 3차 베지어 [(p0, c1, c2, p3)]."""
    toks = re.findall(r'[MCLZ]|-?\d*\.?\d+(?:e-?\d+)?', d)
    i = 0
    contours, cur, pos, start, cmd = [], None, None, None, None

    def num():
        nonlocal i
        i += 1
        return float(toks[i - 1])

    while i < len(toks):
        if toks[i] in 'MCLZ':
            cmd = toks[i]
            i += 1
        if cmd == 'M':
            pos = start = (num(), num())
            cur = []
            contours.append(cur)
            cmd = 'L'
        elif cmd == 'C':
            c1, c2, p = (num(), num()), (num(), num()), (num(), num())
            cur.append((pos, c1, c2, p))
            pos = p
        elif cmd == 'L':
            p = (num(), num())
            cur.append((pos, pos, p, p))
            pos = p
        elif cmd == 'Z':
            if pos != start:
                cur.append((pos, pos, start, start))
            pos = start
    return contours


def bezier(seg, t):
    a, b, c, d = seg
    u = 1 - t
    return tuple(u ** 3 * a[k] + 3 * u * u * t * b[k] + 3 * u * t * t * c[k] + t ** 3 * d[k] for k in (0, 1))


# ---- 중심선 ----

def skeleton(contours):
    k = RASTER_SCALE
    img = np.zeros((120 * k, 300 * k), bool)
    for contour in contours:
        pts = [bezier(s, j / 20) for s in contour for j in range(20)]
        rr, cc = fill_polygon([p[1] * k for p in pts], [p[0] * k for p in pts], img.shape)
        img[rr, cc] = True
    return skeletonize(img)


def skeleton_edges(sk):
    """골격선을 노드(끝점 · 갈림점 뭉치) 사이 간선으로 나눈다. 간선은 워드마크 좌표 점 목록."""
    pix = set(zip(*np.nonzero(sk)))

    def nbrs(p):
        y, x = p
        return [(y + dy, x + dx) for dy in (-1, 0, 1) for dx in (-1, 0, 1)
                if (dy or dx) and (y + dy, x + dx) in pix]

    special = {p for p in pix if len(nbrs(p)) != 2}
    node_of = {}
    for p in special:
        if p in node_of:
            continue
        node_of[p] = p
        stack = [p]
        while stack:
            q = stack.pop()
            for r in nbrs(q):
                if r in special and r not in node_of:
                    node_of[r] = p
                    stack.append(r)
    edges, seen = [], set()
    for p in special:
        for q in nbrs(p):
            if q in node_of or (p, q) in seen:
                continue
            path, prev, cur = [p, q], p, q
            while cur not in node_of:
                nxt = [r for r in nbrs(cur) if r != prev and r not in path[-3:]]
                if not nxt:
                    break
                prev, cur = cur, nxt[0]
                path.append(cur)
            seen.add((p, q))
            seen.add((path[-1], path[-2]))
            edges.append([(x / RASTER_SCALE, y / RASTER_SCALE) for y, x in path])
    return edges


def edge_between(edges, a, b):
    pa, pb = NODES[a], NODES[b]
    for e in edges:
        if math.dist(e[0], pa) < 3 and math.dist(e[-1], pb) < 3:
            return e
        if math.dist(e[-1], pa) < 3 and math.dist(e[0], pb) < 3:
            return e[::-1]
    raise SystemExit(f'골격선에서 {a} → {b} 간선을 찾지 못함. 로고가 바뀌었으면 NODES 를 다시 잡는다.')


def smooth(p, step=8, win=2):
    q = p[::step]
    if q[-1] != p[-1]:
        q.append(p[-1])
    out = []
    for k in range(len(q)):
        if k < win or k >= len(q) - win:
            out.append(q[k])
            continue
        xs = q[k - win:k + win + 1]
        out.append((sum(a for a, _ in xs) / len(xs), sum(b for _, b in xs) / len(xs)))
    return out


def writing_path(edges):
    """쓰는 순서대로 이은 점 목록과, 점 사이 구간마다 (길이, 시간 가중치)."""
    pts, weights = [], []
    for a, b, back in ORDER:
        s = smooth(edge_between(edges, a, b))
        if pts and math.dist(pts[-1], s[0]) < 2:
            s = s[1:]
        start = len(pts)
        pts.extend(s)
        for k in range(max(start - 1, 0), len(pts) - 1):
            weights.append((math.dist(pts[k], pts[k + 1]), BACKTRACK_WEIGHT if back else 1.0))
    return pts, weights


# ---- Lottie ----

def static(v):
    return {'a': 0, 'k': v}


def anim(ks):
    ks[-1] = {'t': ks[-1]['t'], 's': ks[-1]['s']}
    return {'a': 1, 'k': ks}


def linear():
    return {'i': {'x': [1], 'y': [1]}, 'o': {'x': [0], 'y': [0]}}


def ease_in_out(n=1):
    return {'i': {'x': [0.4] * n, 'y': [1] * n}, 'o': {'x': [0.6] * n, 'y': [0] * n}}


def transform(a=(0, 0), p=(0, 0), s=(100, 100), o=100, r=0):
    def prop(v, pad):
        return v if isinstance(v, dict) else static(list(v) + pad if isinstance(v, tuple) else v)
    return {'a': prop(a, [0]), 'p': prop(p, [0]), 's': prop(s, [100]), 'r': prop(r, []), 'o': prop(o, [])}


def shape_transform():
    return {'ty': 'tr', 'p': static([0, 0]), 'a': static([0, 0]), 's': static([100, 100]), 'r': static(0),
            'o': static(100), 'sk': static(0), 'sa': static(0)}


def bezier_shape(contour, name):
    n = len(contour)
    v = [list(p0) for (p0, _, _, _) in contour]
    o = [[c1[0] - p0[0], c1[1] - p0[1]] for (p0, c1, _, _) in contour]
    i = [None] * n
    for k, (_, _, c2, p3) in enumerate(contour):
        i[(k + 1) % n] = [c2[0] - p3[0], c2[1] - p3[1]]
    return {'ty': 'sh', 'nm': name, 'ks': static({'c': True, 'v': v, 'i': i, 'o': o})}


def brand_gradient(rule):
    return {'ty': 'gf', 'nm': 'brand gradient', 'o': static(100), 'r': rule, 't': 1,
            's': static([49, 60.5]), 'e': static([251, 60.5]),
            'g': {'p': 2, 'k': static([0, *C0, 1, *C1])}}


def layer(ind, name, shapes, ks=None, **kw):
    out = {'ddd': 0, 'ind': ind, 'ty': 4, 'nm': name, 'sr': 1, 'ks': ks or transform(), 'ao': 0,
           'shapes': shapes, 'ip': 0, 'op': T_END, 'st': 0, 'bm': 0}
    out.update(kw)
    return out


def group(name, items):
    return {'ty': 'gr', 'nm': name, 'it': items + [shape_transform()]}


def build():
    logo_d, dot_d = read_paths()
    logo = parse(logo_d)[0]
    dot_contour = parse(dot_d)[0]
    pts, weights = writing_path(skeleton_edges(skeleton([logo, dot_contour])))
    total_len = sum(length for length, _ in weights)
    total_w = sum(length * w for length, w in weights)

    def length_fraction(wfrac):
        """시간 가중 진행률 → 실제 길이 비율(트림 퍼센트)."""
        target, acc_w, acc_l = wfrac * total_w, 0, 0
        for length, w in weights:
            if acc_w + length * w >= target:
                return (acc_l + (target - acc_w) / w) / total_len
            acc_w += length * w
            acc_l += length
        return 1.0

    def smoothstep(t):
        return t * t * (3 - 2 * t)

    def track(t0, t1, step=2):
        frames = list(range(t0, t1, step)) + [t1]
        return [{'t': f, 's': [round(100 * length_fraction(smoothstep((f - t0) / (t1 - t0))), 3)], **linear()}
                for f in frames]

    # 1) 써지는 획. 아래 워드마크 채움의 알파 매트다.
    trim_e = anim(track(T_DRAW0, T_DRAW1))
    trim_s = anim([{'t': 0, 's': [0], 'h': 1}] + track(T_ERASE0, T_ERASE1))
    # 길이 0 인 트림에서 둥근 끝이 점으로 찍히지 않게 시작 · 끝 프레임은 선을 숨긴다.
    pen_o = anim([{'t': 0, 's': [0], 'h': 1}, {'t': 1, 's': [100], 'h': 1}, {'t': T_ERASE1, 's': [0], 'h': 1},
                  {'t': T_END, 's': [0]}])
    centerline = {'ty': 'sh', 'nm': 'centerline', 'ks': static({
        'c': False, 'v': [[round(x, 2), round(y, 2)] for x, y in pts],
        'i': [[0, 0]] * len(pts), 'o': [[0, 0]] * len(pts)})}
    write_matte = layer(4, 'write matte', [group('write', [
        centerline,
        {'ty': 'tm', 'nm': 'write trim', 's': trim_s, 'e': trim_e, 'o': static(0), 'm': 1},
        {'ty': 'st', 'nm': 'pen', 'c': static([1, 1, 1, 1]), 'o': pen_o, 'w': static(STROKE_W),
         'lc': 2, 'lj': 2, 'ml': 4},
    ])], td=1)
    wordmark = layer(5, 'wordmark', [group('laimr', [bezier_shape(logo, 'laimr'), brand_gradient(2)])], tt=1)

    # 2) i 의 점. 위에서 떨어져 한 번 튕긴 뒤 자리 잡고, 지우기 직전에 톡 사라진다.
    dx, dy = DOT
    no_tangent = {'to': [0, 0, 0], 'ti': [0, 0, 0]}
    dot_p = anim([
        {'t': 0, 's': [dx, dy - 34, 0], 'h': 1},
        {'t': T_DOT0, 's': [dx, dy - 34, 0], 'i': {'x': [0.6], 'y': [1]}, 'o': {'x': [0.5], 'y': [0]}, **no_tangent},
        {'t': T_DOT0 + 14, 's': [dx, dy + 1.5, 0], 'i': {'x': [0.4], 'y': [1]}, 'o': {'x': [0.3], 'y': [0]},
         **no_tangent},
        {'t': T_DOT0 + 22, 's': [dx, dy - 5, 0], 'i': {'x': [0.5], 'y': [1]}, 'o': {'x': [0.5], 'y': [0]},
         **no_tangent},
        {'t': T_DOT_SETTLED, 's': [dx, dy, 0]},
    ])
    dot_s = anim([
        {'t': 0, 's': [80, 80, 100], **ease_in_out(3)},
        {'t': T_DOT0 + 12, 's': [90, 112, 100], **ease_in_out(3)},
        {'t': T_DOT0 + 15, 's': [122, 80, 100], **ease_in_out(3)},
        {'t': T_DOT0 + 21, 's': [94, 106, 100], **ease_in_out(3)},
        {'t': T_DOT_SETTLED, 's': [100, 100, 100], 'h': 1},
        {'t': T_DOTOUT0, 's': [100, 100, 100], **ease_in_out(3)},
        {'t': T_DOTOUT0 + 5, 's': [118, 118, 100], **ease_in_out(3)},
        {'t': T_DOTOUT0 + 14, 's': [0, 0, 100]},
    ])
    dot_o = anim([
        {'t': 0, 's': [0], 'h': 1},
        {'t': T_DOT0, 's': [0], **linear()},
        {'t': T_DOT0 + 5, 's': [100], 'h': 1},
        {'t': T_DOTOUT0 + 14, 's': [0]},
    ])
    dot = layer(3, 'i dot', [group('dot', [bezier_shape(dot_contour, 'dot'), brand_gradient(1)])],
                ks=transform(a=DOT, p=dot_p, s=dot_s, o=dot_o))

    # 3) 다 써진 로고 위로 빛 띠가 한 번 지나간다. 워드마크 사본이 매트다.
    shimmer_matte = layer(1, 'shimmer matte', [
        group('laimr', [bezier_shape(logo, 'laimr'), brand_gradient(2)]),
        group('dot', [bezier_shape(dot_contour, 'dot'), brand_gradient(1)]),
    ], td=1)
    band_p = anim([
        {'t': 0, 's': [20, 60, 0], 'h': 1},
        {'t': T_SHIM0, 's': [20, 60, 0], 'i': {'x': [0.45], 'y': [1]}, 'o': {'x': [0.55], 'y': [0]}, **no_tangent},
        {'t': T_SHIM1, 's': [290, 60, 0]},
    ])
    band = layer(2, 'shimmer band', [group('band', [
        {'ty': 'rc', 'nm': 'band', 'd': 1, 's': static([44, 220]), 'p': static([0, 0]), 'r': static(0)},
        {'ty': 'gf', 'nm': 'glow', 'o': static(100), 'r': 1, 't': 1, 's': static([-22, 0]), 'e': static([22, 0]),
         'g': {'p': 3, 'k': static([0, 1, 1, 1, 0.5, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0.5, 0.75, 1, 0])}},
    ])], ks=transform(p=band_p, r=18), tt=1)

    return {
        'v': '5.7.4', 'fr': FPS, 'ip': 0, 'op': T_END, 'w': 300, 'h': 120, 'nm': 'Laimory wordmark writing',
        'ddd': 0, 'assets': [],
        'layers': [shimmer_matte, band, dot, write_matte, wordmark],
        'markers': [
            {'tm': T_DRAW0, 'cm': 'intro', 'dr': T_DOT_SETTLED - T_DRAW0},
            {'tm': T_SHIM0, 'cm': 'shimmer', 'dr': T_SHIM1 - T_SHIM0},
            {'tm': T_DOTOUT0, 'cm': 'erase', 'dr': T_END - T_DOTOUT0},
        ],
    }


if __name__ == '__main__':
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(build(), separators=(',', ':')))
    print(f'{OUT.relative_to(ROOT)}: {OUT.stat().st_size} bytes')

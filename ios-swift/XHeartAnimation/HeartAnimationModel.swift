import CoreGraphics
import Foundation

/// Numeric values translated from src/components/x-heart-animation.
enum HeartAnimationModel {
    static let canvasSize: CGFloat = 58
    static let center: CGFloat = canvasSize / 2
    static let heartSize: CGFloat = 26
    static let groupRadius = 20.0
    static let stagger = 0.08
    static let gray = RGBColor(hex: 0xAAB8C2)
    static let red = RGBColor(hex: 0xE2264D)

    // Keep the exact relative SVG coordinates instead of using an SF Symbol.
    static let heartPath: CGPath = {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: 20.884, y: 13.19))

        func cubic(_ x1: CGFloat, _ y1: CGFloat,
                   _ x2: CGFloat, _ y2: CGFloat,
                   _ x: CGFloat, _ y: CGFloat) {
            let p = path.currentPoint
            path.addCurve(to: CGPoint(x: p.x + x, y: p.y + y),
                          control1: CGPoint(x: p.x + x1, y: p.y + y1),
                          control2: CGPoint(x: p.x + x2, y: p.y + y2))
        }

        func line(_ x: CGFloat, _ y: CGFloat) {
            let p = path.currentPoint
            path.addLine(to: CGPoint(x: p.x + x, y: p.y + y))
        }

        cubic(-1.351, 2.48, -4.001, 5.12, -8.379, 7.67)
        line(-0.503, 0.3)
        line(-0.504, -0.3)
        cubic(-4.379, -2.55, -7.029, -5.19, -8.382, -7.67)
        cubic(-1.36, -2.5, -1.41, -4.86, -0.514, -6.67)
        cubic(0.887, -1.79, 2.647, -2.91, 4.601, -3.01)
        cubic(1.651, -0.09, 3.368, 0.56, 4.798, 2.01)
        cubic(1.429, -1.45, 3.146, -2.1, 4.796, -2.01)
        cubic(1.954, 0.1, 3.714, 1.22, 4.601, 3.01)
        cubic(0.896, 1.81, 0.846, 4.17, -0.514, 6.67)
        path.closeSubpath()
        return path
    }()

    static let particles: [Particle] = {
        // Each row is [first.start, first.end, second.start, second.end].
        let colors: [[UInt32]] = [
            [0x8CE8C3, 0xA068CE, 0x8BE7C2, 0xB752E1],
            [0x90D2FA, 0x99E9C8, 0x91D1F9, 0xBAE3D7],
            [0xCC8EF5, 0xD3F491, 0xCB8DF4, 0xDCE483],
            [0x8CE8C3, 0x59C392, 0x8CE8C3, 0x67CD9F],
            [0xF58EA7, 0xCAADC7, 0xF48DA6, 0x959FF3],
            [0x91D2FA, 0xCA5ED8, 0x91D2FA, 0xA975D1],
            [0x92D3FC, 0xC35DD1, 0xCB8DF4, 0x90E0BE],
        ]
        return (0..<7).flatMap { group in
            let angle = Double(group) * 2 * .pi / 7 - .pi / 2
            let groupX = groupRadius * cos(angle)
            let groupY = groupRadius * sin(angle)
            return (0..<2).map { pair in
                let particleAngle = angle + 40 * .pi / 180 + Double(pair) * .pi
                return Particle(
                    x: groupX + 2 * cos(particleAngle),
                    y: groupY + 2 * sin(particleAngle),
                    pairIndex: pair,
                    startColor: RGBColor(hex: colors[group][pair * 2]),
                    endColor: RGBColor(hex: colors[group][pair * 2 + 1])
                )
            }
        }
    }()

    static func heartScale(_ progress: Double) -> Double {
        if progress == 0 { return 1 }
        // Reanimated 4.1.1 ignores the source's surplus fifth output value.
        // Its effective final scale is 1.15, including initialLiked == true.
        return interpolate(progress, [0, 0.75, 0.9, 1], [0, 1.15, 1, 1.15])
    }

    static func heartColor(_ progress: Double) -> RGBColor {
        interpolateColor(progress, [0, 0.03, 0.20, 0.30, 1],
                         [gray, gray, red, red, red])
    }

    static func ringScale(_ progress: Double) -> Double {
        interpolate(progress, [0, 0.3, 1], [0, 1, 1])
    }

    static func ringInnerRadius(_ progress: Double) -> Double {
        interpolate(progress, [0, 0.3, 0.5, 0.7, 1], [0, 0, 5, 12, 16])
    }

    static func ringOpacity(_ progress: Double) -> Double {
        interpolate(progress, [0, 0.1, 0.3, 0.5, 0.8, 1], [0, 0.8, 0.9, 0.7, 0.3, 0])
    }

    static func ringColor(_ progress: Double) -> RGBColor {
        let pink = RGBColor(hex: 0xDD4789)
        let purple = RGBColor(hex: 0xD46ABF)
        let lavender = RGBColor(hex: 0xCC8FF5)
        return interpolateColor(progress, [0, 0.2, 0.4, 0.6, 1],
                                [pink, pink, purple, lavender, lavender])
    }

    static func particleFrame(_ particle: Particle, progress: Double) -> ParticleFrame {
        // Delay is subtracted from eased progress, not from wall-clock time.
        let adjusted = progress - Double(particle.pairIndex) * stagger
        let radius: Double
        let size: Double
        let opacity: Double

        if adjusted < 0.20 {
            radius = 0
            size = 0
            opacity = 0
        } else if adjusted < 0.25 {
            radius = interpolate(adjusted, [0.20, 0.25], [0, groupRadius])
            size = interpolate(adjusted, [0.20, 0.25], [0, 1.2])
            opacity = interpolate(adjusted, [0.20, 0.25], [0, 1])
        } else {
            radius = interpolate(adjusted, [0.25, 1], [groupRadius, groupRadius * 1.25])
            size = interpolate(adjusted, [0.25, 0.60, 0.85, 1], [1.2, 1.4, 0.8, 0])
            opacity = adjusted < 0.60 ? 1 : interpolate(adjusted, [0.60, 1], [1, 0])
        }

        return ParticleFrame(
            x: Double(center) + particle.x * radius / groupRadius,
            y: Double(center) + particle.y * radius / groupRadius,
            size: size,
            opacity: opacity,
            // Both members use the original progress for their RGB transition.
            color: interpolateColor(progress, [0.20, 1],
                                    [particle.startColor, particle.endColor])
        )
    }

    /// Reanimated's default linear interpolation extends at both boundaries.
    static func interpolate(_ value: Double, _ input: [Double], _ output: [Double]) -> Double {
        precondition(input.count == output.count && input.count >= 2)
        let segment = input.dropFirst().firstIndex(where: { value <= $0 })
            .map { $0 - 1 } ?? input.count - 2
        let fraction = (value - input[segment]) / (input[segment + 1] - input[segment])
        return output[segment] + (output[segment + 1] - output[segment]) * fraction
    }

    /// Skia's interpolateColors clamps and interpolates raw RGB channels.
    private static func interpolateColor(_ value: Double, _ input: [Double],
                                         _ colors: [RGBColor]) -> RGBColor {
        let clamped = min(max(value, input[0]), input[input.count - 1])
        return RGBColor(
            r: interpolate(clamped, input, colors.map(\.r)),
            g: interpolate(clamped, input, colors.map(\.g)),
            b: interpolate(clamped, input, colors.map(\.b))
        )
    }
}

struct Particle {
    let x: Double
    let y: Double
    let pairIndex: Int
    let startColor: RGBColor
    let endColor: RGBColor
}

struct ParticleFrame {
    let x: Double
    let y: Double
    let size: Double
    let opacity: Double
    let color: RGBColor
}

struct RGBColor {
    let r: Double
    let g: Double
    let b: Double

    init(hex: UInt32) {
        r = Double((hex >> 16) & 0xFF) / 255
        g = Double((hex >> 8) & 0xFF) / 255
        b = Double(hex & 0xFF) / 255
    }

    init(r: Double, g: Double, b: Double) {
        self.r = r
        self.g = g
        self.b = b
    }

    func cgColor(opacity: Double = 1) -> CGColor {
        CGColor(colorSpace: Self.colorSpace,
                components: [CGFloat(r), CGFloat(g), CGFloat(b), CGFloat(opacity)])!
    }

    private static let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!
}

struct CubicBezier {
    let x1: Double
    let y1: Double
    let x2: Double
    let y2: Double

    static let like = CubicBezier(x1: 0.21, y1: 0.61, x2: 0.35, y2: 1)
    // Exact Easing.ease from react-native-reanimated 4.1.1.
    static let unlike = CubicBezier(x1: 0.42, y1: 0, x2: 1, y2: 1)

    func value(at time: Double) -> Double {
        if time <= 0 { return 0 }
        if time >= 1 { return 1 }
        var low = 0.0
        var high = 1.0
        // Invert x(t), then evaluate y(t); using y(time) is a different easing.
        for _ in 0..<32 {
            let t = (low + high) / 2
            if coordinate(t, first: x1, second: x2) < time {
                low = t
            } else {
                high = t
            }
        }
        return coordinate((low + high) / 2, first: y1, second: y2)
    }

    private func coordinate(_ t: Double, first: Double, second: Double) -> Double {
        let inverse = 1 - t
        return 3 * inverse * inverse * t * first + 3 * inverse * t * t * second + t * t * t
    }
}

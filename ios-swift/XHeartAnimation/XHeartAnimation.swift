import SwiftUI
import UIKit

/// A reusable SwiftUI wrapper with the same three inputs as the original.
/// initialLiked initializes local state once; debugMode disables interaction.
struct XHeartAnimation: UIViewRepresentable {
    let initialLiked: Bool
    var onToggle: ((Bool) -> Void)?
    var debugMode: Bool

    init(initialLiked: Bool = false,
         onToggle: ((Bool) -> Void)? = nil,
         debugMode: Bool = false) {
        self.initialLiked = initialLiked
        self.onToggle = onToggle
        self.debugMode = debugMode
    }

    func makeUIView(context: Context) -> XHeartAnimationControl {
        let control = XHeartAnimationControl(initialLiked: initialLiked)
        control.onToggle = onToggle
        control.debugMode = debugMode
        return control
    }

    func updateUIView(_ control: XHeartAnimationControl, context: Context) {
        control.onToggle = onToggle
        control.debugMode = debugMode
    }
}

/// UIKit/Core Graphics draws the original SVG, ring, and fourteen particles.
/// CADisplayLink only runs while timing is active; SwiftUI does not drive frames.
final class XHeartAnimationControl: UIControl {
    var onToggle: ((Bool) -> Void)?
    var debugMode = false {
        didSet {
            isEnabled = !debugMode
            updateAccessibility()
        }
    }

    private(set) var isLiked: Bool
    private var progress: Double
    private var isLiking: Bool
    private var showParticles: Bool
    private var animation: TimingAnimation?
    private var displayLink: CADisplayLink?
    private lazy var displayLinkTarget = DisplayLinkTarget(control: self)
    private let haptics = UIImpactFeedbackGenerator(style: .medium)

    init(initialLiked: Bool = false) {
        isLiked = initialLiked
        progress = initialLiked ? 1 : 0
        isLiking = initialLiked
        showParticles = initialLiked
        super.init(frame: .zero)
        isOpaque = false
        backgroundColor = .clear
        clipsToBounds = true
        contentMode = .redraw
        isAccessibilityElement = true
        accessibilityIdentifier = "x-heart-animation"
        addTarget(self, action: #selector(toggleLiked), for: .touchUpInside)
        updateAccessibility()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("Use init(initialLiked:) instead")
    }

    deinit {
        displayLink?.invalidate()
    }

    override var intrinsicContentSize: CGSize {
        CGSize(width: HeartAnimationModel.canvasSize, height: HeartAnimationModel.canvasSize)
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            stopDisplayLink()
        } else if animation != nil {
            advanceAnimation(at: CACurrentMediaTime())
            if animation != nil { startDisplayLink() }
        }
    }

    override func accessibilityActivate() -> Bool {
        guard !debugMode else { return false }
        toggleLiked()
        return true
    }

    @objc private func toggleLiked() {
        guard !debugMode else { return }
        let now = CACurrentMediaTime()
        // An interrupted timing continues from the current progress.
        advanceAnimation(at: now)
        isLiked.toggle()
        isLiking = isLiked
        showParticles = isLiked

        if isLiked {
            haptics.prepare()
            haptics.impactOccurred()
        }

        animation = TimingAnimation(
            startTime: now,
            duration: isLiked ? 1 : 0.2,
            from: progress,
            to: isLiked ? 1 : 0,
            easing: isLiked ? .like : .unlike
        )
        startDisplayLink()
        updateAccessibility()
        setNeedsDisplay()
        onToggle?(isLiked)
        sendActions(for: .valueChanged)
    }

    fileprivate func displayLinkDidFire(_ link: CADisplayLink) {
        advanceAnimation(at: link.timestamp)
    }

    private func advanceAnimation(at time: CFTimeInterval) {
        guard let timing = animation else { return }
        let elapsed = min(max((time - timing.startTime) / timing.duration, 0), 1)
        progress = timing.from + (timing.to - timing.from) * timing.easing.value(at: elapsed)
        if elapsed >= 1 {
            progress = timing.to
            animation = nil
            stopDisplayLink()
        }
        setNeedsDisplay()
    }

    private func startDisplayLink() {
        guard displayLink == nil else { return }
        let link = CADisplayLink(target: displayLinkTarget,
                                 selector: #selector(DisplayLinkTarget.step(_:)))
        link.preferredFrameRateRange = CAFrameRateRange(minimum: 60, maximum: 120, preferred: 120)
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    private func stopDisplayLink() {
        displayLink?.invalidate()
        displayLink = nil
    }

    private func updateAccessibility() {
        accessibilityLabel = "Thích"
        accessibilityValue = isLiked ? "Đã thích" : "Chưa thích"
        accessibilityHint = debugMode ? nil : (isLiked ? "Chạm hai lần để bỏ thích" : "Chạm hai lần để thích")
        accessibilityTraits = isLiked ? [.button, .selected] : [.button]
        if debugMode { accessibilityTraits.insert(.notEnabled) }
    }

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else { return }
        context.setAllowsAntialiasing(true)
        context.setShouldAntialias(true)
        context.saveGState()
        context.translateBy(x: (bounds.width - HeartAnimationModel.canvasSize) / 2,
                            y: (bounds.height - HeartAnimationModel.canvasSize) / 2)
        drawRing(in: context)
        drawParticles(in: context)
        drawHeart(in: context)
        context.restoreGState()
    }

    private func drawRing(in context: CGContext) {
        guard isLiking else { return }
        let scale = HeartAnimationModel.ringScale(progress)
        let opacity = HeartAnimationModel.ringOpacity(progress)
        guard scale > 0 && opacity > 0 else { return }
        let center = HeartAnimationModel.center
        let inner = CGFloat(HeartAnimationModel.ringInnerRadius(progress))

        context.saveGState()
        context.translateBy(x: center, y: center)
        context.scaleBy(x: CGFloat(scale), y: CGFloat(scale))
        context.translateBy(x: -center, y: -center)
        context.setFillColor(HeartAnimationModel.ringColor(progress).cgColor(opacity: opacity))
        context.addEllipse(in: CGRect(x: center - 16, y: center - 16, width: 32, height: 32))
        if inner > 0 {
            context.addEllipse(in: CGRect(x: center - inner, y: center - inner,
                                          width: inner * 2, height: inner * 2))
        }
        context.drawPath(using: .eoFill)
        context.restoreGState()
    }

    private func drawParticles(in context: CGContext) {
        guard showParticles else { return }
        for particle in HeartAnimationModel.particles {
            let frame = HeartAnimationModel.particleFrame(particle, progress: progress)
            guard frame.size > 0 && frame.opacity > 0 else { continue }
            context.setFillColor(frame.color.cgColor(opacity: frame.opacity))
            context.fillEllipse(in: CGRect(x: frame.x - frame.size, y: frame.y - frame.size,
                                           width: frame.size * 2, height: frame.size * 2))
        }
    }

    private func drawHeart(in context: CGContext) {
        let center = HeartAnimationModel.center
        let scale = CGFloat(HeartAnimationModel.heartScale(progress))
        context.saveGState()
        // Parent Group scale around (29,29), followed by the Path matrix.
        context.translateBy(x: center, y: center)
        context.scaleBy(x: scale, y: scale)
        context.translateBy(x: -center, y: -center)
        context.translateBy(x: center, y: center)
        context.scaleBy(x: HeartAnimationModel.heartSize / 24,
                        y: HeartAnimationModel.heartSize / 24)
        context.translateBy(x: -12, y: -12)
        context.addPath(HeartAnimationModel.heartPath)

        let color = HeartAnimationModel.heartColor(progress).cgColor()
        if progress < 0.03 {
            // Skia applies the SVG matrix to its canvas, so width 2 scales too.
            context.setStrokeColor(color)
            context.setLineWidth(2)
            context.setLineJoin(.miter)
            context.setLineCap(.butt)
            context.setMiterLimit(4)
            context.strokePath()
        } else {
            context.setFillColor(color)
            context.fillPath()
        }
        context.restoreGState()
    }
}

private struct TimingAnimation {
    let startTime: CFTimeInterval
    let duration: CFTimeInterval
    let from: Double
    let to: Double
    let easing: CubicBezier
}

/// A weak target prevents the display link from keeping the control alive.
private final class DisplayLinkTarget: NSObject {
    weak var control: XHeartAnimationControl?

    init(control: XHeartAnimationControl) {
        self.control = control
    }

    @objc func step(_ link: CADisplayLink) {
        control?.displayLinkDidFire(link)
    }
}

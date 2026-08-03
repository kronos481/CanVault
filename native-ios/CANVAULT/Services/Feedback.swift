import AVFoundation
import UIKit

enum FeedbackEffect {
    case standard
    case navigation
    case scan
    case success
    case color
    case shake
    case archive
    case destructive
}

@MainActor
final class Feedback {
    static let shared = Feedback()
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let format = AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 1)!
    private var ready = false

    private init() {
        engine.attach(player)
        engine.connect(player, to: engine.mainMixerNode, format: format)
        try? AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
    }

    func play(_ effect: FeedbackEffect) {
        guard UIAccessibility.isVoiceOverRunning == false else {
            haptic(effect)
            return
        }
        if !ready {
            try? engine.start()
            ready = engine.isRunning
        }
        haptic(effect)
        guard ready, let buffer = makeBuffer(effect) else { return }
        player.scheduleBuffer(buffer, at: nil, options: .interrupts)
        if !player.isPlaying { player.play() }
    }

    private func haptic(_ effect: FeedbackEffect) {
        switch effect {
        case .success: UINotificationFeedbackGenerator().notificationOccurred(.success)
        case .destructive: UINotificationFeedbackGenerator().notificationOccurred(.warning)
        case .scan, .shake, .archive: UIImpactFeedbackGenerator(style: .medium).impactOccurred(intensity: 0.7)
        default: UIImpactFeedbackGenerator(style: .light).impactOccurred(intensity: 0.45)
        }
    }

    private func makeBuffer(_ effect: FeedbackEffect) -> AVAudioPCMBuffer? {
        let duration: Double
        switch effect {
        case .standard, .navigation: duration = 0.035
        case .scan, .success, .color: duration = 0.09
        case .shake: duration = 0.12
        case .archive, .destructive: duration = 0.075
        }
        let frames = AVAudioFrameCount(format.sampleRate * duration)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames),
              let channel = buffer.floatChannelData?[0] else { return nil }
        buffer.frameLength = frames

        for frame in 0..<Int(frames) {
            let t = Double(frame) / format.sampleRate
            let progress = t / duration
            let envelope = Float(pow(max(1 - progress, 0), 2.2))
            let sample: Float
            switch effect {
            case .standard:
                sample = Float(sin(2 * .pi * 920 * t)) * 0.07
            case .navigation:
                sample = Float(sin(2 * .pi * 680 * t)) * 0.055
            case .scan:
                let frequency = progress < 0.48 ? 760.0 : 1_180.0
                sample = Float(sin(2 * .pi * frequency * t)) * 0.075
            case .success:
                let frequency = progress < 0.45 ? 740.0 : 1_080.0
                sample = Float(sin(2 * .pi * frequency * t)) * 0.065
            case .color:
                sample = Float(sin(2 * .pi * (620 + progress * 520) * t)) * 0.06
            case .shake:
                let noise = Float.random(in: -1...1)
                sample = (noise * 0.045 + Float(sin(2 * .pi * 150 * t)) * 0.035)
            case .archive:
                sample = Float(sin(2 * .pi * (260 - progress * 80) * t)) * 0.075
            case .destructive:
                sample = Float(sin(2 * .pi * 190 * t)) * 0.085
            }
            channel[frame] = sample * envelope
        }
        return buffer
    }
}

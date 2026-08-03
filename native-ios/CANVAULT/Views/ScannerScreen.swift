import AVFoundation
import SwiftUI
import UIKit

private enum ScannerMode: String, CaseIterable, Identifiable {
    case product = "Produkt-Barcode"
    case canVaultQR = "CANVAULT QR"
    var id: String { rawValue }
}

private struct ScannerResolution: Identifiable {
    var id = UUID()
    var title: String
    var message: String
    var rawValue: String
    var format: String
    var prefill: ScanPrefill?
}

struct ScannerScreen: View {
    @EnvironmentObject private var store: InventoryStore
    @Environment(\.dismiss) private var dismiss
    @State private var mode: ScannerMode = .product
    @State private var torchOn = false
    @State private var scanningEnabled = true
    @State private var resolution: ScannerResolution?
    @State private var cameraError: String?
    @State private var candidateValue = ""
    @State private var candidateFormat = ""
    @State private var candidateHits = 0
    @State private var lastSeen = Date.distantPast
    @State private var scanLine = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let onResult: (ScanPrefill) -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            CameraScannerView(
                mode: mode,
                torchOn: torchOn,
                enabled: scanningEnabled,
                onCode: acceptFrame,
                onError: { cameraError = $0 }
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Picker("Scanmodus", selection: $mode) {
                    ForEach(ScannerMode.allCases) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                .padding(12)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .padding(.horizontal, 16)
                .padding(.top, 8)

                Spacer()
                scanFrame
                Spacer()

                VStack(spacing: 12) {
                    Text(mode == .product ? "EAN, UPC, Code 128 und weitere lineare Barcodes direkt in der App scannen." : "Quadratischen CANVAULT-Code vollständig im Rahmen halten.")
                        .font(.subheadline.weight(.medium)).multilineTextAlignment(.center).foregroundStyle(.white)
                    HStack(spacing: 12) {
                        Button {
                            torchOn.toggle(); Feedback.shared.play(.standard)
                        } label: {
                            Label(torchOn ? "Licht aus" : "Licht an", systemImage: torchOn ? "flashlight.on.fill" : "flashlight.off.fill")
                                .frame(maxWidth: .infinity, minHeight: 48)
                        }
                        .buttonStyle(.borderedProminent).tint(Color.white.opacity(0.2))
                        Button { dismiss() } label: { Text("Abbrechen").frame(maxWidth: .infinity, minHeight: 48) }
                            .buttonStyle(.bordered).tint(.white)
                    }
                }
                .padding(16)
                .background(.ultraThinMaterial)
            }
        }
        .navigationTitle("Scanner")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbarBackground(.black.opacity(0.75), for: .navigationBar)
        .onAppear { animateLine() }
        .onChange(of: mode) { _ in resetCandidate() }
        .sheet(item: $resolution, onDismiss: { scanningEnabled = true; resetCandidate() }) { value in
            ScanConfirmationSheet(value: value) {
                guard let prefill = value.prefill else { resolution = nil; return }
                Feedback.shared.play(.success)
                onResult(prefill)
            } onRetry: {
                resolution = nil
                scanningEnabled = true
                resetCandidate()
            }
            .presentationDetents([.medium, .large])
        }
        .alert("Kamera nicht verfügbar", isPresented: Binding(get: { cameraError != nil }, set: { if !$0 { cameraError = nil } })) {
            Button("Einstellungen öffnen") { if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) } }
            Button("Abbrechen", role: .cancel) { dismiss() }
        } message: { Text(cameraError ?? "Bitte erlaube den Kamerazugriff in den iOS-Einstellungen.") }
    }

    private var scanFrame: some View {
        ZStack {
            RoundedRectangle(cornerRadius: mode == .product ? 18 : 26, style: .continuous)
                .stroke(CVColor.mint, style: StrokeStyle(lineWidth: 3, dash: mode == .product ? [] : [12, 8]))
            Rectangle()
                .fill(CVColor.mint)
                .frame(height: 2)
                .shadow(color: CVColor.mint, radius: 7)
                .offset(y: scanLine ? (mode == .product ? 58 : 108) : (mode == .product ? -58 : -108))
                .opacity(scanningEnabled ? 1 : 0)
        }
        .frame(width: mode == .product ? 330 : 270, height: mode == .product ? 126 : 270)
        .animation(reduceMotion ? nil : .easeInOut(duration: 1.35).repeatForever(autoreverses: true), value: scanLine)
        .accessibilityLabel(mode == .product ? "Breiter Scanrahmen für Produkt-Barcodes" : "Quadratischer Scanrahmen für CANVAULT QR")
    }

    private func animateLine() { if !reduceMotion { scanLine = true } }

    private func acceptFrame(_ raw: String, _ format: String) {
        guard scanningEnabled, resolution == nil else { return }
        let now = Date()
        if raw == candidateValue && format == candidateFormat && now.timeIntervalSince(lastSeen) <= 0.9 {
            candidateHits += 1
        } else {
            candidateValue = raw
            candidateFormat = format
            candidateHits = 1
        }
        lastSeen = now
        guard candidateHits >= 2 else { return }
        scanningEnabled = false
        Feedback.shared.play(.scan)
        resolution = resolve(raw, format: format)
    }

    private func resolve(_ raw: String, format: String) -> ScannerResolution {
        if mode == .canVaultQR {
            guard let payload = CanVaultQR.decode(raw) else {
                return ScannerResolution(title: "QR-Code nicht erkannt", message: "Der Code ist kein gültiger CANVAULT-Code oder wurde beschädigt.", rawValue: raw, format: format, prefill: nil)
            }
            return ScannerResolution(
                title: "CANVAULT-Code erkannt",
                message: "Produkt- und Farbdaten wurden aus dem CANVAULT-Code übernommen.",
                rawValue: raw,
                format: format,
                prefill: ScanPrefill(brandId: payload.brandId, lineId: payload.canLineId, colorName: payload.colorName, colorCode: payload.colorCode, customHex: payload.customHex, message: "CANVAULT-Code erkannt – Produkt- und Farbdaten übernommen.")
            )
        }
        let catalog = CatalogStore.shared
        if let product = catalog.verifiedProduct(barcode: raw) {
            return ScannerResolution(
                title: "Verifiziertes Produkt",
                message: "\(catalog.lineName(product.lineId)) · \(product.colorName)\nQuelle: \(product.sourceName), geprüft \(product.verifiedAt)",
                rawValue: raw,
                format: format,
                prefill: ScanPrefill(brandId: product.brandId, lineId: product.lineId, colorName: product.colorName, colorCode: product.colorCode, customHex: product.customHex, volumeMl: product.volumeMl, externalBarcode: raw, message: "Im verifizierten Katalog gefunden · Quelle: \(product.sourceName).")
            )
        }
        if let can = store.knownCan(barcode: raw) {
            return ScannerResolution(
                title: "Bekannter Barcode",
                message: "Gespeicherte Produkt- und Farbdaten werden automatisch übernommen.",
                rawValue: raw,
                format: format,
                prefill: ScanPrefill(brandId: can.brandId, lineId: can.canLineId, colorName: can.colorName, colorCode: can.colorCode, customHex: can.customHex, volumeMl: can.volumeMl, purchasePriceCents: can.purchasePriceCents, externalBarcode: raw, message: "Barcode bekannt – Produkt- und Farbdaten automatisch übernommen.")
            )
        }
        return ScannerResolution(
            title: "Neuer Produkt-Barcode",
            message: "Der Code ist noch nicht im Katalog. Ergänze das Produkt einmal; danach erkennt CANVAULT es automatisch.",
            rawValue: raw,
            format: format,
            prefill: ScanPrefill(externalBarcode: raw, message: "Neuer Barcode – vervollständige das Produkt einmal für zukünftige automatische Erkennung.")
        )
    }

    private func resetCandidate() {
        candidateValue = ""; candidateFormat = ""; candidateHits = 0; lastSeen = .distantPast
    }
}

private struct ScanConfirmationSheet: View {
    let value: ScannerResolution
    let onUse: () -> Void
    let onRetry: () -> Void

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 18) {
                HStack(spacing: 12) {
                    Image(systemName: value.prefill == nil ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                        .font(.largeTitle).foregroundStyle(value.prefill == nil ? CVColor.warning : CVColor.mint)
                    Text(value.title).font(.title2.bold())
                }
                Text(value.message).foregroundStyle(.secondary)
                VStack(alignment: .leading, spacing: 5) {
                    Text("Gelesener Code").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                    Text(value.rawValue).font(.body.monospaced()).lineLimit(3).textSelection(.enabled)
                    Text(value.format).font(.caption).foregroundStyle(.secondary)
                }
                .padding(14).cvCard(radius: 14)
                Spacer()
                if value.prefill != nil {
                    Button(action: onUse) { Label("Daten übernehmen", systemImage: "arrow.right.circle.fill") }
                        .buttonStyle(CVPrimaryButtonStyle())
                }
                Button(action: onRetry) { Text("Erneut scannen").frame(maxWidth: .infinity, minHeight: 48) }
                    .buttonStyle(.bordered)
            }
            .padding(20)
            .navigationTitle("Scan prüfen")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct CameraScannerView: UIViewControllerRepresentable {
    var mode: ScannerMode
    var torchOn: Bool
    var enabled: Bool
    var onCode: (String, String) -> Void
    var onError: (String) -> Void

    func makeUIViewController(context: Context) -> BarcodeCameraController {
        BarcodeCameraController(onCode: onCode, onError: onError)
    }

    func updateUIViewController(_ controller: BarcodeCameraController, context: Context) {
        controller.update(mode: mode, torchOn: torchOn, enabled: enabled, onCode: onCode)
    }
}

private final class BarcodeCameraController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    private let session = AVCaptureSession()
    private let output = AVCaptureMetadataOutput()
    private let sessionQueue = DispatchQueue(label: "com.canvault.camera", qos: .userInitiated)
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var onCode: (String, String) -> Void
    private let onError: (String) -> Void
    private var configured = false

    init(onCode: @escaping (String, String) -> Void, onError: @escaping (String) -> Void) {
        self.onCode = onCode
        self.onError = onError
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        requestAndConfigure()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    func update(mode: ScannerMode, torchOn: Bool, enabled: Bool, onCode: @escaping (String, String) -> Void) {
        self.onCode = onCode
        if configured {
            let requested: [AVMetadataObject.ObjectType] = mode == .canVaultQR ? [.qr] : [.ean8, .ean13, .upce, .code39, .code93, .code128, .itf14, .dataMatrix, .pdf417, .aztec]
            output.metadataObjectTypes = requested.filter { output.availableMetadataObjectTypes.contains($0) }
        }
        setTorch(torchOn)
        sessionQueue.async { [weak self] in
            guard let self else { return }
            if enabled && configured && !session.isRunning { session.startRunning() }
            else if !enabled && session.isRunning { session.stopRunning() }
        }
    }

    private func requestAndConfigure() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: configure()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] allowed in
                DispatchQueue.main.async { allowed ? self?.configure() : self?.onError("Kamerazugriff wurde nicht erlaubt.") }
            }
        default: onError("Erlaube CANVAULT den Kamerazugriff in den iOS-Einstellungen.")
        }
    }

    private func configure() {
        guard !configured else { return }
        var configurationStarted = false
        do {
            guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else { throw CameraError.unavailable }
            let input = try AVCaptureDeviceInput(device: camera)
            session.beginConfiguration()
            configurationStarted = true
            session.sessionPreset = .high
            guard session.canAddInput(input), session.canAddOutput(output) else { throw CameraError.unavailable }
            session.addInput(input)
            session.addOutput(output)
            output.setMetadataObjectsDelegate(self, queue: .main)
            output.metadataObjectTypes = [.ean8, .ean13, .upce, .code39, .code93, .code128, .itf14, .dataMatrix, .pdf417, .aztec].filter { output.availableMetadataObjectTypes.contains($0) }
            session.commitConfiguration()
            let layer = AVCaptureVideoPreviewLayer(session: session)
            layer.videoGravity = .resizeAspectFill
            view.layer.insertSublayer(layer, at: 0)
            previewLayer = layer
            configured = true
            sessionQueue.async { [weak self] in self?.session.startRunning() }
        } catch {
            if configurationStarted { session.commitConfiguration() }
            onError("Die Rückkamera konnte nicht gestartet werden.")
        }
    }

    private func setTorch(_ enabled: Bool) {
        guard let device = AVCaptureDevice.default(for: .video), device.hasTorch else { return }
        do {
            try device.lockForConfiguration()
            device.torchMode = enabled ? .on : .off
            device.unlockForConfiguration()
        } catch { }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let object = metadataObjects.compactMap({ $0 as? AVMetadataMachineReadableCodeObject }).first,
              let value = object.stringValue else { return }
        onCode(value, object.type.rawValue)
    }

    private enum CameraError: Error { case unavailable }
}

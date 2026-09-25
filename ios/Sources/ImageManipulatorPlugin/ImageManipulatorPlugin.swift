import Capacitor
import Foundation

/// Please read the Capacitor iOS Plugin Development Guide
/// here: https://capacitorjs.com/docs/plugins/ios
@objc(ImageManipulatorPlugin)
public class ImageManipulatorPlugin: CAPPlugin, CAPBridgedPlugin {

    public let identifier = "ImageManipulatorPlugin"
    public let jsName = "ImageManipulator"
    public let pluginMethods: [CAPPluginMethod] = [
        .promise("getDimensions", ImageManipulatorPlugin.getDimensions),
        .promise("resize", ImageManipulatorPlugin.resize)
    ]

    // NOTE: Error code constants
    private enum ErrorCodes {
        static let failedToLoadImage = "Failed to load image"
        static let failedToCreateImageData = "Failed to create image data"
        static let failedToSaveResizedImage = "Failed to save resized image"
        static let failedToGetResizedJpegImageFromData = "Failed to get resized JPEG image from data"
    }

    private var implementation: ImageManipulator?
    override public func load() {
        guard let bridge = bridge else { return }
        implementation = ImageManipulator(bridge: bridge)
    }

    // Both methods stay synchronous on the bridge queue: they touch no UIKit state and answer when the work is done.

    func getDimensions(_ call: CAPPluginCall) throws {

        guard let imagePath = call.options["imagePath"] as? String else {
            throw CAPPluginError("Must provide an imagePath")
        }

        guard let implementation = implementation else {
            throw CAPPluginError("Failed to initialize plugin")
        }

        let dimensions: ImageDimensions
        do {
            dimensions = try implementation.getDimensions(imagePath: imagePath)
        } catch {
            throw Self.pluginError(for: error)
        }
        call.resolve([
            "width": dimensions.width,
            "height": dimensions.height
        ])
    }

    func resize(_ call: CAPPluginCall) throws {

        guard let imagePath = call.options["imagePath"] as? String else {
            throw CAPPluginError("Must provide an imagePath")
        }
        let fileName = call.getString("fileName")
        let quality = call.getInt("quality", 85)
        let maxWidth = call.getInt("maxWidth", 0)
        let maxHeight = call.getInt("maxHeight", 0)
        if maxWidth <= 0 && maxHeight <= 0 {
            throw CAPPluginError("Either maxWidth or maxHeight param must be provided and be greater then 0.")
        }
        let fixRotation = call.getBool("fixRotation", false)

        guard let implementation = implementation else {
            throw CAPPluginError("Failed to initialize plugin")
        }

        let imageResizingResult: ImageResizingResult
        do {
            imageResizingResult = try implementation.resize(
                imagePath: imagePath, fileName: fileName, quality: quality,
                maxWidth: maxWidth, maxHeight: maxHeight, fixRotation: fixRotation
            )
        } catch {
            throw Self.pluginError(for: error)
        }

        var result: [String: Any] = [
            "originalWidth": imageResizingResult.originalWidth,
            "originalHeight": imageResizingResult.originalHeight,
            "resizedWidth": imageResizingResult.resizedWidth,
            "resizedHeight": imageResizingResult.resizedHeight,
            "imagePath": imageResizingResult.imagePath,
            "resized": imageResizingResult.resized
        ]
        if let webPath = imageResizingResult.webPath, !webPath.isEmpty {
            result["webPath"] = webPath
        }
        call.resolve(result)
    }

    /// The rejection for an error of the implementation: its fixed message, or the error's localized description.
    private static func pluginError(for error: Error) -> CAPPluginError {
        switch error {
        case ImageManipulatorError.failedToLoadImage:
            return CAPPluginError(ErrorCodes.failedToLoadImage)
        case ImageManipulatorError.failedToCreateImageData:
            return CAPPluginError(ErrorCodes.failedToCreateImageData)
        case ImageManipulatorError.failedToSaveResizedImage:
            return CAPPluginError(ErrorCodes.failedToSaveResizedImage)
        case ImageManipulatorError.failedToGetResizedJPEGImageFromData:
            return CAPPluginError(ErrorCodes.failedToGetResizedJpegImageFromData)
        default:
            return CAPPluginError(error.localizedDescription, underlyingError: error)
        }
    }

}

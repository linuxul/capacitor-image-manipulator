import UIKit
import WebKit
import XCTest
import Capacitor
@testable import ImageManipulatorPlugin

class ImageManipulatorPluginTests: XCTestCase {
    func testResultingDimensionsKeepTheAspectRatio() {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let image = UIGraphicsImageRenderer(size: CGSize(width: 400, height: 200), format: format).image { _ in }

        XCTAssertEqual(CGSize(width: 100, height: 50), image.getResultingImageDimensions(maxWidth: 100, maxHeight: 0))
        XCTAssertEqual(CGSize(width: 100, height: 50), image.getResultingImageDimensions(maxWidth: 0, maxHeight: 50))
        XCTAssertEqual(CGSize(width: 100, height: 50), image.getResultingImageDimensions(maxWidth: 100, maxHeight: 100))
        XCTAssertEqual(CGSize(width: 400, height: 200), image.getResultingImageDimensions(maxWidth: 0, maxHeight: 0))
    }

    func testTheValidationErrorsKeepTheirMessages() {
        let plugin = ImageManipulatorPlugin()
        assertThrows("Must provide an imagePath") { try plugin.getDimensions(makeCall([:])) }
        assertThrows("Must provide an imagePath") { try plugin.resize(makeCall(["maxWidth": 10])) }
        assertThrows("Either maxWidth or maxHeight param must be provided and be greater then 0.") {
            try plugin.resize(self.makeCall(["imagePath": "/tmp/image.png"]))
        }
        // Without a bridge the plugin has no implementation.
        assertThrows("Failed to initialize plugin") { try plugin.getDimensions(makeCall(["imagePath": "/tmp/image.png"])) }
    }

    func testGetDimensionsOfAnImageAndOfAMissingFile() throws {
        let bridge = FakeBridge()
        let plugin = ImageManipulatorPlugin()
        plugin.bridge = bridge
        plugin.load()

        let missing = FileManager.default.temporaryDirectory.appendingPathComponent("missing-\(UUID().uuidString).png").path
        assertThrows("Failed to load image") { try plugin.getDimensions(makeCall(["imagePath": missing])) }

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let image = UIGraphicsImageRenderer(size: CGSize(width: 40, height: 20), format: format).image { _ in }
        let path = FileManager.default.temporaryDirectory.appendingPathComponent("image-\(UUID().uuidString).png")
        try XCTUnwrap(image.pngData()).write(to: path)
        defer { try? FileManager.default.removeItem(at: path) }

        var resolved: PluginCallResultData?
        let call = CAPPluginCall(callbackId: "test", methodName: "getDimensions", options: ["imagePath": path.path], success: { result, _ in
            resolved = result.data
        }, error: { _ in
            XCTFail("getDimensions must resolve")
        })
        try plugin.getDimensions(call)
        XCTAssertEqual(resolved?["width"] as? Int, 40)
        XCTAssertEqual(resolved?["height"] as? Int, 20)
    }

    /// Asserts that `body` throws the CAPPluginError the bridge rejects the call with, with `message` and no code.
    private func assertThrows(_ message: String, file: StaticString = #filePath, line: UInt = #line, _ body: () throws -> Void) {
        XCTAssertThrowsError(try body(), file: file, line: line) { error in
            XCTAssertEqual((error as? CAPPluginError)?.message, message, file: file, line: line)
            XCTAssertNil((error as? CAPPluginError)?.code, file: file, line: line)
        }
    }

    private func makeCall(_ options: JSObject) -> CAPPluginCall {
        return CAPPluginCall(callbackId: "test", methodName: "test", options: options, success: { _, _ in
            XCTFail("the method must throw")
        }, error: { _ in
            XCTFail("the method answers by throwing")
        })
    }
}

/// A bridge with just enough behaviour for the plugin to load. Members it never uses trap.
private final class FakeBridge: CAPBridgeProtocol {
    var viewController: UIViewController?
    var webView: WKWebView?
    var isSimEnvironment = true
    var isDevEnvironment = true
    var userInterfaceStyle = UIUserInterfaceStyle.unspecified
    var autoRegisterPlugins = false
    var statusBarVisible = true
    var statusBarStyle = UIStatusBarStyle.default
    var statusBarAnimation = UIStatusBarAnimation.fade
    var config: InstanceConfiguration { fatalError("unused") }
    var notificationRouter: NotificationRouter { fatalError("unused") }

    func plugin(withName: String) -> CAPPlugin? { nil }
    func saveCall(_ call: CAPPluginCall) {}
    func savedCall(withID: String) -> CAPPluginCall? { nil }
    func releaseCall(_ call: CAPPluginCall) {}
    func releaseCall(withID: String) {}
    // swiftlint:disable identifier_name
    func evalWithPlugin(_ plugin: CAPPlugin, js: String) {}
    func eval(js: String) {}
    // swiftlint:enable identifier_name
    func triggerJSEvent(eventName: String, target: String) {}
    func triggerJSEvent(eventName: String, target: String, data: String) {}
    func triggerWindowJSEvent(eventName: String) {}
    func triggerWindowJSEvent(eventName: String, data: String) {}
    func triggerDocumentJSEvent(eventName: String) {}
    func triggerDocumentJSEvent(eventName: String, data: String) {}
    func localURL(fromWebURL webURL: URL?) -> URL? { webURL }
    func portablePath(fromLocalURL localURL: URL?) -> URL? { localURL }
    func setServerBasePath(_ path: String) {}
    func registerPluginType(_ pluginType: CAPPlugin.Type) {}
    func registerPluginInstance(_ pluginInstance: CAPPlugin) {}
    func showAlertWith(title: String, message: String, buttonTitle: String) {}
}

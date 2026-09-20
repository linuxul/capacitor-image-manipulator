import UIKit
import XCTest
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
}

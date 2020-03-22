require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-matrix-sdk"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  react-native-matrix-sdk
                   DESC
  s.homepage     = "https://github.com/hannojg/react-native-matrix-sdk"
  s.license      = "MIT"
  # s.license    = { :type => "MIT", :file => "FILE_LICENSE" }
  s.authors      = { "Hanno Gödcke" => "hanno.goedecke@gmail.com" }
  s.platforms    = { :ios => "9.0" }
  s.swift_version = '5.0'
  s.source       = { :git => "https://github.com/hannojg/react-native-matrix-sdk.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,swift}"
  s.requires_arc = true

  s.dependency "React"

end

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const iosRoot = path.join(root, 'native-ios');
const appRoot = path.join(iosRoot, 'CANVAULT');
const projectRoot = path.join(iosRoot, 'CANVAULT.xcodeproj');

const swiftFiles = [];
function walk(folder) {
  for (const entry of fs.readdirSync(folder, { withFileTypes: true })) {
    const absolute = path.join(folder, entry.name);
    if (entry.isDirectory() && entry.name !== 'Assets.xcassets') walk(absolute);
    else if (entry.isFile() && entry.name.endsWith('.swift')) swiftFiles.push(path.relative(appRoot, absolute).replaceAll('\\', '/'));
  }
}
walk(appRoot);
swiftFiles.sort();

const resources = ['Assets.xcassets', 'Resources/CatalogData.json'];
const id = (key) => crypto.createHash('sha1').update(key).digest('hex').slice(0, 24).toUpperCase();
const quote = (value) => `"${value.replaceAll('"', '\\"')}"`;
const fileType = (file) => file.endsWith('.swift') ? 'sourcecode.swift' : file.endsWith('.json') ? 'text.json' : 'folder.assetcatalog';

const fileRefs = [...swiftFiles, ...resources].map((file) =>
  `\t\t${id(`file:${file}`)} /* ${path.basename(file)} */ = {isa = PBXFileReference; lastKnownFileType = ${fileType(file)}; path = ${quote(file)}; sourceTree = "<group>"; };`,
).join('\n');
const buildFiles = [...swiftFiles, ...resources].map((file) =>
  `\t\t${id(`build:${file}`)} /* ${path.basename(file)} in ${file.endsWith('.swift') ? 'Sources' : 'Resources'} */ = {isa = PBXBuildFile; fileRef = ${id(`file:${file}`)} /* ${path.basename(file)} */; };`,
).join('\n');
const groupChildren = [...swiftFiles, ...resources].map((file) => `\t\t\t\t${id(`file:${file}`)} /* ${path.basename(file)} */,`).join('\n');
const sourceBuildFiles = swiftFiles.map((file) => `\t\t\t\t${id(`build:${file}`)} /* ${path.basename(file)} in Sources */,`).join('\n');
const resourceBuildFiles = resources.map((file) => `\t\t\t\t${id(`build:${file}`)} /* ${path.basename(file)} in Resources */,`).join('\n');

const project = `// !$*UTF8*$!
{
\tarchiveVersion = 1;
\tclasses = {};
\tobjectVersion = 60;
\tobjects = {

/* Begin PBXBuildFile section */
${buildFiles}
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
${fileRefs}
\t\t${id('product')} /* CANVAULT.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = CANVAULT.app; sourceTree = BUILT_PRODUCTS_DIR; };
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
\t\t${id('frameworks')} /* Frameworks */ = {isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; };
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
\t\t${id('mainGroup')} = {isa = PBXGroup; children = (${id('appGroup')} /* CANVAULT */, ${id('productsGroup')} /* Products */); sourceTree = "<group>"; };
\t\t${id('appGroup')} /* CANVAULT */ = {isa = PBXGroup; children = (
${groupChildren}
\t\t\t); path = CANVAULT; sourceTree = "<group>"; };
\t\t${id('productsGroup')} /* Products */ = {isa = PBXGroup; children = (${id('product')} /* CANVAULT.app */); name = Products; sourceTree = "<group>"; };
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
\t\t${id('target')} /* CANVAULT */ = {isa = PBXNativeTarget; buildConfigurationList = ${id('targetConfigList')} /* Build configuration list for PBXNativeTarget "CANVAULT" */; buildPhases = (${id('sources')} /* Sources */, ${id('frameworks')} /* Frameworks */, ${id('resources')} /* Resources */); buildRules = (); dependencies = (); name = CANVAULT; productName = CANVAULT; productReference = ${id('product')} /* CANVAULT.app */; productType = "com.apple.product-type.application"; };
/* End PBXNativeTarget section */

/* Begin PBXProject section */
\t\t${id('project')} /* Project object */ = {isa = PBXProject; attributes = {BuildIndependentTargetsInParallel = 1; LastSwiftUpdateCheck = 1600; LastUpgradeCheck = 1600; TargetAttributes = {${id('target')} = {CreatedOnToolsVersion = 16.0; };};}; buildConfigurationList = ${id('projectConfigList')} /* Build configuration list for PBXProject "CANVAULT" */; compatibilityVersion = "Xcode 15.0"; developmentRegion = de; hasScannedForEncodings = 0; knownRegions = (de, Base); mainGroup = ${id('mainGroup')}; productRefGroup = ${id('productsGroup')} /* Products */; projectDirPath = ""; projectRoot = ""; targets = (${id('target')} /* CANVAULT */); };
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
\t\t${id('resources')} /* Resources */ = {isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = (
${resourceBuildFiles}
\t\t\t); runOnlyForDeploymentPostprocessing = 0; };
/* End PBXResourcesBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
\t\t${id('sources')} /* Sources */ = {isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = (
${sourceBuildFiles}
\t\t\t); runOnlyForDeploymentPostprocessing = 0; };
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
\t\t${id('projectDebug')} /* Debug */ = {isa = XCBuildConfiguration; buildSettings = {ALWAYS_SEARCH_USER_PATHS = NO; CLANG_ENABLE_MODULES = YES; CLANG_ENABLE_OBJC_ARC = YES; DEBUG_INFORMATION_FORMAT = dwarf; ENABLE_TESTABILITY = YES; GCC_C_LANGUAGE_STANDARD = gnu17; GCC_OPTIMIZATION_LEVEL = 0; IPHONEOS_DEPLOYMENT_TARGET = 16.0; ONLY_ACTIVE_ARCH = YES; SDKROOT = iphoneos; SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG; SWIFT_OPTIMIZATION_LEVEL = "-Onone"; }; name = Debug; };
\t\t${id('projectRelease')} /* Release */ = {isa = XCBuildConfiguration; buildSettings = {ALWAYS_SEARCH_USER_PATHS = NO; CLANG_ENABLE_MODULES = YES; CLANG_ENABLE_OBJC_ARC = YES; DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym"; GCC_C_LANGUAGE_STANDARD = gnu17; IPHONEOS_DEPLOYMENT_TARGET = 16.0; SDKROOT = iphoneos; SWIFT_COMPILATION_MODE = wholemodule; VALIDATE_PRODUCT = YES; }; name = Release; };
\t\t${id('targetDebug')} /* Debug */ = {isa = XCBuildConfiguration; buildSettings = {ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon; ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor; CODE_SIGN_STYLE = Automatic; CURRENT_PROJECT_VERSION = 23; DEVELOPMENT_ASSET_PATHS = ""; ENABLE_PREVIEWS = YES; GENERATE_INFOPLIST_FILE = NO; INFOPLIST_FILE = CANVAULT/Info.plist; INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents = YES; IPHONEOS_DEPLOYMENT_TARGET = 16.0; MARKETING_VERSION = 1.9.3; PRODUCT_BUNDLE_IDENTIFIER = com.kronos481.canvault; PRODUCT_NAME = "$(TARGET_NAME)"; SUPPORTED_PLATFORMS = "iphoneos iphonesimulator"; SWIFT_EMIT_LOC_STRINGS = YES; SWIFT_VERSION = 5.0; TARGETED_DEVICE_FAMILY = "1,2"; }; name = Debug; };
\t\t${id('targetRelease')} /* Release */ = {isa = XCBuildConfiguration; buildSettings = {ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon; ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor; CODE_SIGN_STYLE = Automatic; CURRENT_PROJECT_VERSION = 23; DEVELOPMENT_ASSET_PATHS = ""; ENABLE_PREVIEWS = YES; GENERATE_INFOPLIST_FILE = NO; INFOPLIST_FILE = CANVAULT/Info.plist; INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents = YES; IPHONEOS_DEPLOYMENT_TARGET = 16.0; MARKETING_VERSION = 1.9.3; PRODUCT_BUNDLE_IDENTIFIER = com.kronos481.canvault; PRODUCT_NAME = "$(TARGET_NAME)"; SUPPORTED_PLATFORMS = "iphoneos iphonesimulator"; SWIFT_EMIT_LOC_STRINGS = YES; SWIFT_VERSION = 5.0; TARGETED_DEVICE_FAMILY = "1,2"; }; name = Release; };
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
\t\t${id('projectConfigList')} /* Build configuration list for PBXProject "CANVAULT" */ = {isa = XCConfigurationList; buildConfigurations = (${id('projectDebug')} /* Debug */, ${id('projectRelease')} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; };
\t\t${id('targetConfigList')} /* Build configuration list for PBXNativeTarget "CANVAULT" */ = {isa = XCConfigurationList; buildConfigurations = (${id('targetDebug')} /* Debug */, ${id('targetRelease')} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; };
/* End XCConfigurationList section */
\t};
\trootObject = ${id('project')} /* Project object */;
}
`;

fs.mkdirSync(projectRoot, { recursive: true });
fs.writeFileSync(path.join(projectRoot, 'project.pbxproj'), project, 'utf8');
console.log(`Generated Xcode project with ${swiftFiles.length} Swift files.`);

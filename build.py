"""Build both debug APKs with Python + installed SDK/JDK, without Gradle.
Usage: python build.py [--test-only] [--flavor full|rootOnly|all]
Dependencies are pinned; rootOnly downloads/includes no Shizuku library.
"""
from pathlib import Path
import argparse
import os
import re
import shutil
import subprocess
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent
SDK = Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or
           str(Path.home() / 'AppData/Local/Android/Sdk'))
PLATFORM = SDK / 'platforms/android-36'
TOOLS = SDK / 'build-tools/36.0.0'
BUILD = ROOT / 'build'
LIB = ROOT / 'libs'
ANDROID_NS = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', ANDROID_NS)


def run(*args):
    args = list(map(str, args))
    print('>', ' '.join(args), flush=True)
    subprocess.run(args, check=True, cwd=ROOT)


def jdk_bin():
    result = subprocess.run(['java', '-XshowSettings:properties', '-version'],
                            capture_output=True, text=True, check=True)
    match = re.search(r'java.home\s*=\s*(.+)', result.stderr)
    if not match:
        raise RuntimeError('JDK 17+ required: cannot determine java.home')
    return Path(match.group(1).strip()) / 'bin'


def tool(name):
    return TOOLS / (name + ('.exe' if os.name == 'nt' else ''))


def java_tool(jdk, name):
    return jdk / (name + ('.exe' if os.name == 'nt' else ''))


def download(url, target):
    if target.exists():
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    temp = target.with_suffix(target.suffix + '.download')
    print('Download:', url, flush=True)
    if shutil.which('curl.exe'):
        run('curl.exe', '--fail', '--location', '--retry', '2', '--max-time', '90', '--output', temp, url)
    else:
        with urllib.request.urlopen(url, timeout=60) as response, temp.open('wb') as f:
            shutil.copyfileobj(response, f)
    temp.replace(target)


def dependencies():
    jars = []
    for artifact in ('api', 'provider', 'aidl', 'shared'):
        aar = LIB / f'shizuku-{artifact}-13.1.5.aar'
        download(f'https://repo.maven.apache.org/maven2/dev/rikka/shizuku/{artifact}/13.1.5/{artifact}-13.1.5.aar', aar)
        jar = LIB / f'shizuku-{artifact}-13.1.5.jar'
        with zipfile.ZipFile(aar) as archive:
            jar.write_bytes(archive.read('classes.jar'))
        jars.append(jar)
    annotation = LIB / 'annotation-1.3.0.jar'
    download('https://dl.google.com/dl/android/maven2/androidx/annotation/annotation/1.3.0/annotation-1.3.0.jar', annotation)
    return jars + [annotation]


def fresh(path):
    # Delete only our derived subdirectory after proving its resolved boundary.
    path = path.resolve()
    if not path.is_relative_to(BUILD.resolve()) or path == BUILD.resolve():
        raise RuntimeError('Refusing cleanup outside build subdirectory')
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)


def tests(jdk):
    directory = BUILD / 'tests'
    fresh(directory)
    common = ROOT / 'app/src/main/java/kr/local/anticamera'
    run(java_tool(jdk, 'javac'), '-encoding', 'UTF-8', '--release', '8', '-classpath', PLATFORM / 'android.jar',
        '-d', directory, common / 'Shell.java', common / 'CommandResult.java', common / 'SettingsLogic.java',
        ROOT / 'tests/LogicTest.java')
    run(java_tool(jdk, 'java'), '-cp', directory, 'kr.local.anticamera.LogicTest')


def manifest(flavor, output):
    root = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    package = 'kr.local.anticamera' + ('.root' if flavor == 'rootOnly' else '')
    root.set('package', package)
    root.set(f'{{{ANDROID_NS}}}versionCode', '1')
    root.set(f'{{{ANDROID_NS}}}versionName', '1.0')
    ET.SubElement(root, 'uses-sdk', {f'{{{ANDROID_NS}}}minSdkVersion': '28', f'{{{ANDROID_NS}}}targetSdkVersion': '36'})
    app = root.find('application')
    # Resolve relative class names before applying rootOnly applicationIdSuffix.
    app.find('activity').set(f'{{{ANDROID_NS}}}name', 'kr.local.anticamera.MainActivity')
    overlay = ET.parse(ROOT / f'app/src/{flavor}/AndroidManifest.xml').getroot()
    for element in overlay:
        if element.tag == 'application':
            app.attrib.update(element.attrib)
            for child in element:
                for attribute, value in list(child.attrib.items()):
                    child.set(attribute, value.replace('${applicationId}', package))
                app.append(child)
        else:
            root.append(element)
    ET.ElementTree(root).write(output, encoding='utf-8', xml_declaration=True)


def build(flavor, jdk):
    directory = BUILD / flavor
    fresh(directory)
    generated, classes, dex = [directory / name for name in ('generated', 'classes', 'dex')]
    for path in (generated, classes, dex):
        path.mkdir()
    jars = dependencies() if flavor == 'full' else []
    sources = list((ROOT / 'app/src/main/java').rglob('*.java'))
    sources += list((ROOT / f'app/src/{flavor}/java').rglob('*.java'))
    if flavor == 'full':
        aidl_dir = ROOT / 'app/src/full/aidl'
        run(tool('aidl'), '-p' + str(PLATFORM / 'framework.aidl'), '-I' + str(aidl_dir), '-o' + str(generated),
            aidl_dir / 'kr/local/anticamera/ISettingsService.aidl')
        sources += list(generated.rglob('*.java'))
    cp = os.pathsep.join(map(str, [PLATFORM / 'android.jar'] + jars))
    run(java_tool(jdk, 'javac'), '-encoding', 'UTF-8', '--release', '8', '-classpath', cp, '-d', classes, *sources)
    own = directory / 'app-classes.jar'
    with zipfile.ZipFile(own, 'w', zipfile.ZIP_DEFLATED) as archive:
        for f in classes.rglob('*.class'):
            archive.write(f, f.relative_to(classes).as_posix())
    run(java_tool(jdk, 'java'), '-cp', TOOLS / 'lib/d8.jar', 'com.android.tools.r8.D8', '--min-api', '28',
        '--lib', PLATFORM / 'android.jar', '--output', dex, own, *jars)
    merged = directory / 'AndroidManifest.xml'
    manifest(flavor, merged)
    unsigned = directory / 'unsigned.apk'
    run(tool('aapt2'), 'link', '-I', PLATFORM / 'android.jar', '--manifest', merged, '-o', unsigned)
    with zipfile.ZipFile(unsigned, 'a', zipfile.ZIP_DEFLATED) as archive:
        for f in dex.glob('classes*.dex'):
            archive.write(f, f.name)
    aligned = directory / 'aligned.apk'
    run(tool('zipalign'), '-f', '-p', '4', unsigned, aligned)
    key = BUILD / 'debug.keystore'
    if not key.exists():
        run(java_tool(jdk, 'keytool'), '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android',
            '-alias', 'androiddebugkey', '-dname', 'CN=AntiCameraOff Local Debug', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000')
    output = ROOT / ('AntiCameraOff-' + flavor + '-debug.apk')
    run(java_tool(jdk, 'java'), '-jar', TOOLS / 'lib/apksigner.jar', 'sign', '--ks', key, '--ks-pass', 'pass:android',
        '--key-pass', 'pass:android', '--out', output, aligned)
    run(java_tool(jdk, 'java'), '-jar', TOOLS / 'lib/apksigner.jar', 'verify', '--verbose', output)
    run(tool('aapt2'), 'dump', 'badging', output)
    with zipfile.ZipFile(output) as archive:
        if any(f.startswith('lib/') for f in archive.namelist()):
            raise RuntimeError('Unexpected native dependency')
    print('APK:', output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-only', action='store_true')
    parser.add_argument('--flavor', choices=['full', 'rootOnly', 'all'], default='all')
    opts = parser.parse_args()
    if not (PLATFORM / 'android.jar').exists() or not TOOLS.exists():
        parser.error('Install Android SDK platform 36 + build-tools 36.0.0; set ANDROID_SDK_ROOT if needed.')
    BUILD.mkdir(exist_ok=True)
    jdk = jdk_bin()
    tests(jdk)
    if not opts.test_only:
        for flavor in (['full', 'rootOnly'] if opts.flavor == 'all' else [opts.flavor]):
            build(flavor, jdk)

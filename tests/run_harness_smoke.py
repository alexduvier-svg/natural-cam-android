#!/usr/bin/env python3
"""Reproducible host-only smoke checks; never claims Android/device validation."""
from pathlib import Path
import subprocess, tempfile, xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
java = root/'app/src/main/java/br/com/duvier/camera'
engine = (java/'CameraEngine.java').read_text(encoding='utf-8')
ui = (java/'MainActivity.java').read_text(encoding='utf-8')
manifest = ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
assert manifest.attrib['package']=='br.com.duvier.camera.lab'
assert manifest.attrib[ns+'versionCode']=='4'
assert manifest.attrib[ns+'versionName']=='0.2.2-teste.1'
checks = {
    'bounded AWB grace': 'CaptureInteractionPolicy.MAX_AWB_GRACE_MS' in engine and 'attempt<5' not in engine,
    'immediate finger-down shutter': 'case MotionEvent.ACTION_DOWN:' in ui and 'performClick();return true;' in ui,
    'one-shot recovery': 'CaptureInteractionPolicy.restorePrevious' in engine and 'recoveringOpen=true' in engine,
    'lens error recovery': 'onError(CameraDevice camera,int code){camera.close();if(token==generation)recoverOpening' in engine,
    'lens disconnect recovery': 'onDisconnected(CameraDevice camera){camera.close();if(token==generation)recoverOpening' in engine,
    'open timeout recovery': 'onConfigureTimeout(){if(active&&opening&&!configured)recoverOpening' in engine,
    'no pre-emptive old-lens crop': 'Do not digitally stretch the OLD lens' in ui,
    'new-frame zoom gate': 'if(awaitingFrame&&frameReady)' in ui,
    'new-frame ready callback': 'frameReady=true;details.setText(text)' in ui,
    'high mode state restoration': 'public void highRestored(boolean restored)' in ui,
    'correct latency instrumentation': 'shutter_request_to_capture_submit_ms' in engine and 'shutter_press_to_save_ms' in engine,
    'AWB auto retained': 'set(r,CaptureRequest.CONTROL_AWB_MODE,CameraMetadata.CONTROL_AWB_MODE_AUTO)' in engine,
    'jpeg noise processing retains template': 'if(processingOff&&contains(noise,CameraMetadata.NOISE_REDUCTION_MODE_OFF))' in engine,
    'original RAW branch retained': 'DngCreator creator=new DngCreator' in engine,
    'image quality retained': 'r.set(CaptureRequest.JPEG_QUALITY,(byte)100)' in engine,
    'large ML asset retained': (root/'app/src/main/assets/depth_anything_v2.tflite').stat().st_size>95_000_000,
}
for name, passed in checks.items():
    print(('OK ' if passed else 'FAIL ')+name)
assert all(checks.values()), 'Host static smoke failed'
with tempfile.TemporaryDirectory() as out:
    subprocess.run(['javac','--release','8','-Xlint:-options','-encoding','UTF-8','-d',out,
                    *map(str, [java/(x+'.java') for x in ('CaptureMath','CaptureInteractionPolicy','ExposureScale','FrameGeometry','LensRouter')]),
                    str(root/'tests/CaptureMathTest.java'),str(root/'tests/CaptureInteractionPolicyTest.java')],check=True)
    for test in ['CaptureMathTest','CaptureInteractionPolicyTest']:
        subprocess.run(['java','-cp',out,test],check=True)
print(f'APROVADOS: {len(checks)} contratos estruturais + 22 asserções Java puras')
print('NÃO EXECUTADOS: compilação Android, instrumentação Android, ensaios fotográficos ou latência física')

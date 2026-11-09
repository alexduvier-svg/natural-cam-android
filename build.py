#!/usr/bin/env python3
"""Natural Cam. JDK 17+, Android SDK 36; dependencies are pinned in deps/."""
import argparse,pathlib,subprocess,zipfile,shutil,hashlib,json
p=argparse.ArgumentParser();p.add_argument('--sdk',required=True,type=pathlib.Path);p.add_argument('--keystore',required=True,type=pathlib.Path);p.add_argument('--password-file',required=True,type=pathlib.Path);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent;out=root/'out';out.mkdir(exist_ok=True)
tools=a.sdk/'build-tools/36.0.0';tools=tools if (tools/'aapt2').is_file() else None
if tools is None:raise SystemExit('Android build-tools missing')
android=a.sdk/'platforms/android-36/android.jar'
def run(*args):subprocess.run([str(x) for x in args],check=True,cwd=root)
for name in ['classes','dex']:
 d=out/name
 if d.exists():shutil.rmtree(d)
 d.mkdir()
for n in ('aapt2','zipalign'):(tools/n).chmod((tools/n).stat().st_mode|0o111)
run(tools/'aapt2','compile','--dir',root/'app/src/main/res','-o',out/'resources.zip')
run(tools/'aapt2','link','-I',android,'--manifest',root/'app/src/main/AndroidManifest.xml','--min-sdk-version','31','--target-sdk-version','36','-A',root/'app/src/main/assets','-0','tflite','-o',out/'resources.apk',out/'resources.zip')
dependencies=sorted((root/'deps').glob('*.jar'))
classpath=':'.join(map(str,[android]+dependencies));sources=sorted((root/'app/src/main/java').rglob('*.java'))
run('java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','--release','8','-classpath',classpath,'-d',out/'classes',*sources)
with zipfile.ZipFile(out/'classes.jar','w',zipfile.ZIP_DEFLATED) as z:
 for f in (out/'classes').rglob('*.class'):z.write(f,f.relative_to(out/'classes'))
run('java','-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--min-api','31','--output',out/'dex',out/'classes.jar',*dependencies)
shutil.copyfile(out/'resources.apk',out/'unsigned.apk')
with zipfile.ZipFile(out/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in (out/'dex').glob('*.dex'):z.write(f,f.name)
 for f in (root/'app/src/main/jniLibs').rglob('*.so'):z.write(f,'lib/'+str(f.relative_to(root/'app/src/main/jniLibs')),compress_type=zipfile.ZIP_STORED)
run(tools/'zipalign','-P','16','-f','4',out/'unsigned.apk',out/'aligned.apk')
apk=out/'Natural_Cam_0.2.2-teste.1.apk'
run('java','-jar',tools/'lib/apksigner.jar','sign','--ks',a.keystore,'--ks-key-alias','duvier-camera-lab','--ks-pass','file:'+str(a.password_file),'--out',apk,out/'aligned.apk')
run('java','-jar',tools/'lib/apksigner.jar','verify','--verbose','--print-certs',apk);run(tools/'zipalign','-P','16','-c','4',apk)
print(json.dumps({'apk':str(apk),'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest()}))

from pathlib import Path
import subprocess,zipfile,shutil,argparse
p=argparse.ArgumentParser();p.add_argument('--sdk',required=True,type=Path);p.add_argument('--keystore',required=True,type=Path);p.add_argument('--password-file',required=True,type=Path);a=p.parse_args()
root=Path(__file__).resolve().parents[1];out=root/'out/instrumentation';out.mkdir(parents=True,exist_ok=True);sdk=a.sdk.resolve();tools=next(x for x in (sdk/'build-tools').iterdir() if (x/'aapt2').is_file());android=sdk/'platforms/android-36/android.jar';deps=list((root/'deps').glob('*.jar'));cp=':'.join(map(str,[android,root/'out/classes.jar']+deps))
def run(*args):subprocess.run(list(map(str,args)),check=True)
classes=out/'classes';classes.mkdir(exist_ok=True)
run('java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-classpath',cp,'-d',classes,*sorted((root/'tests/android').glob('*.java')))
with zipfile.ZipFile(out/'test-classes.jar','w') as z:
 for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
dex=out/'dex';dex.mkdir(exist_ok=True)
run('java','-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--lib',root/'out/classes.jar','--min-api','31','--output',dex,out/'test-classes.jar')
run(tools/'aapt2','link','-I',android,'--manifest',root/'tests/android/AndroidManifest.xml','-o',out/'unsigned.apk')
with zipfile.ZipFile(out/'unsigned.apk','a') as z:z.write(dex/'classes.dex','classes.dex')
run(tools/'zipalign','-f','4',out/'unsigned.apk',out/'aligned.apk')
run('java','-jar',tools/'lib/apksigner.jar','sign','--ks',a.keystore,'--ks-key-alias','duvier-camera-lab','--ks-pass','file:'+str(a.password_file),'--out',out/'Natural_Cam_QA.apk',out/'aligned.apk')
print(out/'Natural_Cam_QA.apk')

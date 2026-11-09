#!/usr/bin/env python3
"""Deterministic core checks; provide a real org.json JAR, not Android SDK stubs."""
import argparse,pathlib,subprocess
p=argparse.ArgumentParser();p.add_argument('--json-jar',required=True,type=pathlib.Path);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];out=root/'out/host-tests';out.mkdir(parents=True,exist_ok=True)
package=root/'app/src/main/java/br/com/duvier/camera'
names=['CaptureMath','FrameGeometry','LensRouter','RawDngCrop','EditRecipe','StyleCatalog','ColorPipeline','SceneIntelligence','ScenePolicy','ExposureScale']
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-cp',str(a.json_jar.resolve()),'-d',str(out),*[str(package/(n+'.java')) for n in names],str(root/'tests/QualityRegressionTest.java'),str(root/'tests/CaptureMathTest.java')],check=True)
for name in ('QualityRegressionTest','CaptureMathTest'):
 subprocess.run(['java','-cp',str(out)+':'+str(a.json_jar.resolve()),name],check=True)

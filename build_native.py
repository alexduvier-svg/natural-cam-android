#!/usr/bin/env python3
import argparse,pathlib,subprocess,os
p=argparse.ArgumentParser();p.add_argument('--ndk',required=True,type=pathlib.Path);p.add_argument('--cmake',required=True);p.add_argument('--ninja',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent;ndk=a.ndk.resolve();llvm=ndk/'toolchains/llvm/prebuilt/linux-x86_64';vendor=root/'deps/libjpeg-turbo-3.1.2'
for abi,triple in [('arm64-v8a','aarch64-linux-android'),('x86_64','x86_64-linux-android')]:
    out=root/'out/native'/abi;out.mkdir(parents=True,exist_ok=True)
    subprocess.run([a.cmake,'-G','Ninja','-S',str(vendor),'-B',str(out),'-DCMAKE_MAKE_PROGRAM='+a.ninja,'-DCMAKE_TOOLCHAIN_FILE='+str(ndk/'build/cmake/android.toolchain.cmake'),'-DANDROID_ABI='+abi,'-DANDROID_PLATFORM=android-31','-DENABLE_SHARED=OFF','-DENABLE_STATIC=ON','-DWITH_TURBOJPEG=OFF','-DWITH_SIMD=OFF','-DCMAKE_BUILD_TYPE=Release','-DCMAKE_POSITION_INDEPENDENT_CODE=ON'],check=True)
    subprocess.run([a.cmake,'--build',str(out),'--target','jpeg-static','-j','4'],check=True)
    target=root/'app/src/main/jniLibs'/abi;target.mkdir(parents=True,exist_ok=True)
    subprocess.run([str(llvm/'bin'/f'{triple}31-clang'),'-shared','-fPIC','-O3','-Wl,-z,max-page-size=16384','-Wl,-soname,libduvier_jpeg.so','-I'+str(vendor/'src'),'-I'+str(out),str(root/'app/src/main/cpp/jpeg_stream.c'),str(out/'libjpeg.a'),'-o',str(target/'libduvier_jpeg.so')],check=True)
    print('JPEG streaming',abi,'built',flush=True)

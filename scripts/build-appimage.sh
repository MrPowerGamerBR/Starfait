#!/bin/bash
set -euxo pipefail

SCRIPT_FOLDER="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
APPIMAGE_METADATA=$SCRIPT_FOLDER/appimage/

rm -rf /tmp/starfait-appimage/
mkdir -p /tmp/starfait-appimage/Starfait.AppDir
cp -r starfait-runner-sdl3/build/install/starfait-runner-sdl3/* /tmp/starfait-appimage/Starfait.AppDir/
jlink --add-modules java.base,java.desktop,java.logging,java.sql,jdk.unsupported --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /tmp/starfait-appimage/Starfait.AppDir/runtime
cp $APPIMAGE_METADATA/AppRun $APPIMAGE_METADATA/icon.png $APPIMAGE_METADATA/starfait.desktop /tmp/starfait-appimage/Starfait.AppDir/
cd /tmp/starfait-appimage/
wget -O /tmp/starfait-appimage/appimagetool-x86_64.AppImage "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"
chmod 777 appimagetool-x86_64.AppImage
ARCH=x86_64 ./appimagetool-x86_64.AppImage Starfait.AppDir
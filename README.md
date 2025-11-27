<div align="center">
  <h1>War Claims</h1>
  <a href="https://github.com/fawnoculus/WarClaims/blob/master/LICENSE.txt"><img src="https://img.shields.io/github/license/fawnoculus/WarClaims?style=flat&color=900c3f" alt="Licence: GPL-3.0"></a>
  <a href="https://github.com/fawnoculus/WarClaims/actions/workflows/build.yml"><img src="https://github.com/fawnoculus/WarClaims/actions/workflows/build.yml/badge.svg" alt="Build Workflow"></a>
</div>

## What is War Claims?
War Claims is Claim mod with War in Mind.
This is not just the only (maybe) Claim with Xaero's World-/ MiniMap support but also ...
I forgot how to formulate sentences... ...
I should probably not be writing READMEs at 5 in the Morning.
I'll finish the README later, I swear.

## Downloading from GitHub actions
1. Navigate to the Latest (topmost) successfully ran (green check) [Action](https://github.com/fawnoculus/WarClaims/actions/workflows/build.yml)
2. Click on that bad boy
3. On the bottom right there should be a button for downloading the Artifact (you may need to scroll down)
4. If you unzip the File you should now have a working version of the mod (you don't want the file that ends in "-sources.jar")

## Building it from Source
Building it from source should be unnecessary as you can download a jar of the latest commit from [GitHub Actions](https://github.com/fawnoculus/WarClaims/actions/workflows/build.yml)
* Make sure you have [**JDK-21**](https://adoptium.net/temurin/releases/?variant=openjdk8&jvmVariant=hotspot&package=jdk&version=21) and [**git**](https://git-scm.com/downloads) installed
* Open PowerShell (or Bash if you are using Linux)
* Navigate to the Directory you wish to copy the Sources to
```bash
cd $HOME/Downloads/
```
* Download the Sources
```bash
git clone https://github.com/fawnoculus/WarClaims
```
* enter the sources directory
```bash
cd WarClaims
```
* build the Mod
```bash
./gradlew build
```
If the Command Returns with saying **BUILD SUCCESSFUL** then you should be able to find the mod file at "Downloads/WarClaims/build/libs/WarClaims-VERSION.jar"

## Licence
This software is licensed under the GNU Public License version 3. In short: This software is free, you may run the software freely, create modified versions,
distribute this software and distribute modified versions, as long as the modified software too has a free software license. The full license can be found in the `LICENSE.txt` file.

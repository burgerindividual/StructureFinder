# StructureFinder
StructureFinder is a program that is built to find structures from a seed in Minecraft and list their coordinates.

It relies on [Amidst](https://github.com/toolbox4minecraft/amidst) used as a library do most of the work.

## [Releases](https://github.com/burgerguy/StructureFinder/releases/)

## Features

- Easy to use
- Much quicker than using plain Amidst or /locate
- Can get structures in bulk

## TODO's

- [ ] Add a logger
- [x] Sort by nearest
- [x] Make UI look better
- [ ] Accurate End City finding through chunk generation
- [x] Fix Nether Fortresses
- [x] Add Buried Treasure Support
- [x] Add Pillager Outpost Support
- [x] Fix Strongholds
- [ ] ~~Better multithreading based off core count~~ (Worsens performance in most cases)

## Build Requirements

- [Java 1.8](https://www.java.com/en/download/windows-64bit.jsp) or newer
- [Eclipse Java](https://www.eclipse.org/downloads/packages/installer) (2019-06)
- [Apache Maven 3.6.1](https://maven.apache.org/download.cgi)
- All project files including libraries (/lib)

In Eclipse, go to File > Open Projects from File System... and locate the project files. Then just click Finish.

### Building the jar

With maven downloaded and set up, enter the command `mvn clean compile assembly:single`
The jar will be made in the /target directory

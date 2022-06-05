# PengLib

Collecting some infrastructure library in SpinalHDL. The feature of this library includes:

* Customizable SRAM converter
* Automatic Ping pong wrapper
* Some mathematics libraries
* and others...

## Usage

1. create your own SpinalHDL project.

```shell
sbt new wswslzp/spinal.g8
```

2. clone this project in the same directory as your project resides.

```shell
git clone git@github.com:wswslzp/PengLib.git
```

3. configure your `build.sbt` in your own project as follows

```scala
// give the user a nice default project!
ThisBuild / organization := "com.github.wswslzp"
ThisBuild / scalaVersion := "2.12.13"
ThisBuild / version := "v0.1"

val spinalVersion = "1.7.0"
val spinal = Seq(
  "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
  "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion,
  compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)
)

// point to the PengLib directory
lazy val penglib = ProjectRef(file("../PengLib"), "penglib")

lazy val root = (project in file(".")).
  settings(
    name := "usepeng",
    libraryDependencies ++= spinal,
    fork := true,
    javaOptions := Seq("-Xmx16G")
  ).dependsOn(penglib) // remember adding dependency on penglib
```

Then your project now is depending on `PengLib`. Also you can download this repo and publish local `jar` files then use the jar library in your project as well.

## Math library

The math lib lies in `src/main/MathLib`. Two basic number types
are included - Fixed number and Complex number. The Fixed number 
here is an extension of `spinal` built-in `SFix` and `UFix`. And 
the Complex number `HComplex` is implemented as a bundle, combining
real part and imagine part of `SFix`. The basic operations like 
+/-/*/div/conj etc., are implemented as methods. 

Some math functions like `sqrt` are also implemented based on these two number types.

Fast Fourier Transformation (FFT) is also implemented, both for 
1D and 2D, with fully configurable feature.

Several interpolation methods are also included, such as nearest interpolation,
linear interpolation, bi-linear interpolation. 

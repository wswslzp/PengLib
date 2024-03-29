organization := "com.github.penglib"
name := "PengLib"
version := "1.0"
scalaVersion := "2.12.13"
//scalaVersion := "2.13.6"
val spinalVersion = "1.7.3a"

publishTo := Some("Sonatype Snapshots Nexus" at "https://oss.sonatype.org/content/repositories/snapshots")

libraryDependencies ++= Seq(
  "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
  "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion,
  compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.5"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.10"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"


fork := true

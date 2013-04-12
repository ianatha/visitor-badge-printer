seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

scalaVersion := "2.10.1"

organization := "io.atha"

name := "visitor-badge-printer"

version := "0.10-SNAPSHOT"

libraryDependencies += "batik" % "batik-rasterizer" % "1.6-1"

libraryDependencies += "joda-time" % "joda-time" % "2.2"

libraryDependencies += "org.joda" % "joda-convert" % "1.3.1"

libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.3"

mainClass in oneJar := Some("io.atha.VisitorBadgePrinter.Main")

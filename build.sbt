name := "GeminiTumbler"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(ehcache,
  ws,
  guice,
  filters,
  "net.sf.ehcache" % "ehcache" % "2.10.6" )

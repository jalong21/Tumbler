name := "GeminiTumbler"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(ehcache,
  ws,
  guice,
  filters,
  "net.sf.ehcache" % "ehcache" % "2.10.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "org.mockito" % "mockito-core" % "3.9.0" % Test)

name := """play-scala-forms-select"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"

libraryDependencies += "org.webjars" % "bootstrap" % "3.3.6"

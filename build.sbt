
name := "play-filters"

organization := "eu.byjean"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

libraryDependencies += "com.typesafe.play" %% "play" % "2.6.6"

scalaVersion:= "2.12.3"

crossScalaVersions := Seq("2.11.11","2.12.3")

lazy val playFilters = (project in file(".")).enablePlugins(ScalafmtPlugin)

publishTo in ThisBuild:= Some("eu.byjean.repo" at "https://api.bintray.com/maven/jeantil/maven/play-filters")

version in ThisBuild := {git.gitHeadCommit.value map (sha => sha)}.getOrElse("0")

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

versionWithGit

// Optionally:
git.baseVersion := "2.6.0"

enablePlugins(ScalafmtPlugin)

scalafmtVersion := "1.3.0"

updateOptions := updateOptions.value.withCachedResolution(true)

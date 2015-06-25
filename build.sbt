import com.typesafe.sbt.SbtScalariform._

name := "play-filters"

organization := "eu.byjean"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

libraryDependencies += "com.typesafe.play" %% "play" % "2.4.0"

scalaVersion:= "2.11.6"

crossScalaVersions := Seq("2.11.6", "2.10.4")

lazy val playFilters = (project in file("."))

publishTo in ThisBuild:= Some("eu.byjean.repo" at "https://api.bintray.com/maven/jeantil/maven/play-filters")

version in ThisBuild <<= git.gitHeadCommit apply ((headCommit)=> {headCommit map (sha => sha)}.getOrElse("0"))

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

versionWithGit

// Optionally:
git.baseVersion := "2.4.0"

scalariformSettings

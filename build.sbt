name := "play-filters"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

libraryDependencies += "com.typesafe.play" %% "play" % "2.3.8"

scalaVersion:= "2.11.5"

crossScalaVersions := Seq("2.11.5", "2.10.4")

lazy val playFilters = (project in file("."))

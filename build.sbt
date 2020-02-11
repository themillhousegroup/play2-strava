name := "play2-strava"

// If the CI supplies a "build.version" environment variable, inject it as the rev part of the version number:
version := s"${sys.props.getOrElse("build.majorMinor", "0.4")}.${sys.props.getOrElse("build.version", "SNAPSHOT")}"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.12.8", "2.11.7")

organization := "com.themillhousegroup"

val targetPlayVersion = "2.6.25"

val minimumSpecs2Version = "[4.8,)"

libraryDependencies ++= Seq(
    "com.typesafe.play"           %%  "play-ws"               % targetPlayVersion      	% "provided",
    "com.typesafe.play"           %%  "play-cache"            % targetPlayVersion      	% "provided",
    "joda-time"                   % "joda-time"               % "2.9.9",
    "org.mockito"                 %   "mockito-all"           % "1.10.19"       				% "test",
		"org.specs2"                  %%  "specs2-core"           % minimumSpecs2Version      % "test",
    "org.specs2"                  %%  "specs2-mock"           % minimumSpecs2Version      % "test"
)

resolvers ++= Seq(  "oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
                    "oss-releases"  at "https://oss.sonatype.org/content/repositories/releases",
                    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/")

jacoco.settings

publishArtifact in (Compile, packageDoc) := false

seq(bintraySettings:_*)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalariformSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings


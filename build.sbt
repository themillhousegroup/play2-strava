name := "play2-strava"

// If the CI supplies a "build.version" environment variable, inject it as the rev part of the version number:
version := s"${sys.props.getOrElse("build.majorMinor", "0.2")}.${sys.props.getOrElse("build.version", "SNAPSHOT")}"

scalaVersion := "2.11.7"

organization := "com.themillhousegroup"

val targetPlayVersion = "2.5.3"

libraryDependencies ++= Seq(
    "com.typesafe.play"           %%  "play-ws"               % targetPlayVersion      % "provided",
    "com.typesafe.play"           %%  "play-cache"            % targetPlayVersion      % "provided",
    "joda-time"                   % "joda-time"               % "2.9.9",
    "org.mockito"                 %   "mockito-all"           % "1.10.19"       % "test",
    "org.specs2"                  %%  "specs2"                % "2.3.13"      % "test"
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


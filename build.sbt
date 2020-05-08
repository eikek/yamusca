import libs._
import xerial.sbt.Sonatype._
import ReleaseTransformations._

val scalacOpts: Seq[String] = Seq(
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import"
)

lazy val commonSettings = Seq(
  name := "yamusca",
  organization := "com.github.eikek",
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek")),
  scalaVersion := scalaVersion213,
  crossScalaVersions := Seq(scalaVersion212, scalaVersion213),
  scalacOptions := {
    if (scalaBinaryVersion.value.startsWith("2.13")) {
      scalacOpts.filter(o => o != "-Yno-adapted-args" && o != "-Ywarn-unused-import")
    } else {
      scalacOpts
    }
  },
  scalacOptions in (Compile, console) := Seq(),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  initialCommands := """
    import yamusca.imports._
    import yamusca.implicits._
    import yamusca.parser.ParseInput
  """
) ++ publishSettings

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/eikek/yamusca.git"),
      "scm:git:git@github.com:eikek/yamusca.git"
    )
  ),
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = url("https://github.com/eikek"),
      email = ""
    )
  ),
  publishTo := sonatypePublishToBundle.value,
  publishArtifact in Test := false,
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    // For non cross-build projects, use releaseStepCommand("publishSigned")
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  sonatypeProjectHosting := Some(GitHubHosting("eikek", "yamusca", "eike.kettner@posteo.de"))
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val macros = project.in(file("modules/macros")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "yamusca-macros",
    incOptions := incOptions.value.withLogRecompileOnMacro(false),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    ))

lazy val core = (project in file("modules/core")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "yamusca-core",
    libraryDependencies ++= Seq(
      scalatest % "test",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    )).
  dependsOn(macros)

lazy val circe = project.in(file("modules/circe")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "yamusca-circe",
    description := "Provide value converter for circes json values",
    libraryDependencies ++= Seq(
      `circe-core`,
      scalatest % "test", `circe-generic` % "test"
    )).
  dependsOn(core)

lazy val benchmark = project.in(file("modules/benchmark")).
  enablePlugins(JmhPlugin).
  settings(commonSettings).
  settings(noPublish).
  settings(
    name := "yamusca-benchmark",
    libraryDependencies ++= Seq(
      `mustache-java`, `circe-parser`, `circe-generic`, `scalate-core`
    )
  ).
  dependsOn(core, circe)

lazy val root = project.in(file(".")).
  settings(commonSettings).
  settings(noPublish).
  aggregate(core, macros, circe, benchmark)

addCommandAlias("bench-parse-quick", ";project benchmark ;jmh:run -f1 -wi 2 -i 2 .*ParserBenchmark")

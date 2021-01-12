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

val updateReadme = inputKey[Unit]("Update readme")

lazy val commonSettings = Seq(
  name := "yamusca",
  organization := "com.github.eikek",
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek")),
  scalaVersion := Dependencies.scalaVersion213,
  crossScalaVersions := Seq(
    Dependencies.scalaVersion212,
    Dependencies.scalaVersion213),
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

lazy val macros = crossProject(JSPlatform, JVMPlatform).
  crossType(CrossType.Pure).
  in(file("modules/macros")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "yamusca-macros",
    incOptions := incOptions.value.withLogRecompileOnMacro(false),
    libraryDependencies ++=
      Dependencies.scalaReflect(scalaVersion.value)
    )

lazy val macrosJVM = macros.jvm
lazy val macrosJS = macros.js

lazy val core = crossProject(JSPlatform, JVMPlatform).
  crossType(CrossType.Pure).
  in(file("modules/core")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "yamusca-core",
    libraryDependencies ++=
      Dependencies.scalatest.map(_ % Test) ++
      Dependencies.scalaReflect(scalaVersion.value)
  ).
  dependsOn(macros)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val circe = crossProject(JSPlatform, JVMPlatform).
  crossType(CrossType.Pure).
  in(file("modules/circe")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "yamusca-circe",
    description := "Provide value converter for circes json values",
    libraryDependencies ++=
      Dependencies.circeCore ++
      (Dependencies.scalatest ++ Dependencies.circeGeneric).map(_ % Test)
    ).
  dependsOn(core)

lazy val circeJVM = circe.jvm
lazy val circeJS = circe.js

lazy val benchmark = project.in(file("modules/benchmark")).
  enablePlugins(JmhPlugin).
  settings(commonSettings).
  settings(noPublish).
  settings(
    name := "yamusca-benchmark",
    libraryDependencies ++=
      Dependencies.mustacheJava ++
      Dependencies.circeParser ++
      Dependencies.circeGeneric ++
      Dependencies.scalateCore
  ).
  dependsOn(coreJVM, circeJVM)

lazy val readme = project
  .in(file("modules/readme"))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(noPublish)
  .settings(
    name := "yamusca-readme",
    libraryDependencies ++=
      Dependencies.circeAll,
    scalacOptions := Seq(),
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    updateReadme := {
      mdoc.evaluated
      val out = mdocOut.value / "readme.md"
      val target = (LocalRootProject / baseDirectory).value / "README.md"
      val logger = streams.value.log
      logger.info(s"Updating readme: $out -> $target")
      IO.copyFile(out, target)
      ()
    }
  )
  .dependsOn(coreJVM, circeJVM)

lazy val root = project.in(file(".")).
  settings(commonSettings).
  settings(noPublish).
  aggregate(coreJVM, coreJS, macrosJVM, macrosJS, circeJVM, circeJS, benchmark)

addCommandAlias("bench-parse-quick", ";project benchmark ;jmh:run -f1 -wi 2 -i 2 .*ParserBenchmark")

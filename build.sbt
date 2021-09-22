addCommandAlias("ci", "; lint; +test; +publishLocal")
addCommandAlias(
  "lint",
  "; scalafmtSbtCheck; scalafmtCheckAll; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "; Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")

addCommandAlias(
  "bench-parse-quick",
  ";project benchmark ;jmh:run -f1 -wi 2 -i 2 .*ParserBenchmark"
)

val updateReadme = inputKey[Unit]("Update readme")

def makeScalacOptions(binaryVersion: String) =
  Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding",
    "UTF-8",
    "-language:higherKinds"
  ) ++
    (if (binaryVersion.startsWith("2.12"))
       List(
         "-Xfatal-warnings", // fail when there are warnings
         "-Xlint",
         "-Yno-adapted-args",
         "-Ywarn-dead-code",
         "-Ywarn-unused",
         "-Ypartial-unification",
         "-Ywarn-value-discard"
       )
     else if (binaryVersion.startsWith("2.13"))
       List("-Werror", "-Wdead-code", "-Wunused", "-Wvalue-discard")
     else
       Nil)

lazy val commonSettings = Seq(
  name := "yamusca",
  organization := "com.github.eikek",
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek/yamusca")),
  versionScheme := Some("early-semver"),
  scalaVersion := Dependencies.Version.scalaVersion213,
  crossScalaVersions := Seq(
    Dependencies.Version.scalaVersion212,
    Dependencies.Version.scalaVersion213
  ),
  scalacOptions ++= makeScalacOptions(scalaBinaryVersion.value),
  Test / scalacOptions := (Compile / scalacOptions).value.filter(e =>
    !e.endsWith("value-discard")
  ),
  Compile / console / scalacOptions := Seq(),
  Test / console / scalacOptions := Seq(),
  initialCommands := """
    import yamusca.imports._
    import yamusca.implicits._
    import yamusca.parser.ParseInput
  """
) ++ publishSettings

lazy val publishSettings = Seq(
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
  )
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val scalafixSettings = Seq(
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
  ThisBuild / scalafixDependencies ++= Dependencies.organizeImports
)

lazy val macros = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/macros"))
  .settings(commonSettings)
  .settings(scalafixSettings)
  .settings(
    name := "yamusca-macros",
    incOptions := incOptions.value.withLogRecompileOnMacro(false),
    libraryDependencies ++=
      Dependencies.scalaReflect(scalaVersion.value)
  )

lazy val macrosJVM = macros.jvm
lazy val macrosJS = macros.js

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .settings(commonSettings)
  .settings(scalafixSettings)
  .settings(
    name := "yamusca-core",
    libraryDependencies ++=
      Dependencies.scalatest.map(_ % Test) ++
        Dependencies.scalaReflect(scalaVersion.value)
  )
  .dependsOn(macros)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val circe = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/circe"))
  .settings(commonSettings)
  .settings(scalafixSettings)
  .settings(
    name := "yamusca-circe",
    description := "Provide value converter for circes json values",
    libraryDependencies ++=
      Dependencies.circeCore ++
        (Dependencies.scalatest ++ Dependencies.circeGeneric).map(_ % Test)
  )
  .dependsOn(core)

lazy val circeJVM = circe.jvm
lazy val circeJS = circe.js

lazy val benchmark = project
  .in(file("modules/benchmark"))
  .enablePlugins(JmhPlugin)
  .settings(commonSettings)
  .settings(noPublish)
  .settings(scalafixSettings)
  .settings(
    name := "yamusca-benchmark",
    scalacOptions := makeScalacOptions(scalaBinaryVersion.value).filter(e =>
      !e.endsWith("value-discard")
    ),
    libraryDependencies ++=
      Dependencies.mustacheJava ++
        Dependencies.circeParser ++
        Dependencies.circeGeneric ++
        Dependencies.scalateCore
  )
  .dependsOn(coreJVM, circeJVM)

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

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(noPublish)
  .aggregate(coreJVM, coreJS, macrosJVM, macrosJS, circeJVM, circeJS, benchmark)

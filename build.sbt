import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import libs._

lazy val commonSettings = Seq(
  name := "yamusca",
  organization := "com.github.eikek",
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek")),
  scalaVersion := `scala-version`,
  crossScalaVersions := Seq("2.11.11", `scala-version`),
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xfatal-warnings", // fail when there are warnings
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import"
  ),
  scalacOptions in (Compile, console) ~= (_ filterNot (Set("-Xfatal-warnings", "-Ywarn-unused-import").contains)),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value
)

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
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false
)


lazy val core = (project in file("modules/core")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "yamusca-core",
    libraryDependencies ++= Seq(
      scalatest
    ).map(_ % "test"))


lazy val benchmark = project.in(file("modules/benchmark")).
  enablePlugins(JmhPlugin).
  settings(commonSettings).
  settings(
    name := "yamusca-benchmark",
    publish := (),
    publishLocal := (),
    publishSigned := (),
    publishArtifact := false,
    libraryDependencies ++= Seq(
      `mustache-java`, `circe-parser`, `circe-generic`, `scalate-core`
    )
  ).
  dependsOn(core)

lazy val root = project.in(file(".")).
  settings(commonSettings).
  aggregate(core, benchmark)

addCommandAlias("bench-parse-quick", ";project benchmark ;jmh:run -f1 -wi 2 -i 2 .*ParserBenchmark")

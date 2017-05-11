import libs._

lazy val commonSettings = Seq(
  name := "yamusca",
  homepage := Some(url("https://github.com/eikek/yamusca")),
  scalaVersion := `scala-version`,
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
//    "-Xfatal-warnings", // fail when there are warnings
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import"
  )
)


lazy val yamusca = (project in file(".")).
  settings(commonSettings).
  settings(
    libraryDependencies ++= Seq(
      scalatest
    ).map(_ % "test"))

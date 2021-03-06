import sbt._

object Dependencies {

  val scalaVersion213 = "2.13.5"
  val scalaVersion212 = "2.12.13"
  val scalaTestVersion = "3.2.6"
  val circeVersion = "0.13.0"
  val scalateVersion = "1.9.6"

  // https://github.com/scalatest/scalatest
  // ASL 2.0
  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion
  )

  val mustacheJava = Seq(
    "com.github.spullara.mustache.java" % "compiler" % "0.9.7"
  )

  // https://github.com/circe/circe
  // ASL 2.0
  val circeCore = Seq(
    "io.circe" %% "circe-core" % circeVersion
  )
  val circeGeneric = Seq(
    "io.circe" %% "circe-generic" % circeVersion
  )
  val circeParser = Seq(
    "io.circe" %% "circe-parser" % circeVersion
  )
  val circeAll = circeCore ++ circeGeneric ++ circeParser

  // https://github.com/scalate/scalate
  // ASL 2.0
  val scalateCore = Seq(
    "org.scalatra.scalate" %% "scalate-core" % scalateVersion
  )

  def scalaReflect(scalaVersion: String): Seq[ModuleID] =
    Seq("org.scala-lang" % "scala-reflect" % scalaVersion % "provided")
}

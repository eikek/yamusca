import sbt._

object Dependencies {

  object Version {

    val scalaVersion213 = "2.13.8"
    val scalaVersion212 = "2.12.15"
    val scalaVersion3 = "3.1.1"
    val scalaTestVersion = "3.2.11"
    val circeVersion = "0.14.1"
    val scalateVersion = "1.9.7"
    val organizeImports = "0.6.0"

  }

  // https://github.com/scalatest/scalatest
  // ASL 2.0
  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % Version.scalaTestVersion
  )

  val mustacheJava = Seq(
    "com.github.spullara.mustache.java" % "compiler" % "0.9.10"
  )

  // https://github.com/circe/circe
  // ASL 2.0
  val circeCore = Seq(
    "io.circe" %% "circe-core" % Version.circeVersion
  )
  val circeGeneric = Seq(
    "io.circe" %% "circe-generic" % Version.circeVersion
  )
  val circeParser = Seq(
    "io.circe" %% "circe-parser" % Version.circeVersion
  )
  val circeAll = circeCore ++ circeGeneric ++ circeParser

  val organizeImports = Seq(
    "com.github.liancheng" %% "organize-imports" % Version.organizeImports
  )

  def scalaReflect(scalaVersion: String): Seq[ModuleID] =
    Seq("org.scala-lang" % "scala-reflect" % scalaVersion % "provided")
}

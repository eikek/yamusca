import sbt._

object Dependencies {

  object Version {

    val scalaVersion213  = "2.13.6"
    val scalaVersion212  = "2.12.13"
    val scalaTestVersion = "3.2.9"
    val circeVersion     = "0.13.0"
    val scalateVersion   = "1.9.6"
    val organizeImports  = "0.5.0"

  }

  // https://github.com/scalatest/scalatest
  // ASL 2.0
  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % Version.scalaTestVersion
  )

  val mustacheJava = Seq(
    "com.github.spullara.mustache.java" % "compiler" % "0.9.7"
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

  // https://github.com/scalate/scalate
  // ASL 2.0
  val scalateCore = Seq(
    "org.scalatra.scalate" %% "scalate-core" % Version.scalateVersion
  )

  val organizeImports = Seq(
    "com.github.liancheng" %% "organize-imports" % Version.organizeImports
  )

  def scalaReflect(scalaVersion: String): Seq[ModuleID] =
    Seq("org.scala-lang" % "scala-reflect" % scalaVersion % "provided")
}

import sbt._

object Dependencies {

  object Version {
    val scala213 = "2.13.16"
    val scala212 = "2.12.20"
    val scala3 = "3.3.6"

    val munitVersion = "1.1.0"
    val munitCatsEffectVersion = "2.1.0"
    val circeVersion = "0.14.14"
    val scalateVersion = "1.9.7"
  }

  val munit = Seq(
    "org.scalameta" %% "munit" % Version.munitVersion,
    "org.scalameta" %% "munit-scalacheck" % Version.munitVersion
  )

  // https://github.com/typelevel/munit-cats-effect
  val munitCatsEffect = Seq(
    "org.typelevel" %% "munit-cats-effect" % Version.munitCatsEffectVersion
  )

  val mustacheJava = Seq(
    "com.github.spullara.mustache.java" % "compiler" % "0.9.14"
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

  def scalaReflect(scalaVersion: String): Seq[ModuleID] =
    Seq("org.scala-lang" % "scala-reflect" % scalaVersion % "provided")
}

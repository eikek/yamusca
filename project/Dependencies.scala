import sbt._

object Dependencies {

  object Version {
    val scala213 = "2.13.14"
    val scala212 = "2.12.19"
    val scala3 = "3.3.3"

    val munitVersion = "0.7.29"
    val munitCatsEffectVersion = "2.0.0"
    val circeVersion = "0.14.7"
    val scalateVersion = "1.9.7"
    val organizeImports = "0.6.0"

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
    "com.github.spullara.mustache.java" % "compiler" % "0.9.13"
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

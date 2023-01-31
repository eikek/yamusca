import sbt._

object Dependencies {

  object Version {
    val scala213 = "2.13.10"
    val scala212 = "2.12.17"
    val scala3 = "3.2.2"

    val munitVersion = "0.7.29"
    val munitCatsEffectVersion = "1.0.7"
    val circeVersion = "0.14.3"
    val scalateVersion = "1.9.7"
    val organizeImports = "0.6.0"

  }

  val munit = Seq(
    "org.scalameta" %% "munit" % Version.munitVersion,
    "org.scalameta" %% "munit-scalacheck" % Version.munitVersion
  )

  // https://github.com/typelevel/munit-cats-effect
  val munitCatsEffect = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % Version.munitCatsEffectVersion
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

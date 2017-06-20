import sbt._

object libs {

  val `scala-version` = "2.12.2"

  // https://github.com/scalatest/scalatest
  // ASL 2.0
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.3"

  val `mustache-java` = "com.github.spullara.mustache.java" % "compiler" % "0.8.18"

  // https://github.com/circe/circe
  // ASL 2.0
  val `circe-core` = "io.circe" %% "circe-core" % "0.8.0"
  val `circe-generic` = "io.circe" %% "circe-generic" % "0.8.0"
  val `circe-parser` = "io.circe" %% "circe-parser" % "0.8.0"

  // https://github.com/rickynils/scalacheck
  // unmodified 3-clause BSD
  // val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.5"

}

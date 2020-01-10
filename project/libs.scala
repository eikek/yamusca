import sbt._

object libs {

  val `scala-version` = "2.13.1"

  // https://github.com/scalatest/scalatest
  // ASL 2.0
  val scalatest = "org.scalatest" %% "scalatest" % "3.1.0"

  val `mustache-java` = "com.github.spullara.mustache.java" % "compiler" % "0.9.6"

  // https://github.com/circe/circe
  // ASL 2.0
  val `circe-core` = "io.circe" %% "circe-core" % "0.12.2"
  val `circe-generic` = "io.circe" %% "circe-generic" % "0.12.2"
  val `circe-parser` = "io.circe" %% "circe-parser" % "0.12.2"

  // https://github.com/scalate/scalate
  // ASL 2.0
  val `scalate-core` = "org.scalatra.scalate" %% "scalate-core" % "1.9.4"

  // https://github.com/rickynils/scalacheck
  // unmodified 3-clause BSD
  // val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.5"

}

package yamusca.circe

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import munit._
import yamusca.implicits._

class CirceConverterSpec extends FunSuite {

  case class Person(name: String, year: Int)
  case class University(name: String, students: List[Person])

  test("circe converter should generate mustache value from json") {
    val json = Person("Leibniz", 1646).asJson

    assertEquals(
      json.unsafeRender("{{name}} was born in {{year}}"),
      "Leibniz was born in 1646"
    )
  }

  test("work with lists") {
    val fsu = University("FSU", List(Person("Leibniz", 1646), Person("Frege", 1848)))
    assertEquals(
      fsu.asJson.unsafeRender(
        "Uni {{name}}; students: {{#students}}{{name}} ({{year}}), {{/students}}"
      ),
      "Uni FSU; students: Leibniz (1646), Frege (1848), "
    )
  }

  test("treat null values as absent") {
    val template = "My dog's name is {{name}}, age is {{^age}}unknown{{/age}}{{age}}."
    assertEquals(
      Json
        .obj("name" -> "Bello".asJson, "age" -> Json.Null)
        .unsafeRender(template),
      "My dog's name is Bello, age is unknown."
    )
    assertEquals(
      Json
        .obj("name" -> "Bello".asJson)
        .unsafeRender(template),
      "My dog's name is Bello, age is unknown."
    )
    assertEquals(
      Json
        .obj("name" -> "Bello".asJson, "age" -> 8.asJson)
        .unsafeRender(template),
      "My dog's name is Bello, age is 8."
    )
  }
}

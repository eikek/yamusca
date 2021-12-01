package yamusca.circe

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import yamusca.implicits._

class CirceConverterSpec extends AnyFlatSpec with Matchers {

  case class Person(name: String, year: Int)
  case class University(name: String, students: List[Person])

  "circe converter" should "generate mustache value from json" in {
    val json = Person("Leibniz", 1646).asJson

    json.unsafeRender("{{name}} was born in {{year}}") should be(
      "Leibniz was born in 1646"
    )
  }

  it should "work with lists" in {
    val fsu = University("FSU", List(Person("Leibniz", 1646), Person("Frege", 1848)))
    fsu.asJson.unsafeRender(
      "Uni {{name}}; students: {{#students}}{{name}} ({{year}}), {{/students}}"
    ) should be(
      "Uni FSU; students: Leibniz (1646), Frege (1848), "
    )
  }

  it should "treat null values as absent" in {
    val template = "My dog's name is {{name}}, age is {{^age}}unknown{{/age}}{{age}}."
    Json
      .obj("name" -> "Bello".asJson, "age" -> Json.Null)
      .unsafeRender(template) should be(
      "My dog's name is Bello, age is unknown."
    )
    Json
      .obj("name" -> "Bello".asJson)
      .unsafeRender(template) should be(
      "My dog's name is Bello, age is unknown."
    )
    Json
      .obj("name" -> "Bello".asJson, "age" -> 8.asJson)
      .unsafeRender(template) should be(
      "My dog's name is Bello, age is 8."
    )
  }
}

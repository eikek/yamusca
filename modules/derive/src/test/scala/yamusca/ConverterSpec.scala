package yamusca

import java.io.File
import java.math.{BigDecimal => JavaBigDecimal, BigInteger => JavaBigInteger}
import java.net.{URI, URL}
import java.nio.file.Path
import java.time.{Duration, Instant}
import java.util.UUID

import munit._
import yamusca.derive._
import yamusca.implicits._
import yamusca.imports._

class ConverterSpec extends FunSuite {

  case class Numbers(
      int: Int = 15,
      double: Double = 15.5,
      float: Float = 4.3f,
      jbd: JavaBigDecimal = new JavaBigDecimal("0.0001"),
      jbi: JavaBigInteger = new JavaBigInteger("121"),
      sbd: BigDecimal = BigDecimal("0.0002"),
      sbi: BigInt = BigInt(212)
  )
  object Numbers {
    implicit val valueConverter: ValueConverter[Numbers] = deriveValueConverter[Numbers]
  }

  case class ManyValues(
      name: String = "many values",
      year: Int = 2022,
      file: File = new File("test.txt"),
      path: Path = new File("path.txt").toPath,
      uri: URI = URI.create("jdbc:postgres://localhost"),
      url: URL = new URL("http://github.com"),
      uuid: UUID = UUID.fromString("2384fe1c-b962-470f-817e-9b167d93c0b7"),
      duration: Duration = Duration.ofSeconds(20),
      instant: Instant = Instant.parse("2017-06-29T12:30:00Z"),
      strings: List[String] = List("a", "b", "c"),
      numbers: Numbers = Numbers()
  )

  object ManyValues {
    implicit val valueConverter: ValueConverter[ManyValues] =
      deriveValueConverter[ManyValues]
  }

  test("converter should be available for some types") {
    assertEquals(
      ManyValues().unsafeRender(
        "name:{{name}}, year:{{year}}, file:{{file}}, path:{{path}}, uri:{{uri}}, url:{{url}}, uuid:{{uuid}}, " +
          "duration:{{duration}}, instant:{{instant}}, strings:{{#strings}}({{.}}){{/strings}}, " +
          "int:{{numbers.int}}, double:{{numbers.double}}, float:{{numbers.float}}, " +
          "jbd:{{numbers.jbd}}, jbi:{{numbers.jbi}}, sbd:{{numbers.sbd}}, sbi:{{numbers.sbi}}"
      ),
      "name:many values, year:2022, file:test.txt, path:path.txt, uri:jdbc:postgres://localhost, url:http://github.com, " +
        "uuid:2384fe1c-b962-470f-817e-9b167d93c0b7, duration:PT20S, " +
        "instant:2017-06-29T12:30:00Z, strings:(a)(b)(c), int:15, double:15.50, float:4.30, jbd:0.0001, " +
        "jbi:121, sbd:0.0002, sbi:212"
    )
  }
}

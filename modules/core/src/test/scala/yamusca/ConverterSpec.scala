package yamusca

import java.io.File
import java.nio.file.Path
import java.net.{URI,URL}
import java.util.UUID
import java.time.{Duration,Instant}
import java.math.{BigDecimal => JavaBigDecimal, BigInteger => JavaBigInteger}

import yamusca.imports._
import yamusca.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConverterSpec extends AnyFlatSpec with Matchers {

  case class Numbers(
    int: Int = 15,
    double: Double = 15.5,
    float: Float = 4.3f,
    jbd: JavaBigDecimal = new JavaBigDecimal("0.0001"),
    jbi: JavaBigInteger = new JavaBigInteger("121"),
    sbd: BigDecimal = BigDecimal("0.0002"),
    sbi: BigInt = BigInt(212))

  case class ManyValues(
    file: File = new File("test.txt"),
    path: Path = new File("path.txt").toPath,
    uri: URI = URI.create("jdbc:postgres://localhost"),
    url: URL = new URL("http://github.com"),
    uuid: UUID = UUID.fromString("2384fe1c-b962-470f-817e-9b167d93c0b7"),
    duration: Duration = Duration.ofSeconds(20),
    instant: Instant = Instant.parse("2017-06-29T12:30:00Z"),
    strings: List[String] = List("a","b","c"),
    numbers: Numbers = Numbers()
  )

  implicit val nconv: ValueConverter[Numbers] = ValueConverter.deriveConverter[Numbers]
  implicit val mconv: ValueConverter[ManyValues] = ValueConverter.deriveConverter[ManyValues]

  "converter" should "be available for some types" in {
    ManyValues().unsafeRender(
      "file:{{file}}, path:{{path}}, uri:{{uri}}, url:{{url}}, uuid:{{uuid}}, "+
        "duration:{{duration}}, instant:{{instant}}, strings:{{#strings}}({{.}}){{/strings}}, "+
        "int:{{numbers.int}}, double:{{numbers.double}}, float:{{numbers.float}}, "+
        "jbd:{{numbers.jbd}}, jbi:{{numbers.jbi}}, sbd:{{numbers.sbd}}, sbi:{{numbers.sbi}}") should be (

      "file:test.txt, path:path.txt, uri:jdbc:postgres://localhost, url:http://github.com, "+
        "uuid:2384fe1c-b962-470f-817e-9b167d93c0b7, duration:PT20S, "+
        "instant:2017-06-29T12:30:00Z, strings:(a)(b)(c), int:15, double:15.50, float:4.30, jbd:0.0001, "+
        "jbi:121, sbd:0.0002, sbi:212"
    )
  }
}

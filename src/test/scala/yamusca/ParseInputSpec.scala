package yamusca

import org.scalatest._
import yamusca.parser._

class ParseInputSpec extends FlatSpec with Matchers {

  "current" should "return current substring" in {
    ParseInput("ab").dropLeft(1).current should be ("b")
    ParseInput("ab").dropLeft(5).current should be ("")
    ParseInput("ab12de").dropRight(4).current should be ("ab")
  }

  "charAt" should "return char at given position" in {
    ParseInput("abc").charAt(0) should be (Some('a'))
    ParseInput("abc").charAt(-2) should be (None)
    ParseInput("abc").charAt(1) should be (Some('b'))
    ParseInput("abc").charAt(2) should be (Some('c'))
    ParseInput("abc").charAt(3) should be (None)
  }

  "takeLeft" should "return valid substrings" in {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.takeLeft(-5))
    in.takeLeft(3).current should be ("abc")
    in.takeLeft(0).exhausted should be (true)
    in.takeLeft(6) should be theSameInstanceAs (in)
    in.takeLeft(10) should be theSameInstanceAs (in)
  }

  "dropLeft" should "return valid substrings" in {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.dropLeft(-5))
    in.dropLeft(3).current should be ("123")
    in.dropLeft(0) should be theSameInstanceAs (in)
    in.dropLeft(6).exhausted should be (true)
    in.dropLeft(10).exhausted should be (true)
  }

  "takeRight" should "return valid substrings" in {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.takeRight(-5))
    in.takeRight(3).current should be ("123")
    in.takeRight(0).exhausted should be (true)
    in.takeRight(6) should be theSameInstanceAs(in)
    in.takeRight(10) should be theSameInstanceAs(in)
  }

  "dropRight" should "return valid substrings" in {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.dropRight(-5))
    in.dropRight(3).current should be ("abc")
    in.dropRight(0) should be theSameInstanceAs (in)
    in.dropRight(6).exhausted should be (true)
    in.dropRight(10).exhausted should be (true)
  }

  "expandRight" should "expand within raw string" in {
    val in = ParseInput("abc123").dropLeft(1).dropRight(1)
    in.current should be ("bc12")

    in.expandRight(1).current should be ("bc123")
    intercept[IndexOutOfBoundsException](in.expandRight(2))
    intercept[IllegalArgumentException](in.expandRight(-2))
  }

  "expandLeft" should "expand within raw string" in {
    val in = ParseInput("abc123").dropLeft(1).dropRight(1)
    in.current should be ("bc12")

    in.expandLeft(1).current should be ("abc12")
    intercept[IndexOutOfBoundsException](in.expandLeft(2))
    intercept[IllegalArgumentException](in.expandLeft(-2))
  }

  "splitAt" should "split within input" in {
    val in = ParseInput("abc||def||hij")

    in.splitAtNext("||") match {
      case None => fail("|| not found")
      case Some((left, right)) =>
        left.current should be ("abc")
        right.current should be ("||def||hij")
    }

    in.dropLeft(5).splitAtNext("||") match {
      case None => fail("|| not found")
      case Some((left, right)) =>
        left.current should be ("def")
        right.current should be ("||hij")
    }

    in.dropLeft(5).dropRight(5).splitAtNext("||") should be (None)
  }
}

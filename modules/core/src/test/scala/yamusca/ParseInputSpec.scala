package yamusca

import munit._
import yamusca.parser._

class ParseInputSpec extends FunSuite {

  test("current should return current substring") {
    assertEquals(ParseInput("ab").dropLeft(1).current, "b")
    assertEquals(ParseInput("ab").dropLeft(5).current, "")
    assertEquals(ParseInput("ab12de").dropRight(4).current, "ab")
  }

  test("charAt should return char at given position") {
    assertEquals(ParseInput("abc").charAt(0), Some('a'))
    assertEquals(ParseInput("abc").charAt(-2), None)
    assertEquals(ParseInput("abc").charAt(1), Some('b'))
    assertEquals(ParseInput("abc").charAt(2), Some('c'))
    assertEquals(ParseInput("abc").charAt(3), None)
  }

  test("takeLeft should return valid substrings") {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.takeLeft(-5))
    assertEquals(in.takeLeft(3).current, "abc")
    assertEquals(in.takeLeft(0).exhausted, true)
    assert(in.takeLeft(6) eq in)
    assert(in.takeLeft(10) eq in)
  }

  test("dropLeft return valid substrings") {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.dropLeft(-5))
    assertEquals(in.dropLeft(3).current, "123")
    assert(in.dropLeft(0) eq in)
    assertEquals(in.dropLeft(6).exhausted, true)
    assertEquals(in.dropLeft(10).exhausted, true)
  }

  test("takeRight should return valid substrings") {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.takeRight(-5))
    assertEquals(in.takeRight(3).current, "123")
    assertEquals(in.takeRight(0).exhausted, true)
    assert(in.takeRight(6) eq in)
    assert(in.takeRight(10) eq in)
  }

  test("dropRight should return valid substrings") {
    val in = ParseInput("abc123")

    intercept[IllegalArgumentException](in.dropRight(-5))
    assertEquals(in.dropRight(3).current, "abc")
    assert(in.dropRight(0) eq in)
    assertEquals(in.dropRight(6).exhausted, true)
    assertEquals(in.dropRight(10).exhausted, true)
  }

  test("expandRight should expand within raw string") {
    val in = ParseInput("abc123").dropLeft(1).dropRight(1)
    assertEquals(in.current, "bc12")

    assertEquals(in.expandRight(1).current, "bc123")
    intercept[IndexOutOfBoundsException](in.expandRight(2))
    intercept[IllegalArgumentException](in.expandRight(-2))
  }

  test("expandLeft should expand within raw string") {
    val in = ParseInput("abc123").dropLeft(1).dropRight(1)
    assertEquals(in.current, "bc12")

    assertEquals(in.expandLeft(1).current, "abc12")
    intercept[IndexOutOfBoundsException](in.expandLeft(2))
    intercept[IllegalArgumentException](in.expandLeft(-2))
  }

  test("splitAt should split within input") {
    val in = ParseInput("abc||def||hij")

    in.splitAtNext("||") match {
      case None                => fail("|| not found")
      case Some((left, right)) =>
        assertEquals(left.current, "abc")
        assertEquals(right.current, "||def||hij")
    }

    in.dropLeft(5).splitAtNext("||") match {
      case None                => fail("|| not found")
      case Some((left, right)) =>
        assertEquals(left.current, "def")
        assertEquals(right.current, "||hij")
    }

    assertEquals(in.dropLeft(5).dropRight(5).splitAtNext("||"), None)
  }
}

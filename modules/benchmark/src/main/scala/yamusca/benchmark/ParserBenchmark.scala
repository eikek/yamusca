package yamusca.benchmark

import org.openjdk.jmh.annotations._
import yamusca.parser._
import yamusca.parser.mustache._

@State(Scope.Thread)
class ParserBenchmark {

  val in = ParseInput(template)
  val rn = ParseInput("\r\n")
  val n  = ParseInput("\n")

  val tag1 = ParseInput("{{name}}")
  val tag2 = ParseInput("{{#name}}")
  val tag3 = ParseInput("{{=<< >>=}}")
  val tag4 = ParseInput("    {{/end}}  \n")
  val tag5 = ParseInput("abe\n    {{/end}}  \n")
  val tag6 = ParseInput("{{#start}}This is a {{firstname}} and {{lastname}}{{/start}}")

  val lit1 = ParseInput("abc \n  {{#section}}")

//  @Benchmark
  def consume1(): Unit =
    consume("<div>Timeline</div>")(in)

//  @Benchmark
  def newLine1(): Unit =
    newLine(in)

//  @Benchmark
  def newLine2(): Unit =
    newLine(rn)

//  @Benchmark
  def newLine3(): Unit =
    newLine(n)

//  @Benchmark
  def consumeUntil1(): Unit =
    consumeUntil("</div>")(in)

//  @Benchmark
  def parseTag1(): Unit =
    parseTag(tag1)

  // @Benchmark
  // def parseTag2(): Unit = {
  //   parseTag(c => c == '#' || c == '^' || c == '/')(tag2)
  // }

  // @Benchmark
  // def parseVariable1(): Unit = {
  //   parseVariable(tag1)
  // }

  // @Benchmark
  // def parseSetDelimiter1(): Unit = {
  //   parseSetDelimiter(tag3)
  // }

  // @Benchmark
  // def parseLiteral1(): Unit = {
  //   parseLiteral(lit1)
  // }

  // @Benchmark
  // def parseStandalone1(): Unit = {
  //   standalone(parseEndSection)(tag5)
  // }

  @Benchmark
  def consumeUntilEndSection1(): Unit =
    consumeUntilEndSection("tweets")(in)

  // @Benchmark
  // def parseSection1(): Unit = {
  //   parseSection(tag6)
  // }

  // @Benchmark
  // def parseElement1(): Unit = {
  //   parseElement(tag5)
  // }

  @Benchmark
  def parseTemplate1(): Unit =
    parseTemplate(in)
}

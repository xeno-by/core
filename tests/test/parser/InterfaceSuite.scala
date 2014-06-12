import org.scalatest._
import scala.reflect.core._

class InterfaceSuite extends FunSuite {
  test("String.parse (partial import)") {
    import scala.reflect.syntactic.parsers.RichSource
    val Term.Name("x") = "x".parse[Term]
  }

  test("String.parse (full import)") {
    import scala.reflect.syntactic.parsers._
    val Term.Name("x") = "x".parse[Term]
  }
}
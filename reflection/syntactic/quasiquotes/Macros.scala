package scala.reflect
package syntactic.quasiquotes

import scala.reflect.macros.whitebox.Context
import org.scalareflect.adt._
import scala.reflect.syntactic.parsers.ParseSyntaxError

class Macros[C <: Context](val c: C) extends AdtReflection with NewLiftables {
  val u: c.universe.type = c.universe
  import c.universe.{Tree => _, _}
  import c.universe.{Tree => ScalaTree}
  import scala.reflect.core.{Tree => PalladiumTree}
  def apply(macroApplication: ScalaTree, parse: String => PalladiumTree): ScalaTree = {
    val q"$_($_.apply(..$partlits)).$_.apply[..$_](..$argtrees)" = macroApplication
    val parts = partlits.map{ case q"${part: String}" => part }
    def ndots(s: String): Int = if (s.endsWith(".")) ndots(s.stripSuffix(".")) + 1 else 0
    val args = argtrees.zipWithIndex.map{ case (tree, i) => (c.freshName(tree.toString), ndots(parts(i)), tree) }
    val snippet = (parts.init.zip(args).flatMap{ case (part, (id, ndots, _)) => List(part.stripSuffix("." * ndots), s"`$id`") } :+ parts.last).mkString("")
    val liveTree: PalladiumTree = parse(snippet)
    val reifiedTree: ScalaTree = implicitly[Liftable[PalladiumTree]].apply(liveTree)
    reifiedTree
  }
}

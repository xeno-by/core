package scala.reflect
package syntactic.quasiquotes

import scala.reflect.macros.whitebox.Context
import scala.reflect.core.{Tree => PalladiumTree}
import org.scalareflect.adt._

class Macros[C <: Context](val c: C) extends AdtReflection with NewLiftables {
  val u: c.universe.type = c.universe
  import c.universe.{Tree => _, _}
  import c.universe.{Tree => ScalaTree}
  def apply(macroApplication: ScalaTree, parse: String => PalladiumTree): ScalaTree = {
    val q"$_($_.apply(..$parts)).$_.apply[..$_](..$args)" = macroApplication
    if (args.length != 0) c.abort(macroApplication.pos, "unquoting is not supported yet")
    val liveTree: PalladiumTree = parse(parts.map{case q"${part: String}" => part}.mkString(""))
    val reifiedTree: ScalaTree = implicitly[Liftable[PalladiumTree]].apply(liveTree)
    reifiedTree
  }
}

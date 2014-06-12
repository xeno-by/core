package scala.reflect
package syntactic.quasiquotes

import scala.reflect.macros.whitebox.Context
import scala.reflect.core.{Tree => PalladiumTree}

class Macros[C <: Context](val c: C) {
  import c.universe._
  def apply(macroApplication: Tree, parse: String => PalladiumTree): Tree = {
    ???
  }
}

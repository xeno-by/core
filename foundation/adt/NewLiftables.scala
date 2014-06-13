package org.scalareflect.adt

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import org.scalareflect.adt.Internal.Adt
import scala.{Seq => _}
import scala.collection.immutable.Seq

trait NewLiftables {
  val u: scala.reflect.macros.Universe
  implicit def materializeAdt[T <: Adt]: u.Liftable[T] = macro NewLiftableMacros.impl[T]
  implicit def liftSeq[T](implicit ev: u.Liftable[T]): u.Liftable[Seq[T]] = u.Liftable { seq => u.Liftable.liftList[T](ev).apply(seq.toList) }
}

class NewLiftableMacros(val c: Context) extends AdtReflection {
  val u: c.universe.type = c.universe
  import c.universe._
  def impl[T: WeakTypeTag]: c.Tree = {
    val root = weakTypeOf[T].typeSymbol.asAdt.root
    val leafs = weakTypeOf[T].typeSymbol.asAdt.root.allLeafs
    if (leafs.isEmpty) c.abort(c.enclosingPosition, s"$root hasn't been initialized properly")
    val u = q"${c.prefix}.u"
    val mainParam = c.freshName(TermName("x"))
    val mainModule = c.freshName(TermName("Module"))
    val mainMethod = c.freshName(TermName("instanceFor" + root.prefix.capitalize.replace(".", "")))
    val localParam = c.freshName(TermName("x"))
    val leafLiftNames = leafs.map(leaf => c.freshName(TermName("lift" + leaf.prefix.capitalize.replace(".", ""))))
    val lifts = leafs.zip(leafLiftNames).map({ case (leaf, name) =>
      val init = q"""$u.Ident($u.TermName("_root_"))""": Tree
      val namePath = leaf.sym.fullName.split('.').foldLeft(init)((acc, part) => q"$u.Select($acc, $u.TermName($part))")
      val localParam = c.freshName(TermName("x"))
      val args = leaf.fields.map(f => {
        val name = q"$u.Ident($u.TermName(${f.name.toString}))"
        val value = q"_root_.scala.Predef.implicitly[$u.Liftable[${f.tpe}]].apply($localParam.${f.name})"
        q"$u.AssignOrNamedArg($name, $value)"
      })
      val body = {
        def reify = q"$u.Apply($namePath, $args)"
        if (!(leaf.sym.asType.toType <:< c.mirror.staticClass("scala.reflect.core.Name").asType.toType)) reify
        else q"""
          val maybeUnquotee = args.collectFirst{ case (id, unquotee) if (id: String) == ($localParam.value: String) => unquotee }
          maybeUnquotee.getOrElse($reify)
        """
      }
      q"def $name($localParam: ${leaf.sym.asType.toType}): $u.Tree = $body"
    })
    val clauses = leafs.zip(leafLiftNames).map({ case (leaf, name) =>
      q"if ($localParam.isInstanceOf[${leaf.sym.asType.toType}]) result = $name($localParam.asInstanceOf[${leaf.sym.asType.toType}])"
    })
    q"""
      $u.Liftable(($mainParam: ${weakTypeOf[T]}) => {
        object $mainModule {
          val materializeAdt = "shadow the adt materializer"
          ..$lifts
          implicit def $mainMethod[T <: ${root.sym}]: $u.Liftable[T] = $u.Liftable(($localParam: T) => {
            var result: $u.Tree = null
            if ($localParam == null) result = q"null"
            ..$clauses
            if (result == null) sys.error("none of leafs matched " + $localParam.getClass)
            result
          })
        }
        $mainModule.$mainMethod.apply($mainParam)
      })
    """
  }
}

package io.suggest.sjs.common.vm.wnd.compstyle

import org.scalajs.dom.raw.CSSStyleDeclaration

import scala.collection.{AbstractIterator, AbstractSeq}


/** scala.collection.Seq враппер над инстансом CSSStyleDeclaration. */
case class CssStyleDeclKeys(decl: CSSStyleDeclaration) extends AbstractSeq[String] { that =>

  override def length: Int = {
    decl.length
  }

  override def apply(i: Int): String = {
    decl(i)
  }

  override def iterator: Iterator[String] = {
    new AbstractIterator[String] {
      private var i = 0

      override def next(): String = {
        val r = apply(i)
        i += 1
        r
      }

      override def hasNext: Boolean = {
        i < that.length
      }
    }
  }

}

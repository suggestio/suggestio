package com.quilljs.quill

import org.scalajs.dom.Node

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSName}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 12:48
  * Description: Sjs-facade for quill wysiwyg editor core.
  */
//@JSImport("quill", "Quill")   // TODO Вероятно, надо default
@js.native
trait Quill extends js.Object


/** Static API trait for Quill. */
@js.native
trait QuillStatic extends js.Object {

  def debug(level: String): Unit = js.native
  /** Passing true is equivalent to passing 'log'. Passing false disables all messages. */
  def debug(enabled: Boolean): Unit = js.native

  @JSName("import")
  def `import`[T <: js.Object](module: String): T = js.native

  @JSName("register")
  def register1(path: String, `def`: js.Any, suppressWarnings: Boolean = js.native): Unit = js.native

  @JSName("register")
  def register2(defs: js.Object, suppressWarnings: Boolean = js.native): Unit = js.native

  @JSName("addContainer")
  def addContainer1(className: String, refNode: Node = js.native): Unit = js.native

  @JSName("addContainer")
  def addContainer2(domNode: Node, refNode: Node = js.native): Unit = js.native

  def getModule(name: String): js.Any = js.native

  def disable(): Unit = js.native
  def enable(value: Boolean = js.native): Unit = js.native

}


@JSImport("quill", "Quill")
@js.native
object Quill extends QuillStatic

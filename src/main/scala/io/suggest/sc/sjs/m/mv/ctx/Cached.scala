package io.suggest.sc.sjs.m.mv.ctx

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:47
 * Description: Конструктор для быстрой сборки кешей в [[io.suggest.sc.sjs.m.mv.IVCtx]].
 */
trait ElAccessT {
  type T <: AnyRef

  def getFromDOM(): T
  
  /** Получить значение, из кеша если возможно. */
  def apply(): T = {
    get.get
  }

  def get: Option[T] = {
    Option(getFromDOM())
  }
}


/** Добавить кеширование при доступе к элементу. */
trait Cached extends ElAccessT with ClearableCache {

  private var _cacheOrNull: Option[T] = None

  override def get: Option[T] = {
    var v = _cacheOrNull
    if (v.isEmpty) {
      v = super.get
      if (v.nonEmpty)
        set(v.get)
    }
    v
  }

  /** Выставить в кеш указанное значение. */
  def set(v: T): Unit = {
    _cacheOrNull = Option(v)
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    _cacheOrNull = None
  }
}


/** Поддержка поиска элемента по id. */
trait Id extends ElAccessT {
  def id: String

  override def getFromDOM(): T = {
    dom.document.getElementById(id).asInstanceOf[T]
  }
}


/** Кеширование div-тега. */
trait Div extends ElAccessT {
  override type T = HTMLDivElement
}


/** Для цепочной чистки кешей используется иерархическое сие: */
trait ClearableCache {
  def cachesClear(): Unit = {}
}


// Объединять Div и Id в abstract class не имеет смысла, это только увеличивает скомпиленный размер.


package io.suggest.sc.sjs.m.mv.ctx

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:47
 * Description: Конструктор для быстрой сборки кешей в [[io.suggest.sc.sjs.m.mv.IVCtx]].
 */
trait ElCacheT extends ClearableCache {

  type T <: AnyRef

  private var _cacheOrNull: Option[T] = None

  def getFromDOM(): T

  /** Получить значение, из кеша если возможно. */
  def apply(): T = {
    get.get
  }

  def get: Option[T] = {
    var v = _cacheOrNull
    if (v.isEmpty) {
      v = Option(getFromDOM())
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
    _cacheOrNull = None
  }
}


/** Поддержка поиска элемента по id. */
trait IdFind extends ElCacheT {

  def id: String

  override def getFromDOM(): T = {
    dom.document.getElementById(id).asInstanceOf[T]
  }
}


/** Кеширование div-тега. */
trait Div extends ElCacheT {
  override type T = HTMLDivElement
}



/** Для цепочной чистки кешей используется иерархическое сие: */
trait ClearableCache {
  def cachesClear(): Unit = {}
}

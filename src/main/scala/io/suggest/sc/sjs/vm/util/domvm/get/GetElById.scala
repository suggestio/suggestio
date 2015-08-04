package io.suggest.sc.sjs.vm.util.domvm.get

import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.raw.{HTMLSpanElement, HTMLDivElement, HTMLImageElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 10:11
 * Description: Аддон для быстрого получения элементов из DOM во вьюшках как Option[T].
 */
trait GetElById {

  protected def getElementByIdOrNull[T <: Element](id: String): T = {
    dom.document
      .getElementById(id)
      .asInstanceOf[T]
  }

  /**
   * Быстро и кратко записывать получение элементов из DOM по id.
   * @param id id элемента.
   * @tparam T Тип возвращаемого элемента, если найден.
   * @return Опционально-найденный элемент DOM.
   */
  protected def getElementById[T <: Element](id: String): Option[T] = {
    Option( getElementByIdOrNull[T](id) )
  }

}

/** Аддон для краткого поиска div'ов по id. */
trait GetDivById extends GetElById {
  protected def getDivById(id: String)        = getElementById[HTMLDivElement](id)
  protected def getDivByIdOrNull(id: String)  = getElementByIdOrNull[HTMLDivElement](id)
}


trait GetSpanById extends GetElById {
  protected def getSpanById(id: String)        = getElementById[HTMLSpanElement](id)
  protected def getSpanByIdOrNull(id: String)  = getElementByIdOrNull[HTMLSpanElement](id)
}

/** Аддон для краткого поисква img-тегов по id. */
trait GetImgById extends GetElById {
  protected def getImgById(id: String) = getElementById[HTMLImageElement](id)
}
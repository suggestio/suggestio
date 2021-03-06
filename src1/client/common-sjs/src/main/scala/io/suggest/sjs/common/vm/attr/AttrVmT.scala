package io.suggest.sjs.common.vm.attr

import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.{Element, NamedNodeMap, Node}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.06.15 13:43
 * Description: Простой и безопасный доступ к аттрибутам узла.
 * @see [[https://jsperf.com/jquery-attr-or-jquery-0-getattribute/2 getAttribute vs getNamedItem vs $.attr]]
 */
trait AttrVmUtilT extends IVm {

  override type T <: Node


  /** Вспомогательная подсистема для отработки разных вариантов чтения аттрибута.  */
  protected trait Helper[T] {

    def attrName: String

    /** Есть значение аттрибута. */
    def withAttrValue(attrValue: Option[String]): T
    
    def notFoundExceptionMsg: String = "n/a"

    /** Не найдено способа получения значения аттрибута. */
    def notFound: T = throw new NoSuchElementException(notFoundExceptionMsg)

    protected def _usingElGetAttrValue(attrValue: Option[String]): T = {
      withAttrValue(attrValue)
    }

    protected def _usingNodeAttrsGetValue(attrValue: Option[String]): T = {
      withAttrValue(attrValue)
    }

    def execute(): T = {
      val el = _underlying
      val el1 = AttrElStub(el)
      val maybeGetAttrF = el1.getAttribute
      val _attrName = attrName
      if (maybeGetAttrF.nonEmpty) {
        // В обход через аттрибуты тега.
        val attrValue = Option( el.asInstanceOf[Element].getAttribute(_attrName) )
        _usingElGetAttrValue(attrValue)

      } else {
        val maybeAttrs = el1.attributes
        if (maybeAttrs.nonEmpty) {
          val attrValue = Option( maybeAttrs.get.getNamedItem(_attrName) )
            .flatMap(attr => Option(attr.value))
          _usingNodeAttrsGetValue(attrValue)

        } else {
          notFound
        }
      }
    }
  }

}


/** Аддон для чтения/записи аттрибутов. */
trait AttrVmT extends AttrVmUtilT {

  /**
   * Прочитать аттрибут тега/узла любыми возможными способами.
   * @param name Название аттрибута.
   * @return Опционально-прочитанный аттрибут.
   */
  def getAttribute(name: String): Option[String] = {
    val h = new Helper[Option[String]] {
      override def attrName = name
      override def notFound: Option[String] = None
      override def withAttrValue(attrValue: Option[String]): Option[String] = {
        attrValue
      }
    }
    h.execute()
  }

  /** Получить значение аттрибута без пробелов в начале и конце. Вместо пустой строки возвращается None. */
  def getNonEmptyAttribute(name: String): Option[String] = {
    val nonEmptyF: String => Boolean = { !_.isEmpty }
    getAttribute(name)
      .filter(nonEmptyF)
      .map(_.trim)
      .filter(nonEmptyF)
  }


  /** Извлечь целочисленное значение аттрибута, даже если тот содержит какие-то строковые данные. */
  def getIntAttribute(name: String): Option[Int] = {
    getNonEmptyAttribute(name)
      .flatMap { DataUtil.extractInt(_) }
  }

}


/** Интерфейс для необязательно доступного свойства Element.classList. */
@js.native
sealed trait AttrElStub extends js.Object {

  /** Стандартный getAttribute() для чтения из тегов. */
  def getAttribute: UndefOr[_] = js.native

  /** W3C XML API. Доступно в SVG, хоть и медленнее по benchmark'ам. */
  def attributes: UndefOr[NamedNodeMap] = js.native
}


/** Поддержка приведения Element к SafeCssClass. */
object AttrElStub {
  def apply(el: Node): AttrElStub = {
    el.asInstanceOf[AttrElStub]
  }
}

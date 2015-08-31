package io.suggest.sjs.common.view.safe.wnd.hist

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.History

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.15 15:08
 * Description: Враппер безопасного доступа к методам window.history.
 */
object SafeHistoryObj {

  type WriteF_t = js.Function3[js.Any, String, UndefOr[String], Unit]

}


import SafeHistoryObj.WriteF_t


trait SafeHistoryObjT extends ISafe {

  override type T <: History

  /**
   * Враппер для безопасного вызова pushState().
   * @param state Добавляемые данные состояния.
   * @param title Заголовок вкладки/окна.
   * @param url Ссылка, если есть.
   * @return true если всё ок.
   *         false, если браузер не поддерживает этот метод HTML5 History API.
   */
  def pushState(state: js.Any, title: String, url: Option[String] = None): Boolean = {
    _writeState(state, title, url)(_.pushState)
  }

  /**
   * Враппер для безопасного вызова window.history.replaceState().
   * @param state Новые данные текущего состояния.
   * @param title Заголовок вкладки/окна.
   * @param url Ссылка, если есть.
   * @return true если всё ок
   *         false, если браузер не поддерживает это метод HTML5 History API.
   */
  def replaceState(state: js.Any, title: String, url: Option[String] = None): Boolean = {
    _writeState(state, title, url)(_.replaceState)
  }

  /** Общий код логики записи состояния с помощью какой-то нативной функции вынесен сюда. */
  private def _writeState(state: js.Any, title: String, url: Option[String])
                         (howF: HistoryObjStub => UndefOr[WriteF_t]): Boolean = {
    val und = _underlying
    val pushF = howF( HistoryObjStub(und) )
    pushF.isDefined && {
      // Почему-то вызов pushF.get.apply(...) приводит к Illegal invocation. Поэтому дергаем исходный pushState().
      url match {
        case Some(_url) => und.pushState(state, title, _url)
        case None       => und.pushState(state, title)
      }
      true
    }
  }


}


/** Дефолтовая реализация [[SafeHistoryObjT]]. */
case class SafeHistoryObj(override val _underlying: History) extends SafeHistoryObjT {
  override type T = History
}


/** Интересующий интерфейс для доступа к history. */
@js.native
sealed trait HistoryObjStub extends js.Object {

  /** Безопасный доступ к dom.window.history.pushState() */
  def pushState: UndefOr[WriteF_t] = js.native

  def replaceState: UndefOr[WriteF_t] = js.native
}


object HistoryObjStub {
  def apply(h: History): HistoryObjStub = {
    h.asInstanceOf[HistoryObjStub]
  }
}

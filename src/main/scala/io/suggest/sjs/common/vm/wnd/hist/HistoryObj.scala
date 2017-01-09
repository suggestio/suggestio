package io.suggest.sjs.common.vm.wnd.hist

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.History

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.15 15:08
 * Description: Враппер безопасного доступа к методам window.history.
 */
object HistoryObj {

  type WriteF_t = js.Function3[js.Any, String, UndefOr[String], Unit]

}


import HistoryObj.WriteF_t


trait HistoryObjT extends IVm {

  override type T <: History

  /**
    * Враппер для безопасного вызова pushState().
    *
    * @param state Добавляемые данные состояния.
    * @param title Заголовок вкладки/окна.
    * @param url Ссылка, если есть.
    * @return true если всё ок.
    *         false, если браузер не поддерживает этот метод HTML5 History API.
    */
  def pushState(state: js.Any, title: String, url: Option[String] = None): Boolean = {
    val und = _underlying
    HistoryObjStub(und).pushState.isDefined && {
      // FIXME в API два раза объявлен этот pushState, что нелогично как-то.
      url.fold {
        und.pushState(state, title)
      } { _url =>
        und.pushState(state, title, _url)
      }
      true
    }
  }

  /** Узнать текущее значение state в состоянии. */
  def currentState: Option[js.Any] = {
    HistoryObjStub(_underlying)
      .state
      .toOption
      // Устранить возможный null внутри Option.
      .filter(_ != null)
  }

}


/** Дефолтовая реализация [[HistoryObjT]]. */
case class HistoryObj(override val _underlying: History) extends HistoryObjT {
  override type T = History
}


/** Интересующий интерфейс для доступа к history. */
@js.native
sealed trait HistoryObjStub extends js.Object {

  /** Безопасный доступ к dom.window.history.pushState() */
  def pushState: UndefOr[WriteF_t] = js.native

  def replaceState: UndefOr[WriteF_t] = js.native

  /** Явно описываем в типе возможный undefined в возвращаемом значении, от греха по-дальше. */
  def state: js.UndefOr[js.Any] = js.native

}


object HistoryObjStub {
  def apply(h: History): HistoryObjStub = {
    h.asInstanceOf[HistoryObjStub]
  }
}

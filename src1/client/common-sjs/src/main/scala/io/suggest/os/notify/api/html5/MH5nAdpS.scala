package io.suggest.os.notify.api.html5

import diode.data.Pot
import io.suggest.os.notify.MOsToast
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import org.scalajs.dom.experimental.Notification
import io.suggest.ueq.JsUnivEqUtil._

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.04.2020 11:51
  * Description: Состояние html5-адаптера.
  */
object MH5nAdpS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MH5nAdpS] = UnivEq.derive

  def permission = GenLens[MH5nAdpS]( _.permission )
  def notifications = GenLens[MH5nAdpS]( _.notifications )

}


/** Контейнер данных состояния HTML5-адаптера.
  *
  * @param permission Состояние запроса разрешения на вывод уведомлений.
  * @param notifications Текущие нотификации.
  */
case class MH5nAdpS(
                     permission       : Pot[Boolean]                       = Pot.empty,
                     notifications    : HashMap[String, MH5nToastInfo]     = HashMap.empty,
                   )


object MH5nToastInfo {
  @inline implicit def univEq: UnivEq[MH5nToastInfo] = UnivEq.force
}
case class MH5nToastInfo(
                          osToast     : MOsToast,
                          h5Not       : Notification,
                        )

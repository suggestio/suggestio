package io.suggest.sc.m.dia

import diode.data.Pot
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.nodes.form.m.{MLkNodesMode, MLkNodesModes}
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 12:19
  * Description: Модель данных выдачи для формы узлов.
  */
object MScNodes {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScNodes] = UnivEq.derive

  def opened = GenLens[MScNodes]( _.opened )
  def mode = GenLens[MScNodes]( _.mode )
  def circuit = GenLens[MScNodes]( _.circuit )
  def focusedAdId = GenLens[MScNodes]( _.focusedAdId )
  def unSubsCribeBcnr = GenLens[MScNodes]( _.unSubsCribeBcnr )

}


/** Контейнер данных формы управления узлами.
  *
  * @param opened Видимо ли окно диалога?
  * @param mode Текущий режим работы формы.
  * @param circuit Логика lk-nodes-формы.
  * @param focusedAdId Дамп id открытой рекламной карточки на момент открытия формы.
  *                    Карточки в плитке имеют свойство в фоне перестраиваться, слетать, расфокусировываться.
  *                    Это всё не должно нарушать управление формой, поэтому значение дампится сюда заранее.
  * @param unSubsCribeBcnr Функция отписки от событий beaconer'а.
  *                        Если не подписаны, то тут empty.
  */
final case class MScNodes(
                           opened               : Boolean                           = false,
                           mode                 : MLkNodesMode                      = MLkNodesModes.NodesManage,
                           circuit              : Option[LkNodesFormCircuit]        = None,
                           focusedAdId          : Option[String]                    = None,
                           unSubsCribeBcnr      : Pot[() => Unit]                   = Pot.empty,
                         )

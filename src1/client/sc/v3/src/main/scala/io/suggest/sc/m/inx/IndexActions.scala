package io.suggest.sc.m.inx

import io.suggest.maps.m.ReqRcvrPopup
import io.suggest.sc.m.ISc3Action
import io.suggest.sc.sc3.MSc3Resp

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:41
  * Description: Diode-экшены для index'а.
  */

/** Интерфейс-маркер для index-экшенов. */
sealed trait IIndexAction extends ISc3Action

/** Дёрнуть индекс с сервера и накатить.
  *
  * @param withWelcome Требуется ли картинка приветствия?
  */
case class GetIndex(withWelcome: Boolean) extends IIndexAction


/** Получен какой-то ответ сервера по поводу индекса выдачи.
  *
  * @param reqTimestamp Таймштамп index-запроса.
  *                     Может быть None, чтобы форсировать обновление выдачи без учёта timestamp'а запроса.
  */
case class HandleIndexResp(
                            tryResp: Try[MSc3Resp],
                            reqTimestamp: Option[Long],
                            reason: Option[GetIndex]
                          )
  extends IIndexAction


/** Нажатие на экран приветствия. */
case object WcClick extends IIndexAction

/** Срабатывание таймера автоматического переключения фазы welcome.
  * @param timestamp Таймштамп-отметка, для проверки актуальности сработавшего таймера.
  */
case class WcTimeOut(timestamp: Long) extends IIndexAction


/** Отложенная реакция на клик по ресиверу на карте.*/
case class MapRcvrClickDelayed(reason: ReqRcvrPopup) extends IIndexAction


/** Отложенный экшен реакции на окончание перетаскивания карты.
  * Нужен, чтобы карта успела проанимироваться с минимумом рывков.
  */
case object MapDragEndDelayed extends IIndexAction
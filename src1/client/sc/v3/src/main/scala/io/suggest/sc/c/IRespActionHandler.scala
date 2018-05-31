package io.suggest.sc.c

import diode.Effect
import diode.data.Pot
import io.suggest.sc.m.{HandleScApiResp, MScRoot}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.18 10:52
  * Description: Интерфейс для обработчиков ответа сервера.
  */
trait IRespHandler {

  /** Определить, относится ли запрос в целом к данному handler'у.
    *
    * @param reason Экшен, запустивший запрос к серверу ранее.
    * @return
    */
  def isMyReqReason(ctx: MRhCtx): Boolean

  /** Вернуть Pot, на котором повешен запрос данного handler'а.
    *
    * @return None - Pot'а нет.
    *         Some(Pot()) - искомый инстанс Pot'а из состояния.
    */
  def getPot(ctx: MRhCtx): Option[Pot[_]]

  /** Залить ошибку в состояние выдачи.
    *
    * @param reason Экшен причины реквеста.
    * @param value0 Состояние выдачи.
    * @param ex Ошибка реквеста.
    * @return None - не сюда запрос.
    *         Some(новое состояние выдачи).
    */
  def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot

}


/** Интерфейс обработчика resp-экшенов. */
trait IRespActionHandler {

  /** Является ли resp-экшен
    *
    * @param raType Тип resp-экшена.
    * @return
    */
  def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean

  /** Применить respAction к состоянию выдачи.
    *
    * @param ra Resp action.
    * @param value0 Начальное значение.
    * @return Обновлённое состояние + опциональный сайд-эффект.
    */
  def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): (MScRoot, Option[Effect])

}


/** Объединение [[IRespHandler]] + [[IRespActionHandler]], т.к. обычно так и надо. */
trait IRespWithActionHandler
  extends IRespHandler
  with IRespActionHandler


/** Класс контекста для методов [[IRespActionHandler]] и [[IRespHandler]]. */
case class MRhCtx(
                   value0 : MScRoot,
                   m      : HandleScApiResp
                 )
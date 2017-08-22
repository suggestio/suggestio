package models.mws

import io.suggest.common.menum.{EnumJsonReadsT, EnumMaybeWithName}
import io.suggest.common.ws.proto.MAnswerStatusesBaseT
import io.suggest.common.ws.proto.AnswerStatusConstants._

/** Модель статусов ответов для WebSocket JSON протокола. */
object AnswerStatuses extends Enumeration with EnumMaybeWithName with MAnswerStatusesBaseT with EnumJsonReadsT {

  /** Класс-заготовка одного инстанса модели. */
  protected abstract class Val(override val jsStr: String)
    extends super.Val(jsStr)
    with ValT

  override type T = Val

  override val Success      : T = new Val(ST_SUCCESS) with VSuccess
  override val Error        : T = new Val(ST_ERROR) with VError
  override val FillContext  : T = new Val(ST_FILL_CONTEXT) with VFillCtx

}

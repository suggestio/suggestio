package io.suggest.common.ws.proto

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 11:40
 * Description: Абстрактная модель допустимых статусов ответов.
 * В слегка модифицированных видах используется на клиенте и на сервере.
 */
object MAnswerStatuses extends StringEnum[MAnswerStatus] {

  /** Действие исполнено полностью. */
  case object Success extends MAnswerStatus( "success" ) {
    override def isSuccess  = true
  }

  /** Возникла ошибка при выполнении действия. Дальнейшее исполнение действия невозможно. */
  case object Error extends MAnswerStatus( "error" ) {
    override def isError    = true
  }

  /** В контексте недостаточно данных для выполнения действия.
    * Адресат должен проверить содержимое полученного контекста, найти и заполнить недостающие данные
    * и вернуть контекст отправителю.
    * Это НЕ ошибочное состояние, а часть нормального workflow в рамках какой-то конкретной задачи. */
  case object FillContext extends MAnswerStatus( "fillCtx" )


  override def values = findValues

}


sealed abstract class MAnswerStatus(override val value: String) extends StringEnumEntry {
  def isSuccess: Boolean  = false
  def isError: Boolean    = false

  override def toString = value
}

object MAnswerStatus {

  implicit def mAnswerStatusFormat: Format[MAnswerStatus] =
    EnumeratumUtil.valueEnumEntryFormat( MAnswerStatuses )

  @inline implicit def univEq: UnivEq[MAnswerStatus] = UnivEq.derive

}


package io.suggest.ws

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 11:32
  * Description: Модель типов websocket-сообщений.
  */
object MWsMsgTypes extends StringEnum[MWsMsgType] {

  /** Сообщение с гистограммой цветов.
    * @see [[io.suggest.color.MHistogramWs]] */
  case object ColorsHistogram extends MWsMsgType("a")


  override val values = findValues

}


/** Класс одного элемента модели. */
sealed abstract class MWsMsgType(override val value: String) extends StringEnumEntry


object MWsMsgType {

  /** Поддержка play-json. */
  implicit val MWS_MSG_TYPE_FORMAT: Format[MWsMsgType] = {
    EnumeratumUtil.valueEnumEntryFormat( MWsMsgTypes )
  }

  @inline implicit def univEq: UnivEq[MWsMsgType] = UnivEq.derive

}

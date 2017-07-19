package io.suggest.sc.resp

import io.suggest.primo.IStrId
import io.suggest.sc.ScConstants.Resp._

import enumeratum._
import play.api.libs.json._
import play.api.libs.functional.syntax._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.09.16 11:28
  * Description: Общий клиент-серверный код модели типов sc-resp-экшенов.
  */


/** Статическая поддержка элементов модели [[MScRespActionType]]. */
object MScRespActionType {

  implicit val MSC_RESP_ACTION_TYPE_FORMAT: Format[MScRespActionType] = {
    implicitly[Format[String]]
      .inmap( MScRespActionTypes.withName, _.strId )
  }

}

/** Класс одного элемента модели типов. */
sealed abstract class MScRespActionType
  extends EnumEntry
  with IStrId



/** Кросс-платформенная модель типов экшенов sc-ответов. */
object MScRespActionTypes extends Enum[MScRespActionType] {

  /** Тип экшена с индексом выдачи. */
  case object Index extends MScRespActionType {
    override def strId = INDEX_RESP_ACTION
  }

  /** Тип экшена с плиткой выдачи. */
  case object AdsTile extends MScRespActionType {
    override def strId = ADS_TILE_RESP_ACTION
  }

  /** Тип экшена с карточками focused-выдачи. */
  case object AdsFoc extends MScRespActionType {
    override def strId = FOC_ANSWER_ACTION
  }


  /** Список всех значений модели. */
  override val values = findValues

}

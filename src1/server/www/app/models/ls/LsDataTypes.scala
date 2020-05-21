package models.ls

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.15 19:13
 * Description: Модель содержит идентификаторы моделей, хранимых вместе с данными на стороне
 * пользовательского хранилища.
 */
object LsDataTypes extends StringEnum[LsDataType] {

  /** Пакет с этой отметкой хранит oauth1 access token. */
  case object OAuth1AccessToken extends LsDataType("a")

  override def values = findValues

}

sealed abstract class LsDataType( override val value: String ) extends StringEnumEntry
object LsDataType {

  @inline implicit def univEq: UnivEq[LsDataType] = UnivEq.derive

  implicit def lsDataTypeJson: Format[LsDataType] =
    EnumeratumUtil.valueEnumEntryFormat( LsDataTypes )


  /** Имя поля для сохранения идентификатора модели в сериализованных данных. */
  def LS_DATA_TYPE_FN  = "_LSD"

  /** JSON-парсер с привязкой к названию поля. */
  def readsKv = (__ \ LS_DATA_TYPE_FN).read[LsDataType]

  def writesKv = (__ \ LS_DATA_TYPE_FN).write[LsDataType]

}


/** Отметка модели данных, относящейся к localStorage или иным хранилищам браузера. */
trait ILsModel {

  def lsDataType: LsDataType

}

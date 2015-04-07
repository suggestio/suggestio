package models.ls

import play.api.libs.json.Reads
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.15 19:13
 * Description: Модель содержит идентификаторы моделей, хранимых вместе с данными на стороне
 * пользовательского хранилища.
 */
object LsDataTypes extends Enumeration {

  /** Имя поля для сохранения идентификатора модели в сериализованных данных. */
  def LS_DATA_TYPE_FN  = "_LSD"

  type T = Value

  /** Пакет с этой отметкой хранит oauth1 access token. */
  val OAuth1AccessToken = Value("a")


  /** JSON-маппер значения из переданных данны. */
  implicit def reads: Reads[T] = {
    __.read[String]
      .map { withName }
  }

  /** JSON-парсер с привязкой к названию поля. */
  def readsKv: Reads[T] = {
    (__ \ LS_DATA_TYPE_FN).read(reads)
  }

}


/** Отметка модели данных, относящейся к localStorage или иным хранилищам браузера. */
trait ILsModel {

  def lsDataType: LsDataType

}

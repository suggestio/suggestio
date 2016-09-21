package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 11:18
  * Description: Суб-модель данных по юзер-агенту и его кускам индексируемым.
  */

object MUa extends IGenEsMappingProps with IEmpty {

  override type T = MUa

  object Fields {

    /** Имя поля сырой строки юзер-агента. */
    val UA_STR_FN             = "raw"

    val BROWSER_FN            = "browser"
    val DEVICE_FN             = "device"

    val OS_FAMILY_FN          = "osFamily"
    val OS_VSN_FN             = "osVsn"

  }

  import Fields._

  /** Обязательная поддержка JSON. */
  implicit val FORMAT: OFormat[MUa] = (
    (__ \ UA_STR_FN).formatNullable[String] and
    (__ \ BROWSER_FN).formatNullable[String] and
    (__ \ DEVICE_FN).formatNullable[String] and
    (__ \ OS_FAMILY_FN).formatNullable[String] and
    (__ \ OS_VSN_FN).formatNullable[String]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(UA_STR_FN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldString(BROWSER_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(DEVICE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(OS_FAMILY_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(OS_VSN_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
    )
  }

  override def empty = MUa()

}


/**
  * Модель статистических данных по юзер-агенту.
  *
  * @param ua Строка User-Agent.
  * @param browser Браузер.
  * @param device Устройство.
  * @param osFamily Семейство ОС.
  * @param osVsn Версия ОС в рамках семейства.
  */
case class MUa(
  ua            : Option[String] = None,
  browser       : Option[String] = None,
  device        : Option[String] = None,
  osFamily      : Option[String] = None,
  osVsn         : Option[String] = None
)
  extends EmptyProduct

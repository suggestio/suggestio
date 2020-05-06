package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 11:18
  * Description: Суб-модель данных по юзер-агенту и его кускам индексируемым.
  */

object MUa
  extends IEsMappingProps
  with IEmpty {

  override type T = MUa

  object Fields {

    /** Имя поля сырой строки юзер-агента. */
    val UA_STR_FN             = "raw"

    /**
      * Имя поля для имени браузера.
      * Движки браузеров или приложений в любом случае представляется какими-то браузерами.
      */
    val BROWSER_FN            = "browser"
    val DEVICE_FN             = "device"

    val OS_FAMILY_FN          = "osFamily"
    val OS_VSN_FN             = "osVsn"

    /** Имя поля типа или типов UA. Это может быть браузер, или мобильное приложение и т.д. */
    val UA_TYPE_FN            = "type"

  }

  import Fields._

  /** Обязательная поддержка JSON. */
  implicit val FORMAT: OFormat[MUa] = (
    (__ \ UA_STR_FN).formatNullable[String] and
    (__ \ BROWSER_FN).formatNullable[String] and
    (__ \ DEVICE_FN).formatNullable[String] and
    (__ \ OS_FAMILY_FN).formatNullable[String] and
    (__ \ OS_VSN_FN).formatNullable[String] and
    (__ \ UA_TYPE_FN).formatNullable[Seq[MUaType]]
      .inmap [Seq[MUaType]] (
        { _.getOrElse(Nil) },
        { uaTypes => if (uaTypes.isEmpty) None else Some(uaTypes) }
      )
  )(apply, unlift(unapply))

  override def empty = MUa()

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._

    Json.obj(
      UA_STR_FN     -> FText.notIndexedJs,
      BROWSER_FN    -> FKeyWord.indexedJs,
      DEVICE_FN     -> FKeyWord.indexedJs,
      OS_FAMILY_FN  -> FKeyWord.indexedJs,
      OS_VSN_FN     -> FKeyWord.indexedJs,
      UA_TYPE_FN    -> FKeyWord.indexedJs,
    )
  }

}


/**
  * Модель статистических данных по юзер-агенту.
  *
  * @param ua Строка User-Agent.
  * @param browser Браузер.
  * @param device Устройство.
  * @param osFamily Семейство ОС.
  * @param osVsn Версия ОС в рамках семейства.
  * @param uaType Типы UA. Изначально браузер или мобильное приложение.
  *               Если последнее, то например на базе кордовы.
  */
final case class MUa(
  ua            : Option[String]    = None,
  browser       : Option[String]    = None,
  device        : Option[String]    = None,
  osFamily      : Option[String]    = None,
  osVsn         : Option[String]    = None,
  uaType        : Seq[MUaType]      = Nil
)
  extends EmptyProduct
{

  def toStringSb(sb: StringBuilder = new StringBuilder(256)): StringBuilder = {
    sb.append("{")
    val s = ' '
    for (_ua <- ua)
      sb.append('`')
        .append( _ua )
        .append('`')
        .append(s)
    for (bro <- browser)
      sb.append(bro).append(s)
    for (dev <- device)
      sb.append('`').append(dev)
        .append('`').append(s)
    for(osF <- osFamily)
      sb.append(osF).append(s)
    for (osV <- osVsn)
      sb.append(osV).append(s)
    for (uaT <- uaType)
      sb.append(uaT).append(s)
    sb.append("}")
  }

  override def toString = toStringSb().toString()

}

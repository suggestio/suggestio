package io.suggest.model.n2.extra.mdr

import io.suggest.model.es.{IGenEsMappingProps, EsModelUtil}
import io.suggest.util.SioEsUtil._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.{util => ju}
import EsModelUtil.Implicits.jodaDateTimeFormat

/** Модель для данных по результатам модерации узла N2. */

object MFreeAdv extends IGenEsMappingProps {

  val IS_ALLOWED_ESFN   = "ia"
  val WHEN_ESFN         = "w"
  val BY_USER_ESFN      = "bu"
  val REASON_ESFN       = "r"

  /** Создать под-маппинг для индекса. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldBoolean(IS_ALLOWED_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldDate(WHEN_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(BY_USER_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(REASON_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )
  }

  /** Десериализация распарсенных данных FreeAdvStatus, ранее сериализованных в JSON. */
  // TODO compat с EMModeration. После MAd N2 это надо удалить начисто.
  val deserialize: PartialFunction[Any, MFreeAdv] = {
    case jmap: ju.Map[_,_] =>
      MFreeAdv(
        isAllowed = Option(jmap get IS_ALLOWED_ESFN)
          .fold[Boolean] (false) (EsModelUtil.booleanParser),
        when = Option(jmap get WHEN_ESFN)
          .fold (new DateTime(1970, 1, 1, 0, 0)) (EsModelUtil.dateTimeParser),
        byUser = EsModelUtil.stringParser(jmap get BY_USER_ESFN),
        reason = Option(jmap get REASON_ESFN) map EsModelUtil.stringParser
      )
  }

  /** Поддержка play.json. */
  implicit val FORMAT: OFormat[MFreeAdv] = (
    (__ \ IS_ALLOWED_ESFN).format[Boolean] and
    (__ \ BY_USER_ESFN).format[String] and
    (__ \ WHEN_ESFN).format[DateTime] and
    (__ \ REASON_ESFN).formatNullable[String]
  )(apply, unlift(unapply))

}


/**
 * Экземпляр информации по результатам модерации карточки для бесплатного размещения.
 * @param isAllowed Разрешена ли карточка к эксплуатации?
 * @param when Когда было выставлено [не]разрешение?
 * @param byUser personId модератора sio.
 * @param reason Опциональное текстовое пояснение вердикта модератора.
 */
case class MFreeAdv(
  isAllowed : Boolean,
  byUser    : String,
  when      : DateTime        = DateTime.now,
  reason    : Option[String]  = None
) {

  // TODO compat с EMModeration. Спилить это вместе с EMModeration.
  def toPlayJson: JsObject = {
    MFreeAdv.FORMAT
      .writes( this )
  }

}

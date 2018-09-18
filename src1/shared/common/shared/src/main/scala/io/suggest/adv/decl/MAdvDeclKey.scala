package io.suggest.adv.decl

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.18 16:48
  * Description: Модель ключа adv-decl.
  * Он описывает одну цель размещения, поддерживаемую биллингом на сервере.
  */
object MAdvDeclKey {

  @inline implicit def univEq: UnivEq[MAdvDeclKey] = UnivEq.derive

  // TODO Поддержка tagFace пока выключена за неготовностью всего остального кода даже к rcvrKey.

  implicit def mAdvDeclKeyFormat: OFormat[MAdvDeclKey] = (
    (__ \ "i").format[MItemType] and
    (__ \ "r").formatNullable[RcvrKey] //and
    //(__ \ "t").formatNullable[String]
  )(apply, unlift(unapply))

  import scalaz.{ValidationNel, Validation}
  import scalaz.syntax.apply._

  /** Валидация decl-ключа размещения. */
  def validate(k: MAdvDeclKey): ValidationNel[String, MAdvDeclKey] = {
    val itypeVld = Validation.liftNel(k.itype)(i => !MItemTypes.advDeclTypes.contains(i), "!itype.decl")

    val rcvrKeyVld = if (MItemTypes.advDirectTypes contains k.itype) {
      ScalazUtil.liftNelSome(k.rcvrKey, "direct.rcvr.expected")( RcvrKey.rcvrKeyV )
    } else {
      ScalazUtil.liftNelNone(k.rcvrKey, "rcvr.key.unexpected")
    }

    /*
    val tagFaceVld = if (MItemTypes.tagTypes contains k.itype) {
      ScalazUtil.liftNelSome(k.tagFace, "tag.face.missing")( TagsEditConstants.Constraints.tagFaceV )
    } else {
      ScalazUtil.liftNelNone(k.tagFace, "tag.face.unexpected")
    }
    */

    (
      itypeVld |@|
      rcvrKeyVld /*|@|
      tagFaceVld*/
    )(apply)
  }

}


/** Контейнер данных по описанию уникальной цели размещения.
  *
  * @param itype Тип item'а. Не все item'ы допутимы.
  * @param rcvrKey Ключ до узла ресивера, это размещений в узле или в теге узла.
  * param tagFace Название тега.
  */
case class MAdvDeclKey(
                        itype   : MItemType,
                        rcvrKey : Option[RcvrKey],
                        //tagFace : Option[String]
                      )

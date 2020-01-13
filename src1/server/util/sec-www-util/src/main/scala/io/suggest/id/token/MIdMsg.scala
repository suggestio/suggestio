package io.suggest.id.token

import java.time.Instant

import io.suggest.common.empty.EmptyUtil
import io.suggest.n2.edge.MPredicate
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 18:56
  * Description: Модель данных одного проверочного сообщения.
  */
object MIdMsg {

  implicit def mIdMsgJson: OFormat[MIdMsg] = (
    (__ \ "t").format[MPredicate] and
    (__ \ "c").format[String] and
    (__ \ "r").formatNullable[Set[String]]
      .inmap[Set[String]](
        EmptyUtil.opt2ImplEmptyF(Set.empty),
        { els => if (els.isEmpty) None else Some(els) }
      ) and
    (__ \ "a").format[Instant] and
    (__ \ "i").formatNullable[String] and
    (__ \ "v").formatNullable[Instant] and
    (__ \ "e").format[Int]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIdMsg] = UnivEq.derive

  val validated     = GenLens[MIdMsg](_.validated)
  val errorsCount   = GenLens[MIdMsg](_.errorsCount)

}


/** Контейнер метаданных одного отправленного сообщения идентификации.
  *
  * @param sentAt Когда отправлено.
  * @param msgId id отправленного сообщения, если есть.
  * @param checkCode Секретный код проверки.
  * @param validated Уже проверено? Если да, то когда.
  */
case class MIdMsg(
                   rcptType    : MPredicate,
                   checkCode   : String,
                   rcpt        : Set[String]        = Set.empty,
                   sentAt      : Instant            = Instant.now(),
                   msgId       : Option[String]     = None,
                   validated   : Option[Instant]    = None,
                   errorsCount : Int                = 0,
                 )

package io.suggest.sys.mdr

import diode.FastEq
import io.suggest.jd.MJdData
import io.suggest.mbill2.m.item.MItem
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 11:04
  * Description: JSON-модель данных по модерации одной карточки.
  */
object MNodeMdrInfo {

  implicit object MNodeMdrInfoFastEq extends FastEq[MNodeMdrInfo] {
    override def eqv(a: MNodeMdrInfo, b: MNodeMdrInfo): Boolean = {
      (a.nodeId ===* b.nodeId) &&
      (a.ad ===* b.ad) &&
      (a.items ===* b.items) &&
      (a.nodes ===* b.nodes) &&
      (a.directSelfNodeIds ===* b.directSelfNodeIds)
    }
  }

  /** Поддержка play-json. */
  implicit def mNodeMdrInfoFormat: OFormat[MNodeMdrInfo] = (
    (__ \ "i").format[String] and
    (__ \ "a").formatNullable[MJdData] and
    (__ \ "t").format[Seq[MItem]] and
    (__ \ "n").format[Iterable[MSc3IndexResp]] and
    (__ \ "d").format[Set[String]]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MNodeMdrInfo] = UnivEq.derive

}


/** Контейнер данных для модерации узла.
  *
  * @param nodeId id модерируемого узла.
  * @param ad Данные для рендера рекламной карточки.
  * @param items item'ы для модерации.
  * @param directSelfNodeIds Размещение на своих узлах в обход биллинга.
  */
case class MNodeMdrInfo(
                         nodeId               : String,
                         ad                   : Option[MJdData],
                         items                : Seq[MItem],
                         nodes                : Iterable[MSc3IndexResp],
                         directSelfNodeIds    : Set[String],
                       )

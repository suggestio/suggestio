package io.suggest.sys.mdr

import diode.FastEq
import io.suggest.jd.MJdAdData
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.item.MItem
import io.suggest.primo.id.IId
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

  implicit def univEq: UnivEq[MNodeMdrInfo] = UnivEq.derive


  /** Поддержка play-json. */
  implicit def mNodeMdrInfoFormat: OFormat[MNodeMdrInfo] = (
    (__ \ "i").format[String] and
    (__ \ "a").formatNullable[MJdAdData] and
    (__ \ "t").format[Seq[MItem]] and
    (__ \ "n").format[Iterable[MAdvGeoMapNodeProps]] and
    (__ \ "d").format[Iterable[String]]
  )(apply, unlift(unapply))

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
                         ad                   : Option[MJdAdData],
                         items                : Seq[MItem],
                         nodes                : Iterable[MAdvGeoMapNodeProps],
                         directSelfNodeIds    : Iterable[String],
                       ) {

  /** Сгруппированные item'ы по типам. */
  lazy val itemsByType = items.groupBy(_.iType)

  /** Карта узлов по id. */
  lazy val nodesMap = IId.els2idMap[String, MAdvGeoMapNodeProps]( nodes )

  /** Список бесплатных размещений на "своих" узлах в обход биллингов. */
  lazy val directSelfNodesSorted: Seq[MAdvGeoMapNodeProps] = {
    directSelfNodeIds
      .iterator
      .flatMap( nodesMap.get )
      .toSeq
      .sortBy { m =>
        m.hint getOrElse m.nodeId
      }
  }

}

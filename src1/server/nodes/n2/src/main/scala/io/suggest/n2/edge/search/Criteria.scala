package io.suggest.n2.edge.search

import io.suggest.common.empty.EmptyProduct
import io.suggest.dev.MOsFamily
import io.suggest.es.model.{IMust, Must_t}
import io.suggest.ext.svc.MExtService
import io.suggest.geo.MGeoPoint
import io.suggest.n2.edge.MPredicate
import io.suggest.n2.media.storage.MStorage

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.15 14:42
  * Description: Модель критерия для поиска по node-edges.
  * Заданные здесь множества id-узлов и их предикатов проверяются в рамках каждого nested-объкета.
  * Для задания нескольких критериев поиска нужно несколько критериев перечислить.
  *
  * @param nodeIds id искомых узлов.
  * @param predicates искомые предикаты.
  * @param must
  * @param flag Состояние дополнительного флага в контейнера info. (v1)
  * @param flags Флаги: критерии флагов.
  *              Несколько критериев объединяются через ИЛИ.
  * @param date Поиск по индексируемой дате-времени.
  *             Несколько интервалов - объединяются через ИЛИ.
  * @param tags Критерии для поиска по тегам.
  * @param gsIntersect Данные для поиска по гео-шейпам.
  * @param nodeIdsMatchAll Каким образом трактовать nodeIds, если их >= 2?
  *                        true значит объединять запрошенные nodeId через AND.
  *                        false - OR.
  * @param extService Искомые внешние сервисы.
  * @param osFamilies Искомые операционки.
  * @param fileHashesHex Поиск по файловым хэшам.
  * @param fileMimes Поиск по mime-типам файлов.
  * @param fileSizeB Поиск по размеру (размерам) хранимых файлов.
  * @param pictureHeightPx Высота картинки.
  *                        List(x) - точное совпадение размера.
  *                        List(x1, x2) - интервал от и до.
  * @param pictureWidthPx Ширина картинки. Аналогично высоте.
  */
final case class Criteria(
                           nodeIds           : Seq[String]              = Nil,
                           predicates        : Seq[MPredicate]          = Nil,
                           must              : Must_t                   = IMust.SHOULD,
                           // info
                           flag              : Option[Boolean]          = None,
                           flags             : Seq[EdgeFlagCriteria]    = Nil,
                           date              : Seq[EsRange]             = Nil,
                           tags              : Seq[TagCriteria]         = Nil,
                           gsIntersect       : Option[IGsCriteria]      = None,
                           nodeIdsMatchAll   : Boolean                  = false,
                           geoDistanceSort   : Option[MGeoPoint]        = None,
                           extService        : Option[Seq[MExtService]] = None,
                           osFamilies        : Option[Seq[MOsFamily]]   = None,
                           // media
                           fileHashesHex     : Iterable[MHashCriteria]  = Nil,
                           fileMimes         : Iterable[String]         = Nil,
                           fileSizeB         : Iterable[Long]           = Nil,
                           fileIsOriginal    : Option[Boolean]          = None,
                           fileStorType      : Set[MStorage]            = Set.empty,
                           fileStorMetaData  : Set[String]              = Set.empty,
                           // media.picture
                           pictureHeightPx   : List[Int]                = Nil,
                           pictureWidthPx    : List[Int]                = Nil,
                         )
  extends EmptyProduct
  with IMust
{

  def isContainsSort: Boolean =
    geoDistanceSort.nonEmpty

  override def toString: String = {
    val sb = new StringBuilder(128, productPrefix)
    sb.append('(')

    sb.append(
      IMust.toString(must)
    )

    val _preds = predicates
    if (_preds.nonEmpty) {
      sb.append( ",p=[" )
        .append( _preds.mkString(",") )
        .append( ']')
    }

    val _nodeIds = nodeIds
    if (_nodeIds.nonEmpty) {
      val delim = s" ${if (nodeIdsMatchAll) "&" else "|"} "
      sb.append(",nodes=[")
      for (nodeId <- _nodeIds) {
        sb.append(nodeId)
          .append(delim)
      }
      sb.append(']')
    }

    for (_flag <- flag) {
      sb.append(",flag=")
        .append( _flag )
    }

    for (_tag <- tags) {
      sb.append(",tag=")
        .append(_tag)
    }

    for (_gsCr <- gsIntersect) {
      sb.append(",gs=")
        .append(_gsCr)
    }

    for (mgp <- geoDistanceSort) {
      sb.append(",geoDistanceSort=")
        .append(mgp.toHumanFriendlyString)
    }

    sb.append(')')
      .toString()
  }

}

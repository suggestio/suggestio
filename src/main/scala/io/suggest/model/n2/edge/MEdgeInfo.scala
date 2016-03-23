package io.suggest.model.n2.edge

import io.suggest.common.empty.{IEmpty, EmptyProduct, IIsNonEmpty}
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.geo.GeoShape
import io.suggest.model.sc.common.SinkShowLevel
import io.suggest.util.SioConstants
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.model.es.EsModelUtil.Implicits.jodaDateTimeFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 12:47
 * Description: У рёбер [[io.suggest.model.n2.edge.MEdge]] могут быть дополнительные данные.
 * Здесь модель для этих опциональных данных.
 *
 * Это неявно-пустая модель, т.е. все поля модели могут быть пустыми.
 */
object MEdgeInfo extends IGenEsMappingProps with IEmpty {

  override type T = MEdgeInfo

  /** Статический пустой экземпляр модели. */
  override val empty: MEdgeInfo = {
    new MEdgeInfo() {
      override def nonEmpty = false
    }
  }


  object Fields {

    val DYN_IMG_ARGS_FN   = "di"
    val SLS_FN            = "sls"
    val DATE_NI_FN        = "dtni"
    val COMMENT_NI_FN     = "coni"
    val FLAG_FN           = "flag"
    val GEO_SHAPE_FN      = "gs"
    val ITEM_IDS_FN       = "bgid"

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MEdgeInfo] = (
    (__ \ DYN_IMG_ARGS_FN).formatNullable[String] and
    (__ \ SLS_FN).formatNullable[Set[SinkShowLevel]]
      .inmap [Set[SinkShowLevel]] (
        { _ getOrElse Set.empty },
        { ssls => if (ssls.nonEmpty) Some(ssls) else None }
      ) and
    (__ \ DATE_NI_FN).formatNullable[DateTime] and
    (__ \ COMMENT_NI_FN).formatNullable[String] and
    (__ \ FLAG_FN).formatNullable[Boolean] and
    (__ \ GEO_SHAPE_FN).formatNullable[GeoShape] and
    (__ \ ITEM_IDS_FN).formatNullable[Set[Long]]
      .inmap [Set[Long]] (
        { _ getOrElse Set.empty },
        { bgs => if (bgs.nonEmpty) Some(bgs) else None }
      )
  )(apply, unlift(unapply))




  import io.suggest.util.SioEsUtil._

  /** Сборка полей ES-маппинга. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(
        id              = DYN_IMG_ARGS_FN,
        index           = FieldIndexingVariants.no,
        include_in_all  = false
      ),
      FieldString(
        id              = SLS_FN,
        index           = FieldIndexingVariants.analyzed,
        include_in_all  = false,
        index_analyzer  = SioConstants.DEEP_NGRAM_AN,
        search_analyzer = SioConstants.MINIMAL_AN
      ),
      FieldDate(
        id              = DATE_NI_FN,
        index           = FieldIndexingVariants.no,
        include_in_all  = false
      ),
      FieldString(
        id              = COMMENT_NI_FN,
        index           = FieldIndexingVariants.no,
        include_in_all  = false
      ),
      FieldBoolean(
        id              = FLAG_FN,
        index           = FieldIndexingVariants.not_analyzed,
        include_in_all  = false
      ),
      FieldGeoShape(
        id              = GEO_SHAPE_FN,
        precision       = "50m"
      ),
      FieldNumber(
        id              = ITEM_IDS_FN,
        fieldType       = DocFieldTypes.long,
        // Изначально было not_analyzed, но как-то не удалось придумать ни одной ситуации, когда оно пригодится.
        // Ибо весь биллинг самодостаточен и живёт в postgresql, здесь просто подсказка для обратной связи с MItems.
        index           = FieldIndexingVariants.no,
        include_in_all  = false
      )
    )
  }

}


/** Интерфейс элементов модели. */
trait IEdgeInfo extends IIsNonEmpty {

  /** При указании на картинку бывает нужно указать исходный кроп или что-то ещё. */
  def dynImgArgs   : Option[String]

  /** При публикации карточке где-то нужно указывать show levels, т.е. где именно карточка отображается. */
  def sls          : Set[SinkShowLevel]

  /** Неиднексируемая дата. */
  def dateNi       : Option[DateTime]

  /** Неиндексируемый комментарий. */
  def commentNi    : Option[String]

  /** Индексируемое значение некоторого флага. */
  def flag         : Option[Boolean]

  /** Геошейп, связанный с этим ребром. */
  def geoShape     : Option[GeoShape]

  /**
   * item ids, напрямую связанные с данным эджем.
   * НЕ надо сюда запихивать эджи, которые живут внутри карты гео-шейпов. Т.е. например размещения в гео-тегах.
   * Теоретически например, некое размещение direct adv в несколько параллельных заходов на одном и том же узле.
   * В норме здесь не более одного id.
   * Поле не индексируется: нет смысла, всё уже проиндексировано в биллинге. И не возникалов нужды.
   * Поле используется только как подсказка для самоконтроля или некий вспомогательный инструмент.
   * НЕ надо переделывать в Option[Long] -- от этого ушли ещё в начале 2016.
   */
  def itemIds       : Set[Long]

}


/** Класс экземпляров модели [[IEdgeInfo]]. */
case class MEdgeInfo(
  override val dynImgArgs   : Option[String]        = None,
  override val sls          : Set[SinkShowLevel]    = Set.empty,
  override val dateNi       : Option[DateTime]      = None,
  override val commentNi    : Option[String]        = None,
  override val flag         : Option[Boolean]       = None,
  override val geoShape     : Option[GeoShape]      = None,
  override val itemIds      : Set[Long]             = Set.empty
)
  extends EmptyProduct
  with IEdgeInfo
{

  /** Форматирование для вывода в шаблонах. */
  override def toString: String = {
    if (nonEmpty) {
      val sb = new StringBuilder(32)
      dynImgArgs.foreach { dia =>
        sb.append("dynImg=")
          .append(dia)
          .append(' ')
      }
      if (sls.nonEmpty) {
        sb.append("sls=")
        for (sl <- sls) {
          sb.append(sl).append(',')
        }
        sb.append(' ')
      }
      if (dateNi.nonEmpty) {
        sb.append("dateNi=")
          .append(dateNi.get)
          .append(' ')
      }
      if (commentNi.nonEmpty) {
        sb.append("commentNi=")
          .append(commentNi.get)
          .append(' ')
      }
      for (gs <- geoShape) {
        sb.append("gs=")
          .append(gs)
      }
      if (itemIds.nonEmpty) {
        sb.append("itemIds=")
        for (bgid <- itemIds) {
          sb.append(bgid).append(',')
        }
        sb.append(' ')
      }
      sb.toString()

    } else {
      ""
    }
  }


  def _extraKeyData: EdgeXKey_t = {
    itemIds.toList
  }

}

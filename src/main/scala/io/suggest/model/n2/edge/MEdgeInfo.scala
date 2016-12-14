package io.suggest.model.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty, IIsNonEmpty}
import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.sc.common.SinkShowLevel
import io.suggest.util.SioConstants
import io.suggest.ym.model.NodeGeoLevel
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.model.es.EsModelUtil.Implicits.jodaDateTimeFormat
import io.suggest.model.geo.GeoPoint

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


  /** Модель названий полей модели [[MEdgeInfo]]. */
  object Fields {

    val DYN_IMG_ARGS_FN   = "di"
    val SLS_FN            = "sls"
    val DATE_NI_FN        = "dtni"
    val COMMENT_NI_FN     = "coni"
    val FLAG_FN           = "flag"
    val GEO_SHAPES_FN     = "gss"
    val ITEM_IDS_FN       = "bgid"
    val TAGS_FN           = "tags"
    val GEO_POINT_FN      = "gpt"

    /** Поле тегов внутри является multi-field. Это нужно для аггрегации, например. */
    object Tags extends PrefixedFn {

      override protected def _PARENT_FN: String = TAGS_FN

      /** Имя raw-подполя, индексирующего всё без анализа. */
      def RAW_FN = "raw"

      def TAGS_RAW_FN = _fullFn(RAW_FN)

    }

    /** Модель названий полей, проброшенных сюда из [[MEdgeGeoShape.Fields]]. */
    object GeoShapes extends PrefixedFn {

      override protected def _PARENT_FN = GEO_SHAPES_FN

      import MEdgeGeoShape.{Fields => Fs}

      def GS_GLEVEL_FN                    = _fullFn( Fs.GLEVEL_FN )
      def GS_GJSON_COMPAT_FN              = _fullFn( Fs.GJSON_COMPAT_FN )
      def GS_SHAPE_FN(ngl: NodeGeoLevel)  = _fullFn( Fs.SHAPE_FN(ngl) )

    }

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MEdgeInfo] = (
    (__ \ DYN_IMG_ARGS_FN).formatNullable[String] and
    (__ \ SLS_FN).formatNullable[Set[SinkShowLevel]]
      .inmap [Set[SinkShowLevel]] (
        _.getOrElse(Set.empty),
        { ssls => if (ssls.nonEmpty) Some(ssls) else None }
      ) and
    (__ \ DATE_NI_FN).formatNullable[DateTime] and
    (__ \ COMMENT_NI_FN).formatNullable[String] and
    (__ \ FLAG_FN).formatNullable[Boolean] and
    (__ \ ITEM_IDS_FN).formatNullable[Set[Long]]
      .inmap [Set[Long]] (
        _.getOrElse( Set.empty ),
        { bgs => if (bgs.nonEmpty) Some(bgs) else None }
      ) and
    (__ \ TAGS_FN).formatNullable[Set[String]]
      .inmap [Set[String]] (
        _.getOrElse(Set.empty),
        { tags => if (tags.nonEmpty) Some(tags) else None }
      ) and
    (__ \ GEO_SHAPES_FN).formatNullable[ List[MEdgeGeoShape] ]
      .inmap [List[MEdgeGeoShape]] (
        _.getOrElse(Nil),
        { geos => if (geos.nonEmpty) Some(geos) else None }
      ) and
    (__ \ GEO_POINT_FN).formatNullable[ Seq[GeoPoint] ]
      .inmap [Seq[GeoPoint]] (
        _.getOrElse(Nil),
        { gps => if (gps.nonEmpty) Some(gps) else None }
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
        analyzer        = SioConstants.DEEP_NGRAM_AN,
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
      FieldNumber(
        id              = ITEM_IDS_FN,
        fieldType       = DocFieldTypes.long,
        // Изначально было not_analyzed, но как-то не удалось придумать ни одной ситуации, когда оно пригодится.
        // Ибо весь биллинг самодостаточен и живёт в postgresql, здесь просто подсказка для обратной связи с MItems.
        index           = FieldIndexingVariants.no,
        include_in_all  = false
      ),
      // 2016.mar.24 Теперь теги живут внутри эджей.
      FieldString(
        id              = TAGS_FN,
        index           = FieldIndexingVariants.analyzed,
        include_in_all  = true,
        analyzer        = SioConstants.ENGRAM_AN_1,
        search_analyzer = SioConstants.DFLT_AN,
        fields = Seq(
          // Для аггрегации нужны ненормированные термы. Они позволят получать необрезанные слова.
          FieldString(
            id              = Tags.RAW_FN,
            include_in_all  = false,
            index           = FieldIndexingVariants.analyzed,
            analyzer        = SioConstants.KW_LC_AN
          )
        )
      ),
      // Список геошейпов идёт как nested object, чтобы расширить возможности индексации (ценой усложнения запросов).
      FieldNestedObject(
        id              = GEO_SHAPES_FN,
        enabled         = true,
        properties      = MEdgeGeoShape.generateMappingProps
      ),
      // 2016.sep.29 Геоточки, используются как для информации, так и для индексации.
      // Пока не очень ясно, какие именно настройки индексации поля здесь необходимы.
      // Изначальное назначение: экспорт на карту узлов выдачи, чтобы в кружках с цифрами отображались.
      // Окружности и прочее фигурное добро для этого элементарного действа не подходят ни разу.
      FieldGeoPoint(
        id              = GEO_POINT_FN,
        geohash         = true
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

  /** Список геошейпов, которые связаны с данным эджем.
    * Изначально было Seq, но из-за частой пошаговой пересборки этого лучше подходит List. */
  def geoShapes    : List[MEdgeGeoShape]

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

  /** Названия тегов, которые индексируются для полноценного поиска по тегам. */
  def tags          : Set[String]

  /** Некие опорные точки, если есть. */
  def geoPoints     : Seq[GeoPoint]


  /** Форматирование для вывода в шаблонах. */
  override def toString: String = {
    val sb = new StringBuilder(32)

    for (dia <- dynImgArgs) {
      sb.append("dynImg{")
        .append(dia)
        .append("} ")
    }

    val _sls = sls
    if (_sls.nonEmpty) {
      sb.append("sls=")
      for (sl <- _sls) {
        sb.append(sl)
          .append(',')
      }
      sb.append(' ')
    }

    for (dt <- dateNi) {
      sb.append("dateNi=")
        .append(dt)
        .append(' ')
    }

    for (comment <- commentNi) {
      sb.append("commentNi=")
        .append(comment)
        .append(' ')
    }

    val _itemIds = itemIds
    if (_itemIds.nonEmpty) {
      sb.append("itemIds=")
      for (bgid <- _itemIds) {
        sb.append(bgid).append(',')
      }
      sb.append(' ')
    }

    val _tags = tags
    if (_tags.nonEmpty) {
      sb.append("tags=")
      for (tag <- _tags) {
        sb.append(tag).append(',')
      }
      sb.append(' ')
    }

    val _geoShapes = geoShapes
    if (_geoShapes.nonEmpty) {
      sb.append(_geoShapes.size)
        .append("gss,")
    }

    val _geoPoints = geoPoints
    if (_geoPoints.nonEmpty) {
      sb.append("geoPoints={")
      for (gp <- _geoPoints) {
        sb.append( GeoPoint.toEsStr(gp) )
      }
      sb.append('}')
    }

    sb.toString()
  }

}


/** Класс экземпляров модели [[IEdgeInfo]]. */
case class MEdgeInfo(
  override val dynImgArgs   : Option[String]        = None,
  override val sls          : Set[SinkShowLevel]    = Set.empty,
  override val dateNi       : Option[DateTime]      = None,
  override val commentNi    : Option[String]        = None,
  override val flag         : Option[Boolean]       = None,
  override val itemIds      : Set[Long]             = Set.empty,
  override val tags         : Set[String]           = Set.empty,
  override val geoShapes    : List[MEdgeGeoShape]   = Nil,
  override val geoPoints    : Seq[GeoPoint]         = Nil
)
  extends EmptyProduct
  with IEdgeInfo
{

  override def toString = super.toString

}

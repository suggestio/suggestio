package io.suggest.n2.edge

import java.time.OffsetDateTime
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.dev.MOsFamily
import io.suggest.es.{EsConstants, IEsMappingProps, MappingDsl}
import io.suggest.ext.svc.MExtService
import io.suggest.geo.{MGeoPoint, MNodeGeoLevel}
import io.suggest.model.PrefixedFn
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.xplay.json.PlayJsonUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 12:47
 * Description: У рёбер [[io.suggest.n2.edge.MEdge]] могут быть дополнительные данные.
 * Здесь модель для этих опциональных данных.
 *
 * Это неявно-пустая модель, т.е. все поля модели могут быть пустыми.
 */
object MEdgeInfo
  extends IEsMappingProps
  with IEmpty
{

  override type T = MEdgeInfo

  /** Статический пустой экземпляр модели. */
  override val empty = apply()


  /** Модель названий полей модели [[MEdgeInfo]]. */
  object Fields {

    val DATE_FN           = "date"
    val DATE_NI_FN        = "dateNoInx"
    val COMMENT_NI_FN     = "comment"
    val FLAG_FN           = "flag"
    val FLAGS_FN          = "edgeFlags"
    val GEO_SHAPES_FN     = "geoShapes"
    val TAGS_FN           = "tags"
    val GEO_POINT_FN      = "geoPoints"
    val EXT_SERVICE_FN    = "externalService"
    val OS_FAMILY_FN      = "osFamily"

    /** Поле тегов внутри является multi-field. Это нужно для аггрегации, например. */
    object Tags extends PrefixedFn {

      override protected def _PARENT_FN: String = TAGS_FN

      /** Имя raw-подполя, индексирующего всё без анализа. */
      def RAW_FN = "raw"
      def KW_FN  = "kw"

      def TAGS_RAW_FN = _fullFn(RAW_FN)

    }

    /** Модель названий полей, проброшенных сюда из [[MEdgeGeoShape.Fields]]. */
    object GeoShapes extends PrefixedFn {

      override protected def _PARENT_FN = GEO_SHAPES_FN

      import MEdgeGeoShape.{Fields => Fs}

      def GS_GLEVEL_FN                    = _fullFn( Fs.GLEVEL_FN )
      def GS_GJSON_COMPAT_FN              = _fullFn( Fs.GEO_JSON_COMPAT_FN )
      def GS_SHAPE_FN(ngl: MNodeGeoLevel) = _fullFn( Fs.SHAPE_FN(ngl) )

    }

    /** Имена полей флагов. */
    object Flags extends PrefixedFn {
      import MEdgeFlagData.{Fields => Fs}

      override protected def _PARENT_FN = FLAGS_FN
      def FLAG_FN = _fullFn( Fs.FLAG_FN )
    }

  }


  /** Поддержка JSON. */
  implicit val mEdgeInfoFormat: Format[MEdgeInfo] = {
    val F = Fields
    (
      PlayJsonUtil.fallbackPathFormatNullable[OffsetDateTime]( F.DATE_FN, "dt" ) and
      PlayJsonUtil.fallbackPathFormatNullable[OffsetDateTime]( F.DATE_NI_FN, "dtni" ) and
      PlayJsonUtil.fallbackPathFormatNullable[String]( F.COMMENT_NI_FN, "coni" ) and
      (__ \ F.FLAG_FN).formatNullable[Boolean] and
      PlayJsonUtil.fallbackPathFormatNullable[Iterable[MEdgeFlagData]]( F.FLAGS_FN, "flags" )
        .inmap[Iterable[MEdgeFlagData]](
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { flags => if (flags.isEmpty) None else Some(flags) }
        ) and
      (__ \ F.TAGS_FN).formatNullable[Set[String]]
        .inmap [Set[String]] (
          EmptyUtil.opt2ImplEmptyF( Set.empty ),
          { tags => if (tags.nonEmpty) Some(tags) else None }
        ) and
      PlayJsonUtil.fallbackPathFormatNullable[List[MEdgeGeoShape]]( F.GEO_SHAPES_FN, "gss" )
        .inmap [List[MEdgeGeoShape]] (
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { geos => if (geos.nonEmpty) Some(geos) else None }
        ) and
      PlayJsonUtil.fallbackPathFormatNullable[Seq[MGeoPoint]]( F.GEO_POINT_FN, "gpt" )
        .inmap [Seq[MGeoPoint]] (
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { gps => if (gps.nonEmpty) Some(gps) else None }
        ) and
      PlayJsonUtil.fallbackPathFormatNullable[MExtService]( F.EXT_SERVICE_FN, "xs" ) and
      PlayJsonUtil.fallbackPathFormatNullable[MOsFamily]( F.OS_FAMILY_FN, "os" )
    )(apply, unlift(unapply))
  }


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.DATE_FN -> FDate(
        index           = someTrue,
        docValues       = someTrue,
        ignoreMalformed = someFalse,
      ),
      F.DATE_NI_FN -> FDate.notIndexedJs,
      F.COMMENT_NI_FN -> FText.notIndexedJs,
      F.FLAG_FN -> FBoolean.indexedJs,
      F.FLAGS_FN -> FObject.nested(
        properties = MEdgeFlagData.esMappingProps,
      ),
      // Теги
      F.TAGS_FN -> FText(
        index           = someTrue,
        analyzer        = Some( EsConstants.ENGRAM_1LETTER_ANALYZER ),
        searchAnalyzer  = Some( EsConstants.DEFAULT_ANALYZER ),
        fields = Some( Json.obj(
          // При поиске тегов надо игнорить регистр:
          F.Tags.RAW_FN -> FText(
            index = someTrue,
            analyzer = Some( EsConstants.KEYWORD_LOWERCASE_ANALYZER ),
            // TODO Нужна поддержка аггрегации тут: нужен какой-то параметр тут + переиндексация. И можно удалить KW_FN.
          ),
          // Для аггрегации нужно keyword-термы. Они позволят получать необрезанные слова.
          // Появилась для DirectTagsUtil, но из-за других проблем там, это поле не используется.
          F.Tags.KW_FN -> FKeyWord.indexedJs,
        ))
      ),
      // Список геошейпов идёт как nested object, чтобы расширить возможности индексации (ценой усложнения запросов).
      F.GEO_SHAPES_FN -> FObject.nested( MEdgeGeoShape.esMappingProps ),
      // 2016.sep.29 Геоточки, используются как для информации, так и для индексации.
      // Пока не очень ясно, какие именно настройки индексации поля здесь необходимы.
      // Изначальное назначение: экспорт на карту узлов выдачи, чтобы в кружках с цифрами отображались.
      // Окружности и прочее фигурное добро для этого элементарного действа не подходят ни разу.
      F.GEO_POINT_FN -> FGeoPoint.indexedJs,
      F.EXT_SERVICE_FN -> FKeyWord.indexedJs,
      F.OS_FAMILY_FN -> FKeyWord.indexedJs,
    )
  }


  def date = GenLens[MEdgeInfo](_.date)
  def dateNi = GenLens[MEdgeInfo](_.dateNi)
  def textNi = GenLens[MEdgeInfo](_.textNi)
  def flag = GenLens[MEdgeInfo](_.flag)
  def flags = GenLens[MEdgeInfo](_.flags)
  def tags = GenLens[MEdgeInfo](_.tags)
  def geoShapes = GenLens[MEdgeInfo](_.geoShapes)
  def geoPoints = GenLens[MEdgeInfo](_.geoPoints)
  def extService = GenLens[MEdgeInfo](_.extService)
  def osFamily = GenLens[MEdgeInfo](_.osFamily)

  @inline implicit def univEq: UnivEq[MEdgeInfo] = UnivEq.derive

}



/** Класс экземпляров модели MEdgeInfo.
  *
  * @param date Индексируемая дата, пригодная для сортировки.
  * @param dateNi Неиднексируемая дата.
  * @param textNi Неиндексируемый текст при эдже.
  *                  Используется для хэша пароля в Password-эджах с 2019-02-28.
  *                  Изначально использовался как произвольный служебный комментарий от/для админа.
  * @param flag Индексируемое значение некоторого флага. Следует использовать flags вместо этого поля.
  * @param flags Новый формат флагов.
  * @param tags Названия тегов, которые индексируются для полноценного поиска по тегам.
  *             2018-07-24 Сюда же падает индексируемые названия узла на карте.
  * @param geoShapes Список геошейпов, которые связаны с данным эджем.
  * Изначально было Seq, но из-за частой пошаговой пересборки этого лучше подходит List.
  * @param geoPoints Некие опорные геокоординаты, если есть.
  * @param extService id внешнего сервиса, с которым ангажированна данная связь.
  * @param osFamily Привязка эджа к семейству операционных систем.
  */
final case class MEdgeInfo(
                            date         : Option[OffsetDateTime]  = None,
                            dateNi       : Option[OffsetDateTime]  = None,
                            textNi       : Option[String]          = None,
                            // TODO flag надо убрать, чтобы использовалось множество флагов flags.
                            flag         : Option[Boolean]         = None,
                            flags        : Iterable[MEdgeFlagData] = Nil,
                            tags         : Set[String]             = Set.empty,
                            geoShapes    : List[MEdgeGeoShape]     = Nil,
                            geoPoints    : Seq[MGeoPoint]          = Nil,
                            extService   : Option[MExtService]     = None,
                            osFamily     : Option[MOsFamily]       = None,
                          )
  extends EmptyProduct
{

  /** Карта флагов, если вдруг нужна будет. */
  lazy val flagsMap = flags
    .zipWithIdIter[MEdgeFlag]
    .to( Map )

  /** Форматирование для вывода в шаблонах. */
  override def toString: String = {
    val sb = new StringBuilder(32)

    for (dt <- dateNi) {
      sb.append("dateNi=")
        .append(dt)
        .append(' ')
    }

    for (comment <- textNi) {
      sb.append("textNi=")
        .append( StringUtil.strLimitLen(comment, 10) )
        .append(' ')
    }

    for (flag1 <- flag)
      sb.append(flag1)

    if (flags.nonEmpty) {
      sb.append('[')
      for (flagData <- flags)
        sb.append(flagData)
          .append(',')
      sb.append(']')
    }

    if (tags.nonEmpty) {
      sb.append("tags=")
      for (tag <- tags) {
        sb.append(tag).append(',')
      }
      sb.append(' ')
    }

    if (geoShapes.nonEmpty) {
      sb.append(geoShapes.size)
        .append("gss")
    }

    if (geoPoints.nonEmpty) {
      sb.append(",geoPoints={")
      for (gp <- geoPoints) {
        sb.append( MGeoPoint.toEsStr(gp) )
      }
      sb.append('}')
    }

    for (extService <- this.extService) {
      sb.append(",extSvc=")
        .append(extService)
    }

    for (osFamily <- this.osFamily) {
      sb.append(",os=")
        .append(osFamily)
    }

    sb.toString()
  }

}

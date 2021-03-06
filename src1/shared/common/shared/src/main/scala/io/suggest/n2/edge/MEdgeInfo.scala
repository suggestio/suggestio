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
import io.suggest.n2.edge.payout.MEdgePayOut
import io.suggest.pay.MPaySystem
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.ueq.UnivEqUtil._

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
    val DATE_NO_INDEX_FN        = "dateNoInx"
    val TEXT_NO_INDEX_FN     = "comment"
    val FLAG_FN           = "flag"
    val FLAGS_FN          = "edgeFlags"
    val GEO_SHAPES_FN     = "geoShapes"
    val TAGS_FN           = "tags"
    val GEO_POINT_FN      = "geoPoints"
    val EXT_SERVICE_FN    = "externalService"
    val OS_FAMILY_FN      = "osFamily"
    val PAY_SYSTEM_FN     = "paySystem"
    val PAYOUT_FN         = "payout"

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


    /** Field names for edge's payOut sub-model. */
    object PayOut extends PrefixedFn {
      import MEdgePayOut.{Fields => F}

      override protected def _PARENT_FN = PAYOUT_FN
      def TYPE_FN = _fullFn( F.TYPE )
    }

  }


  /** Поддержка JSON. */
  implicit val mEdgeInfoFormat: Format[MEdgeInfo] = {
    val F = Fields
    (
      (__ \ F.DATE_FN).formatNullable[OffsetDateTime] and
      (__ \ F.DATE_NO_INDEX_FN).formatNullable[OffsetDateTime] and
      (__ \ F.TEXT_NO_INDEX_FN).formatNullable[String] and
      (__ \ F.FLAG_FN).formatNullable[Boolean] and
      (__ \ F.FLAGS_FN).formatNullable[Iterable[MEdgeFlagData]]
        .inmap[Iterable[MEdgeFlagData]](
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { flags => if (flags.isEmpty) None else Some(flags) }
        ) and
      (__ \ F.TAGS_FN).formatNullable[Set[String]]
        .inmap [Set[String]] (
          EmptyUtil.opt2ImplEmptyF( Set.empty ),
          { tags => if (tags.nonEmpty) Some(tags) else None }
        ) and
      (__ \ F.GEO_SHAPES_FN).formatNullable[List[MEdgeGeoShape]]
        .inmap [List[MEdgeGeoShape]] (
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { geos => if (geos.nonEmpty) Some(geos) else None }
        ) and
      (__ \ F.GEO_POINT_FN).formatNullable[Seq[MGeoPoint]]
        .inmap [Seq[MGeoPoint]] (
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { gps => if (gps.nonEmpty) Some(gps) else None }
        ) and
      (__ \ F.EXT_SERVICE_FN).formatNullable[MExtService] and
      (__ \ F.OS_FAMILY_FN).formatNullable[MOsFamily] and
      (__ \ F.PAY_SYSTEM_FN).formatNullable[MPaySystem] and
      (__ \ F.PAYOUT_FN).formatNullable[MEdgePayOut]
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
      F.DATE_NO_INDEX_FN -> FDate.notIndexedJs,
      F.TEXT_NO_INDEX_FN -> FText.notIndexedJs,
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
      F.PAY_SYSTEM_FN -> FKeyWord.indexedJs,
      F.PAYOUT_FN -> FObject.plain( MEdgePayOut ),
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
  def paySystem = GenLens[MEdgeInfo](_.paySystem)
  def payOut = GenLens[MEdgeInfo](_.payOut)

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
  * @param paySystem Payment system, if any.
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
                            paySystem    : Option[MPaySystem]      = None,
                            payOut       : Option[MEdgePayOut]     = None,
                          )
  extends EmptyProduct
{

  /** Карта флагов, если вдруг нужна будет. */
  lazy val flagsMap = flags
    .zipWithIdIter[MEdgeFlag]
    .to( Map )

  /** Форматирование для вывода в шаблонах. */
  override def toString: String = StringUtil.toStringHelper(null, delimiter = ' ') {
    StringUtil.toStringRenderProduct(this)
  }

}

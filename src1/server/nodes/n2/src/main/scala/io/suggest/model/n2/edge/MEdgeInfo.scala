package io.suggest.model.n2.edge

import java.time.OffsetDateTime

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.geo.{GeoPoint, MGeoPoint, MNodeGeoLevel}
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.img.MImgFmts
import io.suggest.model.PrefixedFn
import io.suggest.util.SioConstants
import play.api.libs.functional.syntax._
import play.api.libs.json._

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
  override val empty = MEdgeInfo()


  /** Модель названий полей модели [[MEdgeInfo]]. */
  object Fields {

    val DYN_IMG_ARGS_FN   = "im"
    val DATE_NI_FN        = "dtni"
    val COMMENT_NI_FN     = "coni"
    val FLAG_FN           = "flag"
    val GEO_SHAPES_FN     = "gss"
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
      def GS_SHAPE_FN(ngl: MNodeGeoLevel) = _fullFn( Fs.SHAPE_FN(ngl) )

    }

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MEdgeInfo] = {
    val dynImgArgsFormat = {
      // В норме (после resaveMany() должно быть просто: (__ \ DYN_IMG_ARGS_FN).formatNullable[MEdgeDynImgArgs]
      // Но тут поддержка приемственности старого формата (кропа), поэтому отрабатываются сразу два варианта: новый объект и старая строка.
      // TODO Удалить поддержку старого формата.
      val fmt0 = (__ \ DYN_IMG_ARGS_FN).formatNullable[MEdgeDynImgArgs]

      val readsOrOldDynOps = fmt0
        .filter(_.nonEmpty)
        .orElse {
           for {
             dynOpsStrOpt <- (__ \ "di").readNullable[String]
           } yield {
             for (_ <- dynOpsStrOpt) yield {
               MEdgeDynImgArgs(
                 dynFormat = MImgFmts.JPEG,
                 dynOpsStr = dynOpsStrOpt
               )
             }
           }
        }

      OFormat(readsOrOldDynOps, fmt0)
    }

    (
      dynImgArgsFormat and
      (__ \ DATE_NI_FN).formatNullable[OffsetDateTime] and
      (__ \ COMMENT_NI_FN).formatNullable[String] and
      (__ \ FLAG_FN).formatNullable[Boolean] and
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
      (__ \ GEO_POINT_FN).formatNullable[ Seq[MGeoPoint] ]
        .inmap [Seq[MGeoPoint]] (
        _.getOrElse(Nil),
        { gps => if (gps.nonEmpty) Some(gps) else None }
      )
    )(apply, unlift(unapply))
  }




  import io.suggest.es.util.SioEsUtil._

  /** Сборка полей ES-маппинга. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(
        id              = DYN_IMG_ARGS_FN,
        enabled         = true,
        properties      = MEdgeDynImgArgs.generateMappingProps
      ),
      FieldDate(
        id              = DATE_NI_FN,
        index           = false,
        include_in_all  = false
      ),
      FieldText(
        id              = COMMENT_NI_FN,
        index           = false,
        include_in_all  = false
      ),
      FieldBoolean(
        id              = FLAG_FN,
        index           = true,
        include_in_all  = false
      ),
      // 2016.mar.24 Теперь теги живут внутри эджей.
      FieldText(
        id              = TAGS_FN,
        index           = true,
        include_in_all  = true,
        analyzer        = SioConstants.ENGRAM_AN_1,
        search_analyzer = SioConstants.DFLT_AN,
        fields = Seq(
          // Для аггрегации нужны ненормированные термы. Они позволят получать необрезанные слова.
          FieldText(
            id              = Tags.RAW_FN,
            include_in_all  = false,
            index           = true,
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
      FieldGeoPoint( GEO_POINT_FN )
    )
  }

}



/** Класс экземпляров модели MEdgeInfo.
  *
  * @param dynImgArgs При указании на картинку бывает нужно указать исходный кроп или что-то ещё.
  * @param dateNi Неиднексируемая дата.
  * @param commentNi Неиндексируемый комментарий.
  * @param flag Индексируемое значение некоторого флага.
  * @param tags Названия тегов, которые индексируются для полноценного поиска по тегам.
  * @param geoShapes Список геошейпов, которые связаны с данным эджем.
  * Изначально было Seq, но из-за частой пошаговой пересборки этого лучше подходит List.
  * @param geoPoints Некие опорные геокоординаты, если есть.
  */
final case class MEdgeInfo(
                            dynImgArgs   : Option[MEdgeDynImgArgs] = None,
                            dateNi       : Option[OffsetDateTime]  = None,
                            commentNi    : Option[String]          = None,
                            flag         : Option[Boolean]         = None,
                            tags         : Set[String]             = Set.empty,
                            geoShapes    : List[MEdgeGeoShape]     = Nil,
                            geoPoints    : Seq[MGeoPoint]          = Nil
                          )
  extends EmptyProduct
{

  def withDynImgArgs(dynImgArgs: Option[MEdgeDynImgArgs]) = copy(dynImgArgs = dynImgArgs)

  /** Форматирование для вывода в шаблонах. */
  override def toString: String = {
    val sb = new StringBuilder(32)

    for (dia <- dynImgArgs) {
      sb.append(dia)
        .append(' ')
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

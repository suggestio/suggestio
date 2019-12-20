package io.suggest.model.n2.edge

import java.time.OffsetDateTime

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.geo.{IGeoShape, MNodeGeoLevel, MNodeGeoLevels}
import io.suggest.geo.IGeoShape.JsonFormats.allStoragesEsFormat
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

import scala.util.Random

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.16 15:07
  * Description: Модель геошейпа, хранимого внутри эджа.
  * Модель пришла в ходе унификации геотегов и mnode.geo.shapes.
  *
  * ngl (геоуровень, масштаб (индексации)) -- определяет название поля на стороне ES.
  * Используется для облегчения индексации больших шейпов.
  */
object MEdgeGeoShape
  extends IEsMappingProps
{

  /** Названия полей модели на стороне ElasticSearch. */
  object Fields {

    val GLEVEL_FN                   = "l"
    val GJSON_COMPAT_FN             = "gjc"
    val FROM_URL_FN                 = "url"
    val DATE_EDITED_FN              = "dt"
    val ID_FN                       = "id"

    /** Название поля на стороне ES для самого шейпа в каком-то масштабе индексации. */
    def SHAPE_FN(ngl: MNodeGeoLevel): String = {
      ngl.esfn
    }

  }


  /** Нетривиальная поддержка JSON.
    * Т.к. шейп может быть сохранен в разных масштабах, то требуется соотв.поддержка в маппере. */
  implicit val MEDGE_GEO_SHAPE_FORMAT: Format[MEdgeGeoShape] = {
    // Всё разом закинуто внутрь метода, чтобы GC мог вычистить ненужное из кучи Format'ов.

    import Fields._

    val GLEVEL_FORMAT       = (__ \ GLEVEL_FN).format[MNodeGeoLevel]
    val GJC_FORMAT          = (__ \ GJSON_COMPAT_FN).format[Boolean]
    val FROM_URL_FORMAT     = (__ \ FROM_URL_FN).formatNullable[String]
    val DATE_EDITED_FORMAT  = (__ \ DATE_EDITED_FN).formatNullable[OffsetDateTime]
    val ID_FORMAT           = (__ \ ID_FN).formatNullable[Int]
      // TODO Возникла проблема во время запиливания, id стал опционален, потом снова обязательным, это вызвало проблемы.
      // Удалять Nullable можно сразу после обновления мастера (с апреля 2016).
      .inmap [Int] (
        EmptyUtil.opt2ImplEmpty1F( Math.abs( new Random().nextInt ) ),
        EmptyUtil.someF
      )

    def _shapeFormat(ngl: MNodeGeoLevel): OFormat[IGeoShape] = {
      (__ \ SHAPE_FN(ngl)).format[IGeoShape]
    }

    // Десериализация.
    val READS = Reads[MEdgeGeoShape] {
      case jo: JsObject =>
        for {
          glevel      <- jo.validate( GLEVEL_FORMAT )
          gshape      <- jo.validate( _shapeFormat(glevel) )
          fromUrl     <- jo.validate( FROM_URL_FORMAT )
          dateEdited  <- jo.validate( DATE_EDITED_FORMAT )
          id          <- jo.validate( ID_FORMAT )
        } yield {
          MEdgeGeoShape(
            glevel      = glevel,
            shape       = gshape,
            fromUrl     = fromUrl,
            dateEdited  = dateEdited,
            id          = id
          )
        }

      case other =>
        JsError( JsonValidationError("expected.jsobject", other) )
    }

    // Сериализация
    val WRITES = Writes[MEdgeGeoShape] { mgs =>
      // Собираем промежуточный JSON-врайтер.
      val write1: Writes[MEdgeGeoShape] = (
        (__ \ SHAPE_FN(mgs.glevel)).write[IGeoShape] and
          GLEVEL_FORMAT and
          GJC_FORMAT and
          FROM_URL_FORMAT and
          DATE_EDITED_FORMAT and
          ID_FORMAT
        ) { mgs =>
          (mgs.shape,
            mgs.glevel,
            mgs.shape.shapeType.isGeoJsonCompatible,
            mgs.fromUrl,
            mgs.dateEdited,
            mgs.id )
        }
      // Запускаем сериализацию.
      write1.writes(mgs)
    }

    Format[MEdgeGeoShape](READS, WRITES)
  }


  /** Предложить новый id. */
  def nextShapeId(shapes: IterableOnce[MEdgeGeoShape]): Int = {
    if (shapes.isEmpty) {
      SHAPE_ID_START
    } else {
      shapes
        .iterator
        .map(_.id)
        .max
    }
  }

  def SHAPE_ID_START = 1

  @inline implicit def univEq: UnivEq[MEdgeGeoShape] = UnivEq.derive

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    val nglJson = MNodeGeoLevels
      .values
      .iterator
      .map { ngl =>
        Json.obj(
          F.SHAPE_FN(ngl) -> FGeoShape(
            precision = Some(ngl.precision),
          )
        )
      }
      .reduce(_ ++ _)

    val fields2 = Json.obj(
      F.GLEVEL_FN       -> FKeyWord( index = someTrue, store = someTrue ),
      F.GJSON_COMPAT_FN -> FBoolean.indexedJs,
      F.FROM_URL_FN     -> FText.notIndexedJs,
      F.DATE_EDITED_FN  -> FDate.notIndexedJs,
      F.ID_FN           -> FNumber(
        typ = DocFieldTypes.Integer,
        index = someFalse,
      ),
    )

    fields2 ++ nglJson
  }

}


/**
  * Экземпляр модели гео-шейпов в рамках эджа.
  *
  * @param id Уникальный номер этого внутри списка шейпов.
  *           Попытка сделать его опциональным была ошибочной -- он нужен, и точка!
  * @param glevel Гео-уровень шейпа (масштаб).
  * @param shape Шейп.
  * @param fromUrl URL-источкик, если есть.
  * @param dateEdited Дата редактирования, если требуется.
  */
case class MEdgeGeoShape(
                          id            : Int,
                          glevel        : MNodeGeoLevel,
                          shape         : IGeoShape,
                          fromUrl       : Option[String]          = None,
                          dateEdited    : Option[OffsetDateTime]  = None
                        )

package io.suggest.model.n2.edge

import io.suggest.common.empty.EmptyUtil
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.geo.GeoShape
import io.suggest.model.es.EsModelUtil.Implicits.jodaDateTimeFormat
import io.suggest.ym.model.{NodeGeoLevels, NodeGeoLevel}
import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
object MEdgeGeoShape extends IGenEsMappingProps {

  /** Названия полей модели на стороне ElasticSearch. */
  object Fields {

    val GLEVEL_FN                   = "l"
    val GJSON_COMPAT_FN             = "gjc"
    val FROM_URL_FN                 = "url"
    val DATE_EDITED_FN              = "dt"
    val ID_FN                       = "id"

    /** Название поля на стороне ES для самого шейпа в каком-то масштабе индексации. */
    def SHAPE_FN(ngl: NodeGeoLevel)  = ngl.esfn

  }


  /** Нетривиальная поддержка JSON.
    * Т.к. шейп может быть сохранен в разных масштабах, то требуется соотв.поддержка в маппере. */
  implicit val FORMAT: Format[MEdgeGeoShape] = {
    // Всё разом закинуто внутрь метода, чтобы GC мог вычистить ненужное из кучи Format'ов.

    import Fields._

    val GLEVEL_FORMAT       = (__ \ GLEVEL_FN).format[NodeGeoLevel]
    val GJC_FORMAT          = (__ \ GJSON_COMPAT_FN).format[Boolean]
    val FROM_URL_FORMAT     = (__ \ FROM_URL_FN).formatNullable[String]
    val DATE_EDITED_FORMAT  = (__ \ DATE_EDITED_FN).formatNullable[DateTime]
    val ID_FORMAT           = (__ \ ID_FN).formatNullable[Int]
      // TODO Возникла проблема во время запиливания, id стал опционален, потом снова обязательным, это вызвало проблемы.
      // Удалять Nullable можно сразу после обновления мастера (с апреля 2016).
      .inmap [Int] (
        EmptyUtil.opt2ImplEmpty1F( Math.abs( new Random().nextInt ) ),
        EmptyUtil.someF
      )

    def _shapeFormat(ngl: NodeGeoLevel): OFormat[GeoShape] = {
      (__ \ SHAPE_FN(ngl)).format[GeoShape]
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
        JsError( ValidationError("expected.jsobject", other) )
    }

    // Сериализация
    val WRITES = Writes[MEdgeGeoShape] { mgs =>
      // Собираем промежуточный JSON-врайтер.
      val write1: Writes[MEdgeGeoShape] = (
        (__ \ SHAPE_FN(mgs.glevel)).write[GeoShape] and
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


  import io.suggest.util.SioEsUtil._

  /** ES-маппинги полей. */
  override def generateMappingProps: List[DocField] = {
    val nglFields = NodeGeoLevels.values
      .foldLeft( List.empty[DocField] ) {
        (acc, nglv)  =>
          val ngl: NodeGeoLevel = nglv
          FieldGeoShape( Fields.SHAPE_FN(ngl), precision = ngl.precision)  ::  acc
      }
    FieldString(Fields.GLEVEL_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = true) ::
      FieldBoolean(Fields.GJSON_COMPAT_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = false) ::
      FieldString(Fields.FROM_URL_FN, index = FieldIndexingVariants.no, include_in_all = false) ::
      FieldDate(Fields.DATE_EDITED_FN, index = FieldIndexingVariants.no, include_in_all = false) ::
      FieldNumber(Fields.ID_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.no, include_in_all = false) ::
      nglFields
  }


  /** Предложить новый id. */
  def nextShapeId(shapes: TraversableOnce[MEdgeGeoShape]): Int = {
    if (shapes.isEmpty) {
      SHAPE_ID_START
    } else {
      shapes
        .toIterator
        .map(_.id)
        .max
    }
  }

  def SHAPE_ID_START = 1

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
  glevel        : NodeGeoLevel,
  shape         : GeoShape,
  fromUrl       : Option[String]    = None,
  dateEdited    : Option[DateTime]  = None
)

package io.suggest.model.n2.edge

import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.geo.GeoShape
import io.suggest.model.es.EsModelUtil.Implicits.jodaDateTimeFormat
import io.suggest.ym.model.{NodeGeoLevels, NodeGeoLevel}
import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    val GEO_JSON_COMPATIBLE_FN      = "gjc"
    val FROM_URL_FN                 = "url"
    val DATE_EDITED_FN              = "dt"
    val ID_FN                       = "id"

    /** Название поля на стороне ES для самого шейпа в каком-то масштабе индексации. */
    def shapeFn(ngl: NodeGeoLevel)  = ngl.esfn

  }


  /** Нетривиальная поддержка JSON.
    * Т.к. шейп может быть сохранен в разных масштабах, то требуется соотв.поддержка в маппере. */
  implicit val FORMAT: Format[MEdgeGeoShape] = {
    // Всё разом закинуто внутрь метода, чтобы GC мог вычистить ненужное из кучи Format'ов.

    import Fields._

    val GLEVEL_FORMAT       = (__ \ GLEVEL_FN).format[NodeGeoLevel]
    val GJC_FORMAT          = (__ \ GEO_JSON_COMPATIBLE_FN).format[Boolean]
    val FROM_URL_FORMAT     = (__ \ FROM_URL_FN).formatNullable[String]
    val DATE_EDITED_FORMAT  = (__ \ DATE_EDITED_FN).formatNullable[DateTime]
    val ID_FORMAT           = (__ \ ID_FN).formatNullable[Int]

    def _shapeFormat(ngl: NodeGeoLevel): OFormat[GeoShape] = {
      (__ \ shapeFn(ngl)).format[GeoShape]
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
        (__ \ shapeFn(mgs.glevel)).write[GeoShape] and
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
          FieldGeoShape( Fields.shapeFn(ngl), precision = ngl.precision)  ::  acc
      }
    FieldString(Fields.GLEVEL_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = true) ::
      FieldBoolean(Fields.GEO_JSON_COMPATIBLE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = false) ::
      FieldString(Fields.FROM_URL_FN, index = FieldIndexingVariants.no, include_in_all = false) ::
      FieldDate(Fields.DATE_EDITED_FN, index = FieldIndexingVariants.no, include_in_all = false) ::
      FieldNumber(Fields.ID_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.no, include_in_all = false) ::
      nglFields
  }

}


/**
  * Экземпляр модели гео-шейпов в рамках эджа.
  * @param glevel Гео-уровень шейпа (масштаб).
  * @param shape Шейп.
  * @param fromUrl URL-источкик, если есть.
  * @param dateEdited Дата редактирования, если требуется.
  */
case class MEdgeGeoShape(
  glevel        : NodeGeoLevel,
  shape         : GeoShape,
  fromUrl       : Option[String]    = None,
  dateEdited    : Option[DateTime]  = None,
  id            : Option[Int]       = None
)

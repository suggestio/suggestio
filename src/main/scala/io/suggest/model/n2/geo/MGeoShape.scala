package io.suggest.model.n2.geo

import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.geo.GeoShape
import io.suggest.ym.model.{NodeGeoLevels, NodeGeoLevel}
import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.model.es.EsModelUtil.Implicits.jodaDateTimeFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 14:55
 * Description: Модель одного геошейпа, встроенного в узел.
 *
 * Эта модель несёт в себе основные данные из MAdnNodeGeo:
 * шейп, уровень, флаг совместимости с GeoJSON.
 */
object MGeoShape extends IGenEsMappingProps {

  object Fields {
    val ID_FN                       = "id"
    val GLEVEL_FN                   = "l"
    val GEO_JSON_COMPATIBLE_FN      = "gjc"
    val FROM_URL_FN                 = "url"
    val DATE_EDITED_FN              = "dt"
    def shapeFn(ngl: NodeGeoLevel)  = ngl.esfn
  }


  val ID_FORMAT           = (__ \ Fields.ID_FN).format[Int]
  val GLEVEL_FORMAT       = (__ \ Fields.GLEVEL_FN).format[NodeGeoLevel]
  val GJC_FORMAT          = (__ \ Fields.GEO_JSON_COMPATIBLE_FN).format[Boolean]
  val FROM_URL_FORMAT     = (__ \ Fields.FROM_URL_FN).formatNullable[String]
  val DATE_EDITED_FORMAT  = (__ \ Fields.DATE_EDITED_FN).format[DateTime]

  private def _shapeFormat(ngl: NodeGeoLevel): OFormat[GeoShape] = {
    (__ \ ngl.esfn).format[GeoShape]
  }


  /** Десериализация. */
  val READS = Reads[MGeoShape] {
    case jo: JsObject =>
      for {
        id          <- jo.validate( ID_FORMAT )
        glevel      <- jo.validate( GLEVEL_FORMAT )
        gshape      <- jo.validate( _shapeFormat(glevel) )
        fromUrl     <- jo.validate( FROM_URL_FORMAT )
        dateEdited  <- jo.validate( DATE_EDITED_FORMAT )
      } yield {
        MGeoShape(
          id          = id,
          glevel      = glevel,
          shape       = gshape,
          fromUrl     = fromUrl,
          dateEdited  = dateEdited
        )
      }

    case other =>
      JsError( ValidationError("expected.jsobject", other) )
  }

  /** Сериализация. */
  val WRITES = Writes[MGeoShape] { mgs =>
    // Собираем промежуточный JSON-врайтер.
    val write1: Writes[MGeoShape] = (
      (__ \ mgs.glevel.esfn).write[GeoShape] and
      GLEVEL_FORMAT and
      GJC_FORMAT and
      FROM_URL_FORMAT
    ) { mgs =>
      (mgs.shape,
       mgs.glevel,
       mgs.shape.shapeType.isGeoJsonCompatible,
       mgs.fromUrl
      )
    }
    // Запускаем сериализацию.
    write1.writes(mgs)
  }

  /** Полновесный JSON FORMAT. */
  implicit val FORMAT = Format[MGeoShape](READS, WRITES)


  import io.suggest.util.SioEsUtil._

  /** ES-маппинги полей. */
  override def generateMappingProps: List[DocField] = {
    val nglFields = NodeGeoLevels.values
      .foldLeft( List.empty[DocField] ) {
        (acc, nglv)  =>
          val ngl: NodeGeoLevel = nglv
          FieldGeoShape(ngl.esfn, precision = ngl.precision)  ::  acc
      }
    FieldNumber(Fields.ID_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.no, include_in_all = false) ::
      FieldString(Fields.GLEVEL_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = true) ::
      FieldBoolean(Fields.GEO_JSON_COMPATIBLE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = false) ::
      FieldString(Fields.FROM_URL_FN, index = FieldIndexingVariants.no, include_in_all = false) ::
      FieldDate(Fields.DATE_EDITED_FN, index = FieldIndexingVariants.no, include_in_all = false) ::
      nglFields
  }

}


/**
 * Класс для экземпляров модели.
 * @param id численный id для различения элементов внутри списка.
 * @param glevel Гео-уровень.
 * @param shape Шейп для данного уровня.
 * @param fromUrl URL исходника, если есть.
 * @param dateEdited Дата последнего изменения, либо дата создания.
 */
case class MGeoShape(
  id            : Int,
  glevel        : NodeGeoLevel,
  shape         : GeoShape,
  fromUrl       : Option[String],
  dateEdited    : DateTime        = DateTime.now()
)

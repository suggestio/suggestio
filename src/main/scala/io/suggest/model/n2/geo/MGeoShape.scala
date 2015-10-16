package io.suggest.model.n2.geo

import io.suggest.model.IGenEsMappingProps
import io.suggest.model.geo.GeoShape
import io.suggest.ym.model.{NodeGeoLevels, NodeGeoLevel}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    val GLEVEL_FN                 = "l"
    val GEO_JSON_COMPATIBLE_FN    = "gjc"
  }

  val GLEVEL_FORMAT = (__ \ Fields.GLEVEL_FN).format[NodeGeoLevel]
  val GJC_FORMAT = (__ \ Fields.GEO_JSON_COMPATIBLE_FN).format[Boolean]

  private def _shapeFormat(ngl: NodeGeoLevel): OFormat[GeoShape] = {
    (__ \ ngl.esfn).format[GeoShape]
  }

  /** Десериализация. */
  val READS = Reads[MGeoShape] {
    case jo: JsObject =>
      for {
        glevel <- jo.validate( GLEVEL_FORMAT )
        gshape <- jo.validate( _shapeFormat(glevel) )
      } yield {
        MGeoShape(glevel, gshape)
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
      GJC_FORMAT
    ) { mgs =>
      (mgs.shape, mgs.glevel, mgs.shape.shapeType.isGeoJsonCompatible)
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
    FieldString(Fields.GLEVEL_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = true) ::
      FieldBoolean(Fields.GEO_JSON_COMPATIBLE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = false) ::
      nglFields
  }

}


case class MGeoShape(
  glevel    : NodeGeoLevel,
  shape     : GeoShape
)

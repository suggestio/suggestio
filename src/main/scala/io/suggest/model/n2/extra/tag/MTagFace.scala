package io.suggest.model.n2.extra.tag

import io.suggest.model.es.IGenEsMappingProps
import io.suggest.primo.IName
import io.suggest.util.SioConstants
import io.suggest.util.SioEsUtil._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 16:26
 * Description: M Tag Face -- лицо тега, с которым "взаимодействуют" пользователи.
 */
object MTagFace extends IGenEsMappingProps {

  /** Имя поля с именем. Индексируется для FTS-поиска. */
  val NAME_FN    = "n"

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(
        id              = NAME_FN,
        index           = FieldIndexingVariants.analyzed,
        include_in_all  = true,
        index_analyzer  = SioConstants.ENGRAM_AN_1,
        search_analyzer = SioConstants.DFLT_AN
      )
    )
  }

  implicit val FORMAT: OFormat[MTagFace] = {
    (__ \ NAME_FN).format[String]
      .inmap [MTagFace] (apply, _.name)
  }

  implicit val FACES_MAP_FORMAT: Format[TagFacesMap] = {
    val facesMapReads: Reads[TagFacesMap] = {
      __.read[Iterable[MTagFace]]
        .map { faces =>
          faces.iterator
            .map { face => face.name -> face }
            .toMap
        }
    }

    val facesMapWrites: Writes[TagFacesMap] = {
      // TODO Тут костыль, избегающий contramap(), Writes[Seq[]], OWrites[]. См.ошибку в http://stackoverflow.com/a/27481370
      // Когда будет больше одного аргумента, можно будет без этих извращений обойтись.
      Writes[TagFacesMap] { tfmap =>
        val writer = implicitly[ Writes[MTagFace] ]
        val seq1 = tfmap
          .valuesIterator
          .map { writer.writes }
          .toSeq
        JsArray(seq1)
      }
    }

    Format(facesMapReads, facesMapWrites)
  }



  def faces2map(faces: TraversableOnce[MTagFace]): TagFacesMap = {
    faces.toIterator
      .map { face => face.name -> face }
      .toMap
  }

}


/** Интерфейс tag-face модели. */
trait ITagFace
  extends IName


/** Дефолтовая реализация [[ITagFace]] модели. */
case class MTagFace(
  name: String
)
  extends ITagFace

package io.suggest.model.n2.tag.vertex

import io.suggest.model.IGenEsMappingProps
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 16:26
 * Description: M Tag Face -- лицо тега, с которым "взаимодействуют" пользователи.
 */
object MTagFace extends IGenEsMappingProps {

  val RAW_FN = "r"

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(RAW_FN, index = FieldIndexingVariants.analyzed, include_in_all = false)
    )
  }

  implicit val READS: Reads[MTagFace] = {
    (__ \ RAW_FN).read[String]
      .map { MTagFace.apply }
  }

  implicit val WRITES: Writes[MTagFace] = {
    (__ \ RAW_FN).write[String]
      .contramap(_.raw)
  }

  implicit val facesMapReads: Reads[TagFacesMap] = {
    __.read[Seq[MTagFace]]
      .map { faces =>
        faces.iterator
          .map { face => face.raw -> face }
          .toMap
      }
  }

  implicit val facesMapWrites: Writes[TagFacesMap] = {
    __.write[Seq[MTagFace]]
      .contramap {
        _.valuesIterator.toSeq
      }
  }

}


case class MTagFace(
  raw: String
)

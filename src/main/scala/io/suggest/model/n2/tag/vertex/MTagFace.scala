package io.suggest.model.n2.tag.vertex

import io.suggest.model.{PrefixedFn, IGenEsMappingProps}
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 16:26
 * Description: M Tag Face -- лицо тега, с которым "взаимодействуют" пользователи.
 */
object MTagFace extends IGenEsMappingProps with PrefixedFn {

  override protected def _PARENT_FN = MTagVertex.FACES_ESFN

  val NAME_FN    = "r"
  def NAME_ESFN  = _fullFn(NAME_FN)

  override def generateMappingProps: List[DocField] = {
    List(
      // TODO Нужно анализировать по ngram и fts_nostop. Сортировка по этому полю не требуется.
      FieldString(NAME_FN, index = FieldIndexingVariants.analyzed, include_in_all = true)
    )
  }

  implicit val READS: Reads[MTagFace] = {
    (__ \ NAME_FN).read[String]
      .map { MTagFace.apply }
  }

  implicit val WRITES: Writes[MTagFace] = {
    (__ \ NAME_FN).write[String]
      .contramap(_.name)
  }

  implicit val facesMapReads: Reads[TagFacesMap] = {
    __.read[Seq[MTagFace]]
      .map { faces =>
        faces.iterator
          .map { face => face.name -> face }
          .toMap
      }
  }

  implicit val facesMapWrites: Writes[TagFacesMap] = {
    __.write[Seq[MTagFace]]
      .contramap {
        _.valuesIterator.toSeq
      }
  }


  def faces2map(faces: TraversableOnce[MTagFace]): TagFacesMap = {
    faces.toIterator
      .map { face => face.name -> face }
      .toMap
  }

}


case class MTagFace(
  name: String
)

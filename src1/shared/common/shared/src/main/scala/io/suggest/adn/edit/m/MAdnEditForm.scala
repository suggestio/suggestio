package io.suggest.adn.edit.m

import io.suggest.jd.{MJdEdge, MJdEdgeVldInfo}
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.model.n2.node.meta.MMetaPub
import io.suggest.scalaz.StringValidationNel
import japgolly.univeq._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.Validation
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 16:36
  * Description: Модель данных узла.
  */
object MAdnEditForm {

  @inline implicit def univEq: UnivEq[MAdnEditForm] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  /** Поддержка play-json. */
  implicit def mAdnEditFormFormat: OFormat[MAdnEditForm] = (
    (__ \ "m").format[MMetaPub] and
    (__ \ "r").format[MAdnResView] and
    (__ \ "e").format[Seq[MJdEdge]]
  )(apply, unlift(unapply))


  /** Выверение данных формы. */
  def validate(form: MAdnEditForm, vldEdgesMap: Map[EdgeUid_t, MJdEdgeVldInfo]): StringValidationNel[MAdnEditForm] = {
    (
      MMetaPub.validate( form.meta ) |@|
      MAdnResView.validate(form.resView, vldEdgesMap) |@|
      Validation.success( vldEdgesMap.valuesIterator.map(_.jdEdge).toSeq )
    )(apply _)
  }

}


/** Контейнер итоговых (и исходных) данных формы редактирования узла-ресивера.
  *
  * @param meta Текстовые метаданные узла.
  * @param edges Список jd-эджей.
  * @param resView Описание отображения картинок (или иных связанных ресурсов).
  */
case class MAdnEditForm(
                         meta     : MMetaPub,
                         resView  : MAdnResView,
                         edges    : Seq[MJdEdge]
                       ) {

  def usedEdgeUids = resView.edgeUids

  def usedEdgeUidsSet = usedEdgeUids.map(_.edgeUid).toSet

}

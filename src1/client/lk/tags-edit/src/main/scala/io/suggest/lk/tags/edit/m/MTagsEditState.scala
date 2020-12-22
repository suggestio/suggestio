package io.suggest.lk.tags.edit.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.common.tags.search.{MTagFound, MTagsFound}
import japgolly.univeq.UnivEq
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 13:54
  * Description: Несериализуемая client-only модель состояния редактора тегов.
  * Содержит всякие рантаймовые поля.
  */
object MTagsEditState {

  def empty = MTagsEditState()

  implicit object MTagsEditStateFastEq extends FastEq[MTagsEditState] {
    override def eqv(a: MTagsEditState, b: MTagsEditState): Boolean = {
      (a.props eq b.props) &&
        (a.found eq b.found) &&
        (a.searchTimer eq b.searchTimer)
    }
  }

  @inline implicit def univEq: UnivEq[MTagsEditState] = UnivEq.derive

  val props = GenLens[MTagsEditState]( _.props )
  val found = GenLens[MTagsEditState]( _.found )
  def searchTimer = GenLens[MTagsEditState]( _.searchTimer )


  implicit final class TagsEditStateExt( private val tes: MTagsEditState ) extends AnyVal {

    def reset: MTagsEditState =
      (MTagsEditState.props set tes.props)(MTagsEditState.empty)

  }

}

/**
  * Класс модели рантаймового состояния поиска тегов.
  *
  * @param found Состояние поиска тегов.
  * @param searchTimer Таймер запуска поиска, если есть.
  *                    Появился из-за https://github.com/ochrons/diode/issues/37 в том числе.
  */
case class MTagsEditState(
                           props        : MTagsEditProps      = MTagsEditProps(),
                           found        : Pot[MTagsFound]     = Pot.empty,
                           searchTimer  : Option[Long]        = None
                         ) {

  lazy val renderOptions: List[MTagFound] = {
    var tagsAcc = found
      .fold( List.empty[MTagFound] )(_.tags)

    // Если человек вводит название неизвестного тега, то отрендерить и его:
    val queryAsMtf = props.query.asTagFound
    val query = props.query.text
    if (query.nonEmpty && !tagsAcc.exists(_.face ==* query))
      tagsAcc ::= queryAsMtf

    tagsAcc
  }

}

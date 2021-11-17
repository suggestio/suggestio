package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.lk.m.color.MColorsState
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.09.2019 15:02
  * Description: Состояния различных под-редакторов.
  */
object MEditorsS {

  implicit object MEditorsSFastEq extends FastEq[MEditorsS] {
    override def eqv(a: MEditorsS, b: MEditorsS): Boolean = {
      (a.qdEdit ===* b.qdEdit) &&
      (a.stripEd ===* b.stripEd) &&
      (a.colorsState ===* b.colorsState) &&
      (a.slideBlocks ===* b.slideBlocks) &&
      (a.events ===* b.events)
    }
  }

  implicit class MEditorsExt( val doc: MEditorsS ) extends AnyVal {

    def withOutQdEdit: MEditorsS = {
      if (doc.qdEdit.isEmpty) doc
      else (qdEdit replace None)(doc)
    }

    def withOutStripEd: MEditorsS = {
      if (doc.stripEd.isEmpty) doc
      else (stripEd replace None)(doc)
    }

  }

  def qdEdit = GenLens[MEditorsS](_.qdEdit)
  def stripEd = GenLens[MEditorsS](_.stripEd)
  val slideBlocks = GenLens[MEditorsS](_.slideBlocks)
  def colorsState = GenLens[MEditorsS](_.colorsState)
  def events = GenLens[MEditorsS](_.events)

  @inline implicit def univEq: UnivEq[MEditorsS] = UnivEq.derive

}


/** @param qdEdit Состояние редактирования контента, если есть.
  * @param stripEd Состояние strip-редактора, если открыт.
  * @param colorsState Общее состояние редактирования цветов:
  *                    разные часто-используемые или подходящие цвета, например.
  * @param slideBlocks Состояние slide-блоков редактора.
  *                    Изначально было в layout, но оно очень активно управляется из DocEditAh.
  */
case class MEditorsS(
                      qdEdit        : Option[MQdEditS]              = None,
                      stripEd       : Option[MStripEdS]             = None,
                      slideBlocks   : MSlideBlocks                  = MSlideBlocks.empty,
                      colorsState   : MColorsState                  = MColorsState.empty,
                      events        : MEventsEdit                   = MEventsEdit.empty,
                    )

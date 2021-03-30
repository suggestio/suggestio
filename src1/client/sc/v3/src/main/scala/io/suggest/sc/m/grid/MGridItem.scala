package io.suggest.sc.m.grid

import io.suggest.common.html.HtmlConstants
import io.suggest.jd.MJdDoc
import io.suggest.primo.id.IId
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.03.2021 18:35
  * Description: Модель, описывающая представление элемента плитки:
  * id в базе элементов плитки, и какие-то дополнительные параметры отображения.
  */
object MGridItem {

  @inline implicit def univEq: UnivEq[MGridItem] = UnivEq.derive

  def gridKey = GenLens[MGridItem](_.gridKey)
  def jdDoc = GenLens[MGridItem](_.jdDoc)

}


/** Внутренний ключ карточки выдачи.
  *
  * @param gridKey Ключ рекламной карточки в плитке.
  * @param jdDoc Документ, используемый для рендера.
  */
final case class MGridItem(
                            gridKey       : GridAdKey_t,
                            jdDoc         : MJdDoc,
                          )
  extends IId[GridAdKey_t]
{

  override def id = gridKey

  /** Ключ для VDOM - обёртка поверх tagId для элемента плитки. */
  def tagIdWithGridKey: String = {
    (gridKey :: jdDoc.tagId.toStringItemsAcc)
      .mkString( HtmlConstants.MINUS )
  }

}

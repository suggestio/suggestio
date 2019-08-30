package io.suggest.sc.m.grid

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.id.OptStrId
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 19:07
  * Description: Модель рантаймовой инфы по одному блоку рекламной карточки в выдаче.
  * Модель собирается на базе созвучной MJdAdData, более простой и абстрактной по сравнению с этой.
  */
object MScAdData {

  /** Поддержка FastEq для инстансов [[MScAdData]]. */
  implicit object MScAdDataFastEq extends FastEq[MScAdData] {
    override def eqv(a: MScAdData, b: MScAdData): Boolean = {
      (a.nodeId ===* b.nodeId) &&
      (a.main ===* b.main) &&
      (a.focused ===* b.focused)
    }
  }

  @inline implicit def univEq: UnivEq[MScAdData] = UnivEq.derive

  val nodeId  = GenLens[MScAdData](_.nodeId)
  val main    = GenLens[MScAdData](_.main)
  val focused = GenLens[MScAdData](_.focused)

}


/** Класс-контейнер данных по одному блоку плитки.
  *
  * @param nodeId id карточки, которую надо раскрывать при клике.
  *               None, если не требуется.
  * @param main Рендеренный главный блок для плитки разных карточек.
  * @param focused Плитка одной открытой карточки.
  *                Приходит после открытия карточки, представленной main-блоком.
  */
case class MScAdData(
                      nodeId      : Option[String],
                      main        : MJdDataJs,
                      focused     : Pot[MScFocAdData] = Pot.empty
                    )
  extends OptStrId
{

  private def _flatGridTemplatesUsing(f: MJdDataJs => Seq[Tree[JdTag]]) = {
    focused.fold [Seq[Tree[JdTag]]] {
      main.template :: Nil
    }(foc => f(foc.blkData))
  }

  /** Вернуть последовательность шаблонов для "плоской" плитки, т.е. где и focused и не-focused одновременно.
    *
    * @return Список шаблонов на рендер.
    */
  def flatGridTemplates: Seq[Tree[JdTag]] = {
    _flatGridTemplatesUsing(_.template.subForest)
  }


  /** Вернуть карту эджей для плоской плитки.
    *
    * @return Карта эджей.
    */
  def flatGridEdges: Map[EdgeUid_t, MEdgeDataJs] = {
    focused
      .fold(main.edges)(_.blkData.edges)
  }

  override final def id: Option[String] = nodeId

}

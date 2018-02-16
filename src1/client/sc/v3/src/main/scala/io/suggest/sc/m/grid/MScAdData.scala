package io.suggest.sc.m.grid

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.MJdAdData
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

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

  implicit def univEq: UnivEq[MScAdData] = UnivEq.derive

  /** Сборка инстанса [[MScAdData]] из инстанса MJdAdData. */
  def apply( jdAdData: MJdAdData ): MScAdData = apply(
    nodeId    = jdAdData.nodeId,
    main      = MBlkRenderData(
      template  = jdAdData.template,
      edges     = jdAdData.edgesMap.mapValues(MEdgeDataJs(_))
    )
  )

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
                      main        : MBlkRenderData,
                      focused     : Pot[MBlkRenderData] = Pot.empty
                    ) {

  def withFocused(focused: Pot[MBlkRenderData]) = copy(focused = focused)

  private def _flatGridTemplatesUsing(f: MBlkRenderData => Seq[Tree[JdTag]]) = {
    focused.fold [Seq[Tree[JdTag]]] {
      main.template :: Nil
    }(f)
  }

  /** Вернуть последовательность шаблонов для "плоской" плитки, т.е. где и focused и не-focused одновременно.
    *
    * @return Список шаблонов на рендер.
    */
  def flatGridTemplates: Seq[Tree[JdTag]] = {
    _flatGridTemplatesUsing(_.template.subForest)
  }

  /** Вернуть последовательность шаблонов с приоритетом на indexed seq.
    *
    * @return List с одним элементом, либо IndexedSeq со списком item'ов.
    */
  def flatGridTemplatesIndexed: Seq[Tree[JdTag]] = {
    _flatGridTemplatesUsing(_.tplSubForestIndexed)
  }


  /** Вернтуь карту эджей для плоской плитки.
    *
    * @return Карта эджей.
    */
  def flatGridEdges: Map[EdgeUid_t, MEdgeDataJs] = {
    focused
      .fold(main.edges)(_.edges)
  }

}
package io.suggest.sc.grid.m

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.MJdAdData
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 19:07
  * Description: Модель рантаймовой инфы по одному блоку рекламной карточки в выдаче.
  * Модель собирается на базе созвучной MJdAdData, более простой и абстрактной по сравнению с этой.
  */
object MGridBlkData {

  /** Поддержка FastEq для инстансов [[MGridBlkData]]. */
  implicit object MGridBlkDataFastEq extends FastEq[MGridBlkData] {
    override def eqv(a: MGridBlkData, b: MGridBlkData): Boolean = {
      (a.nodeId ===* b.nodeId) &&
        (a.main ===* b.main) &&
        (a.focused ===* b.focused)
    }
  }

  implicit def univEq: UnivEq[MGridBlkData] = UnivEq.derive

  /** Сборка инстанса [[MGridBlkData]] из инстанса MJdAdData. */
  def apply( jdAdData: MJdAdData ): MGridBlkData = apply(
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
case class MGridBlkData(
                         nodeId      : Option[String],
                         main        : MBlkRenderData,
                         focused     : Pot[MBlkRenderData] = Pot.empty
                       )

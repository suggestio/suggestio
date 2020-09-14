package io.suggest.lk.nodes.form.m

import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.2020 22:00
  * Description: Модель-контейнер на моделью дерева и другими моделями, вспомогательными для основного дерева.
  */
object MTreeOuter {

  val tree = GenLens[MTreeOuter](_.tree)
  val beacons = GenLens[MTreeOuter](_.beacons)

  @inline implicit def univEq: UnivEq[MTreeOuter] = UnivEq.derive

}


/** Контейнер моделей дерева и того, что около.
  *
  * @param tree Само дерево и его внутренние кэши.
  * @param beacons Состояние сканирования маячков, отражаемое в дереве.
  */
case class MTreeOuter(
                       tree         : MTree,
                       beacons      : MBeaconScan      = MBeaconScan.empty,
                     )

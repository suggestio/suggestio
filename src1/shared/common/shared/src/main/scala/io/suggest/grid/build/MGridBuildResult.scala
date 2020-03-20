package io.suggest.grid.build

import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.MJdTagId
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.2020 13:00
  * Description: Результат сборки плитки.
  *
  * @param coords Координаты блоков.
  * @param gridWh Размеры собранной плитки.
  * @param nextRender Параметр одного следующего рендера.
  *                   Оно НЕ выставляется при каждом ребилде, но можно явно модифицировать через lens/copy()
  *                   после выполнения GridBuilder'а.
  */
final case class MGridBuildResult(
                                   coords       : List[MGbItemRes],
                                   gridWh       : MSize2di,
                                   nextRender   : MGridRenderInfo                 = MGridRenderInfo.default,
                                 ) {

  lazy val coordsById: Map[MJdTagId, MCoords2di] = {
    coords
      .iterator
      .map { gbItemRes =>
        gbItemRes.gbBlock.jdId -> gbItemRes.topLeft
      }
      .toMap
  }

}


object MGridBuildResult {

  def empty = apply(
    coords = Nil,
    gridWh = MSize2di(0, 0),
  )

  def nextRender = GenLens[MGridBuildResult](_.nextRender)

  @inline implicit def univEq: UnivEq[MGridBuildResult] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}



object MGridRenderInfo {

  // Обычно, нужен дефолтовый инстанс.
  lazy val default = apply()

  def animate = GenLens[MGridRenderInfo]( _.animate )
  def animFromBottom = GenLens[MGridRenderInfo]( _.animFromBottom )

  @inline implicit def univEq: UnivEq[MGridRenderInfo] = UnivEq.derive

}

/** Контейнер одноразовых данных, пробрасываемых в обход GridBuilder'а, из контроллера в grid view.
  * Задумано, что он должен сбрасываться и/или явно пересоздаваться после каждой пересборки плитки.
  *
  * @param animate Включить анимацию?
  * @param animFromBottom Новые карточки анимировать снизу, а не по умолчанию.
  */
final case class MGridRenderInfo(
                                  animate             : Boolean       = true,
                                  animFromBottom      : Boolean       = false,
                                )


/** Результат позиционирования одного блока.
  *
  * @param orderN Порядковый номер блока.
  * @param topLeft Отпозиционированные координаты.
  * @param forceCenterX Костыль для принудительной центровки по X вместо координат по сетке.
  */
final case class MGbItemRes(
                             orderN           : Int,
                             topLeft          : MCoords2di,
                             forceCenterX     : Option[Int]   = None,
                             gbBlock          : MGbBlock,
                             wide             : MWideLine,
                           )
object MGbItemRes {
  @inline implicit def univEq: UnivEq[MGbItemRes] = UnivEq.derive

  def topLeft = GenLens[MGbItemRes]( _.topLeft )
}

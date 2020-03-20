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
                                   coords       : List[(MJdTagId, MCoords2di)],
                                   gridWh       : MSize2di,
                                   nextRender   : MGridRenderInfo                 = MGridRenderInfo.default,
                                 ) {

  lazy val coordsById = coords.toMap

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

  @inline implicit def univEq: UnivEq[MGridRenderInfo] = UnivEq.derive

}
final case class MGridRenderInfo(
                                  animate      : Boolean       = true,
                                )

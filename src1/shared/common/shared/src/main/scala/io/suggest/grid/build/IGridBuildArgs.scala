package io.suggest.grid.build

import io.suggest.ad.blk.MBlockExpandMode
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MSzMult
import io.suggest.jd.MJdConf
import io.suggest.jd.tags.JdTag
import japgolly.univeq._
import monocle.macros.GenLens
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.17 14:11
  * Description: Модели для взаимодействия с grid-builder'ом.
  */


/** Модель аргументов для вызова [[GridBuilderUtil]].buildGrid().
  *
  * @param itemsExtDatas Данные по item'мам рассчитываемой плитки.
  * @param jdConf Конфигурация рендера.
  * @param offY Сдвиг по Y.
  * @param jdtWideSzMults Данные по доп.ресайзу wide-блоков.
  */
case class MGridBuildArgs(
                           itemsExtDatas : Stream[Tree[MGbBlock]],
                           jdConf        : MJdConf,
                           offY          : Int,
                           jdtWideSzMults: Map[JdTag, MSzMult],
                         )


/** Контейнер для одного блока.
  * wide-блоки могут передать здесь инфу о фоновой картинке.
  *
  * @param wideBgSz Размер широкой фоновой картинки.
  * @param orderN внутренний порядковый номер, заполняется и используется внутри [[GridBuilderUtil]].
  */
case class MGbBlock(
                     size                          : Option[MGbSize],
                     nodeId                        : Option[String],
                     jdtOpt                        : Option[JdTag],
                     wideBgSz                      : Option[MSize2di]  = None,
                     orderN                        : Option[Int]       = None,
                   )
object MGbBlock {

  @inline implicit def univEq: UnivEq[MGbBlock] = UnivEq.derive

  val orderN = GenLens[MGbBlock](_.orderN)

}


/** Модель описания размеров одного grid-block.
  * Изначально, этой моделью была block-meta, но там модель слишком специализирована,
  * не подходит для внеблокового контента.
  *
  * @param widthCells Ширина, в клетках.
  * @param heightPx Высота, в пикселях.
  * @param expandMode Режим развёртывания по горизонтали.
  */
case class MGbSize(
                    widthCells      : Int,
                    heightPx        : Int,
                    expandMode      : Option[MBlockExpandMode],
                  )
object MGbSize {
  @inline implicit def univEq: UnivEq[MGbSize] = UnivEq.derive
}


/** Результат сборки плитки.
  *
  * @param coords Координаты блоков.
  * @param gridWh Размеры собранной плитки.
  */
case class MGridBuildResult(
                             coords   : List[MCoords2di],
                             gridWh   : MSize2di
                           )

object MGridBuildResult {

  def empty = apply(
    coords = Nil,
    gridWh = MSize2di(0, 0)
  )

  @inline implicit def univEq: UnivEq[MGridBuildResult] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}

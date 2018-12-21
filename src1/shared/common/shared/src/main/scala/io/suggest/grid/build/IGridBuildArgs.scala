package io.suggest.grid.build

import io.suggest.ad.blk.BlockMeta
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.jd.MJdConf
import japgolly.univeq._

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
  */
case class MGridBuildArgs(
                           itemsExtDatas : List[IGbBlockPayload],
                           jdConf        : MJdConf,
                           offY          : Int
                         )


/** Интерфейс для контейнеров с вариантами элементов плитки. */
sealed trait IGbBlockPayload {
  def nodeId: Option[String]
  def isBlock: Boolean
  def isSubBlocks: Boolean = !isBlock
  def fold[T](blockF: MGbBlock => T, subBlocksF: MGbSubItems => T): T
  def flatten: List[IGbBlockPayload]

  def headOption: Option[IGbBlockPayload]
  def headOptionBlock: Option[MGbBlock]
  def headIsWide: Boolean = headOptionBlock.exists(_.bm.wide)

  /** Вернуть параметры первого блока. */
  def firstBlockMeta: BlockMeta = {
    fold(_.bm, _.subItems.head.firstBlockMeta)
  }

}

/** Контейнер для одного блока.
  * wide-блоки могут передать здесь инфу о фоновой картинке.
  *
  * @param bm Описание одного блока.
  * @param wideBgSz Размер широкой фоновой картинки.
  * @param orderN внутренний порядковый номер, заполняется и используется внутри [[GridBuilderUtil]].
  */
case class MGbBlock(
                     override val nodeId           : Option[String],
                     bm                            : BlockMeta,
                     wideBgSz                      : Option[ISize2di]  = None,
                     private[build] val orderN     : Option[Int]       = None,
                   )
  extends IGbBlockPayload {

  override def isBlock = true
  override def flatten = this :: Nil
  override def headOption = headOptionBlock
  override def headOptionBlock = Some(this)

  override def fold[T](blockF: MGbBlock => T, subBlocksF: MGbSubItems => T): T = {
    blockF(this)
  }

}

/** Контейнер для под-блоков. */
case class MGbSubItems(
                        override val nodeId       : Option[String],
                        subItems                  : List[IGbBlockPayload]
                      )
  extends IGbBlockPayload {


  override def headOptionBlock = headOption.flatMap(_.headOptionBlock)
  override def headOption = subItems.headOption
  override def isBlock = false
  override def flatten = subItems

  override def fold[T](blockF: MGbBlock => T, subBlocksF: MGbSubItems => T): T = {
    subBlocksF(this)
  }

}


/** Результат сборки плитки.
  *
  * @param coords Координаты блоков.
  * @param gridWh Размеры собранной плитки.
  */
case class MGridBuildResult(
                             coords   : Seq[MCoords2di],
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

package io.suggest.sc.m.grid

import diode.FastEq
import diode.data.Pot
import io.suggest.ad.blk.MBlockExpandMode
import io.suggest.common.empty.OptionUtil
import io.suggest.jd.{MJdDoc, MJdTagId}
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.jd.tags.{JdTag, MJdTagNames}
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
      (a.main ===* b.main) &&
      (a.focused ===* b.focused)
    }
  }

  @inline implicit def univEq: UnivEq[MScAdData] = UnivEq.derive

  val main    = GenLens[MScAdData](_.main)
  val focused = GenLens[MScAdData](_.focused)

  private def _mkJdIdBlkExpand(blkJdt: Tree[JdTag], jdId0: MJdTagId): Option[MBlockExpandMode] = {
    blkJdt.rootLabel
      .props1
      .expandMode
      .orElse( jdId0.blockExpand )
  }

  private val _jdId_blockExpand_LENS = {
    MJdDoc.jdId
      .composeLens( MJdTagId.blockExpand )
  }

}


/** Класс-контейнер данных по одному блоку плитки.
  *
  * @param main Рендеренный главный блок для плитки разных карточек.
  * @param focused Плитка одной открытой карточки.
  *                Приходит после открытия карточки, представленной main-блоком.
  */
final case class MScAdData(
                            main        : MJdDataJs,
                            focused     : Pot[MScFocAdData] = Pot.empty
                          )
  extends OptStrId
{

  /** Всегда раскрытая карточка в данной плитке. */
  def isAlwaysOpened: Boolean = {
    main.doc.template.rootLabel.name ==* MJdTagNames.DOCUMENT
  }

  // 2019-11-18 Может быть ситуация, что уже развёрнутая карточка прямо в плитке, и вместо одного блока тут целый DOCUMENT.
  def alwaysOpened: Option[MJdDataJs] =
    OptionUtil.maybe( isAlwaysOpened )(main)

  def focOrAlwaysOpened: Option[MJdDataJs] = {
    focused
      .toOption
      .map { foc =>
        foc.blkData
      }
      .orElse {
        alwaysOpened
      }
  }


  /** Вернуть последовательность шаблонов для "плоской" плитки, т.е. где и focused и не-focused одновременно.
    * val - для стабильности инстансов.
    *
    * @return Список шаблонов на рендер.
    */
  def flatGridTemplates: LazyList[MJdDoc] = {
    focOrAlwaysOpened
      .fold {
        {
          val blkExp2 = MScAdData._mkJdIdBlkExpand( main.doc.template, main.doc.jdId )

          if (main.doc.jdId.blockExpand ==* blkExp2)
            main.doc
          else
            (MScAdData._jdId_blockExpand_LENS set blkExp2)(main.doc)
        } #:: LazyList.empty

      } { fullBlkData =>
        fullBlkData.doc.template
          .subForest
          .iterator
          .zipWithIndex
          .map { case (blkJdt, i) =>
            main.doc.copy(
              template = blkJdt,
              jdId = {
                val id = main.doc.jdId
                id.copy(
                  selPathRev  = i :: id.selPathRev,
                  blockExpand = MScAdData._mkJdIdBlkExpand(blkJdt, id),
                )
              }
            )
          }
          .to( LazyList )
      }
  }

  /** Вернуть карту эджей для плоской плитки.
    *
    * @return Карта эджей.
    */
  def flatGridEdges: Map[EdgeUid_t, MEdgeDataJs] = {
    focused
      .fold(main.edges)(_.blkData.edges)
  }

  def nodeId = main.doc.jdId.nodeId
  override def id = nodeId

}

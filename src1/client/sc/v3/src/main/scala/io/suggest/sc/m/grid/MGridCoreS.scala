package io.suggest.sc.m.grid

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.grid.build.{IGbBlockPayload, MGbSubItems, MGridBuildResult}
import io.suggest.jd.MJdConf
import io.suggest.jd.render.v.JdCss
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 21:55
  * Description: Модель данных состояния плитки, которые влияют на рендер.
  * Данные, не влияющие на отображение, не должны находится в этой модели.
  *
  * Неявно-пустая модель.
  */
object MGridCoreS {

  /** Поддержка FastEq для [[MGridCoreSFastEq]]. */
  implicit object MGridCoreSFastEq extends FastEq[MGridCoreS] {
    override def eqv(a: MGridCoreS, b: MGridCoreS): Boolean = {
      (a.jdConf       ==*  b.jdConf) &&
        (a.jdCss      ===* b.jdCss) &&
        (a.ads        ===* b.ads) &&
        (a.gridBuild  ===* b.gridBuild)
    }
  }

  @inline implicit def univEq: UnivEq[MGridCoreS] = UnivEq.derive


  /** Приведение списка карточек к блокам для обсчёта плитки.
    *
    * @param ads Рекламные карточки.
    * @return Список блоков и под-блоков.
    */
  def ads2gridBlocks(ads: TraversableOnce[MScAdData]): Iterator[IGbBlockPayload] = {
    ads
      .toIterator
      .map { scAdData =>
        scAdData.focused.fold [IGbBlockPayload] {
          // Несфокусированная карточка. Вернуть bm единственного стрипа.
          val brd = scAdData.main
          MBlkRenderData.blockRenderData2GbPayload( scAdData.nodeId, brd.template, brd )
        } { foc =>
          // Открытая карточка. Вернуть MGbSubItems со списком фокус-блоков:
          val focBlk = foc.blkData
          MGbSubItems(
            nodeId = scAdData.nodeId,
            // Пройтись по блокам из focused-контейнера...
            subItems = focBlk
              .template
              .subForest
              .iterator
              .map { subTpl =>
                MBlkRenderData.blockRenderData2GbPayload( scAdData.nodeId, subTpl, focBlk )
              }
              .toList
          )
        }
      }
  }

}


/** Класс модели состояния плитки карточек.
  *
  * @param ads Содержимое плитки.
  *            Pot реквеста к серверу за новыми карточками для плитки.
  * @param gridSz Реально-занимаемый размер плитки. Вычисляется во время раскладывания карточек.
  * @param gridBuild Результат сборки плитки в контроллере.
  */
case class MGridCoreS(
                       jdConf         : MJdConf,
                       jdCss          : JdCss,
                       ads            : Pot[Vector[MScAdData]]        = Pot.empty,
                       gridBuild      : MGridBuildResult              = MGridBuildResult.empty,
                     )
  extends EmptyProductPot
{

  def withJdConf(jdConf: MJdConf)                         = copy(jdConf = jdConf)
  def withJdCss(jdCss: JdCss)                             = copy(jdCss = jdCss)
  def withAds(ads: Pot[Vector[MScAdData]])                = copy(ads = ads)
  def withGridBuild(gridBuild: MGridBuildResult)          = copy(gridBuild = gridBuild)

  /** Текущая открытая карточка, если есть. */
  lazy val focusedAdOpt: Option[MScAdData] = {
    ads
      .toOption
      .flatMap { adsVec =>
        adsVec.find { b =>
          b.focused.nonEmpty
        }
      }
  }

  /** Происходит ли сейчас загрузка какой-либо карточки? */
  lazy val _adsHasPending: Boolean =
    ads.iterator.flatten.exists(_.focused.isPending)

  /** Происходит ли сейчас загрузка какой-либо карточки или карточек? */
  def adsHasPending: Boolean =
    ads.isPending || _adsHasPending

  def adsGridBlocksIter: Iterator[IGbBlockPayload] =
    MGridCoreS.ads2gridBlocks( ads.iterator.flatten )

}

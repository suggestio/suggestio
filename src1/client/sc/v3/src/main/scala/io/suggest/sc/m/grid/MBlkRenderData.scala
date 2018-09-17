package io.suggest.sc.m.grid

import diode.FastEq
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.grid.build.MGbBlock
import io.suggest.jd.MJdAdData
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.17 15:38
  * Description: Модель данных для рендера блока плитки.
  */
object MBlkRenderData {

  /** Поддержка FastEq для инстансов [[MBlkRenderData]]. */
  implicit object MBlocksRenderDataFastEq extends FastEq[MBlkRenderData] {
    override def eqv(a: MBlkRenderData, b: MBlkRenderData): Boolean = {
      (a.template ===* b.template) &&
        (a.edges ===* b.edges)
    }
  }

  @inline implicit def univEq: UnivEq[MBlkRenderData] = UnivEq.derive

  /** Сборка инстанса [[MScAdData]] из инстанса MJdAdData. */
  def apply( jdAdData: MJdAdData ): MBlkRenderData = apply(
    template  = jdAdData.template,
    edges     = jdAdData.edgesMap
      .mapValues(MEdgeDataJs(_))
  )


  /** Конвертация одной карточки в один блок для рендера в плитке. */
  def blockRenderData2GbPayload(nodeId: Option[String], stripTpl: Tree[JdTag], brd: MBlkRenderData): MGbBlock = {
    // Несфокусированная карточка. Вернуть blockMeta с единственного стрипа.
    val stripJdt = stripTpl.rootLabel
    val bm = stripJdt
      .props1
      .bm.get

    val wideBgBlk = for {
      _       <- OptionUtil.maybeTrue( bm.wide )
      bg      <- stripJdt.props1.bgImg
      // 2018-01-23: Для wide-фона нужен отдельный блок, т.к. фон позиционируется отдельно от wide-block-контента.
      // TODO Нужна поддержка wide-фона без картинки.
      bgEdge  <- brd.edges.get( bg.edgeUid )
      imgWh   <- bgEdge.origWh
    } yield {
      imgWh
    }

    MGbBlock( nodeId, bm, wideBgBlk )
  }

}



/** Данные для рендера одного блока плитки.
  *
  * @param template Шаблон для рендера.
  *                 Тут может быть и Strip, и Document в зависимости от ситуации.
  * @param edges Карта эджей для рендера.
  */
case class MBlkRenderData(
                           template    : Tree[JdTag],
                           edges       : Map[EdgeUid_t, MEdgeDataJs]
                         ) {

  lazy val tplSubForestIndexed = template.subForest.toIndexedSeq

  override def toString: String = {
    new StringBuilder( productPrefix )
      .append( HtmlConstants.`(` )
      // Гарантированно не рендерим дерево, хотя там вроде toString и так переопределён уже:
      .append( HtmlConstants.DIEZ ).append( HtmlConstants.COMMA )
      // Рендерим кол-во эджей вместо всей карты
      .append( edges.size ).append( "e" )
      .append( HtmlConstants.`)` )
      .toString()
  }

}

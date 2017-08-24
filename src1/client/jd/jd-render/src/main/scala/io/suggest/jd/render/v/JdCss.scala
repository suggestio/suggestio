package io.suggest.jd.render.v

import scalacss.DevDefaults._
import io.suggest.jd.render.m.MJdBlockRa
import io.suggest.model.n2.edge.MPredicates

import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:40
  * Description: Динамические CSS-стили для рендера блоков плитки.
  */

class JdCss(id2docMap: Map[String, MJdBlockRa]) extends StyleSheet.Inline {

  import dsl._

  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    addClassName("sm-block")
  )

  /** Домен позволяет собрать styleF() */
  private val docIdsDomain = Domain.ofValues(
    id2docMap.keys.toSeq: _*
  )

  /** sm-block может содержать какие-то данные для рендера, например размер или цвет фона. */
  val smBlockOuterF = styleF(docIdsDomain) { nodeId =>
    val d = id2docMap( nodeId )
    val stylBg = d.templateBlockStrip.bgColor.fold(StyleS.empty) { mcd =>
      styleS(
        backgroundColor( Color(mcd.hexCode) )
      )
    }
    val stylWh = d.templateBlockStrip.bm.fold(StyleS.empty) { bm =>
      styleS(
        width( bm.width.px ),
        height( bm.height.px )
      )
    }
    stylBg.compose( stylWh )
  }


  /** Словарь картинок. */
  /*private val imgsEdgesMap = {
    val iter = for {
      (nodeId, jd)      <- id2docMap.iterator
      (edgeUid, medge)  <- jd.common.edges.iterator
      if medge.predicate == MPredicates.Bg
    } yield {
      (nodeId, edgeUid) -> medge
    }
    iter.toMap
  }*/

  /** Домен изображений, ориентировка по doc_id и edge uid. */
  //private val imgsDomain = Domain.ofValues( imgsEdgesMap.keys.toSeq: _* )

  /** Ширина и длина -- 100%. */
  val wh100 = {
    val pc100 = 100.%%
    style(
      width(pc100),
      height(pc100)
    )
  }

  /** Стили для изображений: положения, размеры, фон и т.д. */


}

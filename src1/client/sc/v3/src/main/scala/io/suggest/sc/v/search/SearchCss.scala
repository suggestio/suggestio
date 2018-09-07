package io.suggest.sc.v.search

import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.sc.styl.ScScalaCssDefaults._
import io.suggest.sc.m.search.MSearchCssProps
import io.suggest.sc.styl.ScCss
import japgolly.univeq.UnivEq
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.18 14:27
  * Description: CSS stylesheet с доп.стилями для поисковой панели.
  * Возникли, т.к. необходимо было оперативно управлять высотами гео.карты и списка найденных узлов,
  * без пере-рендера оных, без бардака в аттрибутах компонентов.
  */
object SearchCss {

  // 32 - с потолка, для material-ui. В оригинале было 24
  val NODE_ROW_HEIGHT_PX = 54.5

  implicit def univEq: UnivEq[SearchCss] = UnivEq.derive

}


case class SearchCss( args: MSearchCssProps ) extends StyleSheet.Inline {

  import dsl._
  import SearchCss._


  private val TAB_BODY_HEIGHT_PX = args.screenInfo.screen.height - args.screenInfo.unsafeOffsets.top

  private val NODES_LIST_HEIGHT_PX = {
    // Надо оценить кол-во рядов для стилей.
    // Для pending/failed надо рассчитать кол-во рядов на 1 больше (для места на экране).
    var rowsCount = 0
    for (nodes <- args.req)
      rowsCount += Math.max(1, nodes.resp.length)
    if (args.req.isPending)
      rowsCount += 1
    if (args.req.isFailed)
      rowsCount += 2

    rowsCount = Math.min(rowsCount, 3)

    val listHeightPx = rowsCount * SearchCss.NODE_ROW_HEIGHT_PX
    listHeightPx.toInt
  }

  private val GEO_MAP_HEIGHT_PX = TAB_BODY_HEIGHT_PX - NODES_LIST_HEIGHT_PX - ScCss.TABS_OFFSET_PX


  /** Стили для гео-карты гео-картой. */
  object GeoMap {

    val geomap = style(
      height( GEO_MAP_HEIGHT_PX.px )
    )

    val crosshair = style(
      top( -(GEO_MAP_HEIGHT_PX / 2 + 12).px ),
    )

  }


  /** Доп.стили списка найденных узлов. */
  object NodesFound {

    val nodesList = style(
      height( NODES_LIST_HEIGHT_PX.px ),
      paddingTop( 0.px ),
      paddingBottom( 0.px ),
      overflow.auto
    )

    // После втыкания materialUI, возникла необходимость описывать стили не-инлайново через classes.

    private val nodeIdsDomain = new Domain.OverSeq( args.nodesMap.keys.toIndexedSeq )

    /** Стиль фона ряда одного узла. */
    val rowItemBgF = styleF(nodeIdsDomain) { nodeId =>
      val nodeProps = args.nodesMap(nodeId)
      nodeProps.colors.bg.whenDefinedStyleS { mcd =>
        styleS(
          backgroundColor( Color(mcd.hexCode) )
        )
      }
    }

    /** Стиль переднего плана одноу узла. */
    val rowTextPrimaryF = styleF(nodeIdsDomain) { nodeId =>
      val nodeProps = args.nodesMap(nodeId)
      nodeProps.colors.fg.whenDefinedStyleS { mcd =>
        styleS(
          // "0xDD" - 0.87 alpha
          color( Color(mcd.hexCode + "DD") ).important
        )
      }
    }

    val rowTextSecondaryF = styleF(nodeIdsDomain) { nodeId =>
      val nodeProps = args.nodesMap(nodeId)
      nodeProps.colors.fg.whenDefinedStyleS { mcd =>
        styleS(
          // "0x89" - 0.54 alpha
          color( Color(mcd.hexCode + "89") ).important
        )
      }
    }

  }


  initInnerObjects(
    GeoMap.geomap,
    NodesFound.nodesList
  )

}

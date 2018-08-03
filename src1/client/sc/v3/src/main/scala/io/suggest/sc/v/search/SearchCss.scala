package io.suggest.sc.v.search

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

  val NODE_ROW_HEIGHT_PX = 24
  val NODE_ROW_PADDING_PX = 13

  implicit def univEq: UnivEq[SearchCss] = UnivEq.derive

}


case class SearchCss( args: MSearchCssProps ) extends StyleSheet.Inline {

  import dsl._
  import SearchCss._


  private val TAB_BODY_HEIGHT_PX = args.screenInfo.screen.height - ScCss.TABS_OFFSET_PX - args.screenInfo.unsafeOffsets.top

  private val NODES_LIST_HEIGHT_PX = args.nodesFoundShownCount.fold(0)(_ * (NODE_ROW_HEIGHT_PX + 2*NODE_ROW_PADDING_PX))

  private val GEO_MAP_HEIGHT_PX = TAB_BODY_HEIGHT_PX - NODES_LIST_HEIGHT_PX


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

    // TODO Это статический стиль, надо в статику его унести.
    val nodeRow = style(
      height( NODE_ROW_HEIGHT_PX.px ),
      padding( NODE_ROW_PADDING_PX.px, 0.px )
    )

  }


  initInnerObjects(
    GeoMap.geomap,
    NodesFound.nodesList
  )

}

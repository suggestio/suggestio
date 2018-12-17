package io.suggest.sc.v.search

import diode.FastEq
import io.suggest.color.MColorData
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.sc.styl.ScScalaCssDefaults._
import io.suggest.sc.m.search.MSearchCssProps
import io.suggest.sc.m.search.MSearchCssProps.MSearchCssPropsFastEq
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

  val NODE_ROW_HEIGHT_PX = 27

  @inline implicit def univEq: UnivEq[SearchCss] = UnivEq.derive

  implicit object SearchCssFastEq extends FastEq[SearchCss] {
    override def eqv(a: SearchCss, b: SearchCss): Boolean = {
      MSearchCssPropsFastEq.eqv(a.args, b.args)
    }
  }

}


case class SearchCss( args: MSearchCssProps ) extends StyleSheet.Inline {

  import dsl._


  private val TAB_BODY_HEIGHT_PX = args.screenInfo.screen.height - args.screenInfo.unsafeOffsets.top

  private val NODES_LIST_HEIGHT_PX = {
    // Надо оценить кол-во рядов для стилей.
    // Для pending/failed надо рассчитать кол-во рядов на 1 больше (для места на экране).
    var rowsCount = 0
    for (nodes <- args.req) {
      // Теги могут занимать и треть и пол-ряда. Поэтому ряды тегов надо считать по-особому:
      val nodesDoubleCount = nodes.resp
        .iterator
        .map { n =>
          n.props.ntype match {
            case MNodeTypes.Tag => 1
            case _ => 4
          }
        }
        .sum
        .toInt
      rowsCount += Math.max(1, nodesDoubleCount / 2)
    }
    if (args.req.isPending)
      rowsCount += 1
    if (args.req.isFailed)
      rowsCount += 2

    rowsCount = Math.min(rowsCount, 6)

    val listHeightPx = rowsCount * SearchCss.NODE_ROW_HEIGHT_PX
    listHeightPx.toInt
  }

  private val NODES_WITH_FIELD_HEIGHT_PX = NODES_LIST_HEIGHT_PX + ScCss.TABS_OFFSET_PX

  private val GEO_MAP_HEIGHT_PX = TAB_BODY_HEIGHT_PX - NODES_LIST_HEIGHT_PX - ScCss.TABS_OFFSET_PX


  /** Стили для гео-карты гео-картой. */
  object GeoMap {

    val geomap = style(
      height( GEO_MAP_HEIGHT_PX.px )
    )

    val crosshair = style(
      top( -(GEO_MAP_HEIGHT_PX / 2 + 12).px ),
      width( 0.px )
    )

  }


  /** Доп.стили списка найденных узлов. */
  object NodesFound {

    /** Контейнер поиска узлов и текстового поля для единого скроллинга. */
    val container = style(
      overflow.auto,
      height( NODES_WITH_FIELD_HEIGHT_PX.px ),
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

    private def _colorTransparent(mcd: MColorData, transparent: Double) = {
      val rgb = mcd.getRgb
      val colorScss = rgba(
        r = rgb.red,
        g = rgb.green,
        b = rgb.blue,
        a = transparent
      )
      color( colorScss ).important
    }

    /** Стиль переднего плана одноу узла. */
    val rowTextPrimaryF = styleF(nodeIdsDomain) { nodeId =>
      val nodeProps = args.nodesMap(nodeId)
      nodeProps.colors.fg.whenDefinedStyleS { mcd =>
        styleS(
          // "0xDD" - 0.87 alpha
          _colorTransparent( mcd, 0.87 )
        )
      }
    }

    val rowTextSecondaryF = styleF(nodeIdsDomain) { nodeId =>
      val nodeProps = args.nodesMap(nodeId)
      nodeProps.colors.fg.whenDefinedStyleS { mcd =>
        styleS(
          // "0x89" - 0.54 alpha
          _colorTransparent( mcd, 0.54 )
        )
      }
    }

  }


  initInnerObjects(
    GeoMap.geomap,
    NodesFound.container,
  )

}

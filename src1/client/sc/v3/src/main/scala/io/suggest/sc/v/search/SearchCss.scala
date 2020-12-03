package io.suggest.sc.v.search

import diode.FastEq
import io.suggest.color.MColorData
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.n2.node.MNodeTypes
import io.suggest.css.ScalaCssDefaults._
import io.suggest.img.MImgFormats
import io.suggest.sc.m.search.MSearchCssProps
import io.suggest.sc.m.search.MSearchCssProps.MSearchCssPropsFastEq
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import scalacss.internal.DslBase.ToStyle
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

  def NODE_ROW_HEIGHT_PX = 60

  @inline implicit def univEq: UnivEq[SearchCss] = UnivEq.derive

  implicit object SearchCssFastEq extends FastEq[SearchCss] {
    override def eqv(a: SearchCss, b: SearchCss): Boolean =
      MSearchCssPropsFastEq.eqv(a.args, b.args)
  }

  def args = GenLens[SearchCss]( _.args )

  private val _nameSuffixGen = { (idOrName: String, i: Int) =>
    if (StringUtil.isBase64UrlSafe(idOrName))
      idOrName
    else
      (idOrName.hashCode * i).toString
  }

}


case class SearchCss( args: MSearchCssProps ) extends StyleSheet.Inline {

  import dsl._

  private def isScrollWithField = args.screenInfo.screen.isHeightEnought

  private def TAB_BODY_HEIGHT_PX = {
    val si = args.screenInfo
    si.screen.wh.height - si.unsafeOffsets.top
  }

  private val NODES_LIST_HEIGHT_PX = {
    val MAX_ROWS_COUNT = 7
    args.nodesFound.rHeightPx.fold {
      // Надо оценить кол-во рядов для стилей по-старинке. TODO Удалить этот код?
      // Для pending/failed надо рассчитать кол-во рядов на 1 больше (для места на экране).
      var rowsCount = 0
      for (nodes <- args.nodesFound.req) {
        // Теги могут занимать и треть и пол-ряда. Поэтому ряды тегов надо считать по-особому:
        val nodesDoubleCount = (for {
          nodeFound <- nodes.resp.nodes
          ntype <- nodeFound.props.ntype
        } yield {
          ntype match {
            case MNodeTypes.Tag => 1
            case _ => 2
          }
        })
          .sum
          .toInt

        rowsCount += Math.max(1, nodesDoubleCount / 2)
      }
      if (args.nodesFound.req.isPending)
        rowsCount += 1
      if (args.nodesFound.req.isFailed)
        rowsCount += 2

      rowsCount = Math.min(rowsCount, MAX_ROWS_COUNT)

      val rowHeightPx = SearchCss.NODE_ROW_HEIGHT_PX

      var listHeightPx = rowsCount * rowHeightPx
      if (rowsCount > MAX_ROWS_COUNT) listHeightPx += rowHeightPx/2

      //println("search rows height pxx: ", rowHeightPx, rowsCount, args.req.isFailed, args.req.isPending, args.req.fold(0)(_.resp.nodes.length), rowHeightPx, rowsCount > MAX_ROWS_COUNT)

      listHeightPx.toInt

    } { rHeightPx =>
      Math.min( rHeightPx, MAX_ROWS_COUNT * SearchCss.NODE_ROW_HEIGHT_PX )
    }
  }

  /*
  private val NODES_WITH_FIELD_HEIGHT_PX: Int = {
    var nlh = NODES_LIST_HEIGHT_PX
    if (!isScrollWithField) nlh += ScCss.TABS_OFFSET_PX

    var maxH = args.screenInfo.screen.wh.height
    if (isScrollWithField) maxH -= ScCss.TABS_OFFSET_PX

    Math.min( maxH, nlh )
  }
  */

  private val GEO_MAP_HEIGHT_PX: Int = {
    Math.max(
      0,
      TAB_BODY_HEIGHT_PX - ScCss.TABS_OFFSET_PX - NODES_LIST_HEIGHT_PX
    )
  }


  /** Стили для гео-карты гео-картой. */
  object GeoMap {

    val geomap = style(
      if (GEO_MAP_HEIGHT_PX > 0)
        height( GEO_MAP_HEIGHT_PX.px )
      else
        display.none,
    )

    val crosshair = style(
      if (GEO_MAP_HEIGHT_PX > 0)
        top( -(GEO_MAP_HEIGHT_PX / 2 + 12).px )
      else
        display.none,
    )

  }


  /** Доп.стили списка найденных узлов. */
  object NodesFound {

    /** Контейнер поиска узлов и текстового поля для единого скроллинга. */
    val container = style(
      overflowX.hidden,
      overflowY.auto,
    )

    // После втыкания materialUI, возникла необходимость описывать стили не-инлайново через classes.

    private val nodeIdsDomain = new Domain.OverSeq( args.nodesFound.nodesMap.keys.toIndexedSeq )

    /** Стиль фона ряда одного узла. */
    val rowItemBgF = styleF(nodeIdsDomain) (
      { nodeId =>
        val nodeProps = args.nodesFound.nodesMap(nodeId).props
        nodeProps.colors.bg.whenDefinedStyleS { mcd =>
          styleS(
            backgroundColor( Color(mcd.hexCode) )
          )
        }
      },
      SearchCss._nameSuffixGen,
    )

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
    val rowTextPrimaryF = styleF(nodeIdsDomain) (
      { nodeId =>
        val nodeProps = args.nodesFound.nodesMap(nodeId).props
        nodeProps.colors.fg.whenDefinedStyleS { mcd =>
          styleS(
            // "0xDD" - 0.87 alpha
            _colorTransparent( mcd, 0.87 )
          )
        }
      },
      SearchCss._nameSuffixGen,
    )

    val rowTextSecondaryF = styleF(nodeIdsDomain)(
      {nodeId =>
        val nodeProps = args.nodesFound.nodesMap(nodeId).props
        nodeProps.colors.fg.whenDefinedStyleS { mcd =>
          styleS(
            // "0x89" - 0.54 alpha
            _colorTransparent( mcd, 0.54 )
          )
        }
      },
      SearchCss._nameSuffixGen,
    )


    val rowItemIconF = {
      val NF = ScCssStatic.Search.NodesFound
      val maxWidthPx = NF.LOGO_WIDTH_MAX_PX
      val maxHeightPx = NF.LOGO_HEIGHT_MAX_PX

      styleF {
        new Domain.OverSeq(
          args.nodesFound.nodesMap
            .iterator
            .filter(_._2.props.wcFgOrLogo.nonEmpty)
            .map(_._1)
            .toIndexedSeq
        )
      } { nodeId =>
        var acc = List.empty[ToStyle]

        for {
          nodeProps <- args.nodesFound.nodesMap.get(nodeId)
          logo <- nodeProps.props.wcFgOrLogo
          isVector = MImgFormats
            .withMime( logo.contentType )
            .exists(_.isVector)
          // Для векторной и для растровой картинок надо использовать различный подход:
          // Векторную растягиваем до разрешённого максимума, растровую - подчиняем размеру
          wh <- logo.whPx
          widthPx1 = if (isVector) {
            // SVG: Берём за основу ширины максимально-допустимую ширину, проецируем высоту, сравниваем с макс.высотой,
            // проецируем итоговую ширину из выбранной высоты.
            val heightPx1 = maxWidthPx * wh.height / wh.width
            val heightPx2 = Math.min( maxHeightPx, heightPx1 )
            wh.width * heightPx2 / wh.height
          } else wh.width
          widthPx2 = Math.min( widthPx1, maxWidthPx )
        } {
          acc ::= minWidth( widthPx2.px )
        }

        styleS( acc: _* )
      }
    }
  }


  initInnerObjects(
    GeoMap.geomap,
    NodesFound.container,
  )

}

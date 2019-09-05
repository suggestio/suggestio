package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter._
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.css.Css
import io.suggest.grid.build.{GridBuilderUtil, MGbBlock, MGridBuildArgs}
import io.suggest.jd.{MJdEdgeId, MJdTagId}
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.univeq._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */

class JdR(
           jdCssStatic        : JdCssStatic,
           jdGridUtil         : JdGridUtil
         ) { jdR =>

  type Props_t = MJdArgs
  type Props = ModelProxy[Props_t]


  /** Для разделения edit и read-only режима используется этот трейт,
    * реализующий только базовый рендер с возможностью внутреннего расширения.
    */
  trait JdRrr {

    class QdContentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) {

      def _qdContentRrrHtml(p: MJdRrrProps): QdRrrHtml = {
        QdRrrHtml(
          jdCssStatic = jdCssStatic,
          rrrProps    = p,
        )
      }

      def _renderQdContentTag(state: MJdRrrProps): TagOf[html.Div] = {
        import state.jdArgs.jdRuntime.jdCss

        val qdTag = state.subTree.rootLabel

        <.div(

          // Опциональный цвет фона
          _bgColorOpt( qdTag, state.jdArgs ),

          // Опциональная ротация элемента.
          _maybeRotate( qdTag, state.jdArgs ),

          // Поддержка перетаскивания
          jdCssStatic.absPosStyleAll,

          jdCss.absPosStyleF( state.tagId ),

          // CSS-класс принудительной ширины, если задан.
          ReactCommonUtil.maybe( qdTag.props1.widthPx.nonEmpty ) {
            jdCss.forcedWidthStyleF( state.tagId )
          },

          // Стиль для теней
          ReactCommonUtil.maybe( qdTag.props1.textShadow.nonEmpty ) {
            jdCss.contentShadowF( state.tagId )
          },

          // Рендер qd-контента в html.
          _qdContentRrrHtml( state )
            .render()
        )
      }

      def render(p: MJdRrrProps): VdomElement = {
        _renderQdContentTag( p )
      }

    }

    /** There are no virtual classes in Scala (yet), so you can't write override class Val ...,
      * and then be sure that invoking new Val will dynamically choose the right class for the new instance. (...)
      *
      * The general trick to emulate virtual classes is to write class Val extends super.Val, and then override
      * a protected method which serves as a factory for instances of the class.
      *
      * see [[https://stackoverflow.com/a/4337744]]
      */
    def mkQdContentB = new QdContentB(_)

    /** Сборка react-компонента. */
    val qdContentComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[QdContentB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .backend( mkQdContentB )
      .renderBackend
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdtRrrPropsFastEq) )
      .build

    // -------------------------------------------------------------------------------------------------

    class BlockB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) {

      /** Доп.наполнение для тега фоновой картинки блока. */
      def _bgImgAddons(bgImgData: MJdEdgeId, edge: MEdgeDataJs, state: MJdRrrProps): TagMod = {
        // Просто заполнение всего блока картинкой. Т.к. фактический размер картинки отличается от размера блока
        // на px ratio, надо подогнать картинку по размерам:
        state.jdArgs.jdRuntime.jdCss.stripBgStyleF( state.tagId )
      }

      /** Доп.наполнение для sm-block div'a. */
      def _smBlockAddons(state: MJdRrrProps): TagMod =
        TagMod.empty

      /** Доп.наполнения для самого внешнего div'а. */
      def _outerContainerAddons(state: MJdRrrProps): TagMod =
        TagMod.empty


      def _renderBlockTag(propsProxy: ModelProxy[MJdRrrProps]): TagOf[html.Div] = {
        val state = propsProxy.value
        val s = state.subTree.rootLabel
        val C = state.jdArgs.jdRuntime.jdCss

        val isWide = s.props1.bm.hasExpandMode

        val bgColor = _bgColorOpt(s, state.jdArgs)

        val groupOutlineTm = state.jdArgs.renderArgs
          .groupOutLined
          .whenDefined { mcd =>
            TagMod(
              C.blockGroupOutline,
              ^.outlineColor := mcd.hexCode
            )
          }

        val bgImgOpt = for {
          bgImgData <- s.props1.bgImg
          edge      <- state.jdArgs.data.edges.get( bgImgData.edgeUid )
          if edge.jdEdge.predicate ==>> MPredicates.JdContent.Image
          bgImgSrc  <- edge.origImgSrcOpt
        } yield {
          <.img(
            ^.`class` := Css.Block.BG,
            ^.src := bgImgSrc,

            _bgImgAddons(bgImgData, edge, state),

            // В jdArgs может быть задан дополнительные модификации изображения, если selected tag.
            state.jdArgs.renderArgs.selJdtBgImgMod
              .filter(_ => state.isCurrentSelected)
              .whenDefined
          )
        }
        val bgImgTm = bgImgOpt.whenDefined

        val maybeSelAV = _outerContainerAddons(state)

        // Скрывать не-main-стрипы, если этого требует рендер.
        // Это касается только стрипов, у которых нет isMain = Some(true)
        val hideNonMainStrip = ReactCommonUtil.maybe(
          state.jdArgs.renderArgs.hideNonMainStrips &&
            !s.props1.isMain.getOrElseFalse
        ) {
          // Данный стип надо приглушить с помощью указанных css-стилей.
          ^.visibility.hidden
        }

        val smBlock = <.div(
          jdCssStatic.smBlockS,
          C.smBlock,
          C.bmStyleF( state.tagId ),

          if (isWide) {
            jdCssStatic.wideBlockStyle
          } else {
            TagMod(
              hideNonMainStrip,
              bgColor,
              maybeSelAV,
              groupOutlineTm
            )
          },

          _smBlockAddons(state),

          // Если задана фоновая картинка, от отрендерить её.
          bgImgTm.unless(isWide),

          renderChildren( propsProxy )
            .toVdomArray
        )

        if (isWide) {
          // Широкоформатное отображение, рендерим фон без ограничений блока:
          <.div(
            groupOutlineTm,
            hideNonMainStrip,
            bgColor,
            C.wideContStyleF(state.tagId),
            maybeSelAV,
            ^.`class` := Css.flat( Css.Overflow.HIDDEN, Css.Position.RELATIVE ),
            bgImgTm.when(isWide),
            smBlock
          )
        } else {
          // Обычное отображение, просто вернуть блок.
          smBlock
        }

      }

      def render(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        _renderBlockTag(propsProxy)
      }

    }
    def mkBlockB = new BlockB(_)
    val blockComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .backend( mkBlockB )
      .renderBackend
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdtRrrPropsFastEq) )
      .build

    // -------------------------------------------------------------------------------------------------

    class DocumentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) {

      def _renderTag(propsProxy: ModelProxy[MJdRrrProps]): TagOf[html.Div] = {
        val state = propsProxy.value
        <.div(
          // Плитку отсюда полностью вынести не удалось.
          CSSGrid {
            jdGridUtil.mkCssGridArgs(
              gbRes = GridBuilderUtil.buildGrid {
                MGridBuildArgs(
                  itemsExtDatas = (for {
                    (jdtTree, i) <- state.subTree.subForest.zipWithIndex
                    jdt = jdtTree.rootLabel
                    if jdt.props1.bm.nonEmpty
                  } yield {
                    Tree.Leaf(
                      MGbBlock( None, Some(jdt), orderN = Some(i) )
                    )
                  })
                    .toStream,
                  jdConf = state.jdArgs.conf,
                  offY = 0,
                  jdtWideSzMults = state.jdArgs.jdRuntime.jdtWideSzMults,
                )
              },
              conf = state.jdArgs.conf,
              tagName = GridComponents.DIV,
            )
          } (
            renderChildrenWithId( propsProxy )
              // Тут тег-обёртка-костыль - что для CSSGrid нужен обязательно теги, а НЕ react-компоненты в children.
              .map { case (id, p) =>
                <.li(
                  ^.key := id.toString,
                  p
                )
              }: _*
          )
        )
      }

      def render(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        _renderTag( propsProxy )
      }

    }
    def mkDocumentB = new DocumentB(_)
    val documentComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .backend( mkDocumentB )
      .renderBackend
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdtRrrPropsFastEq) )
      .build


    // -------------------------------------------------------------------------------------------------

    /**
      * Запуск рендеринга произвольных тегов.
      */
    // TODO parent может быть необязательным. Но это сейчас не востребовано, поэтому он обязательный
    def renderTag( proxy: ModelProxy[MJdRrrProps] ): VdomElement = {
      import MJdTagNames._
      val p = proxy.value
      p.subTree.rootLabel.name match {
        case QD_CONTENT                => qdContentComp.withKey(p.tagId.toString)(proxy)
        case STRIP                     => blockComp.withKey(p.tagId.toString)(proxy)
        case DOCUMENT                  => documentComp.withKey(p.tagId.toString)(proxy)
        // Это пока не вызывается, т.к. QD_OP отрабатывается в QdRrrHtml.
        case other =>
          throw new UnsupportedOperationException( other.toString )
      }
    }


    /** Отрендерить дочерние элементы тега обычным методом.
      * @return Итератор отрендеренных vdom-узлов.
      */
    def renderChildrenWithId(proxy: ModelProxy[MJdRrrProps]): Stream[(MJdTagId, VdomNode)] = {
      val p = proxy.value
      val chs = p.subTree.subForest

      if (chs.nonEmpty) {
        val parent = p.subTree.rootLabel
        // Для qd-content-тегов надо указывать родительский тег.
        val parentSome = OptionUtil.maybe(parent.name ==* MJdTagNames.STRIP)( parent )
        chs
          .zipWithIndex
          .map { case (childJdTree, i) =>
            val ch = childJdTree.rootLabel
            val tagId = p.tagId.copy(
              selPathRev  = i :: p.tagId.selPathRev,
              blockExpand = if (ch.name ==* MJdTagNames.STRIP) {
                ch.props1.bm
                  .flatMap(_.expandMode)
              } else {
                p.tagId.blockExpand
              }
            )
            val p2 = p.copy(
              subTree = childJdTree,
              tagId   = tagId,
              parent  = parentSome,
            )
            // Вместо wrap используем прямой зум, чтобы избежать вызова цепочек функций, создающих множественные
            // инстансы одинаковых MJdRrrProps, которые будут удлиняться с каждым под-уровнем.
            val proxy2 = proxy.resetZoom( p2 )
            tagId -> renderTag( proxy2 )
          }
      } else {
        Stream.empty
      }
    }
    def renderChildren(proxy: ModelProxy[MJdRrrProps]): Stream[VdomNode] = {
      renderChildrenWithId(proxy)
        .map(_._2)
    }


    val anyTagComp = ScalaComponent
      .builder[ModelProxy[MJdArgs]]( jdR.getClass.getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .render_P { propsProxy =>
        propsProxy.wrap { jdArgs =>
          MJdRrrProps(
            subTree = jdArgs.data.doc.template,
            tagId   = jdArgs.data.doc.jdId,
            jdArgs  = jdArgs,
          )
        }( renderTag )(implicitly, MJdRrrProps.MJdtRrrPropsFastEq)
      }
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdArgs.MJdArgsFastEq) )
      .build

  }
  /** Обычная рендерилка. */
  object JdRrr extends JdRrr


  private def _bgColorOpt(jdTag: JdTag, jdArgs: MJdArgs): TagMod = {
    jdTag.props1.bgColor.whenDefined { mcd =>
      jdArgs.jdRuntime.jdCss.bgColorOptStyleF( mcd.hexCode )
    }
  }

  /** Ротация элемента. */
  private def _maybeRotate(jdt: JdTag, jdArgs: MJdArgs): TagMod = {
    jdt.props1.rotateDeg
      .whenDefined { jdArgs.jdRuntime.jdCss.rotateF.apply }
  }


  private def _apply(jdArgsProxy: Props) = JdRrr.anyTagComp( jdArgsProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}

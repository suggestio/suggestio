package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter._
import com.github.souporserious.react.measure.{Bounds, Measure}
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.geom.coord.{MCoords2dD, MCoords2di}
import io.suggest.css.Css
import io.suggest.grid.build.{GridBuilderUtil, MGbBlock, MGridBuildArgs}
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.lk.r.img.ImgRenderUtilJs
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.MimeConst
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.log.Log
import io.suggest.react.ReactDiodeUtil._
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement, html_<^}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Element, html}
import org.scalajs.dom.raw.CSSStyleDeclaration
import play.api.libs.json.Json
import scalacss.ScalaCssReact._
import scalacss.internal.LengthUnit
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */

class JdR(
           jdCssStatic        : JdCssStatic,
           imgRenderUtilJs    : ImgRenderUtilJs,
           jdGridUtil         : JdGridUtil
         )
  extends Log
{ jdR =>


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
          jdArgs      = p.jdArgs,
          qdTag       = p.subTree,
        )
      }

      def _renderQdContentTag(state: MJdRrrProps): TagOf[html.Div] = {
        val qdTag = state.subTree.rootLabel

        <.div(
          ^.key := state.tagId.toString,

          // Опциональный цвет фона
          _bgColorOpt( qdTag, state.jdArgs ),

          // Опциональная ротация элемента.
          _maybeRotate( qdTag, state.jdArgs ),

          // Поддержка перетаскивания
          jdCssStatic.absPosStyleAll,

          state.jdArgs.jdRuntime.jdCss.absPosStyleF(qdTag),

          // CSS-класс принудительной ширины, если задан.
          ReactCommonUtil.maybe( qdTag.props1.widthPx.nonEmpty ) {
            state.jdArgs.jdRuntime.jdCss.forcedWidthStyleF(qdTag)
          },

          // Стиль для теней
          ReactCommonUtil.maybe( qdTag.props1.textShadow.nonEmpty ) {
            state.jdArgs.jdRuntime.jdCss.contentShadowF( qdTag )
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

      def _bgImgAddons(edge: MEdgeDataJs, state: MJdRrrProps): TagMod =
        TagMod.empty

      def _smBlockAddons(state: MJdRrrProps): TagMod =
        TagMod.empty

      def _renderContentTag(propsProxy: ModelProxy[MJdRrrProps]): TagOf[html.Div] = {
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
          edge      <- state.jdArgs.edges.get( bgImgData.edgeUid )
          if edge.jdEdge.predicate ==>> MPredicates.JdContent.Image
          bgImgSrc  <- edge.origImgSrcOpt
        } yield {
          <.img(
            ^.`class` := Css.Block.BG,
            ^.src := bgImgSrc,

            imgRenderUtilJs
              // Размеры и позиционирование фоновой картинки в блоке (эмуляция кропа):
              .htmlImgCropEmuAttrsOpt(
                cropOpt     = bgImgData.crop,
                outerWhOpt  = s.props1.bm,
                origWhOpt   = edge.origWh,
                szMult      = state.jdArgs.conf.szMult
              )
              .getOrElse {
                // Просто заполнение всего блока картинкой. Т.к. фактический размер картинки отличается от размера блока
                // на px ratio, надо подогнать картинку по размерам:
                C.stripBgStyleF(s)
              },

            _bgImgAddons(edge, state),

            // В jdArgs может быть задан дополнительные модификации изображения, если selected tag.
            state.jdArgs.renderArgs.selJdtBgImgMod
              .filter(_ => state.isCurrentSelected)
              .whenDefined
          )
        }
        val bgImgTm = bgImgOpt.whenDefined

        val keyAV = {
          ^.key := state.tagId.toString
        }

        val maybeSelAV = _maybeSelected( s, state.jdArgs )

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
          keyAV
            .unless(isWide),
          C.smBlock,
          C.bmStyleF( s ),

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
            keyAV,
            groupOutlineTm,
            hideNonMainStrip,
            bgColor,
            C.wideContStyleF(s),
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
        _renderContentTag(propsProxy)
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
          ^.key := state.tagId.toString,
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
              tagName = GridComponents.DIV
            )
          } (
            renderChildrenWithId( propsProxy )
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
      proxy.value.subTree.rootLabel.name match {
        case QD_CONTENT                => qdContentComp(proxy)
        case STRIP                     => blockComp(proxy)
        case DOCUMENT                  => documentComp(proxy)
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
              selPath     = i :: p.tagId.selPath,
              blockExpand = OptionUtil.maybeOpt {
                (ch.name ==* MJdTagNames.STRIP) //&&
                // TODO Если wide запрещён, то это влияет на рендер?
                //(p.jdArgs.jdRuntime.jdtWideSzMults contains ch)
              } {
                ch.props1.bm
                  .flatMap(_.expandMode)
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
            subTree = jdArgs.template,
            tagId = MJdTagId(
              nodeId      = None, // TODO XXX
              selPath     = Nil,
              blockExpand = None,
            ),
            jdArgs = jdArgs,
          )
        }( renderTag )(implicitly, MJdRrrProps.MJdtRrrPropsFastEq)
      }
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdArgs.MJdArgsFastEq) )
      .build

  }
  /** Обычная рендерилка. */
  object JdRrr extends JdRrr


  // TODO Унести эти методы API внутрь jd-edit.
  /**
    * Является ли указанный тег текущим выделенным?
    * Если да, то присвоить ему соотв.стиль для выделения визуально.
    */
  private def _maybeSelected(dt: JdTag, jdArgs: MJdArgs): TagMod = {
    // Если происходит перетаскивание, то нужно избавляться от рамок: так удобнее.
    ReactCommonUtil.maybe(
      jdArgs.renderArgs.dnd.jdt.isEmpty &&
        (jdArgs.selJdt.treeLocOpt containsLabel dt)
    ) {
      jdCssStatic.selectedTag
    }
  }

  trait JdRrrEdit extends JdRrr {

    private def _parseStylePx(e: ReactMouseEventFromHtml)(f: CSSStyleDeclaration => String): Option[Int] = {
      for {
        // Используем currentTarget, т.к. хром возвращает события откуда попало, а не из точки аттача.
        // TODO Если за пределами блока отпускание мыши, то и это не помогает.
        target        <- Option(e.currentTarget)
        if e.button ==* 0
        style         <- Option(target.style)
        sizePxStyl    <- Option(f(style))
        pxIdx = sizePxStyl.indexOf( LengthUnit.px.value )
        if pxIdx > 0
      } yield {
        sizePxStyl
          .substring(0, pxIdx)
          .toInt
      }
    }
    private def _parseWidth(e: ReactMouseEventFromHtml): Option[Int] =
      _parseStylePx(e)(_.width)
    private def _parseHeight(e: ReactMouseEventFromHtml): Option[Int] =
      _parseStylePx(e)(_.height)


    /** Повесить onload для картинки, считывающий ей wh.
      * Нужно, чтобы редактор мог узнать wh оригинала изображения. */
    private def _notifyImgWhOnEdit[P <: ModelProxy[_], S]($: BackendScope[P,S], e: MEdgeDataJs, jdArgs: MJdArgs): TagMod = {
      // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
      ReactCommonUtil.maybe( jdArgs.conf.isEdit && e.fileJs.exists(_.whPx.isEmpty) ) {
        ^.onLoad ==> onNewImageLoaded($, e.id)
      }
    }
    /** Callback о завершении загрузки в память картинки, у которой неизвестны какие-то рантаймовые параметры. */
    private def onNewImageLoaded[P <: ModelProxy[_], S]($: BackendScope[P,S], edgeUid: EdgeUid_t)(e: ReactEvent): Callback = {
      imgRenderUtilJs.notifyImageLoaded($, edgeUid)(e)
    }

    /** Реакция на клик по отрендеренному тегу. */
    private def jdTagClick[P <: ModelProxy[_], S]($: BackendScope[P,S], jdt: JdTag)(e: ReactMouseEvent): Callback = {
      // Если не сделать stopPropagation, то наружный strip перехватит клик
      e.stopPropagationCB >>
        dispatchOnProxyScopeCB($, JdTagSelect(jdt) )
    }
    private def _clickableOnEdit[P <: ModelProxy[_], S]($: BackendScope[P,S], jdt: JdTag, jdArgs: MJdArgs): TagMod = {
      // В режиме редактирования -- надо слать инфу по кликам на стрипах
      ReactCommonUtil.maybe(jdArgs.conf.isEdit) {
        ^.onClick ==> jdTagClick($, jdt)
      }
    }

    private def _draggableUsing[P <: ModelProxy[_], S]($: BackendScope[P,S], jdt: JdTag, jdArgs: MJdArgs)(onDragStartF: ReactDragEvent => Callback): TagMod = {
      TagMod(
        ^.draggable := true,
        ^.onDragStart ==> onDragStartF,
        ^.onDragEnd   ==> jdTagDragEnd($, jdt)
      )
    }
    private def jdTagDragEnd[P <: ModelProxy[_], S]($: BackendScope[P,S], jdt: JdTag)(e: ReactDragEvent): Callback = {
      dispatchOnProxyScopeCB($, JdTagDragEnd(jdt) )
    }


    class QdContentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends super.QdContentB($) {

      override def _qdContentRrrHtml(p: MJdRrrProps): QdRrrHtml = {
        QdRrrHtml(
          jdCssStatic = jdCssStatic,
          jdArgs      = p.jdArgs,
          qdTag       = p.subTree,
          // Для редактора: следует рендерить img-теги, подслушивая у них wh:
          imgEdgeMods = OptionUtil.maybe( p.jdArgs.conf.isEdit ) {
            _notifyImgWhOnEdit($, _, p.jdArgs)
          },
          // Выбранный qd-тег можно ресайзить:
          resizableCb = OptionUtil.maybe( p.isCurrentSelected ) {
            onQdEmbedResize(_, _, _)(_)
          },
        )
      }

      override def _renderQdContentTag(state: MJdRrrProps): TagOf[Div] = {
        val qdTag = state.subTree.rootLabel

        super._renderQdContentTag(state)(
          _maybeSelected(qdTag, state.jdArgs),
          _clickableOnEdit($, qdTag, state.jdArgs),

          ReactCommonUtil.maybe(
            !state.parent.exists(state.jdArgs.selJdt.treeLocOpt.containsLabel)
          ) {
            _draggableUsing($, qdTag, state.jdArgs) { qdTagDragStart(qdTag) }
          },

          // Рендерить особые указатели мыши в режиме редактирования.
          if (state.isCurrentSelected) {
            // Текущий тег выделен. Значит, пусть будет move-указатель
            TagMod(
              ^.`class` := Css.flat( Css.Overflow.HIDDEN, Css.Cursor.MOVE ),
              jdCssStatic.horizResizable,
              // TODO onResize -> ...
              ^.onMouseUp ==> onQdTagResize(qdTag),
            )
          } else {
            // Текущий тег НЕ выделен. Указатель обычной мышкой.
            ^.`class` := Css.Cursor.POINTER
          },
        )
      }

      override def render(state: MJdRrrProps): VdomElement = {
        val contentDiv = _renderQdContentTag( state )
        // Если рендер ВНЕ блока, то нужно незаметно измерить высоту блока.
        if (state.parent.exists(_.name ==* MJdTagNames.STRIP)) {
          // Рендер внутри блока, просто пропускаем контент на выход
          contentDiv
        } else {
          // Рендер вне блока. Высоту надо измерять,
          Measure.bounds( blocklessQdContentBoundsMeasuredJdCb(state.subTree.rootLabel, _) ) { ref =>
            contentDiv.withRef(ref)
          }
        }
      }

      /** Callback ресайза. */
      private def onQdEmbedResize(qdOp: MQdOp, edgeDataJs: MEdgeDataJs, withHeight: Boolean)
                                 (e: ReactMouseEventFromHtml): Callback = {
        _parseWidth(e).fold(Callback.empty) { widthPx =>
          // stopPropagation() нужен, чтобы сигнал не продублировался в onQdTagResize()
          val heightPxOpt = OptionUtil.maybe(withHeight)(_parseHeight(e).get)
          ReactCommonUtil.stopPropagationCB(e) >>
            dispatchOnProxyScopeCB( $, QdEmbedResize( widthPx, qdOp, edgeDataJs.jdEdge.id, heightPx = heightPxOpt ) )
        }
      }

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      private def blocklessQdContentBoundsMeasuredJdCb(qdTag: JdTag, b: Bounds): Callback =
        dispatchOnProxyScopeCB( $, QdBoundsMeasured(qdTag, b) )

      /** Самописная поддержка ресайза контента только силами браузера. */
      private def onQdTagResize(qdTag: JdTag)(e: ReactMouseEventFromHtml): Callback = {
        _parseWidth(e).fold(Callback.empty) { widthPx =>
          dispatchOnProxyScopeCB( $, CurrContentResize( widthPx ) )
        }
      }

      /** Начало таскания qd-тега.
        * Бывают сложности с рассчётом координат. Особенно, если используется плитка.
        */
      private def qdTagDragStart(jdt: JdTag)(e: ReactDragEvent): Callback = {
        // Обязательно надо в setData() что-то передать.
        val mimes = MimeConst.Sio

        // Засунуть в состояние сериализованный инстанс таскаемого тега TODO с эджами, чтобы можно было перетаскивать за пределы этой страницы
        //e.dataTransfer.setData( mimes.JDTAG_JSON, Json.toJson(jdt).toString() )

        // Используем методику вычисления начального offset отсюда, т.е. через getComputedStyle(e.target)
        // http://jsfiddle.net/robertc/kKuqH/30/
        val srcEl = e.target.asInstanceOf[Element]
        val srcTgStyle = WindowVm().getComputedStyle( srcEl ).get

        // Функция-экстрактор целочисленных значений стилей по их названию.
        def __extractIntStyleProp(name: String): Int = {
          val valueStr = srcTgStyle.getPropertyValue(name)
          DataUtil
            .extractInt( valueStr )
            .get
        }

        val C = Css.Coord
        val srcLeft = __extractIntStyleProp( C.LEFT )
        val srcTop  = __extractIntStyleProp( C.TOP )

        val offsetXy = MCoords2dD(
          x = srcLeft - e.clientX,
          y = srcTop  - e.clientY
        )

        e.dataTransfer.setData( mimes.DATA_CONTENT_TYPE, mimes.DataContentTypes.CONTENT_ELEMENT )
        e.dataTransfer.setData( mimes.COORD_2D_JSON, Json.toJson(offsetXy).toString() )

        dispatchOnProxyScopeCB($, JdTagDragStart(jdt) )
      }

    }
    override def mkQdContentB = new QdContentB(_)

    // -------------------------------------------------------------------------------------------------


    class BlockB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends super.BlockB($) {

      override def _bgImgAddons(edge: MEdgeDataJs, state: MJdRrrProps): html_<^.TagMod = {
        // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
        TagMod(
          // Запретить таскать изображение, чтобы не мешать перетаскиванию strip'ов
          ^.draggable := false,
          _notifyImgWhOnEdit($, edge, state.jdArgs)
        )
      }

      override def _smBlockAddons(state: MJdRrrProps): html_<^.TagMod = {
        // Если текущий стрип выделен, то его можно таскать:
        ReactCommonUtil.maybe( state.isCurrentSelected && state.jdArgs.conf.isEdit ) {
          val s = state.subTree.rootLabel
          TagMod(
            _draggableUsing($, s, state.jdArgs)(stripDragStart(s)),
            ^.`class` := Css.Cursor.GRAB
          )
        }
      }

      /** Начинается перетаскивание целого стрипа. */
      private def stripDragStart(jdt: JdTag)(e: ReactDragEvent): Callback = {
        // Надо выставить в событие, что на руках у нас целый стрип.
        val mimes = MimeConst.Sio
        e.dataTransfer.setData( mimes.DATA_CONTENT_TYPE, mimes.DataContentTypes.STRIP )
        dispatchOnProxyScopeCB($, JdTagDragStart(jdt) )
      }

      private def jdStripDragOver(e: ReactDragEvent): Callback = {
        // В b9710f2 здесь была проверка cookie через getData, но webkit/chrome не поддерживают доступ в getData во время dragOver. Ппппппц.
        e.preventDefaultCB
      }

      /** Что-то было сброшено на указанный стрип. */
      private def onDropToStrip(s: JdTag)(e: ReactDragEvent): Callback = {
        val mimes = MimeConst.Sio

        val dataType = e.dataTransfer.getData( mimes.DATA_CONTENT_TYPE )
        val clientY = e.clientY

        val cb: Callback = if ( dataType ==* mimes.DataContentTypes.CONTENT_ELEMENT ) {
          // Перенос контента.
          val coordsJsonStr = e.dataTransfer.getData( mimes.COORD_2D_JSON )
          val clientX = e.clientX

          // Всё остальное (вне event) заносим в callback-функцию, чтобы максимально обленивить вычисления и дальнейшие действия.
          dispatchOnProxyScopeCBf($) { jdArgsProxy: ModelProxy[MJdRrrProps] =>
            // Узнать разницу между коодинатами мыши и левым верхним углом. Десериализовать из dataTransfer.
            val offsetXy = try {
              Json
                .parse( coordsJsonStr )
                .as[MCoords2dD]
            } catch {
              case ex: Throwable =>
                LOG.log(ErrorMsgs.DND_DROP_ERROR, ex)
                MCoords2dD(0, 0)
            }

            val szMultD = jdArgsProxy.value.jdArgs.conf.szMult.toDouble

            // Вычислить относительную координату в css-пикселях между точкой дропа и левой верхней точкой strip'а.
            // Считаем в client-координатах, т.к. рассчёты мгновенны, и client viewport не сдвинется за это время.
            // Если понадобятся page-координаты, то https://stackoverflow.com/a/37200339
            val topLeftXy = MCoords2di(
              x = ((clientX + offsetXy.x) / szMultD).toInt,
              y = ((clientY + offsetXy.y) / szMultD).toInt
            )

            JdDropContent(
              strip       = s,
              clXy        = topLeftXy,
              foreignTag  = None   // TODO Десериализовать из event, если элемент не принадлежит текущему документу.
            )
          }

        } else if (dataType ==* mimes.DataContentTypes.STRIP) {
          // Перетаскивание целого стрипа. Нужно вычислить, стрип дропнут выше или ниже он середины текущего стрипа.
          val tgEl = e.target.asInstanceOf[Element]
          dispatchOnProxyScopeCBf($) { _ =>
            // szMult тут учитывать не требуется, т.к. вся работа идёт в client-координатах.
            val clRect = tgEl.getBoundingClientRect()
            val pointerY = clientY - clRect.top
            val isUpper = pointerY < clRect.height / 2
            JdDropStrip(
              targetStrip = s,
              isUpper     = isUpper
            )
          }

        } else {
          LOG.log( WarnMsgs.DND_DROP_UNSUPPORTED, msg = e.dataTransfer.types.mkString(",") )
          Callback.empty
        }

        // тут нельзя stopPropagation - всё ломается.
        e.preventDefaultCB >> cb
      }

      private def _droppableOnEdit(jdt: JdTag, jdArgs: MJdArgs): TagMod = {
        ReactCommonUtil.maybe(jdArgs.conf.isEdit) {
          TagMod(
            ^.onDragOver ==> jdStripDragOver,
            ^.onDrop     ==> onDropToStrip(jdt)
          )
        }
      }

      override def _renderContentTag(propsProxy: ModelProxy[MJdRrrProps]): VdomTagOf[Div] = {
        val state = propsProxy.value
        val s = state.subTree.rootLabel
        super._renderContentTag(propsProxy)(
          _clickableOnEdit( $, s, state.jdArgs ),
          _droppableOnEdit( s, state.jdArgs )
        )
      }

    }
    override def mkBlockB = new BlockB(_)

  }


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

package io.suggest.jd.edit.v

import com.github.react.dnd._
import com.github.souporserious.react.measure.ContentRect
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.css.Css
import io.suggest.dev.MSzMult
import io.suggest.img.ImgUtilRJs
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.edit.m._
import io.suggest.jd.render.m._
import io.suggest.jd.render.v.{JdCssStatic, JdR, QdRrrHtml}
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.lk.r.img.LkImgUtilJs
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.MimeConst
import io.suggest.react.{Props2ModelProxy, ReactCommonUtil, ReactDiodeUtil}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import org.scalajs.dom.html
import org.scalajs.dom.raw.CSSStyleDeclaration
import scalacss.ScalaCssReact._
import scalacss.internal.{LengthUnit, Literal}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.2019 10:40
  * Description: Компонент для редактирования jd-карточек.
  */
class JdEditR(
               val jdR            : JdR,
               jdCssStatic        : JdCssStatic,
               imgRenderUtilJs    : LkImgUtilJs,
             )
  extends Log
{

  import MRrrEdit._

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
  private def _notifyImgWhOnEdit[P: Props2ModelProxy, S]($: BackendScope[P,S], edge: MEdgeDataJs): TagMod = {
    // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
    ReactCommonUtil.maybe( edge.fileJs.exists(_.whPx.isEmpty) ) {
      ^.onLoad ==> imgRenderUtilJs.notifyImageLoaded($, edge.id)
    }
  }


  private def _selectableOnClick[P: Props2ModelProxy, S]($: BackendScope[P, S])(p2RrrPF: P => MJdRrrProps): TagMod = {
    // В режиме редактирования -- надо слать инфу по кликам на стрипах
    ^.onClick ==> { e =>
      e.stopPropagationCB >>
        ReactDiodeUtil.dispatchOnProxyScopeCBf($) {
          props: P  =>
            val jdt = p2RrrPF(props).subTree.rootLabel
            JdTagSelect( jdt )
        }
    }
  }


  // Функция-экстрактор целочисленных значений стилей по их названию.
  private def __extractIntStyleProp(name: String, srcTgStyle: CSSStyleDeclaration): Int = {
    val valueStr = srcTgStyle.getPropertyValue(name)
    DataUtil
      .extractInt( valueStr )
      .get
  }


  /** Вычислить относительнюу topLeft-координату в начале перетаскивания. */
  private def _getDragElTopLeft(el: html.Element, mon: DragSourceMonitor): XY = {
    val srcTgStyle = WindowVm().getComputedStyle( el ).get

    val clientOffset = mon.getClientOffset()

    // TODO Надо считать сдвиг между top-left контента и указателем мыши, а тут считается сдвиг относительно клиентской области. Это проявится неправильным рассчётом, если произойдёт скролл во время перетаскивания.
    val srcLeft = __extractIntStyleProp( Literal.left, srcTgStyle ) - clientOffset.x
    val srcTop  = __extractIntStyleProp( Literal.top, srcTgStyle )  - clientOffset.y

    // Используем голый json вместо MCoords2di, т.к. есть риск, что значение MJsDropInfo может быть сериализовано.
    XY(x1 = srcLeft, y1 = srcTop)
  }


  /** Аддоны для обычной рендерилки, которые добавляют возможности редактирования карточки. */
  object JdRrrEdit extends jdR.JdRrrBase {

    class QdContentB(contentRef: Ref.Simple[html.Element],
                     $: BackendScope[MRrrEdit with MRrrEditCollectDrag, MJdRrrProps]) extends QdContentBase {

      override def _qdContentRrrHtml(p: MJdRrrProps): VdomElement = {
        QdRrrHtml(
          jdCssStatic = jdCssStatic,
          rrrProps    = p,
          // Для редактора: следует рендерить img-теги, подслушивая у них wh:
          imgEdgeMods = Some {
            _notifyImgWhOnEdit($, _)
          },
          // Выбранный qd-тег можно ресайзить:
          resizableCb = OptionUtil.maybe( p.isCurrentSelected ) {
            onQdEmbedResize(_, _, _)(_)
          },
        )
          .render()
      }

      override def _renderQdContentTag(state: MJdRrrProps): TagOf[html.Element] = {
        val qdTag = state.subTree.rootLabel

        // Нельзя withRef() для этого внешнего тега, т.к. другой measure-ref выставляется внутри super._renderQdContentTag().
        super._renderQdContentTag(state)(
          _maybeSelected(qdTag, state.jdArgs),
          _selectableOnClick( $ )( _.p.value ),

          /* TODO унести в react-dnd spec.canDrag()
          ReactCommonUtil.maybe(
            !state.parent.exists(state.jdArgs.selJdt.treeLocOpt.containsLabel)
          ) {
            _draggableUsing($) { qdTagDragStart }
          },
           */

          // Рендерить особые указатели мыши в режиме редактирования.
          if (state.isCurrentSelected) {
            // Текущий тег выделен. Значит, пусть будет move-указатель
            TagMod(
              ^.`class` := Css.flat( Css.Overflow.HIDDEN, Css.Cursor.MOVE ),
              jdCssStatic.horizResizable,
              // TODO onResize -> ...
              ^.onMouseUp ==> onQdTagResize,
            )
          } else {
            // Текущий тег НЕ выделен. Указатель обычной мышкой.
            ^.`class` := Css.Cursor.POINTER
          },
        )
      }


      override def _doRender(state: MJdRrrProps): TagOf[html.Element] = {
        // ref для react-dnd можно выставлять только здесь.
        super._doRender(state)
          .withRef( contentRef )
      }

      /** Callback ресайза. */
      private def onQdEmbedResize(qdOp: MQdOp, edgeDataJs: MEdgeDataJs, withHeight: Boolean)
                                 (e: ReactMouseEventFromHtml): Callback = {
        _parseWidth(e).fold(Callback.empty) { widthPx =>
          // stopPropagation() нужен, чтобы сигнал не продублировался в onQdTagResize()
          val heightPxOpt = OptionUtil.maybe(withHeight)(_parseHeight(e).get)
          e.stopPropagationCB >>
            ReactDiodeUtil.dispatchOnProxyScopeCB( $, QdEmbedResize( widthPx, qdOp, edgeUid = edgeDataJs.jdEdge.id, heightPx = heightPxOpt ) )
        }
      }

      /** Самописная поддержка ресайза контента только силами браузера. */
      private def onQdTagResize(e: ReactMouseEventFromHtml): Callback = {
        _parseWidth(e).fold(Callback.empty) { widthPx =>
          ReactDiodeUtil.dispatchOnProxyScopeCB( $, ResizeContent( Some(widthPx) ) )
        }
      }

      def render(props: MRrrEdit with MRrrEditCollectDrag, state: MJdRrrProps): VdomElement = {
        props.dragF
          .applyVdomEl( _doRender(state) )
      }

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      override def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback = {
        ReactDiodeUtil.dispatchOnProxyScopeCBf($) {
          props: MRrrEdit with MRrrEditCollectDrag =>
            _qdBoundsMeasured( props.p, timeStampMs, cr )
        }
      }

    }

    /** Инстанс функции canDrag(), используемый во всех инстансах QdContentDndB. */
    private lazy val _qdCanDragF: js.Function2[MRrrEdit with MRrrEditCollectDrag, DragSourceMonitor, Boolean] = {
      (props, mon) =>
        // Не таскать, если сейчас выделен родительский элемент.
        val p = props.p.value
        !p.parent.exists( p.jdArgs.selJdt.treeLocOpt.containsLabel )
    }

    class QdContentDndB($: BackendScope[ModelProxy[MJdRrrProps], Unit]) {
      // import, иначе будет рантаймовая ошибка валидации DragSourceSpec (лишние поля json-класса)
      import MimeConst.Sio.{DataContentTypes => DCT}

      private val contentRef = Ref[html.Element]


      private val _qdBeginDragF: js.Function3[MRrrEdit with MRrrEditCollectDrag, DragSourceMonitor, js.Any, MJsDropInfo] = {
        (props, mon, _) =>
          // Запустить обработку по circuit в фоне. По логике кажется, что должно быть асинхронно, но не факт: рендер перетаскивания может нарушаться.
          val s = props.p.value
          val jdt = s.subTree.rootLabel
          props.p dispatchNow JdTagDragStart(jdt)

          val el = contentRef.unsafeGet()
          val xyOff: XY = if (s.parent.exists(_.name ==* MJdTagNames.STRIP)) {
            // Контент внутри блока.
            _getDragElTopLeft(el, mon)
          } else {
            // qd-blockless. Ищем координаты для родительского контейнера.
            _getDragElTopLeft(el.parentElement, mon)
          }

          // Отрендерить в json данные, которые будут переданы в DropTarget.
          MJsDropInfo( DCT.CONTENT_ELEMENT, xyOff )
      }

      val contentDndComp = DragSource[MRrrEdit, MRrrEditCollectDrag, MJsDropInfo, Children.None](
        itemType = DCT.CONTENT_ELEMENT,
        spec = new DragSourceSpec[MRrrEdit with MRrrEditCollectDrag, MJsDropInfo] {
          override val beginDrag = _qdBeginDragF
          override val canDrag   = _qdCanDragF
        },
        collect = { (conn, mon) =>
          // Пробросить collecting function + какие-то возможные данные к общим props'ам.
          new MRrrEditCollectDrag {
            override val dragF = conn.dragSource()
          }
        }
      )(
        // Компонент-обёртка, который просто вызывает функцию dropTarget() над исходным document-компонентом.
        ScalaComponent
          .builder[MRrrEdit with MRrrEditCollectDrag]( classOf[QdContentB].getSimpleName )
          .initialStateFromProps( rrrEdit2mproxyValueF )
          .backend( new QdContentB(contentRef, _) )
          .renderBackend
          .configure( ReactDiodeUtil.p2sShouldComponentUpdate(rrrEdit2mproxyValueF)(MJdRrrProps.MJdRrrPropsFastEq) )
          .build
          .toJsComponent
          .raw
      )
        .cmapCtorProps( _rrrPropsCMap )

      def render(props: ModelProxy[MJdRrrProps]): VdomElement =
        contentDndComp(props)
    }
    val qdContentDndComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[QdContentDndB].getSimpleName )
      .renderBackend[QdContentDndB]
      .build
    override def mkQdContent(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      qdContentDndComp.withKey(key)(props)


    // -------------------------------------------------------------------------------------------------

    class BlockB(blockRef: Ref.Simple[html.Div], $: BackendScope[MRrrEdit with MRrrEditCollectDrag with MRrrEditCollectDrop, MJdRrrProps]) extends BlockBase {

      override def _bgImgAddons(bgImgData: MJdEdgeId, edge: MEdgeDataJs, state: MJdRrrProps): TagMod = {
        TagMod(
          super._bgImgAddons(bgImgData, edge, state),

          // Запретить таскать изображение, чтобы не мешать перетаскиванию strip'ов
          ^.draggable := false,

          // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
          _notifyImgWhOnEdit($, edge)
        )
      }

      override def _smBlockAddons(state: MJdRrrProps): TagMod = {
        // Если текущий стрип выделен, то его можно таскать:
        ReactCommonUtil.maybe( state.isCurrentSelected ) {
          TagMod(
            //_draggableUsing($)(blockDragStart),
            ^.`class` := Css.Cursor.GRAB
          )
        }
      }

      override def _outerContainerAddons(state: MJdRrrProps): TagMod = {
        _maybeSelected( state.subTree.rootLabel, state.jdArgs )
      }

      override def _renderBlockTag(propsProxy: ModelProxy[MJdRrrProps]): TagOf[html.Div] = {
        super._renderBlockTag(propsProxy)(
          _selectableOnClick( $ )(_.p.value),
          // Поддержка drop:
          //^.onDragOver ==> jdStripDragOver,
          //^.onDrop     ==> onDropToStrip
        )
          .withRef( blockRef )
      }

      def render(props: MRrrEdit with MRrrEditCollectDrag with MRrrEditCollectDrop, state: MJdRrrProps): VdomElement = {
        props.dropF.applyVdomEl(
          props.dragF.applyVdomEl(
            _renderBlockTag(props.p)
          )
        )
      }

    }


    /** Инстанс функции canDrag() для BlockDndB. */
    private lazy val _blockCanDragF: js.Function2[MRrrEdit with MRrrEditCollectDrop with MRrrEditCollectDrag, DragSourceMonitor, Boolean] = {
      (props, mon) =>
        // Не таскать, если сейчас выделен какой-то иной элемент.
        val p = props.p.value
        p.isCurrentSelected
    }

    /** Компонент блока для jd-редактора.
      * Т.к. нужны ref'ы, то надо их изолировать между инстансами.
      */
    class BlockDndB($: BackendScope[ModelProxy[MJdRrrProps], Unit]) {
      import MimeConst.Sio.{DataContentTypes => DCT}

      private val blockRef = Ref[html.Div]

      private val _blockDropF: js.Function3[MRrrEdit with MRrrEditCollectDrop, DropTargetMonitor, js.Any, js.UndefOr[MJsDropInfo]] = {
        (props, mon, _) =>
          if (!mon.didDrop()) {
            val itemType = mon.getItemType()

            if (itemType == (DCT.CONTENT_ELEMENT: DropAccept_t_0)) {
              // Сброшен qd-контент на текущий блок. Высчитать примерные координаты сброса.
              val itm = mon.getItem().asInstanceOf[MJsDropInfo]

              val rrr = props.p.value
              val szMultD = rrr.jdArgs.conf.szMult.toDouble

              val pointerClXy = mon.getClientOffset()

              // Вычислить относительную координату в css-пикселях между точкой дропа и левой верхней точкой strip'а.
              // Считаем в client-координатах, а если понадобятся page-координаты, то https://stackoverflow.com/a/37200339
              def __calcCoord(coordF: XY => Double): Int = {
                val ptrCoord = coordF(pointerClXy)
                val coord2 = itm.xy.fold(ptrCoord) { xy =>
                  ptrCoord + coordF(xy)
                }
                (coord2 / szMultD).toInt
              }
              val topLeftXy = MCoords2di(
                x = __calcCoord(_.x),
                y = __calcCoord(_.y),
              )

              props.p dispatchNow JdDropToBlock(
                targetBlock       = rrr.subTree.rootLabel,
                clXy        = topLeftXy,
                foreignTag  = None   // TODO Решить, должно ли тут быть что-либо?
              )
            }
          }

          js.undefined
      }

      val blockDndComp = DropTarget[MRrrEdit, MRrrEditCollectDrop, MJsDropInfo, Children.None](
        // Сверху на блок можно скидывать qd-контент
        itemType = DCT.CONTENT_ELEMENT,
        spec = new DropTargetSpec[MRrrEdit with MRrrEditCollectDrop, MJsDropInfo] {
          override val drop = js.defined( _blockDropF )
        },
        collect = { (conn, mon) =>
          new MRrrEditCollectDrop {
            override val dropF = conn.dropTarget()
          }
        }
      ) {

        /** Реакция на начало перетаскивания qd-контента. */
        val _beginDragF: js.Function3[MRrrEdit with MRrrEditCollectDrop with MRrrEditCollectDrag, DragSourceMonitor, js.Any, MJsDropInfo] = {
          (props, monitor, _) =>
            // Запустить обработку по circuit. По логике кажется, что должно быть асинхронно, но рендер перетаскивания может нарушаться.
            val jdt = props.p.value.subTree.rootLabel
            props.p dispatchNow JdTagDragStart(jdt)
            // Отрендерить в json данные, которые будут переданы в DropTarget.
            MJsDropInfo( DCT.STRIP )
        }

        DragSource[MRrrEdit with MRrrEditCollectDrop, MRrrEditCollectDrag, MJsDropInfo, Children.None](
          itemType = DCT.STRIP,
          spec = new DragSourceSpec[MRrrEdit with MRrrEditCollectDrop with MRrrEditCollectDrag, MJsDropInfo] {
            override val beginDrag = _beginDragF
            override val canDrag   = _blockCanDragF
          },
          collect = { (conn, mon) =>
            // Пробросить collecting function + какие-то возможные данные к общим props'ам.
            new MRrrEditCollectDrag {
              override val dragF = conn.dragSource()
            }
          },
        )(
          ScalaComponent
            .builder[MRrrEdit with MRrrEditCollectDrop with MRrrEditCollectDrag]( classOf[BlockB].getSimpleName )
            .initialStateFromProps( rrrEdit2mproxyValueF )
            .backend( new BlockB(blockRef, _) )
            .renderBackend
            .configure( ReactDiodeUtil.p2sShouldComponentUpdate( rrrEdit2mproxyValueF.compose[MRrrEdit with MRrrEditCollectDrop with MRrrEditCollectDrag](identity) )(MJdRrrProps.MJdRrrPropsFastEq) )
            .build
            .toJsComponent
            .raw
        )
          .toJsComponent
          .raw
      }
        .cmapCtorProps( _rrrPropsCMap )

      def render(props: ModelProxy[MJdRrrProps]): VdomElement =
        blockDndComp(props)
    }
    val blockDndComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockDndB].getSimpleName )
      .renderBackend[BlockDndB]
      .build
    override def mkBlock(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      blockDndComp.withKey(key)(props)


    // -------------------------------------------------------------------------------------------------

    /** Сборка документа под редактор. */
    class DocumentB(docRef: Ref.Simple[html.Div], $: BackendScope[MRrrEdit with MRrrEditCollectDrop, Unit]) extends DocumentBase {

      // Т.к. react-dnd присылает null вместо компонента, то узнать относительные координаты
      // в момент перетаскивания получается невозможно.

      def render(p: MRrrEdit with MRrrEditCollectDrop): VdomElement = {
        p.dropF.applyVdomEl(
          // div-обёртка нужна, т.к. react-dnd требует нативных элементов: голые компоненты не принимаются.
          <.div(
            _renderGrid(p.p)
          )
            .withRef( docRef )
        )
      }

    }
    class DocumentDndB($: BackendScope[ModelProxy[MJdRrrProps], Unit]) {
      // Для получения относительных координат, используем дополнительный ref.
      private val docRef = Ref[html.Div]

      import MimeConst.Sio.{DataContentTypes => DCT}

      // Нативный drop-callback. Собирается отдельно из-за проблем со spec-валидацией react и https://github.com/scala-js/scala-js/issues/2748
      private val _dropF: js.Function3[MRrrEdit with MRrrEditCollectDrop, DropTargetMonitor, js.Any, js.UndefOr[MJsDropInfo]] = {
        (props, mon, _) =>
          if (!mon.didDrop()) {
            val itype = mon.getItemType()

            // Это перетаскивание целого блока внутри редактора.
            // Нужно узнать сдвиг указателя относительно верхнего левого угла документа.
            val docEl = docRef.unsafeGet()
            val docRect = docEl.getBoundingClientRect()

            // Узнать szMult для нормирования координат относительно документа.
            val p = props.p.value
            val szMultOpt = p.jdArgs.conf.szMult.ifNot1
            val szMultedF = MSzMult.szMultedF()
              .applyDouble(_: Double, szMultOpt)
              .toInt

            val mouseClXy = mon.getClientOffset()
            val docDropXy = MCoords2di(
              x = szMultedF( mouseClXy.x - docRect.left ),
              y = szMultedF( mouseClXy.y - docRect.top  ),
            )

            props.p dispatchNow JdDropToDocument(
              docXy     = docDropXy,
              dropItem  = mon.getItem()
            )
          }
          js.undefined
      }

      /** Документ должен поддерживать сброс элемента при перетаскивании. */
      val _documentDndWrapperComponent = DropTarget[MRrrEdit, MRrrEditCollectDrop, MJsDropInfo, Children.None](
        itemType = js.Array[DropAccept_t_0](
          DCT.CONTENT_ELEMENT,
          DCT.STRIP,
        ): DropAccept_t_1,

        spec = new DropTargetSpec[MRrrEdit with MRrrEditCollectDrop, MJsDropInfo] {
          override val drop = js.defined( _dropF )
        },

        collect = { (conn, mon) =>
          // Пробросить collecting function + какие-то возможные данные к общим props'ам.
          new MRrrEditCollectDrop {
            override val dropF = conn.dropTarget()
          }
        },
      )(
        // Компонент-обёртка, который просто вызывает функцию dropTarget() над исходным document-компонентом.
        ScalaComponent
          .builder [MRrrEdit with MRrrEditCollectDrop] ( classOf[DocumentB].getSimpleName )
          .backend(new DocumentB(docRef, _))
          .renderBackend
          .build
          .toJsComponent
          .raw
      )
        .cmapCtorProps( _rrrPropsCMap )

      def render(props: ModelProxy[MJdRrrProps]): VdomElement =
        _documentDndWrapperComponent(props)
    }
    val documentDndComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[DocumentDndB].getSimpleName )
      .renderBackend[DocumentDndB]
      .build
    override def mkDocument(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      documentDndComp.withKey(key)(props)

  }


  /** Расшаренный инстанс общей функции для всех вызовов DndComponent().cmapCtorProps().
    * Функция оборачивает ModelProxy в инстанс MRrrEdit.
    */
  private val _rrrPropsCMap: (ModelProxy[MJdRrrProps] => MRrrEdit) = {
    proxy =>
      new MRrrEdit {
        override val p = proxy
      }
  }

  private val rrrEdit2mproxyValueF = ReactDiodeUtil.modelProxyValueF.compose[MRrrEdit with MRrrEditCollectDrag](_.p)

}


/** Боксинг для scala-пропертисов, который необходим для react-dnd HOC. */
trait MRrrEdit extends js.Object {
  /** Исходные scala-пропертисы. */
  val p: ModelProxy[MJdRrrProps]
}
object MRrrEdit {
  implicit object MRrrEdit2MProxy extends Props2ModelProxy[MRrrEdit] {
    override def apply(v1: MRrrEdit): ModelProxy[_] = v1.p
  }
  implicit def mprox: Props2ModelProxy[MRrrEdit with MRrrEditCollectDrag] =
    MRrrEdit2MProxy.asInstanceOf[Props2ModelProxy[MRrrEdit with MRrrEditCollectDrag]]
}
/** Доп.пропертисы к [[MRrrEdit]], инжектируемые из collect-function для DropTarget. */
trait MRrrEditCollectDrop extends js.Object {
  /** Функция активации react-dnd над vdom-тегом. */
  val dropF: DropTargetF
}
/** Доп.пропертисы к [[MRrrEdit]], инжектируемые из collect-function для DragSource. */
trait MRrrEditCollectDrag extends js.Object {
  val dragF: DragSourceF[DragSourceFOptions]
}

/** Инфа по перетаскиваемому элементу для react-dnd. */
trait MJsDropInfo extends IItem {
  val xy: js.UndefOr[XY] = js.undefined
}
object MJsDropInfo {

  def apply(typ: DropAccept_t_0, coords: js.UndefOr[XY] = js.undefined ) : MJsDropInfo = {
    new MJsDropInfo {
      override val `type` = typ
      override val xy     = coords
    }
  }

}

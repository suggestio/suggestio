package io.suggest.jd.edit

import com.github.souporserious.react.measure.{Bounds, Measure}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.{MCoords2dD, MCoords2di}
import io.suggest.css.Css
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.render.m._
import io.suggest.jd.render.v.{JdCssStatic, JdR, QdRrrHtml}
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.lk.r.img.ImgRenderUtilJs
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.MimeConst
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.univeq._
import org.scalajs.dom.{Element, html}
import org.scalajs.dom.raw.CSSStyleDeclaration
import play.api.libs.json.Json
import scalacss.ScalaCssReact._
import scalacss.internal.LengthUnit

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.2019 10:40
  * Description: Компонент для редактирования jd-карточек.
  */
class JdEditR(
               val jdR            : JdR,
               jdCssStatic        : JdCssStatic,
               imgRenderUtilJs    : ImgRenderUtilJs,
             )
  extends Log
{

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
  private def _notifyImgWhOnEdit[P <: ModelProxy[_], S]($: BackendScope[P,S], edge: MEdgeDataJs, jdArgs: MJdArgs): TagMod = {
    // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
    ReactCommonUtil.maybe( jdArgs.conf.isEdit && edge.fileJs.exists(_.whPx.isEmpty) ) {
      ^.onLoad ==> imgRenderUtilJs.notifyImageLoaded($, edge.id)
    }
  }


  private def _clickableOnEdit[P <: ModelProxy[_], S]($: BackendScope[P,S], jdt: JdTag, jdArgs: MJdArgs): TagMod = {
    // В режиме редактирования -- надо слать инфу по кликам на стрипах
    ReactCommonUtil.maybe(jdArgs.conf.isEdit) {
      ^.onClick ==> { e =>
        e.stopPropagationCB >>
          dispatchOnProxyScopeCB($, JdTagSelect(jdt) )
      }
    }
  }


  private def _draggableUsing[P <: ModelProxy[_], S]($: BackendScope[P,S], jdt: JdTag, jdArgs: MJdArgs)
                                                    (onDragStartF: ReactDragEvent => Callback): TagMod = {
    TagMod(
      ^.draggable := true,
      ^.onDragStart ==> onDragStartF,
      ^.onDragEnd   --> dispatchOnProxyScopeCB($, JdTagDragEnd )
    )
  }


  /** Аддоны для обычной рендерилки, которые добавляют возможности редактирования карточки. */
  trait JdRrrEdit extends jdR.JdRrr {

    class QdContentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends super.QdContentB($) {

      override def _qdContentRrrHtml(p: MJdRrrProps): QdRrrHtml = {
        QdRrrHtml(
          jdCssStatic = jdCssStatic,
          rrrProps    = p,
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

      override def _renderQdContentTag(state: MJdRrrProps): TagOf[html.Div] = {
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

      override def _bgImgAddons(bgImgData: MJdEdgeId, edge: MEdgeDataJs, state: MJdRrrProps): TagMod = {
        val s = state.subTree.rootLabel
        // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
        TagMod(

          // Размеры и центровка картинки в редакторе эмулируется на основе оригинала.
          imgRenderUtilJs
            // Размеры и позиционирование фоновой картинки в блоке (эмуляция кропа):
            .htmlImgCropEmuAttrsOpt(
              cropOpt     = bgImgData.crop,
              outerWhOpt  = s.props1.bm,
              origWhOpt   = edge.origWh,
              szMult      = state.jdArgs.conf.szMult
            )
            .getOrElse {
              super._bgImgAddons(bgImgData, edge, state)
            },

          // Запретить таскать изображение, чтобы не мешать перетаскиванию strip'ов
          ^.draggable := false,
          _notifyImgWhOnEdit($, edge, state.jdArgs)
        )
      }

      override def _smBlockAddons(state: MJdRrrProps): TagMod = {
        // Если текущий стрип выделен, то его можно таскать:
        ReactCommonUtil.maybe( state.isCurrentSelected && state.jdArgs.conf.isEdit ) {
          val s = state.subTree.rootLabel
          TagMod(
            _draggableUsing($, s, state.jdArgs)(stripDragStart(s)),
            ^.`class` := Css.Cursor.GRAB
          )
        }
      }

      override def _outerContainerAddons(state: MJdRrrProps): TagMod = {
        _maybeSelected( state.subTree.rootLabel, state.jdArgs )
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

      override def _renderBlockTag(propsProxy: ModelProxy[MJdRrrProps]): TagOf[html.Div] = {
        val state = propsProxy.value
        val s = state.subTree.rootLabel
        super._renderBlockTag(propsProxy)(
          _clickableOnEdit( $, s, state.jdArgs ),
          _droppableOnEdit( s, state.jdArgs )
        )
      }

    }
    override def mkBlockB = new BlockB(_)

  }

  /** Дефолтовая реализация [[JdRrrEdit]]. */
  object JdRrrEdit extends JdRrrEdit

}

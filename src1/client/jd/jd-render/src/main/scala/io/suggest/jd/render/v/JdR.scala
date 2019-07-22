package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter._
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.geom.coord.{MCoords2dD, MCoords2di}
import io.suggest.common.html.HtmlConstants
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
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import org.scalajs.dom.{Element, html}
import org.scalajs.dom.raw.CSSStyleDeclaration
import play.api.libs.json.Json
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
           imgRenderUtilJs    : ImgRenderUtilJs,
           jdGridUtil         : JdGridUtil
         )
  extends Log
{ jdR =>

  import MJdArgs.MJdArgsFastEq

  type Props_t = MJdArgs
  type Props = ModelProxy[Props_t]


  /** Движок рендера для сборки под разные ситуации. Можно вынести за пределы компонента. */
  trait JdRenderer {

    protected def jdTagClick(jdt: JdTag)(e: ReactMouseEvent): Callback

    protected def qdTagDragStart(jdt: JdTag)(e: ReactDragEvent): Callback

    protected def stripDragStart(jdt: JdTag)(e: ReactDragEvent): Callback

    protected def jdTagDragEnd(jdt: JdTag)(e: ReactDragEvent): Callback

    protected def jdStripDragOver(e: ReactDragEvent): Callback

    protected def onDropToStrip(s: JdTag)(e: ReactDragEvent): Callback

    protected def onNewImageLoaded(edgeUid: EdgeUid_t)(e: ReactEvent): Callback

    protected def onQdTagResize(qdTag: JdTag)(e: ReactMouseEventFromHtml): Callback

    protected def onQdEmbedResize(qdOp: MQdOp, edgeDataJs: MEdgeDataJs, withHeight: Boolean)(e: ReactMouseEventFromHtml): Callback


    /** Отрендерить дочерние элементы тега обычным методом.
      *
      * @param dt Jd-тег
      * @param jdArgs Аргументы рендера.
      * @return Итератор отрендеренных vdom-узлов.
      */
    def renderChildren(dt: Tree[JdTag], jdArgs: MJdArgs): Iterator[VdomNode] = {
      renderChildrenUsing(dt) { (childJdTree, i) =>
        renderTag(childJdTree, jdArgs, i, dt.rootLabel)
      }
    }

    /** Рендер дочерних элементов указанного тега, используя произвольную фунцию.
      *
      * @param dt Текущий jd-тег с деревом.
      * @param f Функция рендера.
      * @return Итератор отрендеренных vdom-узлов.
      */
    def renderChildrenUsing(dt: Tree[JdTag])(f: (Tree[JdTag], Int) => VdomNode): Iterator[VdomNode] = {
      dt.subForest
        .iterator
        .zipWithIndex
        .map( f.tupled )
    }


    /**
      * Является ли указанный тег текущим выделенным?
      * Если да, то присвоить ему соотв.стиль для выделения визуально.
      */
    private def _maybeSelected(dt: JdTag, jdArgs: MJdArgs): TagMod = {
      // Если происходит перетаскивание, то нужно избавляться от рамок: так удобнее.
      ReactCommonUtil.maybe(
        jdArgs.renderArgs.dnd.jdt.isEmpty &&
        jdArgs.selJdt.treeLocOpt.containsLabel(dt)
      ) {
        jdCssStatic.selectedTag
      }
    }

    /** Ротация элемента. */
    private def _maybeRotate(jdt: JdTag, jdArgs: MJdArgs): TagMod = {
      jdt.props1.rotateDeg
        .whenDefined { jdArgs.jdCss.rotateF.apply }
    }

    private def _clickableOnEdit(jdt: JdTag, jdArgs: MJdArgs): TagMod = {
      // В режиме редактирования -- надо слать инфу по кликам на стрипах
      ReactCommonUtil.maybe(jdArgs.conf.isEdit) {
        ^.onClick ==> jdTagClick(jdt)
      }
    }


    private def _droppableOnEdit(jdt: JdTag, jdArgs: MJdArgs): TagMod = {
      ReactCommonUtil.maybe(jdArgs.conf.isEdit) {
        TagMod(
          ^.onDragOver ==> jdStripDragOver,
          ^.onDrop     ==> onDropToStrip(jdt)
        )
      }
    }

    private def _draggableUsing(jdt: JdTag, jdArgs: MJdArgs)(onDragStartF: ReactDragEvent => Callback): TagMod = {
      TagMod(
        ^.draggable := true,
        ^.onDragStart ==> onDragStartF,
        ^.onDragEnd   ==> jdTagDragEnd(jdt)
      )
    }


    /** Повесить onload для картинки, считывающий ей wh.
      * Нужно, чтобы редактор мог узнать wh оригинала изображения. */
    private def _notifyImgWhOnEdit(e: MEdgeDataJs, jdArgs: MJdArgs): TagMod = {
      // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
      ReactCommonUtil.maybe( jdArgs.conf.isEdit && e.fileJs.exists(_.whPx.isEmpty) ) {
        ^.onLoad ==> onNewImageLoaded(e.id)
      }
    }


    /** Рендер strip, т.е. одной "полосы" контента. */
    def renderStrip(stripTree: Tree[JdTag], i: Int, jdArgs: MJdArgs): TagOf[html.Div] = {
      val s = stripTree.rootLabel
      val C = jdArgs.jdCss
      val isSelected = jdArgs.selJdt.treeLocOpt.containsLabel(s)
      val isEditSelected = isSelected && jdArgs.conf.isEdit

      val isWide = s.props1.bm.map(_.wide).getOrElseFalse

      val bgColor = _bgColorOpt(s, jdArgs)

      val groupOutlineTm = jdArgs.renderArgs
        .groupOutLined
        .whenDefined { mcd =>
          TagMod(
            C.blockGroupOutline,
            ^.outlineColor := mcd.hexCode
          )
        }

      val bgImgOpt = for {
        bgImgData <- s.props1.bgImg
        edge      <- jdArgs.edges.get( bgImgData.edgeUid )
        if edge.jdEdge.predicate ==>> MPredicates.JdContent.Image
        bgImgSrc  <- edge.origImgSrcOpt
      } yield {
        <.img(
          ^.`class` := Css.Block.BG,
          ^.src := bgImgSrc,

          // Запретить таскать изображение, чтобы не мешать перетаскиванию strip'ов
          ReactCommonUtil.maybe( jdArgs.conf.isEdit ) {
            ^.draggable := false
          },

          imgRenderUtilJs
            // Размеры и позиционирование фоновой картинки в блоке (эмуляция кропа):
            .htmlImgCropEmuAttrsOpt(
              cropOpt     = bgImgData.crop,
              outerWhOpt  = s.props1.bm,
              origWhOpt   = edge.origWh,
              szMult      = jdArgs.conf.szMult
            )
            .getOrElse {
              // Просто заполнение всего блока картинкой. Т.к. фактический размер картинки отличается от размера блока
              // на px ratio, надо подогнать картинку по размерам:
              C.stripBgStyleF(s)
            },

          // Если js-file загружен, но wh неизвестна, то сообщить наверх ширину и длину загруженной картинки.
          _notifyImgWhOnEdit(edge, jdArgs),

          // В jdArgs может быть задан дополнительные модификации изображения, если selected tag.
          jdArgs.renderArgs.selJdtBgImgMod
            .filter(_ => isSelected)
            .whenDefined
        )
      }
      val bgImgTm = bgImgOpt.whenDefined

      val keyAV = {
        ^.key := i.toString
      }

      val maybeSelAV = _maybeSelected( s, jdArgs )

      // Скрывать не-main-стрипы, если этого требует рендер.
      // Это касается только стрипов, у которых нет isMain = Some(true)
      val hideNonMainStrip = ReactCommonUtil.maybe(
        jdArgs.renderArgs.hideNonMainStrips &&
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

        // Если текущий стрип выделен, то его можно таскать.
        ReactCommonUtil.maybe( isEditSelected ) {
          TagMod(
            _draggableUsing(s, jdArgs)(stripDragStart(s)),
            ^.`class` := Css.Cursor.GRAB
          )
        },

        // Если задана фоновая картинка, от отрендерить её.
        bgImgTm.unless(isWide),

        renderChildren( stripTree, jdArgs )
          .toVdomArray
      )

      val tmOuter = if (isWide) {
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

      tmOuter(
        _clickableOnEdit( s, jdArgs ),
        _droppableOnEdit( s, jdArgs )
      )
    }


    /** Выполнить рендер текущего документа, переданного в jdArgs. */
    def renderDocument(i: Int = 0, jdArgs: MJdArgs): TagOf[html.Div] = {
      renderDocument( jdArgs.template, i, jdArgs )
    }

    /** Рендер указанного документа. */
    def renderDocument(jd: Tree[JdTag], i: Int, jdArgs: MJdArgs): TagOf[html.Div]  = {
      <.div(
        ^.key := i.toString,

        // Плитку отсюда полностью вынести не удалось.
        CSSGrid {
          jdGridUtil.mkCssGridArgs(
            gbRes = GridBuilderUtil.buildGrid {
              MGridBuildArgs(
                itemsExtDatas = jdGridUtil
                  .jdTrees2bms(jd.subForest)
                  .map { bm =>
                    MGbBlock( None, bm )
                  }
                  .toList,
                jdConf = jdArgs.conf,
                offY = 0
              )
            },
            conf = jdArgs.conf,
            tagName = GridComponents.DIV
          )
        } (
          renderChildren(jd, jdArgs)
            .toSeq: _*
        )
      )
    }

    private def _bgColorOpt(jdTag: JdTag, jdArgs: MJdArgs): TagMod = {
      jdTag.props1.bgColor.whenDefined { mcd =>
        jdArgs.jdCss.bgColorOptStyleF( mcd.hexCode )
      }
    }

    /** Рендер перекодированных данных quill delta.
      *
      * @param qdTagTree Тег с кодированными данными Quill delta.
      * @return Элемент vdom.
      */
    def renderQd(qdTagTree: Tree[JdTag], i: Int, jdArgs: MJdArgs, parent: JdTag): TagOf[html.Div] = {
      val qdTag = qdTagTree.rootLabel
      val isCurrentSelected = jdArgs.selJdt.treeLocOpt containsLabel qdTag
      val tagMods = {
        val qdRrr = new QdRrrHtml(
          jdCssStatic = jdCssStatic,
          jdArgs      = jdArgs,
          qdTag       = qdTagTree,
          // Для редактора: следует рендерить img-теги, подслушивая у них wh:
          imgEdgeMods = OptionUtil.maybe( jdArgs.conf.isEdit ) {
            _notifyImgWhOnEdit(_, jdArgs)
          },
          // Выбранный qd-тег можно ресайзить:
          resizableCb = OptionUtil.maybe(isCurrentSelected) {
            onQdEmbedResize(_, _, _)(_)
          }
        )
        qdRrr.render()
      }
      <.div(
        ^.key := i.toString,

        // Опциональный цвет фона
        _bgColorOpt( qdTag, jdArgs ),

        // Опциональная ротация элемента.
        _maybeRotate( qdTag, jdArgs ),

        _maybeSelected(qdTag, jdArgs),
        _clickableOnEdit(qdTag, jdArgs),

        // Поддержка перетаскивания
        jdCssStatic.absPosStyleAll,

        ReactCommonUtil.maybe(
          jdArgs.conf.isEdit &&
          !jdArgs.selJdt.treeLocOpt.containsLabel(parent)
        ) {
          _draggableUsing(qdTag, jdArgs) { qdTagDragStart(qdTag) }
        },
        jdArgs.jdCss.absPosStyleF(parent -> qdTag),

        // CSS-класс принудительной ширины, если задан.
        ReactCommonUtil.maybe( qdTag.props1.widthPx.nonEmpty ) {
          jdArgs.jdCss.forcedWidthStyleF(qdTag)
        },

        // Стиль для теней
        ReactCommonUtil.maybe( qdTag.props1.textShadow.nonEmpty ) {
          jdArgs.jdCss.contentShadowF( qdTag )
        },

        // Рендерить особые указатели мыши в режиме редактирования.
        ReactCommonUtil.maybe(jdArgs.conf.isEdit) {
          if (isCurrentSelected) {
            // Текущий тег выделен. Значит, пусть будет move-указатель
            TagMod(
              ^.`class` := Css.flat( Css.Overflow.HIDDEN, Css.Cursor.MOVE ),
              jdCssStatic.horizResizable,
              // TODO onResize -> ...
              ^.onMouseUp ==> onQdTagResize(qdTag)
            )

          } else {
            // Текущий тег НЕ выделен. Указатель обычной мышкой.
            ^.`class` := Css.Cursor.POINTER
          }
        },

        tagMods
      )
    }


    /** QD_OP: Должен быть отрабатон внутри QD_CONTENT: */
    def renderQdOp(qdOp: Tree[JdTag], i: Int, jdArgs: MJdArgs): TagOf[html.Div] = {
      //LOG.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = (l.name, l) )
      // VdomNullElement
      throw new IllegalStateException( ErrorMsgs.SHOULD_NEVER_HAPPEN + HtmlConstants.SPACE + qdOp.rootLabel )
    }


    /**
      * Запуск рендеринга произвольных тегов.
      */
    // TODO parent может быть необязательным. Но это сейчас не востребовано, поэтому он обязательный
    def renderTag(idt: Tree[JdTag], jdArgs: MJdArgs, i: Int = 0, parent: JdTag = null): TagOf[html.Div] = {
      import MJdTagNames._
      idt.rootLabel.name match {
        case QD_CONTENT                => renderQd( idt, i, jdArgs, parent )
        case STRIP                     => renderStrip( idt, i, jdArgs )
        case DOCUMENT                  => renderDocument( idt, i, jdArgs )
        case QD_OP                     => renderQdOp( idt, i, jdArgs )
      }
    }

    def renderJdArgs(jdArgs: MJdArgs): TagOf[html.Div] = {
      renderTag(
        idt     = jdArgs.template,
        jdArgs  = jdArgs,
        parent  = jdArgs.template.rootLabel
      )
    }

  }


  /** Рендерер дерева jd-тегов. */
  protected class Backend($: BackendScope[Props, Props_t]) extends JdRenderer {

    // Callbacks

    /** Реакция на клик по отрендеренному тегу. */
    override protected def jdTagClick(jdt: JdTag)(e: ReactMouseEvent): Callback = {
      // Если не сделать stopPropagation, то наружный strip перехватит клик
      e.stopPropagationCB >>
        dispatchOnProxyScopeCB($, JdTagSelect(jdt) )
    }


    /** Начало таскания qd-тега.
      * Бывают сложности с рассчётом координат. Особенно, если используется плитка.
      */
    override protected def qdTagDragStart(jdt: JdTag)(e: ReactDragEvent): Callback = {
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

    /** Начинается перетаскивание целого стрипа. */
    override protected def stripDragStart(jdt: JdTag)(e: ReactDragEvent): Callback = {
      // Надо выставить в событие, что на руках у нас целый стрип.
      val mimes = MimeConst.Sio
      e.dataTransfer.setData( mimes.DATA_CONTENT_TYPE, mimes.DataContentTypes.STRIP )
      dispatchOnProxyScopeCB($, JdTagDragStart(jdt) )
    }

    override protected def jdTagDragEnd(jdt: JdTag)(e: ReactDragEvent): Callback = {
      dispatchOnProxyScopeCB($, JdTagDragEnd(jdt) )
    }


    override protected def jdStripDragOver(e: ReactDragEvent): Callback = {
      // В b9710f2 здесь была проверка cookie через getData, но webkit/chrome не поддерживают доступ в getData во время dragOver. Ппппппц.
      e.preventDefaultCB
    }


    /** Что-то было сброшено на указанный стрип. */
    override protected def onDropToStrip(s: JdTag)(e: ReactDragEvent): Callback = {
      val mimes = MimeConst.Sio

      e.preventDefault()
      val dataType = e.dataTransfer.getData( mimes.DATA_CONTENT_TYPE )
      val clientY = e.clientY

      if ( dataType ==* mimes.DataContentTypes.CONTENT_ELEMENT ) {
        // Перенос контента.
        val coordsJsonStr = e.dataTransfer.getData( mimes.COORD_2D_JSON )
        val clientX = e.clientX

        // Всё остальное (вне event) заносим в callback-функцию, чтобы максимально обленивить вычисления и дальнейшие действия.
        dispatchOnProxyScopeCBf($) { jdArgsProxy: Props =>
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

          val szMultD = jdArgsProxy.value.conf.szMult.toDouble

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
    }


    /** Callback о завершении загрузки в память картинки, у которой неизвестны какие-то рантаймовые параметры. */
    override protected def onNewImageLoaded(edgeUid: EdgeUid_t)(e: ReactEvent): Callback = {
      imgRenderUtilJs.notifyImageLoaded($, edgeUid, e)
    }


    private def _parseStylePx(e: ReactMouseEventFromHtml)(f: CSSStyleDeclaration => String): Option[Int] = {
      for {
        // Используем currentTarget, т.к. хром возвращает события откуда попало, а не из точки аттача.
        // TODO Если за пределами блока отпускание мыши, то и это не помогает.
        target        <- Option(e.currentTarget)
        if e.button ==* 0
        style         <- Option(target.style)
        sizePxStyl    <- Option(f(style))
        pxIdx = sizePxStyl.indexOf("px")
        if pxIdx > 0
      } yield {
        sizePxStyl
          .substring(0, pxIdx)
          .toInt
      }
    }
    private def _parseWidth(e: ReactMouseEventFromHtml): Option[Int] = {
      _parseStylePx(e)(_.width)
    }
    private def _parseHeight(e: ReactMouseEventFromHtml): Option[Int] = {
      _parseStylePx(e)(_.height)
    }

    /** Самописная поддержка ресайза контента только силами браузера. */
    override protected def onQdTagResize(qdTag: JdTag)(e: ReactMouseEventFromHtml): Callback = {
      _parseWidth(e).fold(Callback.empty) { widthPx =>
        dispatchOnProxyScopeCB( $, CurrContentResize( widthPx ) )
      }
    }


    override protected def onQdEmbedResize(qdOp: MQdOp, edgeDataJs: MEdgeDataJs, withHeight: Boolean)(e: ReactMouseEventFromHtml): Callback = {
      _parseWidth(e).fold(Callback.empty) { widthPx =>
        // stopPropagation() нужен, чтобы сигнал не продублировался в onQdTagResize()
        val heightPxOpt = OptionUtil.maybe(withHeight)(_parseHeight(e).get)
        println(widthPx, withHeight, e.target.style.height, heightPxOpt)
        ReactCommonUtil.stopPropagationCB(e) >>
          dispatchOnProxyScopeCB( $, QdEmbedResize( widthPx, qdOp, edgeDataJs.jdEdge.id, heightPx = heightPxOpt ) )
      }
    }

    /** Рендер компонента. */
    def render(s: Props_t): VdomElement = {
      // Тут была попытка завернуть всё в коннекшен для явной защиты от перерендеров при неизменных MJdArgs.
      // Итог: фейл. Нарушается актуальность JdCss, перерендеры остаются.
      renderJdArgs(s)
    }

  } // Backend


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    // В состоянии храним последний инстанс MJdArgs. Это поможет подавлять паразитные перерендеры.
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    // ReactConnector использовать обычно нельзя, поэтому используем простой самописный компаратор, защищающий от лишних пере-рендеров.
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( MJdArgsFastEq ) )
    .build

  private def _apply(jdArgsProxy: Props) = component( jdArgsProxy )
  val apply: ReactConnectProps[Props_t] = _apply


  def renderSeparated(jdArgsProxy: Props): Iterator[VdomElement] = {
    jdArgsProxy.value.template
      .subForest
      .iterator
      .zipWithIndex
      .map { case (jdStripTree, i) =>
        jdArgsProxy.wrap { jdArgs0 =>
          jdArgs0
            .withTemplate(jdStripTree)
            .withRenderArgs(
              jdArgs0.renderArgs.withSelPath {
                jdArgs0.renderArgs.selPath.flatMap {
                  case h :: t =>
                    OptionUtil.maybe(h ==* i)(t)
                  case Nil =>
                    // should never happen
                    None
                }
              }
            )
        } { jdArgs2Proxy =>
          <.div(
            ^.key := i.toString,
            jdR.apply(jdArgs2Proxy)
          )
        }
      }
  }



  /** Опциональный компонент, который просто является костылём-надстройкой на JdR.component. */
  // TODO Нужен ли? По факту -- не используется.
  val optional = ScalaComponent
    .builder[ModelProxy[Option[MJdArgs]]]("JdOpt")
    .stateless
    .render_P { jdArgsOptProxy =>
      jdArgsOptProxy.value.whenDefinedEl { jdArgs =>
        jdArgsOptProxy.wrap(_ => jdArgs) { component.apply }
      }
    }
    .build


  /** Рендерер HTML всырую.
    * При использовании плитки react-stonecutter возникла проблема с child-монтированием:
    * нет анимации внутри child-компонетов.
    */
  object InlineRender extends JdRenderer {

    override protected def jdTagClick(jdt: JdTag)(e: ReactMouseEvent) = Callback.empty

    override protected def qdTagDragStart(jdt: JdTag)(e: ReactDragEvent) = Callback.empty

    override protected def stripDragStart(jdt: JdTag)(e: ReactDragEvent) = Callback.empty

    override protected def jdTagDragEnd(jdt: JdTag)(e: ReactDragEvent) = Callback.empty

    override protected def jdStripDragOver(e: ReactDragEvent) = Callback.empty

    override protected def onDropToStrip(s: JdTag)(e: ReactDragEvent) = Callback.empty

    override protected def onNewImageLoaded(edgeUid: EdgeUid_t)(e: ReactEvent) = Callback.empty

    override protected def onQdTagResize(qdTag: JdTag)(e: ReactMouseEventFromHtml) = Callback.empty

    override protected def onQdEmbedResize(qdOp: MQdOp, edgeDataJs: MEdgeDataJs, withHeight: Boolean)(e: ReactMouseEventFromHtml) = Callback.empty
  }

}

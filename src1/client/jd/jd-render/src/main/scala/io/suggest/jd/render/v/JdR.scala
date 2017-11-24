package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter._
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.geom.coord.{MCoords2dD, MCoords2di}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.err.ErrorConstants
import io.suggest.grid.build.GridBuildArgs
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.MimeConst
import io.suggest.react.ReactCommonUtil.VdomNullElement
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.scalaz.ZTreeUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.{VdomElement, TagOf}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import org.scalajs.dom.{Element, html}
import org.scalajs.dom.html.Image
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
           jdGridUtil: JdGridUtil
         )
  extends Log
{ jdR =>

  type Props = ModelProxy[MJdArgs]

  override val LOG = super.LOG

  /** Флаг подавления неподдерживаемых функций рендера.
    * true означает, что неподдерживаемые теги, эджи и т.д. будут приводить к null-рендеру.
    */
  private final val SUPPRESS_UNEXPECTED = false

  /** Экзепшен NotImplemented или vdom-null в зависимости от состояния флага. */
  private def _maybeSuppress: VdomElement = {
    if (SUPPRESS_UNEXPECTED) {
      VdomNullElement
    } else {
      ???
    }
  }


  /** Движок рендера для сборки под разные ситуации. Можно вынести за пределы компонента. */
  trait JdRenderer {

    protected def jdTagClick(jdt: JdTag)(e: ReactMouseEvent): Callback

    protected def qdTagDragStart(jdt: JdTag)(e: ReactDragEvent): Callback

    protected def stripDragStart(jdt: JdTag)(e: ReactDragEvent): Callback

    protected def jdTagDragEnd(jdt: JdTag)(e: ReactDragEvent): Callback

    protected def jdStripDragOver(e: ReactDragEvent): Callback

    protected def onDropToStrip(s: JdTag)(e: ReactDragEvent): Callback

    protected def onNewImageLoaded(edgeUid: EdgeUid_t)(e: ReactEvent): Callback


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
      if (jdArgs.dnd.jdt.isEmpty && jdArgs.selectedTag.containsLabel(dt)) {
        jdArgs.jdCss.selectedTag
      } else {
        EmptyVdom
      }
    }

    private def _clickableOnEdit(jdt: JdTag, jdArgs: MJdArgs): TagMod = {
      // В режиме редактирования -- надо слать инфу по кликам на стрипах
      if (jdArgs.conf.isEdit) {
        ^.onClick ==> jdTagClick(jdt)
      } else {
        EmptyVdom
      }
    }


    private def _droppableOnEdit(jdt: JdTag, jdArgs: MJdArgs): TagMod = {
      if (jdArgs.conf.isEdit) {
        TagMod(
          ^.onDragOver ==> jdStripDragOver,
          ^.onDrop     ==> onDropToStrip(jdt)
        )
      } else {
        EmptyVdom
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
      if ( jdArgs.conf.isEdit && e.fileJs.exists(_.whPx.isEmpty) ) {
        ^.onLoad ==> onNewImageLoaded(e.id)
      } else {
        EmptyVdom
      }
    }


    /** Рендер strip, т.е. одной "полосы" контента. */
    def renderStrip(stripTree: Tree[JdTag], i: Int, jdArgs: MJdArgs): TagOf[html.Div]  = {
      val s = stripTree.rootLabel
      val C = jdArgs.jdCss
      val isSelected = jdArgs.selectedTag.containsLabel(s)
      val isEditSelected = isSelected && jdArgs.conf.isEdit

      val isWide = /*!jdArgs.conf.isEdit &&*/ s.props1.bm.map(_.wide).getOrElseFalse
      val bgColor = _bgColorOpt(s, jdArgs)

      val bgImgOpt = for {
        bgImgData <- s.props1.bgImg
        edgeUid   = bgImgData.imgEdge.edgeUid
        edge      <- jdArgs.edges.get( edgeUid )
        if edge.jdEdge.predicate ==>> MPredicates.JdBgPred
        bgImgSrc  <- edge.origImgSrcOpt
      } yield {

        <.img(
          ^.`class` := Css.Block.BG,
          ^.src := bgImgSrc,

          // Запретить таскать изображение, чтобы не мешать перетаскиванию strip'ов
          if (jdArgs.conf.isEdit) {
            ^.draggable := false
          } else {
            EmptyVdom
          },

          // Поддержка эмуляции кропа.
          {
            // Рассчитываем аргументы кропа, если есть.
            val cropEmuOpt = for {
              crop    <- bgImgData.crop
              bm      <- s.props1.bm
              origWh  <- edge.origWh
            } yield {
              //val outerWh = bm.rePadded( jdArgs.conf.blockPadding )
              MEmuCropCssArgs(crop, origWh, bm)
            }

            cropEmuOpt.fold[TagMod] {
              // Просто заполнение всего блока картинкой. TODO Унести в jdCss.
              s.props1.bm.whenDefined { bm =>
                // Заполняем блок по ширине, т.к. дырки сбоку режут глаз сильнее, чем снизу.
                TagMod(
                  ^.width  := (bm.width * jdArgs.conf.szMult.toDouble).px    // Избегаем расплющивания картинок, пусть лучше обрезка будет.
                  //^.height := bm.height.px
                )
              }
            } { ecArgs =>
              // Нужно рассчитать параметры margin, w, h изображения, чтобы оно имитировало заданный кроп.
              // margin: -20px 0px 0px -16px; -- сдвиг вверх и влево.
              // Для этого надо вписать размеры кропа в размеры блока

              // Вычисляем отношение стороны кропа к стороне блока. Считаем, что обе стороны соотносятся одинаково.
              val outer2cropRatio = ecArgs.outerWh.height.toDouble / ecArgs.crop.height.toDouble
              val blkSzMultD = jdArgs.conf.szMult.toDouble

              // Проецируем это отношение на натуральные размеры картинки, top и left:
              TagMod(
                ^.width := (ecArgs.origWh.width  * outer2cropRatio * blkSzMultD).px,
                ^.height := (ecArgs.origWh.height * outer2cropRatio * blkSzMultD).px,
                ^.marginLeft := (-ecArgs.crop.offX * outer2cropRatio * blkSzMultD).px,
                ^.marginTop := (-ecArgs.crop.offY * outer2cropRatio * blkSzMultD).px
              )
            }
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

      val smBlock = <.div(
        keyAV,
        C.smBlock,
        C.bmStyleF( s ),

        if (isWide) {
          jdArgs.jdCss.wideBlockStyle
        } else {
          bgColor
        },

        // Скрыть не-main-стрипы, если этого требует рендер.
        // Это касается только стрипов, у которых нет isMain = Some(true)
        if (jdArgs.renderArgs.hideNonMainStrips && !s.props1.isMain.getOrElseFalse) {
          // Данный стип надо приглушить с помощью указанных css-стилей.
          ^.visibility.hidden
        } else {
          EmptyVdom
        },

        _maybeSelected( s, jdArgs ),

        // Если текущий стрип выделен, то его можно таскать.
        if (isEditSelected) {
          TagMod(
            _draggableUsing(s, jdArgs)(stripDragStart(s)),
            ^.`class` := Css.Cursor.GRAB
          )
        } else {
          EmptyVdom
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
          bgColor.when(isWide),
          C.bmWideStyleF(s),
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
            jds  = jdGridUtil.jdTrees2bms(jd.subForest),
            conf = jdArgs.conf,
            tagName = GridComponents.DIV,
            gridBuildArgsF = { items =>
              GridBuildArgs(
                itemsExtDatas = items,
                jdConf = jdArgs.conf
              )
            }
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
    def renderQd(qdTagTree: Tree[JdTag], i: Int, jdArgs: MJdArgs, parent: JdTag): TagOf[html.Div]  = {
      val tagMods = {
        val qdRrr = new QdRrrHtml(
          jdArgs      = jdArgs,
          qdTag       = qdTagTree,
          // Для редактора: следует проверить эдж
          imgEdgeMods = OptionUtil.maybe( jdArgs.conf.isEdit ) {
            _notifyImgWhOnEdit(_, jdArgs)
          }
        )
        qdRrr.render()
      }
      val qdTag = qdTagTree.rootLabel
      <.div(
        ^.key := i.toString,

        // Опциональный цвет фона
        _bgColorOpt( qdTag, jdArgs ),

        _maybeSelected(qdTag, jdArgs),
        _clickableOnEdit(qdTag, jdArgs),

        // Поддержка перетаскивания
        jdArgs.jdCss.absPosStyleAll,
        if (jdArgs.conf.isEdit && !jdArgs.selectedTag.containsLabel(parent)) {
          _draggableUsing(qdTag, jdArgs) { qdTagDragStart(qdTag) }
        } else {
          EmptyVdom
        },
        jdArgs.jdCss.absPosStyleF(qdTag),

        // Рендерить особые указатели мыши в режиме редактирования.
        if (jdArgs.conf.isEdit) {
          ^.`class` := {
            if (jdArgs.selectedTag.containsLabel(qdTag)) {
              // Текущий тег выделен. Значит, пусть будет move-указатель
              Css.Cursor.MOVE
            } else {
              Css.Cursor.POINTER
            }
          }
        } else {
          EmptyVdom
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
  protected class Backend($: BackendScope[Props, Unit]) extends JdRenderer {

    /** Рендер компонента. */
    def render(jdArgsProxy: Props): VdomElement = {
      renderJdArgs( jdArgsProxy.value )
    }


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

      //println( s"srcStyl($srcLeft $srcTop) - eCl(${e.clientX} ${e.clientY}) => $offsetXy" )
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
        dispatchOnProxyScopeCBf($) { jdArgsProxy =>
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
          //println(s"e.client(${e.clientX} ${e.clientY}) +diff=$offsetXy => $topLeftXy")

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
          //println(s"e.Y=$clientY clRect.top=${clRect.top} clRect.height=${clRect.height}")
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
      // Прочитать natural w/h из экшена.
      try {
        val img = e.target.asInstanceOf[Image]
        val sz = MSize2di(
          // IDEA почему-то ругается на deprecated, это ошибка в scala-плагине.
          width  = img.naturalWidth,
          height = img.naturalHeight
        )
        val minWh = 0
        ErrorConstants.assertArg( sz.width > minWh )
        ErrorConstants.assertArg( sz.height > minWh )
        dispatchOnProxyScopeCB( $, SetImgWh(edgeUid, sz) )
      } catch {
        case ex: Throwable =>
          LOG.error( ErrorMsgs.IMG_EXPECTED, ex = ex, msg = (edgeUid, e.target.toString) )
          Callback.empty
      }
    }

  } // Backend


  val component = ScalaComponent.builder[Props]("Jd")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(jdArgsProxy: Props) = component( jdArgsProxy )


  def renderSeparated(jdArgsProxy: Props): Iterator[VdomElement] = {
    jdArgsProxy.value.template
      .subForest
      .iterator
      .zipWithIndex
      .map { case (jdStripTree, i) =>
        jdArgsProxy.wrap { jdArgs0 =>
          jdArgs0
            .withTemplate(jdStripTree)
            .withSelPath {
              jdArgs0.selPath.flatMap {
                case h :: t =>
                  OptionUtil.maybe(h ==* i)(t)
                case Nil =>
                  // should never happen
                  None
              }
            }
        } { jdArgs2Proxy =>
          <.div(
            ^.key := i.toString,
            jdR(jdArgs2Proxy)
          )
        }
      }
  }


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

  }

}

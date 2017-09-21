package io.suggest.jd.render.v

import diode.react.ModelProxy
import io.suggest.common.geom.coord.{MCoords2dD, MCoords2di}
import io.suggest.css.Css
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.pick.MimeConst
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.VdomNullElement
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import org.scalajs.dom.Element
import play.api.libs.json.Json

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */

class JdR extends Log {

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


  /** Рендерер дерева jd-тегов. */
  protected class Backend($: BackendScope[Props, Unit]) {

    /** Рендер компонента. */
    def render(jdArgsProxy: Props): VdomElement = {
      val jdArgs = jdArgsProxy.value
      renderDocument( jdArgs = jdArgs )
    }


    // Callbacks

    /** Реакция на клик по отрендеренному тегу. */
    private def jdTagClick(jdt: IDocTag)(e: ReactMouseEvent): Callback = {
      // Если не сделать stopPropagation, то наружный strip перехватит клик
      e.stopPropagationCB >>
        dispatchOnProxyScopeCB($, JdTagClick(jdt) )
    }


    /** Начало таскания jd-тега. */
    private def jdTagDragStart(jdt: IDocTag)(e: ReactDragEvent): Callback = {
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
    private def stripDragStart(jdt: IDocTag)(e: ReactDragEvent): Callback = {
      // Надо выставить в событие, что на руках у нас целый стрип.
      val mimes = MimeConst.Sio
      e.dataTransfer.setData( mimes.DATA_CONTENT_TYPE, mimes.DataContentTypes.STRIP )
      dispatchOnProxyScopeCB($, JdTagDragStart(jdt) )
    }

    private def jdTagDragEnd(jdt: IDocTag)(e: ReactDragEvent): Callback = {
      dispatchOnProxyScopeCB($, JdTagDragEnd(jdt) )
    }


    private def jdStripDragOver(e: ReactDragEvent): Callback = {
      // В b9710f2 здесь была проверка cookie через getData, но webkit/chrome не поддерживают доступ в getData во время dragOver. Ппппппц.
      e.preventDefaultCB
    }


    /** Что-то было сброшено на указанный стрип. */
    private def onDropToStrip(s: IDocTag)(e: ReactDragEvent): Callback = {
      val mimes = MimeConst.Sio

      val dataType = e.dataTransfer.getData( mimes.DATA_CONTENT_TYPE )
      val clientY = e.clientY

      if ( dataType ==* mimes.DataContentTypes.CONTENT_ELEMENT ) {
        // Перенос контента.
        val coordsJsonStr = e.dataTransfer.getData( mimes.COORD_2D_JSON )
        val clientX = e.clientX

        // Всё остальное (вне event) заносим в callback-функцию, чтобы максимально обленивить вычисления и дальнейшие действия.
        dispatchOnProxyScopeCBf($) { _ =>
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

          // Вычислить относительную координату в css-пикселях между точкой дропа и левой верхней точкой strip'а.
          // Считаем в client-координатах, т.к. рассчёты мгновенны, и client viewport не сдвинется за это время.
          // Если понадобятся page-координаты, то https://stackoverflow.com/a/37200339
          val topLeftXy = MCoords2di(
            x = (clientX + offsetXy.x).toInt,
            y = (clientY + offsetXy.y).toInt
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


    // Internal API

    /** Рендер payload'а. */
    /*
    private def renderPlainPayload(pp: PlainPayload, jdArgs: MJdArgs): VdomNode = {
      jdArgs.renderArgs.edges
        .get( pp.edgeId )
        .whenDefinedNode { e =>
          e.predicate match {
            case MPredicates.JdContent.Text =>
              e.text
                .whenDefinedNode(identity(_))
            case _ =>
              LOG.error( ErrorMsgs.TEXT_EXPECTED, msg = e )
              _maybeSuppress
          }
        }
    }
    */


    /** Рендер картинки. */
    /*
    private def renderPicture(p: Picture, i: Int, jdArgs: MJdArgs): VdomElement = {
      jdArgs.renderArgs.edges
        .get( p.edgeUid )
        .whenDefinedEl { e =>
          e.predicate match {
            case MPredicates.Bg =>
              e.url.whenDefinedEl { url =>
                <.img(
                  ^.key := i.toString,
                  ^.src := url,
                  // TODO Отрендерить фактические аттрибуты wh загружаемого файла изображения.
                  e.whOpt.whenDefined { wh =>
                    // Это фактические размеры изображения внутри файла по указанной ссылке.
                    TagMod(
                      ^.width  := wh.width.px,
                      ^.height := wh.height.px
                    )
                  }
                )
              }

            case _ =>
              LOG.error(ErrorMsgs.IMG_EXPECTED, msg = e)
              _maybeSuppress
          }
        }
    }
    */


    /** Рендер контейнера, спозиционированного на экране абсолютно. */
    /*
    private def renderAbsPos(ap: IDocTag, i: Int, jdArgs: MJdArgs, parent: IDocTag): VdomElement = {
      <.div(
        ^.key := i.toString,
        jdArgs.jdCss.absPosStyleAll,
        if (jdArgs.conf.withEdit && !jdArgs.selectedTag.contains(parent)) {
          _draggableUsing(ap, jdArgs) { jdTagDragStart(ap) }
        } else {
          EmptyVdom
        },
        jdArgs.jdCss.absPosStyleF(ap),

        renderChildren(ap, jdArgs)
      )
    }
    */


    def renderChildren(dt: IDocTag, jdArgs: MJdArgs): VdomArray = {
      dt.children
        .iterator
        .zipWithIndex
        .toVdomArray { case (jd, i) =>
          renderDocTag(jd, i, jdArgs, dt)
        }
    }

    /**
      * Является ли указанный тег текущим выделенным?
      * Если да, то присвоить ему соотв.стиль для выделения визуально.
      */
    private def _maybeSelected(dt: IDocTag, jdArgs: MJdArgs): TagMod = {
      // Если происходит перетаскивание, то нужно избавляться от рамок: так удобнее.
      if (jdArgs.dnd.jdt.isEmpty && jdArgs.selectedTag.contains(dt)) {
        jdArgs.jdCss.selectedTag
      } else {
        EmptyVdom
      }
    }

    private def _clickableOnEdit(jdt: IDocTag, jdArgs: MJdArgs): TagMod = {
      // В режиме редактирования -- надо слать инфу по кликам на стрипах
      if (jdArgs.conf.withEdit) {
        ^.onClick ==> jdTagClick(jdt)
      } else {
        EmptyVdom
      }
    }


    private def _droppableOnEdit(jdt: IDocTag, jdArgs: MJdArgs): TagMod = {
      if (jdArgs.conf.withEdit) {
        TagMod(
          ^.onDragOver ==> jdStripDragOver,
          ^.onDrop     ==> onDropToStrip(jdt)
        )
      } else {
        EmptyVdom
      }
    }

    private def _draggableUsing(jdt: IDocTag, jdArgs: MJdArgs)(onDragStartF: ReactDragEvent => Callback): TagMod = {
      TagMod(
        ^.draggable := true,
        ^.onDragStart ==> onDragStartF,
        ^.onDragEnd   ==> jdTagDragEnd(jdt)
      )
    }

    /** Рендер strip, т.е. одной "полосы" контента. */
    def renderStrip(s: IDocTag, i: Int, jdArgs: MJdArgs): VdomElement = {
      val C = jdArgs.jdCss
      val isSelected = jdArgs.selectedTag.contains(s)
      val isEditSelected = isSelected && jdArgs.conf.withEdit
      <.div(
        ^.key := i.toString,
        C.smBlock,
        C.bmStyleF( s ),
        _bgColorOpt(s, jdArgs),

        _maybeSelected( s, jdArgs ),
        _clickableOnEdit( s, jdArgs ),
        _droppableOnEdit( s, jdArgs ),

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
        {
          val bgImgOpt = for {
            bgImgEi <- s.props1.bgImg
            edgeUid = bgImgEi.edgeUid
            edge    <- jdArgs.renderArgs.edges.get( edgeUid )
            if edge.predicate ==* MPredicates.Bg
            src     <- edge.url.orElse( edge.text )
          } yield {
            src
          }
          bgImgOpt.whenDefined { bgImgSrc =>
            <.img(
              ^.`class` := Css.Block.BG,
              ^.src := bgImgSrc,
              // Запретить таскать изображение, чтобы не мешать перетаскиванию strip'ов
              if (jdArgs.conf.withEdit) {
                ^.draggable := false
              } else {
                EmptyVdom
              },
              s.props1.bm.whenDefined { bm =>
                TagMod(
                  //^.width  := bm.width.px,    // Избегаем расплющивания картинок, пусть лучше обрезка будет.
                  ^.height := bm.height.px
                )
              },
              // В jdArgs может быть задан дополнительные модификации изображения, если selected tag.
              jdArgs.renderArgs.selJdtBgImgMod
                .filter(_ => isSelected)
                .whenDefined
            )
          }
        },

        renderChildren( s, jdArgs )
      )
    }


    /** Выполнить рендер текущего документа, переданного в jdArgs. */
    def renderDocument(i: Int = 0, jdArgs: MJdArgs): VdomElement = {
      renderDocument( jdArgs.template, i, jdArgs )
    }

    /** Рендер указанного документа. */
    def renderDocument(jd: IDocTag, i: Int, jdArgs: MJdArgs): VdomElement = {
      <.div(
        ^.key := i.toString,
        renderChildren( jd, jdArgs )
      )
    }

    private def _bgColorOpt(jdTag: IDocTag, jdArgs: MJdArgs): TagMod = {
      jdTag.props1.bgColor.whenDefined { mcd =>
        jdArgs.jdCss.bgColorOptStyleF( mcd.hexCode )
      }
    }

    /** Рендер перекодированных данных quill delta.
      *
      * @param qdTag Тег с кодированными данными Quill delta.
      * @return Элемент vdom.
      */
    def renderQd( qdTag: IDocTag, i: Int, jdArgs: MJdArgs, parent: IDocTag): VdomElement = {
      val tagMods = {
        val qdRrr = new QdRrrHtml(jdArgs, qdTag)
        qdRrr.render()
      }
      <.div(
        ^.key := i.toString,

        // Опциональный цвет фона
        _bgColorOpt( qdTag, jdArgs ),

        _maybeSelected(qdTag, jdArgs),
        _clickableOnEdit(qdTag, jdArgs),

        // Поддержка перетаскивания
        jdArgs.jdCss.absPosStyleAll,
        if (jdArgs.conf.withEdit && !jdArgs.selectedTag.contains(parent)) {
          _draggableUsing(qdTag, jdArgs) { jdTagDragStart(qdTag) }
        } else {
          EmptyVdom
        },
        jdArgs.jdCss.absPosStyleF(qdTag),

        // Рендерить особые указатели мыши в режиме редактирования.
        if (jdArgs.conf.withEdit) {
          ^.`class` := {
            if (jdArgs.selectedTag.contains(qdTag)) {
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


    /**
      * Запуск рендеринга произвольных тегов.
      */
    // TODO parent может быть необязательным. Но это сейчас не востребовано, поэтому он обязательный
    def renderDocTag(idt: IDocTag, i: Int, jdArgs: MJdArgs, parent: IDocTag): VdomNode = {
      import MJdTagNames._
      idt.jdTagName match {
        case QUILL_DELTA                => renderQd( idt, i, jdArgs, parent )
        //case pp: PlainPayload           => renderPlainPayload( pp, jdArgs )
        //case p: Picture                 => renderPicture( p, i, jdArgs )
        //case ap: AbsPos                 => renderAbsPos(ap, i, jdArgs, parent)
        case STRIP                      => renderStrip( idt, i, jdArgs )
        case DOCUMENT                   => renderDocument( idt, i, jdArgs )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("JdR")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(jdArgsProxy: Props) = component( jdArgsProxy )

}

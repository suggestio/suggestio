package io.suggest.jd.render.v

import diode.react.ModelProxy
import io.suggest.common.geom.coord.{MCoords2dD, MCoords2di}
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd.QdTag
import io.suggest.model.n2.edge.MPredicates
import io.suggest.pick.MimeConst
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.VdomNullElement
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
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

      val srcLeft = DataUtil.extractInt( srcTgStyle.getPropertyValue("left") ).get
      val srcTop  = DataUtil.extractInt( srcTgStyle.getPropertyValue("top") ).get

      val offsetXy = MCoords2dD(
        x = srcLeft - e.clientX,
        y = srcTop  - e.clientY
      )

      // Узнаём разницу между левым верхним углом элемента и фактической точкой перетаскивания:
      //println( s"srcStyl($srcLeft $srcTop) - eCl(${e.clientX} ${e.clientY}) => $offsetXy" )
      e.dataTransfer.setData( mimes.COORD_2D_JSON, Json.toJson(offsetXy).toString() )

      dispatchOnProxyScopeCB($, JdTagDragStart(jdt, offsetXy) )
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
      // Узнать разницу между коодинатами мыши и левым верхним углом. Десериализовать из dataTransfer.
      val offsetXy = try {
        Json
          .parse( e.dataTransfer.getData( MimeConst.Sio.COORD_2D_JSON ) )
          .as[MCoords2dD]
      } catch {
        case ex: Throwable =>
          LOG.log(ErrorMsgs.DND_DROP_ERROR, ex, e.target)
          MCoords2dD(0, 0)
      }

      // Вычислить относительную координату в css-пикселях между точкой дропа и левой верхней точкой strip'а.
      // Считаем в client-координатах, т.к. рассчёты мгновенны, и client viewport не сдвинется за это время.
      // Если понадобятся page-координаты, то https://stackoverflow.com/a/37200339
      val topLeftXy = MCoords2di(
        x = (e.clientX + offsetXy.x).toInt,
        y = (e.clientY + offsetXy.y).toInt
      )
      //println(s"e.client(${e.clientX} ${e.clientY}) +diff=$offsetXy => $topLeftXy")

      dispatchOnProxyScopeCB( $, JdStripDrop(
        strip       = s,
        clXy        = topLeftXy,
        foreignTag  = None   // TODO Десериализовать из event, если элемент не принадлежит текущему документу.
      ))
    }


    // Internal API

    /** Рендер payload'а. */
    def renderPlainPayload(pp: PlainPayload, jdArgs: MJdArgs): VdomNode = {
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


    /** Рендер картинки. */
    def renderPicture(p: Picture, i: Int, jdArgs: MJdArgs): VdomElement = {
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


    /** Рендер контейнера, спозиционированного на экране абсолютно. */
    def renderAbsPos(ap: AbsPos, i: Int, jdArgs: MJdArgs): VdomElement = {
      <.div(
        ^.key := i.toString,
        jdArgs.jdCss.absPosStyleAll,
        _draggableOnEdit(ap, jdArgs),
        jdArgs.jdCss.absPosStyleF(ap),

        renderChildren(ap, jdArgs)
      )
    }


    def renderChildren(dt: IDocTag, jdArgs: MJdArgs): VdomArray = {
      renderChildren( dt.children, jdArgs )
    }
    def renderChildren(chs: TraversableOnce[IDocTag], jdArgs: MJdArgs): VdomArray = {
      chs.toIterator
        .zipWithIndex
        .toVdomArray { case (jd, i) =>
          renderDocTag(jd, i, jdArgs)
        }
    }

    /**
      * Является ли указанный тег текущим выделенным?
      * Если да, то присвоить ему соотв.стиль для выделения визуально.
      */
    private def _maybeSelected(dt: IDocTag, jdArgs: MJdArgs): TagMod = {
      if (jdArgs.selectedTag contains dt) {
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


    private def _draggableOnEdit(jdt: IDocTag, jdArgs: MJdArgs): TagMod = {
      if (jdArgs.conf.withEdit) {
        TagMod(
          ^.draggable := true,
          ^.onDragStart ==> jdTagDragStart(jdt),
          ^.onDragEnd   ==> jdTagDragEnd(jdt)

          // Во время таскания надо бы скрыть исходный элемент
          //if (jdArgs.dnd.jdt contains jdt) {
          //  ^.`class` := Css.Display.HIDDEN
          //} else {
          //  EmptyVdom
          //}
        )
      } else {
        EmptyVdom
      }
    }


    /** Рендер strip, т.е. одной "полосы" контента. */
    def renderStrip(s: Strip, i: Int, jdArgs: MJdArgs): VdomElement = {
      <.div(
        ^.key := i.toString,
        jdArgs.jdCss.smBlock,
        jdArgs.jdCss.stripOuterStyleF( s ),

        _maybeSelected( s, jdArgs ),
        _clickableOnEdit( s, jdArgs ),
        _droppableOnEdit( s, jdArgs ),

        renderChildren( s, jdArgs )
      )
    }


    /** Выполнить рендер текущего документа, переданного в jdArgs. */
    def renderDocument(i: Int = 0, jdArgs: MJdArgs): VdomElement = {
      renderDocument( jdArgs.template, i, jdArgs )
    }

    /** Рендер указанного документа. */
    def renderDocument(jd: JsonDocument, i: Int, jdArgs: MJdArgs): VdomElement = {
      <.div(
        ^.key := i.toString,
        renderChildren( jd, jdArgs )
      )
    }


    /** Рендер перекодированных данных quill delta.
      *
      * @param qdTag Тег с кодированными данными Quill delta.
      * @return Элемент vdom.
      */
    def renderQd( qdTag: QdTag, i: Int, jdArgs: MJdArgs): VdomElement = {
      val tagMods = {
        val qdRrr = new QdRrrHtml(jdArgs, qdTag)
        qdRrr.render()
      }
      <.div(
        ^.key := i.toString,

        _maybeSelected(qdTag, jdArgs),
        _clickableOnEdit(qdTag, jdArgs),

        tagMods
      )
    }


    /**
      * Запуск рендеринга произвольных тегов.
      */
    def renderDocTag(idt: IDocTag, i: Int, jdArgs: MJdArgs): VdomNode = {
      idt match {
        case qd: QdTag                  => renderQd( qd, i, jdArgs )
        case pp: PlainPayload           => renderPlainPayload( pp, jdArgs )
        case p: Picture                 => renderPicture( p, i, jdArgs )
        case ap: AbsPos                 => renderAbsPos(ap, i, jdArgs)
        case s: Strip                   => renderStrip( s, i, jdArgs )
        case jd: JsonDocument           => renderDocument( jd, i, jdArgs )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("JdR")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(jdArgsProxy: Props) = component( jdArgsProxy )

}

package io.suggest.jd.render.v

import diode.react.ModelProxy
import io.suggest.jd.render.m.{IJdAction, JdTagClick, MJdArgs}
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd.QdTag
import io.suggest.model.n2.edge.MPredicates
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.VdomNullElement
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */

object JdRendererR extends Log {

  override val LOG = super.LOG

  /** Флаг подавления неподдерживаемых функций рендера.
    * true означает, что неподдерживаемые теги, эджи и т.д. будут приводить к null-рендеру.
    */
  final val SUPPRESS_UNEXPECTED = false

  /** Экзепшен NotImplemented или vdom-null в зависимости от состояния флага. */
  private def _maybeSuppress: VdomElement = {
    if (SUPPRESS_UNEXPECTED) {
      VdomNullElement
    } else {
      ???
    }
  }

}


/** Класс рендерера jd-контента.
  *
  * @param proxy Шаблон и данные для рендера.
  */
class JdRendererR(
                   proxy      : ModelProxy[MJdArgs]
                 ) {

  import JdRendererR.{LOG, _maybeSuppress}

  private val jdArgs = proxy.value

  private def _sendActionCB(e: IJdAction): Callback = {
    proxy.dispatchCB( e )
  }


  /** Рендер payload'а. */
  def renderPlainPayload(pp: PlainPayload): VdomNode = {
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
  def renderPicture(p: Picture, i: Int): VdomElement = {
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
  def renderAbsPos(ap: AbsPos, i: Int): VdomElement = {
    <.div(
      ^.key := i.toString,
      jdArgs.jdCss.absPosStyleAll,
      jdArgs.jdCss.absPosStyleF(ap),

      renderChildren(ap)
    )
  }


  def renderChildren(dt: IDocTag): VdomArray = {
    renderChildren( dt.children )
  }
  def renderChildren(chs: TraversableOnce[IDocTag]): VdomArray = {
    chs.toIterator
      .zipWithIndex
      .toVdomArray { case (jd, i) =>
        renderDocTag(jd, i)
      }
  }

  /**
    * Является ли указанный тег текущим выделенным?
    * Если да, то присвоить ему соотв.стиль для выделения визуально.
    */
  private def _maybeSelected(dt: IDocTag): TagMod = {
    if (jdArgs.selectedTag contains dt) {
      jdArgs.jdCss.selectedTag
    } else {
      EmptyVdom
    }
  }

  private def _clickableOnEdit(jdt: IDocTag): TagMod = {
    // В режиме редактирования -- надо слать инфу по кликам на стрипах
    if (jdArgs.conf.withEdit) {
      ^.onClick ==> { e =>
        // Если не сделать stopPropagation, то наружный strip перехватит клик
        e.stopPropagationCB >> _sendActionCB( JdTagClick(jdt) )
      }
    } else {
      EmptyVdom
    }
  }


  private def _draggableOnEdit(jdt: IDocTag): TagMod = {
    if (jdArgs.conf.withEdit) {
      TagMod(
        ^.draggable := true,
        ^.onDragStart ==> { e =>
          Callback {
            // TODO Обязательно надо в setData() что-то передать, но что-то осмысленное, надо бы.
            e.dataTransfer.setData("text/plain", "asdasd")
          }
        }
      )
    } else {
      EmptyVdom
    }
  }

  /** Рендер strip, т.е. одной "полосы" контента. */
  def renderStrip(s: Strip, i: Int): VdomElement = {
    <.div(
      ^.key := i.toString,
      jdArgs.jdCss.smBlock,
      jdArgs.jdCss.stripOuterStyleF( s ),

      _maybeSelected( s ),
      _clickableOnEdit( s ),

      renderChildren( s )
    )
  }


  /** Выполнить рендер текущего документа, переданного в jdArgs. */
  def renderDocument(i: Int = 0): VdomElement = {
    renderDocument( jdArgs.template, i )
  }

  /** Рендер указанного документа. */
  def renderDocument(jd: JsonDocument, i: Int): VdomElement = {
    <.div(
      ^.key := i.toString,
      renderChildren( jd )
    )
  }


  /** Рендер перекодированных данных quill delta.
    *
    * @param qdTag Тег с кодированными данными Quill delta.
    * @return Элемент vdom.
    */
  def renderQd( qdTag: QdTag, i: Int ): VdomElement = {
    val tagMods = {
      val qdRrr = new QdRrrHtml(jdArgs, qdTag)
      qdRrr.render()
    }
    <.div(
      ^.key := i.toString,
      _maybeSelected(qdTag),
      _clickableOnEdit(qdTag),
      _draggableOnEdit(qdTag),
      tagMods
    )
  }


  /**
    * Запуск рендеринга произвольных тегов.
    */
  def renderDocTag(idt: IDocTag, i: Int): VdomNode = {
    idt match {
      case qd: QdTag                  => renderQd( qd, i )
      case pp: PlainPayload           => renderPlainPayload( pp )
      case p: Picture                 => renderPicture( p, i )
      case ap: AbsPos                 => renderAbsPos(ap, i)
      case s: Strip                   => renderStrip( s, i )
      case jd: JsonDocument           => renderDocument( jd, i )
    }
  }


}


package io.suggest.jd.render.v

import scalacss.ScalaCssReact._
import io.suggest.jd.render.m.MJdRenderArgs
import io.suggest.jd.tags._
import io.suggest.model.n2.edge.MPredicates
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.VdomNullElement
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs

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
  * @param bCss CSS для рендера.
  * @param jdArgs Базовые данные.
  */
class JdRendererR(
                   bCss       : JdCss,
                   jdArgs     : MJdRenderArgs
                 ) {

  import JdRendererR.{LOG, _maybeSuppress}


  /** Рендер payload'а. */
  def renderPlainPayload(pp: PlainPayload): VdomNode = {
    jdArgs.edges
      .get( pp.edgeId )
      .whenDefinedNode { e =>
        e.predicate match {
          case MPredicates.Text =>
            e.text
              .whenDefinedNode(identity(_))
          case _ =>
            LOG.error( ErrorMsgs.TEXT_EXPECTED, msg = e )
            _maybeSuppress
        }
      }
  }

  /** Рендер разрыва строки.
    * Используем lazy val для дедубликации одинаковых инстансов в рамках одного рендера.
    */
  lazy val renderLineBreak: VdomElement = {
    <.br
  }


  /** Рендер картинки. */
  def renderPicture(p: Picture): VdomElement = {
    jdArgs.edges
      .get( p.edgeUid )
      .whenDefinedEl { e =>
        e.predicate match {
          case MPredicates.Bg =>
            e.url.whenDefinedEl { url =>
              <.img(
                ^.key := p.hashCode.toString,
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
  def renderAbsPost(ap: AbsPos): VdomElement = {
    <.div(
      ^.key := ap.hashCode.toString,
      bCss.absPosStyleAll,
      bCss.absPosStyleF(ap),

      renderChildren(ap)
    )
  }


  def renderChildren(dt: IDocTag): VdomArray = {
    renderChildren( dt.children )
  }
  def renderChildren(chs: TraversableOnce[IDocTag]): VdomArray = {
    chs.toVdomArray( renderDocTag )
  }


  /** Рендер strip, т.е. одной "полосы" контента. */
  def renderStrip(s: Strip): VdomElement = {
    <.div(
      ^.key := s.hashCode.toString,
      bCss.smBlock,
      bCss.stripOuterStyleF( s ),

      renderChildren( s )
    )
  }



  /** Рендер целого документа. */
  def renderDocument(jd: JsonDocument): VdomElement = {
    <.div(
      ^.key := jd.hashCode.toString,
      renderChildren(jd)
    )
  }


  /**
    * Запуск рендеринга произвольных тегов.
    */
  def renderDocTag(idt: IDocTag): VdomNode = {
    idt match {
      case pp: PlainPayload           => renderPlainPayload( pp )
      case LineBreak                  => renderLineBreak
      case p: Picture                 => renderPicture( p )
      case ap: AbsPos                 => renderAbsPost(ap)
      case s: Strip                   => renderStrip( s )
      case jd: JsonDocument           => renderDocument( jd )
    }
  }

}


import com.softwaremill.macwire._

/** DI-factory для сборки инстансов [[JdRendererR]]. */
class JdRendererFactory {
  def mkRenderer(bCss       : JdCss,
                 jdArgs     : MJdRenderArgs): JdRendererR = {
    wire[JdRendererR]
  }
}

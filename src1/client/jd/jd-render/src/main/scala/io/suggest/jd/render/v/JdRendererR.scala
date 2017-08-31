package io.suggest.jd.render.v

import diode.react.ModelProxy

import scalacss.ScalaCssReact._
import io.suggest.jd.render.m.{IJdAction, JdTagClick, MJdArgs}
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd.{MQdAttrs, MQdOpTypes, QdTag}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.primo.ISetUnset
import japgolly.scalajs.react.vdom.{HtmlTopNode, VdomElement}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.VdomNullElement
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.scalajs.react.Callback
import org.scalajs.dom.Element

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


  /** Рендер текстового объекта: просто рендерить children. */
  def renderText(t: Text): VdomNode = {
    <.div(
      ^.key := t.hashCode.toString,
      _maybeSelected(t),
      _clickableOnEdit(t),
      // TODO Вроде бы должен быть класс title или что-то такое.
      // TODO в edit-режиме нужна поддержка draggable.
      renderChildren(t)
    )
  }


  /** Рендер картинки. */
  def renderPicture(p: Picture): VdomElement = {
    jdArgs.renderArgs.edges
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
  def renderAbsPos(ap: AbsPos): VdomElement = {
    <.div(
      ^.key := ap.hashCode.toString,
      jdArgs.jdCss.absPosStyleAll,
      jdArgs.jdCss.absPosStyleF(ap),

      renderChildren(ap)
    )
  }


  def renderChildren(dt: IDocTag): VdomArray = {
    renderChildren( dt.children )
  }
  def renderChildren(chs: TraversableOnce[IDocTag]): VdomArray = {
    chs.toVdomArray( renderDocTag )
  }

  /**
    * Является ли указанный тег текущим выделенным?
    * Если да, то присвоить ему соотв.стиль для выделения визуально.
    */
  private def _maybeSelected(dt: IDocTag): TagMod = {
    if (jdArgs.selectedTag.contains(dt)) {
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

  /** Рендер strip, т.е. одной "полосы" контента. */
  def renderStrip(s: Strip): VdomElement = {
    <.div(
      ^.key := s.hashCode.toString,
      jdArgs.jdCss.smBlock,
      jdArgs.jdCss.stripOuterStyleF( s ),

      _maybeSelected( s ),
      _clickableOnEdit( s ),

      renderChildren( s )
    )
  }


  /** Выполнить рендер текущего документа, переданного в jdArgs. */
  def renderDocument(): VdomElement = {
    renderDocument( jdArgs.template )
  }

  /** Рендер указанного документа. */
  def renderDocument(jd: JsonDocument): VdomElement = {
    <.div(
      ^.key := jd.hashCode.toString,
      renderChildren( jd )
    )
  }


  /** Рендер перекодированных данных quill delta.
    *
    * @param qdTag Тег с кодированными данными Quill delta.
    * @return Элемент vdom.
    */
  def renderQd( qdTag: QdTag ): VdomElement = {
    val children = qdTag.ops
      .iterator
      .zipWithIndex
      .toVdomArray { case (qdOp, i) =>
        val key = ^.key := i.toString
        val node = qdOp.opType match {
          // По идее, тут только инзерты.
          case MQdOpTypes.Insert =>
            qdOp.edgeInfo.fold[VdomNode] {
              // TODO Внешний embed?
              ???
            } { qdEi =>
              val e = jdArgs.renderArgs.edges(qdEi.edgeUid)
              e.predicate match {
                case MPredicates.Text =>
                  // Рендер текста. Нужно отработать аттрибуты рендера текста.
                  renderQdText( e.text.get, qdOp.attrs, key )
                // TODO Надо image через предикат
              }
            }
        }
        node
      }
    <.div(
      _maybeSelected(qdTag),
      _clickableOnEdit(qdTag),
      children
    )
  }

  /** Рендер текста. */
  def renderQdText(text: String, attrsOpt: Option[MQdAttrs], tm0: TagMod): VdomNode = {
    var acc: VdomNode = text

    // Обвешать текст заданной аттрибутикой
    attrsOpt.filter(_.nonEmpty).fold[Unit] {
      acc = <.span(tm0, acc)
    } { attrs =>
      /** Рендер f() только по true-флагу в Set. */
      def __rBool(boolSuOpt: Option[ISetUnset[Boolean]])(f: => HtmlTagOf[_ <: Element]): Unit = {
        for (xSU <- attrs.bold; isEnabled <- xSU.toOption if isEnabled)
          acc = f(tm0, acc)
      }

      __rBool(attrs.bold)( <.strong )
      __rBool(attrs.italic)( <.em )
      __rBool(attrs.underline)( <.u )
    }

    acc
  }

  /**
    * Запуск рендеринга произвольных тегов.
    */
  def renderDocTag(idt: IDocTag): VdomNode = {
    idt match {
      case qd: QdTag                  => renderQd( qd )
      case pp: PlainPayload           => renderPlainPayload( pp )
      case LineBreak                  => renderLineBreak
      case t: Text                    => renderText(t)
      case p: Picture                 => renderPicture( p )
      case ap: AbsPos                 => renderAbsPos(ap)
      case s: Strip                   => renderStrip( s )
      case jd: JsonDocument           => renderDocument( jd )
    }
  }


}

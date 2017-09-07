package io.suggest.jd.render.v

import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.tags.qd._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.primo.ISetUnset
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import japgolly.scalajs.react.vdom.{HtmlTagOf, TagMod, VdomElement, VdomNode}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.Element

import scalacss.ScalaCssReact._
import scala.annotation.tailrec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.17 17:31
  * Description: Mutable-класс рендерера для quill-delta формата.
  *
  * Правила игры таковы:
  * - Кол-во \n в тексте соответствует кол-ву строк в документике.
  * - Операции insert:'\n' содержат line-level форматирование для всей прошедшей строки.
  *   Поэтому, сегменты текущей строки сбрасываются в отдельный аккамулятор.
  * - insert:'...' может содержать аттрибуты уровня сегмента или концы строк в тексте.
  * - Строки являются неявными, обходя их надо хранить состояние рендера.
  *   Например, заворачивать всю пачку строк в список.
  * - Форматирование сегментов текста внутри строк - тривиально, т.е. также как и в первой версии рендерера.
  *
  */
class QdRrrHtml(jdArgs: MJdArgs, qdTag: QdTag ) {

  import QdRrrHtml.LOG


  /** Аккамулятор сегментов рендера текущей строки.
    * Сбрасывается при каждом переходе на новую строку.
    */
  private var _currLineAccRev: List[TagMod] = Nil


  /** Аккамулятор строк в обратном порядке.
    *
    * Затем можно reverse и последовательно группировать по ключу,
    * чтобы отработать пакетное форматирование, распространяющиееся на неск.строк (list'ы например).
    */
  private var _linesAccRev: List[(Option[MQdAttrsLine], TagMod)] = Nil


  /** Набор delta-операций, подлежащих проработке.
    * В конце работы должен остаться Nil.
    */
  private var _restOps: List[MQdOp] = qdTag.ops.toList


  /** Reverse-акк, куда сбрасываются все окончательно отрендеренные данные. */
  //private var _finalAccRev: List[TagMod] = Nil


  /** Выполнить рендеринг текущего qd-тега. */
  @tailrec
  final def render(): VdomElement = {
    _restOps match {
      // Есть операция для обработки.
      case qdOp :: restOpsTail =>
        _restOps = restOpsTail
        // Надо обработать текущую операцию: поискать \n в тексте.
        // Если это текст, то текст может быть с \n или без \n.
        // Либо только "\n", что будет означать форматирование всей накопленной строки целиком.
        _renderOp( qdOp )
        render()


      // Больше не осталось операций для проработки
      case Nil =>
        // Если delta-синтаксис валиден, то currLineOpsAcc должен быть пустым благодаря финализирующей \n.
        if (_currLineAccRev.nonEmpty) {
          //throw new IllegalStateException("CR/LF error: " + qdTag)
          LOG.warn( WarnMsgs.QDELTA_FINAL_NEWLINE_PROBLEM, msg = qdTag )
          // Надо принудительно закрыть кривую строку.
          _handleEol()
        }
        // Пора перейти к рендеру строк.
        _renderLines()

    }
  }


  /** Прорендерить текущую операцию, распихав изменения по аккамуляторам. */
  private def _renderOp(qdOp: MQdOp): Unit = {
    qdOp.opType match {
      case MQdOpTypes.Insert =>
        qdOp.edgeInfo.fold[Unit] {
          // TODO Внешний embed?
          ???
        } { qdEi =>
          val e = jdArgs.renderArgs.edges(qdEi.edgeUid)
          e.predicate match {
            case MPredicates.Text =>
              // Рендер текста. Нужно отработать аттрибуты рендера текста.
              _insertText( e.text.get, qdOp )
            // TODO Надо осилить image через предикат
          }
        }
    }
  }


  /** Рендер insert-op с текстом. */
  private def _insertText(text: String, qdOp: MQdOp): Unit = {
    text match {
      // Специальный случай: тег завершения строки с возможной стилистикой всей прошедшей строки.
      case "\n" =>
        println("!!!!!!! NEW LINE !!!!!!!!!!!!")
        _handleEol( qdOp.attrsLine )
      // Это обычный текст. Но он может содержать в себе \n-символы в неограниченном количестве.
      case _ =>
        _insertPlainTextWithNls(text, qdOp)
    }
  }


  /** Отработать конец строки. */
  private def _handleEol(attrsOpt: Option[MQdAttrsLine] = None): Unit = {
    println("handleEol(): " + attrsOpt + " acc = " + _currLineAccRev)
    // Это операция рендера накопленной ранее строки. Развернуть отрендеренный контент текущей строки.
    val currLineContent = _currLineAccRev.reverse
    val lineContent = TagMod.fromTraversableOnce(currLineContent)
    _linesAccRev ::= (attrsOpt, lineContent)
    _currLineAccRev = Nil
    println(_linesAccRev)
  }


  /** Отработать просто текст с возможными \n внутри. */
  private def _insertPlainTextWithNls(text: String, qdOp: MQdOp): Unit = {
    val nlCh = HtmlConstants.NEWLINE_UNIX
    if (text contains nlCh) {
      // Внутри строки есть символы \n. Это бывает у простых строк без форматирования.
      val splits = text.split( nlCh )
      // Возможно, тут лишний \n появляется: в самом конце самой последней строки документа. TODO Но почему-то получается, что наоборот: так и надо, иначе возникает ошибка в самой последней строке. Почему?
      val lastSplitIndex = splits.length - 1
      for ((split, i) <- splits.iterator.zipWithIndex) {
        println(split, i, lastSplitIndex)
        _insertVeryPlainText(split, qdOp)
        if (i < lastSplitIndex)
          _handleEol()
      }

    } else {
      println(text)
      // Текст без переносов строк.
      _insertVeryPlainText(text, qdOp)
    }
  }


  private def _insertVeryPlainText(text: String, qdOp: MQdOp): Unit = {
    if ( !text.isEmpty ) {
      // Для максимальной скорости работы и некоторого удобства, тут много переменных.
      var acc: VdomNode = text

      // Обвешать текст заданной аттрибутикой
      for {
        attrs <- qdOp.attrsText
        if attrs.nonEmpty
      } {
        // Отрендерить цвет текста и цвет фона, одним style.
        // Стилизуем текст только через самый внутренний тег оформления текста, управляя этим через переменную.
        var textStyleOpt = OptionUtil.maybe[TagMod]( attrs.isCssStyled ) {
          jdArgs.jdCss.textStyleF( attrs )
        }

        // Рендер f() только по true-флагу внутри SetVal().
        def __rBool(boolSuOpt: Option[ISetUnset[Boolean]])(f: => HtmlTagOf[_ <: Element]): Unit = {
          for (boolSU <- boolSuOpt; bool <- boolSU.toOption if bool) {
            var fArgs: List[TagMod] = acc :: Nil
            for (textStyle <- textStyleOpt) {
              fArgs ::= textStyle
              // Опустошить акк стилей, чтобы на след.теге не было повторного остиливания:
              textStyleOpt = None
            }
            acc = f( fArgs: _* )
          }
        }

        // Орудуем с использованием __rBool() над различными аттрибутами форматирования текста:
        __rBool(attrs.bold)(<.strong)
        __rBool(attrs.italic)(<.em)
        __rBool(attrs.underline)(<.u)
        __rBool(attrs.strike)(<.s)

        // Отработать sup/sub теги
        for (qdScriptSU <- attrs.script; qdScript <- qdScriptSU) {
          val scriptTag = qdScript match {
            case MQdScripts.Super => <.sup
            case MQdScripts.Sub   => <.sub
          }
          var scriptAttrs = List[TagMod]( acc )
          for (textStyle <- textStyleOpt) {
            scriptAttrs ::= textStyle
            textStyleOpt = None
          }
          acc = scriptTag( scriptAttrs: _* )
        }

        // Если задан аттрибут link, то завернуть итоговый выхлоп в ссылку (с учётом возможного textStyleOpt).
        for (linkSU <- attrs.link; link <- linkSU) {
          var hrefAttrs = List[TagMod](
            ^.href := link,
            acc
          )
          for (textStyle <- textStyleOpt) {
            hrefAttrs ::= textStyle
            textStyleOpt = None
          }
          acc = <.a( hrefAttrs: _* )
        }

        // Если всё ещё требуется навесить css-стили на текст, но ни одного тега не было, то сделать span.
        for (textStyle <- textStyleOpt) {
          acc = <.span(
            textStyle,
            acc
          )
        }
      }

      _currLineAccRev ::= acc

    } else {
      println("******************* EMPTY TEXT !!!!!!!!!!!! " + text)
    }
  }


  /** Отработать аккамулятор строк, массово отформатировав все накопленные строки. */
  private def _renderLines(): VdomElement = {

    // Есть сценарий рендера пройденной группы.
    def __renderPrevLinesGrp(acc0: LinesRrrAcc, iStr: String): List[TagMod] = {
      acc0.currLineGrpAttrs.fold {
        // Нет аттрибутов -- нет и текущей группы.
        acc0.renderAcc
      } { grpAttrs =>
        // Есть уже открытая группа с аттрибутами. Закрыть и отрендерить её.
        _renderLinesGroup(grpAttrs, acc0.currLineGrpAcc, iStr) :: acc0.renderAcc
      }
    }

    // Пройтись последовательно по строкам, сгруппировав строки с одинаковыми аттрибутами.
    // Затем, каждую группу отправлять на line-format рендер.
    val acc9 = _linesAccRev
      .iterator
      .zipWithIndex
      .foldLeft( LinesRrrAcc() ) { case (acc0, ((attrsOpt, line), i)) =>
        val iStr = i.toString

        // Отработать аттрибуты строки, если они есть.
        attrsOpt.filter(_.nonEmpty).fold {
          // Нет строковых аттрибутов у текущей строки. Надо отрендерить предшествующую группу, если она есть.
          val renderAcc1 = __renderPrevLinesGrp(acc0, iStr)

          // Текущая строка аттрибутов не имеет, поэтому можно её сразу же рендерить без группирования.
          val firstLine = _renderLineAlone(
            line,
            ^.key := (iStr + "s")   // чтобы не было duplicate key
          )
          val renderAcc2 = firstLine :: renderAcc1

          // Залить весь рендер в аккамулятор, сбросив curr-grp поля в исходное состояние.
          acc0.copy(
            renderAcc         = renderAcc2,
            currLineGrpAttrs  = None,
            currLineGrpAcc    = Nil
          )

        } { attrs =>
          // Есть какие-то аттрибуты строки. Надо понять, совпадает ли это с текущей группой.
          if (acc0.currLineGrpAttrs contains attrs) {
            // Аттрибуты такие же, как и у предыдущих строк. Запихиваем в акк группы.
            acc0.copy(
              currLineGrpAcc = line :: acc0.currLineGrpAcc
            )
          } else {
            // Аттрибуты у данной строки НЕтакие, как у предыдущей группы строк. Отрендерить прошедшую группу строк, начав новую группу.
            val renderAcc1 = __renderPrevLinesGrp(acc0, iStr)

            // Обновить аккамулятор.
            acc0.copy(
              // Старая группа теперь отрендерена:
              renderAcc = renderAcc1,
              // Начать новую группу с аттрибутами текущей строки:
              currLineGrpAttrs = attrsOpt,
              currLineGrpAcc = line :: Nil
            )
          }

        }
      }

    // Убедиться, что последняя незакрытая группа отрендерена
    val renderAcc2 = __renderPrevLinesGrp( acc9, "x" )

    // Вернуть итог
    <.div(
      TagMod.fromTraversableOnce( renderAcc2 )
    )
  }

  private def _renderLineAlone(line: TagMod, tm0: TagMod): TagMod = {
    <.p(
      tm0,
      line
    )
  }

  private def _renderLinesGroup(attrs: MQdAttrsLine, lines: List[TagMod], keyStr: String): TagMod = {
    val customTm: TagMod = {
      // Компилим значение text-align
      val textAlignTm = attrs.align
        .flatMap(_.toOption)
        .fold( EmptyVdom ) { mTextAlign =>
          ^.`class` := Css.flat(
            jdArgs.jdCss.textAlignsStyleF( mTextAlign ).htmlClass,
            Css.Display.BLOCK
          )
        }

      val indentTm = attrs.indent
        .flatMap(_.toOption)
        .fold( EmptyVdom ) { indentLevel =>
          jdArgs.jdCss.indentStyleF( indentLevel )
        }

      TagMod( textAlignTm, indentTm )
    }

    // list: bullet, ordered
    _renderAttrSuOpt( attrs.list ) { listType =>
      val outerTag = listType match {
        case MQdListTypes.Bullet  => <.ul
        case MQdListTypes.Ordered => <.ol
      }
      outerTag(
        ^.key := keyStr,
        lines.iterator.zipWithIndex.toVdomArray { case (line, i) =>
          <.li(
            ^.key := i.toString,
            customTm,
            line
          )
        }
      )
    }
    // - header: 1, 2, 3, ...
    .orElse {
      _renderAttrSuOpt( attrs.header ) { headerLevel =>
        val htag = headerLevel match {
          case 1 => <.h1
          case 2 => <.h2
          case 3 => <.h3
          case 4 => <.h4
          case 5 => <.h5
          case _ => <.h6
        }

        // TODO key как-то надо присвоить? Завернуть в span?
        lines
          .iterator
          .zipWithIndex
          .toVdomArray { case (line, i) =>
            htag(
              ^.key := (keyStr + "." + i),
              customTm,
              line
            )
          }
      }
    }
    // Всякие остальные теги, обрамляющие все строки сразу.
    .getOrElse {
      val tagMods = (^.key := keyStr) ::
        customTm ::
        lines

      // code-block
      val tag = {
        _renderBoolAttrSuOpt( attrs.codeBlock )( <.pre )
          .orElse {
            // blockquote
            _renderBoolAttrSuOpt( attrs.blockQuote )( <.blockquote )
          }.getOrElse {
            // Нет отдельного исключительного формата строк: рендерим строки, как они есть.
            <.p
          }
      }

      tag(
        tagMods: _*
      )
    }

    // TODO indent

  }



  private def __attrSuOptFlatten[T](attrsSuOpt: Option[ISetUnset[T]]): Option[T] = {
    attrsSuOpt
      .flatMap(_.toOption)
  }

  private def _renderAttrSuOpt[T, R](attrsSuOpt: Option[ISetUnset[T]])(f: T => R): Option[R] = {
    __attrSuOptFlatten( attrsSuOpt )
      .map(f)
  }

  private def _renderBoolAttrSuOpt[R](attrsSuOpt: Option[ISetUnset[Boolean]])(f: => R): Option[R] = {
    __attrSuOptFlatten( attrsSuOpt )
      .filter(identity)
      .map(_ => f)
  }

}

/** Модель аккамулятора в _renderLines().
  * Аккамуляторы без "rev" в названиях, потому что исходный список строк уже в обратном порядке.
  * @param renderAcc Отрендеренные строки.
  * @param currLineGrpAttrs Аттрибуты строк группы строк.
  * @param currLineGrpAcc Аккамулятор текущей группы строк.
  */
protected case class LinesRrrAcc(
                                  renderAcc         : List[TagMod]          = Nil,
                                  currLineGrpAttrs  : Option[MQdAttrsLine]  = None,
                                  currLineGrpAcc    : List[TagMod]          = Nil
                                )


object QdRrrHtml extends Log


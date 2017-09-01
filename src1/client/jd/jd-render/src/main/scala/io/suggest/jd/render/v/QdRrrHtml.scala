package io.suggest.jd.render.v

import io.suggest.common.html.HtmlConstants
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.tags.qd._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.primo.ISetUnset
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import japgolly.scalajs.react.vdom.{HtmlTagOf, TagMod, VdomElement, VdomNode}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.Element

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
  private var _linesAccRev: List[(Option[MQdAttrs], TagMod)] = Nil


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
          LOG.error( WarnMsgs.QDELTA_FINAL_NEWLINE_PROBLEM, msg = qdTag )
          // Надо принудительно закрыть кривую строку.
          _handleEol( None )
        }
        // Всё ок, как и ожидалось. Перейти к рендеру строк.
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
              _insertText( e.text.get, qdOp.attrs )
            // TODO Надо осилить image через предикат
          }
        }
    }
  }


  /** Рендер insert-op с текстом. */
  private def _insertText(text: String, attrsOpt: Option[MQdAttrs]): Unit = {
    text match {
      // Специальный случай: тег завершения строки с возможной стилистикой.
      case "\n" =>
        _handleEol(attrsOpt)
      // Это обычный текст. Но он может содержать в себе \n-символы в неограниченном количестве.
      case _ =>
        _insertPlainTextWithNls(text, attrsOpt)
    }
  }


  /** Отработать конец строки. */
  private def _handleEol(attrsOpt: Option[MQdAttrs]): Unit = {
    // Это операция рендера накопленной ранее строки. Развернуть отрендеренный контент текущей строки.
    val currLineContent = _currLineAccRev.reverse
    _currLineAccRev = Nil
    val lineContent = TagMod.fromTraversableOnce(currLineContent)
    _linesAccRev ::= (attrsOpt, lineContent)
  }

  /** Отработать просто текст с возможными \n внутри. */
  private def _insertPlainTextWithNls(text: String, attrsOpt: Option[MQdAttrs]): Unit = {
    val nlCh = HtmlConstants.NEWLINE_UNIX
    if (text contains nlCh) {
      // Внутри строки есть символы \n. Это бывает у простых строк без форматирования.
      val splits = text.split( nlCh )
      val lastSplitIndex = splits.length - 1
      for ((split, i) <- splits.iterator.zipWithIndex) {
        _insertVeryPlainText(split, attrsOpt)
        if (i < lastSplitIndex)
          _handleEol( None )
      }

    } else {
      // Текст без переносов строк.
      _insertVeryPlainText(text, attrsOpt)
    }
  }


  private def _insertVeryPlainText(text: String, attrsOpt: Option[MQdAttrs]): Unit = {
    if ( !text.isEmpty ) {
      var acc: VdomNode = text

      // Обвешать текст заданной аттрибутикой
      for {
        attrs <- attrsOpt
        if attrs.nonEmpty
      } {
        // Рендер f() только по true-флагу в Set.
        def __rBool(boolSuOpt: Option[ISetUnset[Boolean]])(f: => HtmlTagOf[_ <: Element]): Unit = {
          for (boolSU <- boolSuOpt; bool <- boolSU.toOption if bool)
            acc = f(acc)
        }

        __rBool(attrs.bold)(<.strong)
        __rBool(attrs.italic)(<.em)
        __rBool(attrs.underline)(<.u)
      }

      _currLineAccRev ::= acc
    }
  }


  /** Отработать аккамулятор строк, массово отформатировав все накопленные строки. */
  private def _renderLines(): VdomElement = {

    // Пройтись последовательно по строкам, сгруппировав строки с одинаковыми аттрибутами.
    // Затем, каждую группу отправлять на line-format рендер.
    val renderedLineGroups = _linesAccRev
      .iterator
      .zipWithIndex
      .foldLeft( LinesRrrAcc() ) { case (acc0, ((attrsOpt, line), i)) =>
        val keyAttr = ^.key := i.toString

        if (attrsOpt.exists(_.nonEmpty)) {
          // Есть какие-то аттрибуты строки. Надо понять, совпадает ли это с текущей группой.
          val attrs = attrsOpt.get
          if (acc0.currLineGrpAttrs contains attrs) {
            // Аттрибуты такие же, как и у предыдущих строк. Запихиваем в акк группы.
            acc0.copy(
              currLineGrpAcc = line :: acc0.currLineGrpAcc
            )
          } else {
            // Аттрибуты у данной строки не такие, как у предыдущей группы строк. Опустошить текущую группу строк.
            val renderAcc1 = if (acc0.currLineGrpAcc.nonEmpty) {
              // Есть начатая группа. Рендерить её.
              _renderLinesGroup(attrs, acc0.currLineGrpAcc, keyAttr) :: acc0.renderAcc
            } else {
              // Нет начатой группы.
              acc0.renderAcc
            }

            // Обновить аккамулятор.
            acc0.copy(
              // Старая группа теперь отрендерена:
              renderAcc = renderAcc1,
              // Начать новую группу с аттрибутами текущей строки:
              currLineGrpAttrs = attrsOpt,
              currLineGrpAcc = line :: Nil
            )
          }

        } else {
          // Нет строковых аттрибутов у текущей строки. Возможно, надо опустошить текущую группу.
          // TODO дедублицировать с аналогичным кодом внутри if-then.
          val renderAcc1 = acc0.currLineGrpAttrs.fold {
            // Нет аттрибутов -- нет и текущей группы.
            acc0.renderAcc
          } { grpAttrs =>
            // Есть уже открытая группа с аттрибутами. Закрыть и отрендерить её.
            _renderLinesGroup(grpAttrs, acc0.currLineGrpAcc, keyAttr) :: acc0.renderAcc
          }

          // Текущая строка аттрибутов не имеет, поэтому можно её сразу же рендерить без группирования.
          val renderAcc2 = _renderLineAlone( line, keyAttr ) :: renderAcc1

          // Залить весь рендер в аккамулятор, сбросив curr-grp поля в исходное состояние.
          acc0.copy(
            renderAcc         = renderAcc2,
            currLineGrpAttrs  = None,
            currLineGrpAcc    = Nil
          )
        }
      }

    // Вернуть итог
    <.div(
      TagMod.fromTraversableOnce( renderedLineGroups.renderAcc )
    )
  }

  private def _renderLineAlone(line: TagMod, tm0: TagMod): TagMod = {
    <.p(
      tm0,
      line
    )
  }

  private def _renderLinesGroup(attrs: MQdAttrs, lines: List[TagMod], tm0: TagMod): TagMod = {
    // У группы строк могут быть такие аттрибуты:
    // - header: 1, 2, 3.
    // - list: ordered, bullet
    attrs.header
      .flatMap(_.toOption)
      .fold {
        // Это не header
        attrs.list
          .flatMap(_.toOption)
          .fold {
            // Хз что, какой-то неподдерживаемый сейчас аттрибут
            LOG.error( ErrorMsgs.UNSUPPORTED_TEXT_LINE_ATTRS, msg = attrs )
            TagMod.fromTraversableOnce( lines )
          } { listType =>
            val outerTag = listType match {
              case MQdListTypes.Bullet  => <.ul
              case MQdListTypes.Ordered => <.ol
            }
            outerTag(
              tm0,
              lines.iterator.zipWithIndex.toVdomArray { case (line, i) =>
                <.li(
                  ^.key := i.toString,
                  line
                )
              }
            )
          }

      } { headerLevel =>
        val htag = headerLevel match {
          case 1 => <.h1
          case 2 => <.h2
          case 3 => <.h3
          case _ => <.h4
        }

        // TODO key как-то надо присвоить? Завернуть в span?
        lines.iterator.zipWithIndex.toVdomArray { case (line, i) =>
          htag(
            ^.key := i.toString,
            line
          )
        }
      }
  }

}

/** Модель аккамулятора в _renderLines().
  * Аккамуляторы без "rev" в названиях, потому что исходный список строк уже в обратном порядке.
  * @param renderAcc Отрендеренные строки.
  * @param currLineGrpAttrs Аттрибуты строк группы строк.
  * @param currLineGrpAcc Аккамулятор текущей группы строк.
  */
protected case class LinesRrrAcc(
                                  renderAcc         : List[TagMod] = Nil,
                                  currLineGrpAttrs  : Option[MQdAttrs] = None,
                                  currLineGrpAcc    : List[TagMod] = Nil
                                )


object QdRrrHtml extends Log


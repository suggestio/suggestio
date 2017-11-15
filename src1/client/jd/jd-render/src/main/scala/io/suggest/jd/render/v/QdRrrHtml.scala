package io.suggest.jd.render.v

import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.JdTag.Implicits._
import io.suggest.jd.tags.qd._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.ISetUnset
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import japgolly.scalajs.react.vdom.{HtmlTagOf, TagMod, VdomElement}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.Element

import scalacss.ScalaCssReact._
import scala.annotation.tailrec
import scalaz.Tree

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
  * @param qdTag Текущий jd-тег для рендера.
  * @param imgEdgeMods Опциональная функция, возвращающая TagMod при рендере картинок.
  *                    Используется в редакторе для навешивания дополнительных листенеров.
  */
class QdRrrHtml(
                 jdArgs       : MJdArgs,
                 qdTag        : Tree[JdTag],
                 imgEdgeMods  : Option[MEdgeDataJs => TagMod] = None
               ) {

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
  private var _restOps: List[MQdOp] = {
    qdTag
      .qdOpsIter
      .toList
  }


  /** Выполнить рендеринг текущего qd-тега. */
  final def render(): VdomElement = {
    _doRender()
  }
  @tailrec
  private def _doRender(counters: QdRrrOpKeyCounters = QdRrrOpKeyCounters()): VdomElement = {
    _restOps match {
      // Есть операция для обработки.
      case qdOp :: restOpsTail =>
        _restOps = restOpsTail
        // Надо обработать текущую операцию: поискать \n в тексте.
        // Если это текст, то текст может быть с \n или без \n.
        // Либо только "\n", что будет означать форматирование всей накопленной строки целиком.
        val counters2 = _renderOp( qdOp, counters )
          .getOrElse( counters )

        _doRender( counters2 )


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
  private def _renderOp(qdOp: MQdOp, counters: QdRrrOpKeyCounters): Option[QdRrrOpKeyCounters] = {
    val counters2Opt = qdOp.opType match {
      case MQdOpTypes.Insert =>
        for {
          qdEi <- qdOp.edgeInfo
          e <- jdArgs.renderArgs.edges.get( qdEi.edgeUid )
        } yield {
          var videosCnt = counters.video
          var imagesCnt = counters.image
          var othersCnt = counters.other
          e.jdEdge.predicate match {
            case MPredicates.JdContent.Text =>
              // Рендер текста. Нужно отработать аттрибуты рендера текста.
              _insertText( e.jdEdge.text.get, qdOp, othersCnt )
              othersCnt += 1
            // Рендер картинки.
            case MPredicates.JdContent.Image =>
              _insertImage( e, qdOp, imagesCnt )
              imagesCnt += 1

            // Рендер видео.
            case MPredicates.JdContent.Video =>
              _insertVideo( e, qdOp, videosCnt )
              videosCnt += 1

            case other =>
              throw new UnsupportedOperationException( ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT + HtmlConstants.SPACE + (other, e) )
          }
          counters.copy(
            video = videosCnt,
            image = imagesCnt,
            other = othersCnt
          )
        }

      case other =>
        throw new UnsupportedOperationException( ErrorMsgs.NOT_IMPLEMENTED + HtmlConstants.SPACE + other )
    }
    if (counters2Opt.isEmpty)
      LOG.warn(ErrorMsgs.EDGE_NOT_EXISTS, msg = qdOp.edgeInfo)
    counters2Opt
  }



  /** Вставка картинки-изображения. */
  private def _insertImage(e: MEdgeDataJs, qdOp: MQdOp, i: Int): Unit = {
    val resOpt = for {
      // Определяем img.src. Quill не понимает blob-ссылки, только data или http.
      imgSrc <- e.imgSrcOpt
    } yield {
      // Аккамулируем аттрибуты для рендера img-тега.
      var imgArgsAcc = List.empty[TagMod]

      // width/height экранного представления картинки задаётся в CSS:
      val embedStyleOpt = for (embedAttrs <- qdOp.attrsEmbed) yield {
        val embStyl = jdArgs.jdCss.embedAttrStyleF( embedAttrs )
        imgArgsAcc ::= embStyl
        embStyl
      }

      // Если edit-режим, то запретить перетаскивание картинки, чтобы точно таскался весь QdTag сразу:
      if (jdArgs.conf.isEdit) {
        imgArgsAcc ::= (^.draggable := false)
      }

      // Доп.модификации извне.
      for (f <- imgEdgeMods)
        imgArgsAcc ::= f( e )

      // Наконец, отработать src (в самое начало списка -- просто на всякий случай).
      imgArgsAcc =
        (^.key := s"I$i") ::
        (^.src := imgSrc) ::
        imgArgsAcc

      var finalTm: TagMod = <.img(
        imgArgsAcc: _*
      )

      for (attrsText <- qdOp.attrsText if attrsText.isCssStyled) {
        val img = finalTm
        val CSS = Css
        finalTm = <.span(
          jdArgs.jdCss.textStyleF( attrsText ),
          embedStyleOpt.whenDefined,
          ^.`class` := CSS.flat(CSS.Display.INLINE_BLOCK, CSS.Overflow.HIDDEN),
          img
        )
      }

      _currLineAccRev ::= finalTm
    }

    if (resOpt.isEmpty)
      LOG.warn(ErrorMsgs.IMG_EXPECTED, msg = e)
  }


  /** Рендер video. */
  private def _insertVideo(e: MEdgeDataJs, qdOp: MQdOp, i: Int): Unit = {
    val resOpt = for {
      src <- e.jdEdge.url
    } yield {
      _currLineAccRev ::= <.iframe(
        ^.src := src,
        ^.key := s"V$i",
        ^.allowFullScreen := true,
        jdArgs.jdCss.videoStyle  //videoStyleF( (qdOp, e) )
      )
    }

    if (resOpt.isEmpty)
      LOG.warn(ErrorMsgs.VIDEO_EXPECTED, msg = e)
  }


  /** Рендер insert-op с текстом. */
  private def _insertText(text: String, qdOp: MQdOp, i: Int): Unit = {
    if (text matches "[\n]+") {
      // Специальный случай: тег завершения строки с возможной стилистикой всей прошедшей строки.
      for (_ <- 1 to text.length)
        _handleEol( qdOp.attrsLine, Some(i) )
    } else {
      // Это обычный текст. Но он может содержать в себе \n-символы в неограниченном количестве.
      _insertPlainTextWithNls(text, qdOp, i)
    }
  }

  private def _emptyLineContent(iOpt: Option[Int] = None): TagMod = {
    _emptyLineContent1( iOpt.map(_.toString) )
  }
  private def _emptyLineContent1(iOpt: Option[String] = None): TagMod = {
    val tag = <.br
    iOpt.fold[TagMod](tag) { i => ^.key := i }
  }

  /** Отработать конец строки. */
  private def _handleEol(attrsOpt: Option[MQdAttrsLine] = None, iOpt: Option[Int] = None): Unit = {
    // Это операция рендера накопленной ранее строки. Развернуть отрендеренный контент текущей строки.
    if (_currLineAccRev.isEmpty) {
      // Бывает, что данных в акке нет. Поэтому нужно исправить рендер этим костылём.
      _currLineAccRev ::= _emptyLineContent(iOpt)
    }

    val currLineContent = _currLineAccRev.reverse
    val lineContent = TagMod.fromTraversableOnce(currLineContent)
    _linesAccRev ::= (attrsOpt, lineContent)
    _currLineAccRev = Nil
  }


  /** Нарезать строку по \n.
    * Символы \n очень важны в quill-выхлопе, нельзя их терять или рубить неаккуратно.
    */
  /** Отработать просто текст с возможными \n внутри. */
  private def _insertPlainTextWithNls(text: String, qdOp: MQdOp, i: Int): Unit = {
    val nlCh = HtmlConstants.NEWLINE_UNIX
    if (text contains nlCh) {
      // Внутри строки есть символы \n. Обычно, такое возможно при отсуствии какого-либо форматирования.
      // -1 -- иначе "...\n" в конце текста будут утрачены. https://stackoverflow.com/a/14602089
      val splits = text.split( nlCh.toString, -1 )

      // Возможно, тут лишний \n появляется: в самом конце самой последней строки документа.
      val lastSplitIndex = splits.length - 1
      for {
        (split, j) <- splits.iterator.zipWithIndex
      } {
        // /!\ Острожно: здесь самый дикий быдлокод
        // Quill всегда добавляет в конец последней операции \n из каких-то благих намерений.
        // Его нужно правильно отрабатывать, т.е. срезать.
        val isNotLastSplit = j < lastSplitIndex
        if (isNotLastSplit || split.nonEmpty || qdOp.attrsLine.nonEmpty)
          _insertVeryPlainText(split, qdOp, s"$i.$j")

        if (isNotLastSplit)
          _handleEol()
      }

    } else {
      // Текст без переносов строк.
      _insertVeryPlainText(text, qdOp, i.toString)
    }
  }


  private def _insertVeryPlainText(text0: String, qdOp: MQdOp, keyPrefix: String): Unit = {
    // Для максимальной скорости работы и некоторого удобства, тут много переменных.
    var acc: TagMod = if ( text0.isEmpty ) {
      _emptyLineContent1( Some(keyPrefix) )
    } else {
      text0
    }

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

      val keyTm = ^.key := keyPrefix

      // Рендер f() только по true-флагу внутри SetVal().
      def __rBool(boolSuOpt: Option[ISetUnset[Boolean]])(f: => HtmlTagOf[_ <: Element]): Unit = {
        for (boolSU <- boolSuOpt; bool <- boolSU.toOption if bool) {
          var fArgs: List[TagMod] = acc :: keyTm :: Nil
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
        var scriptAttrs = List[TagMod]( keyTm, acc )
        for (textStyle <- textStyleOpt) {
          scriptAttrs ::= textStyle
          textStyleOpt = None
        }
        acc = scriptTag( scriptAttrs: _* )
      }

      // Если задан аттрибут link, то завернуть итоговый выхлоп в ссылку (с учётом возможного textStyleOpt).
      for (linkSU <- attrs.link; link <- linkSU) {
        var hrefAttrs = List[TagMod](
          keyTm,
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
          keyTm,
          textStyle,
          acc
        )
      }
    }

    _currLineAccRev ::= acc
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
    val linesAccLen = _linesAccRev.size
    val acc9 = _linesAccRev
      .iterator
      .zipWithIndex
      .foldLeft( LinesRrrAcc() ) { case (acc0, ((attrsOpt, line), iRev)) =>
        val i = linesAccLen - iRev
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

        // Присваиваем внутри списка свои уникальные под-ключи относительно исходного ключа.
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



/** Модель счётчиков для выставления оптимальных порядковых react key.
  * Для снижения кол-ва пере-рендеров использются раздельные счётчики для разных вариантов.
  * Т.е. отредерив текст инкрементится other, а после реднера видео инкрементится video.
  *
  * Это позволяет избежать пере-рендера video-фрейма при добавлении новой строки в тексте перед видео.
  */
protected sealed case class QdRrrOpKeyCounters(
                                                image   : Int     = 0,
                                                video   : Int     = 0,
                                                other   : Int     = 0
                                              )


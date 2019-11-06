package io.suggest.jd.render.v

import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.jd.MJdTagId
import io.suggest.jd.render.m.MJdRrrProps
import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.qd._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.ISetUnset
import io.suggest.sjs.common.log.Log
import japgolly.scalajs.react.{Callback, ReactMouseEventFromHtml}
import japgolly.scalajs.react.vdom.{HtmlTagOf, TagMod, VdomElement}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.Element
import scalacss.ScalaCssReact._

import scala.annotation.tailrec
import japgolly.univeq._

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
  * @param rrrProps Данные текущего jd-тег для рендера (QD_CONTENT).
  * @param imgEdgeMods Опциональная функция, возвращающая TagMod при рендере картинок.
  *                    Используется в редакторе для навешивания дополнительных листенеров.
  */
case class QdRrrHtml(
                      jdCssStatic  : JdCssStatic,
                      rrrProps     : MJdRrrProps,
                      imgEdgeMods  : Option[MEdgeDataJs => TagMod] = None,
                      resizableCb  : Option[(MQdOp, MEdgeDataJs, Boolean, ReactMouseEventFromHtml) => Callback] = None,
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
  private var _restOps: Stream[MQdOpCont] = {
    for {
      (jdtTree, i) <- rrrProps.subTree
        .subForest
        .zipWithIndex
      jdt = jdtTree.rootLabel
      qdOp <- jdt.qdProps
    } yield {
      MQdOpCont(
        qdOp    = qdOp,
        jdTag   = jdt,
        jdTagId = MJdTagId.selPathRev
          .modify(i :: _)(rrrProps.tagId),
      )
    }
  }

  /** Аттрибут href для ссылки, чтобы не было паразитных переходов в редакторе. */
  private def _hrefAttr =
    if (rrrProps.jdArgs.conf.isEdit) ^.title else ^.href


  /** Выполнить рендеринг текущего qd-тега. */
  final def render(): VdomElement = {
    _doRender()
  }
  @tailrec
  private def _doRender(counters: QdRrrOpKeyCounters = QdRrrOpKeyCounters()): VdomElement = {
    _restOps match {
      // Есть операция для обработки.
      case qdOp #:: restOpsTail =>
        _restOps = restOpsTail
        // Надо обработать текущую операцию: поискать \n в тексте.
        // Если это текст, то текст может быть с \n или без \n.
        // Либо только "\n", что будет означать форматирование всей накопленной строки целиком.
        val counters2 = _renderOp( qdOp, counters )
          .getOrElse( counters )

        _doRender( counters2 )


      // Больше не осталось операций для проработки
      case _ =>
        // Если delta-синтаксис валиден, то currLineOpsAcc должен быть пустым благодаря финализирующей \n.
        if (_currLineAccRev.nonEmpty) {
          //throw new IllegalStateException("CR/LF error: " + qdTag)
          LOG.warn( WarnMsgs.QDELTA_FINAL_NEWLINE_PROBLEM, msg = rrrProps.subTree )
          // Надо принудительно закрыть кривую строку.
          _handleEol()
        }
        // Пора перейти к рендеру строк.
        _renderLines()

    }
  }


  /** Прорендерить текущую операцию, распихав изменения по аккамуляторам. */
  private def _renderOp(jdtQdOp: MQdOpCont, counters: QdRrrOpKeyCounters): Option[QdRrrOpKeyCounters] = {
    val counters2Opt = jdtQdOp.qdOp.opType match {
      case MQdOpTypes.Insert =>
        for {
          qdEi <- jdtQdOp.qdOp.edgeInfo
          e <- rrrProps.jdArgs.data.edges.get( qdEi.edgeUid )
        } yield {
          var framesCnt = counters.frame
          var imagesCnt = counters.image
          var othersCnt = counters.other
          e.jdEdge.predicate match {
            case MPredicates.JdContent.Text =>
              // Рендер текста. Нужно отработать аттрибуты рендера текста.
              _insertText( e.jdEdge.text.get, jdtQdOp, othersCnt )
              othersCnt += 1
            // Рендер картинки.
            case MPredicates.JdContent.Image =>
              _insertImage( e, jdtQdOp, imagesCnt )
              imagesCnt += 1

            // Рендер видео (или иного фрейма).
            case MPredicates.JdContent.Frame =>
              _insertFrame( e, jdtQdOp, framesCnt )
              framesCnt += 1

            case other =>
              throw new UnsupportedOperationException( ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT + HtmlConstants.SPACE + (other, e) )
          }
          counters.copy(
            frame = framesCnt,
            image = imagesCnt,
            other = othersCnt
          )
        }

      case other =>
        throw new UnsupportedOperationException( ErrorMsgs.NOT_IMPLEMENTED + HtmlConstants.SPACE + other )
    }
    if (counters2Opt.isEmpty)
      LOG.warn(ErrorMsgs.EDGE_NOT_EXISTS, msg = jdtQdOp.qdOp.edgeInfo)
    counters2Opt
  }



  /** Вставка картинки-изображения. */
  private def _insertImage(e: MEdgeDataJs, jdtQdOp: MQdOpCont, i: Int): Unit = {
    val resOpt = for {
      // Определяем img.src. Quill не понимает blob-ссылки, только data или http.
      imgSrc <- e.imgSrcOpt
    } yield {
      // Аккамулируем аттрибуты для рендера img-тега.
      var imgArgsAcc = List.empty[TagMod]

      // width/height экранного представления картинки задаётся в CSS:
      // Контейнер ресайза также требует этот стиль, поэтому кэшируем стиль в переменной:
      val embedStyleOpt = for (ae <- jdtQdOp.qdOp.attrsEmbed if ae.nonEmpty) yield {
        rrrProps.jdArgs.jdRuntime.jdCss.embedAttrF( jdtQdOp.jdTagId )
      }
      embedStyleOpt.foreach( imgArgsAcc ::= _ )

      // Если edit-режим, то запретить перетаскивание картинки, чтобы точно таскался весь QdTag сразу:
      if (rrrProps.jdArgs.conf.isEdit)
        imgArgsAcc ::= (^.draggable := false)

      // Доп.модификации извне.
      for (f <- imgEdgeMods)
        imgArgsAcc ::= f( e )

      val keyTm = ^.key := i

      // Наконец, отработать src (в самое начало списка -- просто на всякий случай).
      imgArgsAcc =
        keyTm ::
        (^.src := imgSrc) ::
        imgArgsAcc

      for {
        attrsText <- jdtQdOp.qdOp.attrsText
        if jdtQdOp.jdTag.props1.isContentCssStyled || attrsText.isCssStyled
      } {
        imgArgsAcc ::= rrrProps.jdArgs.jdRuntime.jdCss.textF( jdtQdOp.jdTagId )
      }

      var finalTm: TagMod = <.img(
        imgArgsAcc: _*
      )

      // Поддержка рендера внутри a-тега (ссылка). В редакторе не рендерим её, чтобы не было случайных переходов при кликах по шаблону.
      for (attrsText <- jdtQdOp.qdOp.attrsText; linkSu <- attrsText.link; link <- linkSu) {
        finalTm = <.a(
          keyTm,
          // Если редактор открыт, то не надо рендерить ссылку кликабельной. Просто пусть будет подсказка.
          _hrefAttr := link,
          finalTm
        )
      }

      // Поддержка горизонтального ресайза картинки/видео.
      for {
        resizableF      <- resizableCb
        origWh          <- e.origWh
        attrsEmbed      <- jdtQdOp.qdOp.attrsEmbed
        embedStyle      <- embedStyleOpt
      } {
        // TODO Надо бы сделать маску поверх картинки через div здесь. Это решит проблемы в хроме. Для этого надо провести высоту картинки, не сохраняя её в аттрибутах.
        // Вычислить визуальную ширину в css-пикселях. Она нужна для рассчёта отображаемой ВЫСОТЫ покрывающей маски.
        val maskHeightPx = attrsEmbed
          .width
          .flatMap(_.toOption)
          .fold( origWh.height ) { displayWidthPx =>
            (displayWidthPx.toDouble / origWh.width.toDouble * origWh.height).toInt
          }

        finalTm = <.div(
          keyTm,
          ^.`class` := Css.flat(Css.Display.INLINE_BLOCK, Css.Position.RELATIVE),
          finalTm,
          <.div(
            jdCssStatic.horizResizable,
            ^.onMouseUp ==> { event: ReactMouseEventFromHtml =>
              resizableF(jdtQdOp.qdOp, e, false, event)
            },
            ^.height := maskHeightPx.px,
            embedStyle,
            ^.`class` := Css.flat(Css.Overflow.HIDDEN, Css.Position.ABSOLUTE)
          )
        )
      }

      _currLineAccRev ::= finalTm
    }

    if (resOpt.isEmpty)
      LOG.warn(ErrorMsgs.IMG_EXPECTED, msg = e)
  }


  /** Рендер video. */
  private def _insertFrame(e: MEdgeDataJs, jdtQdOp: MQdOpCont, i: Int): Unit = {
    val resOpt = for {
      src <- e.jdEdge.url
    } yield {
      val whStyl = jdtQdOp.qdOp.attrsEmbed
        .filter(_.nonEmpty)
        .fold( rrrProps.jdArgs.jdRuntime.jdCss.video ) { _ =>
          rrrProps.jdArgs.jdRuntime.jdCss.embedAttrF( jdtQdOp.jdTagId )
        }
      val keyV = "V" + i
      val iframe = <.iframe(
        ^.src := src,
        ^.key := keyV,
        ^.allowFullScreen := true,
        whStyl
      )

      val tm = if ( rrrProps.jdArgs.conf.isEdit ) {
        // Для редактора используем div-контейнер, чтобы меньше мигало видео в редакторе.
        var outerAcc = List.empty[TagMod]
        outerAcc ::= <.div(
          ^.key := (keyV + "z"),
          ^.`class` := Css.flat( Css.Position.ABSOLUTE, Css.Overflow.HIDDEN ),
          whStyl,
          resizableCb.whenDefined { resizableF =>
            TagMod(
              jdCssStatic.hvResizable,
              ^.onMouseUp ==> { event: ReactMouseEventFromHtml =>
                resizableF(jdtQdOp.qdOp, e, true, event)
              }
            )
          }
        )
        outerAcc =
          (^.key := (keyV + "c")) ::
          (^.`class` := Css.flat(Css.Position.RELATIVE, Css.Display.INLINE_BLOCK)) ::
          iframe ::
          outerAcc
        <.div( outerAcc: _* )

      } else {
        // Видео-фрейм без дополнительного div-контейнера.
        iframe
      }

      _currLineAccRev ::= tm
    }

    if (resOpt.isEmpty)
      LOG.warn(ErrorMsgs.VIDEO_EXPECTED, msg = e)
  }


  /** Рендер insert-op с текстом. */
  private def _insertText(text: String, jdtQdOp: MQdOpCont, i: Int): Unit = {
    if (text matches "[\n]+") {
      // Специальный случай: тег завершения строки с возможной стилистикой всей прошедшей строки.
      for (j <- 1 to text.length)
        _handleEol( jdtQdOp.qdOp.attrsLine, Some(i), Some(j) )
    } else {
      // Это обычный текст. Но он может содержать в себе \n-символы в неограниченном количестве.
      _insertPlainTextWithNls(text, jdtQdOp, i)
    }
  }

  private def _emptyLineContent1(key: String): TagMod = {
    <.br(
      ^.key := key
    )
  }

  /** Отработать конец строки. */
  private def _handleEol(attrsOpt: Option[MQdAttrsLine] = None, iOpt: Option[Int] = None, jOpt: Option[Int] = None): Unit = {
    // Это операция рендера накопленной ранее строки. Развернуть отрендеренный контент текущей строки.
    if (_currLineAccRev.isEmpty) {
      val key = "n" + iOpt.getOrElse("") + HtmlConstants.`.` + jOpt.getOrElse("")
      // Бывает, что данных в акке нет. Поэтому нужно исправить рендер этим костылём.
      _currLineAccRev ::= _emptyLineContent1(key)
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
  private def _insertPlainTextWithNls(text: String, jdtQdOp: MQdOpCont, i: Int): Unit = {
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
        if (isNotLastSplit || split.nonEmpty || jdtQdOp.qdOp.attrsLine.nonEmpty)
          _insertVeryPlainText(split, jdtQdOp, s"$i.$j")

        if (isNotLastSplit)
          _handleEol()
      }

    } else {
      // Текст без переносов строк.
      _insertVeryPlainText(text, jdtQdOp, i.toString)
    }
  }


  private def _insertVeryPlainText(text0: String, jdtQdOp: MQdOpCont, keyPrefix: String): Unit = {
    // Для максимальной скорости работы и некоторого удобства, тут много переменных.
    var acc: TagMod = if ( text0.isEmpty ) {
      _emptyLineContent1( keyPrefix )
    } else {
      text0
    }

    // Обвешать текст заданной аттрибутикой
    for {
      attrs <- jdtQdOp.qdOp.attrsText
      if attrs.nonEmpty
    } {
      // Отрендерить цвет текста и цвет фона, одним style.
      // Стилизуем текст только через самый внутренний тег оформления текста, управляя этим через переменную.
      var textStyleOpt = OptionUtil.maybe[TagMod]( attrs.isCssStyled ) {
        rrrProps.jdArgs.jdRuntime.jdCss.textF( jdtQdOp.jdTagId )
      }

      // Выставить lineHeight, если он не выставлен на уровне qd-content тега:
      for {
        szSU <- attrs.size
        if !rrrProps.parent.exists(_.props1.lineHeight.nonEmpty)
        sz   <- szSU
      } {
        val tm: TagMod = rrrProps.jdArgs.jdRuntime.jdCss.lineHeightF( sz.lineHeight )
        val tms = tm :: textStyleOpt.toList
        TagMod( tms: _* )
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
          _hrefAttr := link,
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
          // TODO Тут вызывается isGroupsWith=>true|false, но случаев намного больше двух. См.комменты в .isGroupsWith().
          if (acc0.currLineGrpAttrs.exists(_ isGroupsWith attrs)) {
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
        .fold( TagMod.empty ) { mTextAlign =>
          ^.`class` := Css.flat(
            jdCssStatic.textAlignsStyleF( mTextAlign ).htmlClass,
            Css.Display.BLOCK
          )
        }

      val indentTm = attrs.indent
        .flatMap(_.toOption)
        .fold( TagMod.empty ) { indentLevel =>
          jdCssStatic.indentStyleF( indentLevel )
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
                                                frame   : Int     = 0,
                                                other   : Int     = 0
                                              )


protected final case class MQdOpCont(
                                      qdOp        : MQdOp,
                                      jdTag       : JdTag,
                                      jdTagId     : MJdTagId,
                                    )

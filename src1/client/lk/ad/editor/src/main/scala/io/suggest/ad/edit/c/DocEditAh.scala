package io.suggest.ad.edit.c

import diode._
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.edit.{MAddS, MQdEditS}
import io.suggest.color.MColorData
import io.suggest.common.MHands
import io.suggest.common.coll.Lists
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.html.HtmlConstants
import io.suggest.file.MJsFileInfo
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.render.m._
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags.IDocTag.Implicits._
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd._
import io.suggest.model.n2.edge.{EdgeUid_t, EdgesUtil, MPredicates}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.Base64JsUtil
import io.suggest.quill.m.TextChanged
import io.suggest.quill.u.QuillDeltaJsUtil
import io.suggest.react.ReactDiodeUtil
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import org.scalajs.dom.raw.URL

import scala.util.Random

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 14:34
  * Description: Контроллер событий редактирования документа.
  */
class DocEditAh[M](
                    modelRW           : ModelRW[M, MDocS],
                    jdCssFactory      : JdCssFactory,
                    quillDeltaJsUtil  : QuillDeltaJsUtil
                  )
  extends ActionHandler( modelRW )
  with Log
{

  /** Пост-процессинг изменившихся эджей.
    * Удобен после внесения каких-то изменений из quill.
    *
    * @param oldEdges Карта старых эджей.
    * @param tpl Обновлённый шаблон
    * @param edges Новая карта эджей.
    * @return Возможный эффект и обновлённая карта эджей.
    */
  private def _ppEdges(oldEdges: Map[EdgeUid_t, MEdgeDataJs],
                       tpl: IDocTag,
                       edges: Map[EdgeUid_t, MEdgeDataJs]): (Option[Effect], Map[EdgeUid_t, MEdgeDataJs]) = {
    // 10. Очистить эджи от неиспользуемых.
    val edges2 = quillDeltaJsUtil.purgeUnusedEdges( tpl, edges )

    // 20. Необходимо организовать блобификацию файлов эджей, заданных через dataURL.
    val dataPrefix = HtmlConstants.Proto.DATA_
    val blobEffectsIter = edges2
      .valuesIterator
      .flatMap[Effect] { edgeData =>
        val jde = edgeData.jdEdge
        // Три варианта:
        // - Просто эдж, который надо молча завернуть в EdgeData. Текст, например.
        // - Эдж, сейчас который проходит асинхронную процедуру приведения к блобу. Он уже есть в исходной карте эджей со ссылкой в виде base64.
        // - Эдж, который с base64-URL появился в новой карте, но отсутсвует в старой. Нужно запустить его блоббирование.
        val blobFxOpt = jde.url
          .filter(_ startsWith dataPrefix)
          .fold [Option[Effect]] {
            // Это не-dataURL. А blob или просто без URL. В любом случае -- пропуск без изменений.
            None
          } { dataUrl =>
            // Это dataURL. Тут два варианта: юзер загрузил новую картинку только что, либо загружена ранее.
            // Смотрим в old-эджи, есть ли там текущий эдж с этой картинкой.
            oldEdges
              .get( jde.id )
              .fold [Option[Effect]] {
                // Это новая картинка. Организовать перегонку в blob.
                val fx = Effect {
                  val fut = for (blob <- Base64JsUtil.b64Url2Blob(dataUrl)) yield {
                    B64toBlobDone(dataUrl, blob)
                  }
                  for (ex <- fut.failed)
                    LOG.error(ErrorMsgs.BASE64_TO_BLOB_FAILED, ex = ex)
                  fut
                }
                Some(fx)
              } { _ =>
                // Этот dataURL-эдж уже был ранее, значит blob уже должен обрабатываться в фоне.
                None
              }
          }

        // Аккамулировать эффект и обновлённый data-edge
        blobFxOpt
      }

    // 90. Объединить все собранные эффекты воедино.
    val totalFxOpt = ReactDiodeUtil.mergeEffectsSet( blobEffectsIter )

    // Вернуть итоговую карту эджей и объединённый эффект.
    (totalFxOpt, edges2)
  }



  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Набор текста в wysiwyg-редакторе.
    case m: TextChanged =>
      val v0 = value

      if ( v0.qdEdit.exists(_.initDelta == m.fullDelta) ) {
        // Бывают ложные срабатывания. Например, прямо при инициализации редактора. Но не факт конечно, что они тут подавляются.
        noChange

      } else {
        // Текст действительно изменился. Пересобрать json-document.
        //println( JSON.stringify(m.fullDelta) )
        val currTag0 = v0.jdArgs.selectedTag.get
        // Спроецировать карту сборных эджей в jd-эджи
        val edgesData0 = v0.jdArgs.renderArgs.edges
        val (qdTag2, edgesData2) = quillDeltaJsUtil.delta2qdTag(m.fullDelta, currTag0, edgesData0)

        // Собрать новый json-document
        val jsonDoc2 = v0.jdArgs
          .template
          .deepUpdateChild( currTag0, qdTag2 :: Nil )
          .head

        // Пост-процессить новые эджи, т.к. там может быть мусор или эджи, требующие фоновой обработи.
        val (fxOpt, edgesData3) = _ppEdges( edgesData0, jsonDoc2, edgesData2 )

        val jdArgs2 = v0.jdArgs.copy(
          template    = jsonDoc2,
          renderArgs  = v0.jdArgs.renderArgs
            .withEdges( edgesData3 ),
          selectedTag = Some(qdTag2),
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(jsonDoc2, v0.jdArgs.conf, edgesData3)
          )
        )

        // Залить все данные в новое состояние.
        val v2 = v0
          .withJdArgs(
            jdArgs = jdArgs2
          )
          // Не обновляем init-дельту при редактировании, заменяем только актуальный инстанс.
          .withQdEdit(
            for (qdEdit <- v0.qdEdit) yield {
              qdEdit.withRealDelta( Some(m.fullDelta) )
            }
          )

        // Объединить все эффекты, если они есть.
        fxOpt.fold( updated(v2) ) { updated(v2, _) }
      }


    // Клик по элементу карточки.
    case m: IJdTagClick =>
      val v0 = value
      if (v0.jdArgs.selectedTag contains m.jdTag) {
        // Бывают повторные щелчки по уже выбранным элементам, это нормально.
        noChange

      } else {
        // Юзер выбрал какой-то новый элемент. Залить новый тег в seleted:
        val v1 = v0.withJdArgs(
          v0.jdArgs.withSelectedTag( Some(m.jdTag) )
        )

        // Если это QdTag, то отработать состояние quill-delta:
        val v2 = m.jdTag.jdTagName match {
          // Это qd-тег, значит нужно собрать и залить текущую дельту текста в состояние.
          case MJdTagNames.QUILL_DELTA =>
            val delta2 = quillDeltaJsUtil.qdTag2delta(
              qd    = m.jdTag,
              edges = v0.jdArgs.renderArgs.edges
            )
            v1.withQdEdit(
              Some(
                MQdEditS(
                  initDelta  = delta2
                )
              )
            )
          // Очистить состояние от дельты.
          case _ =>
            v1.withOutQdEdit
        }

        // Если это strip, то активировать состояние strip-редактора.
        val v3 = m.jdTag.jdTagName match {
          // Переключение на новый стрип. Инициализировать состояние stripEd:
          case MJdTagNames.STRIP =>
            v2.withStripEd(
              Some(MStripEdS(
                isLastStrip = {
                  val hasManyStrips = v0.jdArgs.template
                    .deepOfTypeIter( MJdTagNames.STRIP )
                    // Оптимизация: НЕ проходим весь strip-итератор, а считаем только первые два стрипа.
                    .slice(0, 2)
                    .size > 1
                  !hasManyStrips
                }
              ))
            )
          // Это не strip, обнулить состояние stripEd, если оно существует:
          case _ =>
            v2.withOutStripEd
        }

        // Может быть, был какой-то qd-tag и весь текст теперь в нём удалён? Удалить, если старый тег, если осталась дельта
        val v4 = v0.jdArgs.selectedTag.fold(v3) { jdt =>
          val dataEdges0 = v0.jdArgs.renderArgs.edges
          if (
            jdt.jdTagName == MJdTagNames.QUILL_DELTA &&
            QdJsUtil.isEmpty(jdt, dataEdges0) &&
            v3.jdArgs.template.contains(jdt)
          ) {
            val tpl2 = v3.jdArgs.template.deepUpdateOne(jdt, Nil)
              .head
              // Нужно shrink'ать, потому что иначе могут быть пустые AbsPos() теги.
              .shrink
              .head
            // Очистить эджи от лишнего контента
            val dataEdges2 = quillDeltaJsUtil.purgeUnusedEdges(tpl2, dataEdges0)
            v3.withJdArgs(
              v3.jdArgs.copy(
                template    = tpl2,
                renderArgs  = v3.jdArgs.renderArgs
                  .withEdges( dataEdges2 ),
                jdCss       = jdCssFactory.mkJdCss(
                  MJdCssArgs.singleCssArgs(tpl2, v3.jdArgs.conf, dataEdges2)
                )
              )
            )
          } else {
            v3
          }
        }

        updated( v4 )
      }


    // Завершена фоновая конвертация base64-URL в Blob.
    case m: B64toBlobDone =>
      val v0 = value
      val dataEdgesMap0 = v0.jdArgs.renderArgs.edges

      // Вычислить обновлённый эдж, если есть старый эдж для данной картинки.
      val dataEdgeOpt2 = for {
        // Поиска по исходной URL, потому что карта эджей могла изменится за время фоновой задачи.
        dataEdge0 <- dataEdgesMap0.valuesIterator.find { e =>
          e.jdEdge.imgSrcOpt contains m.b64Url
        }
      } yield {
        // Найден исходный эдж. Залить в него инфу по блобу, выкинув оттуда dataURL:
        val blobUrl = URL.createObjectURL( m.blob )
        val blobUrlOpt = Option( blobUrl )
        // Нельзя забыть Base64 dataURL, потому что quill их не понимает, заменяя их на "//:0".
        // Сохранить инфу по блобу.
        val fileJs2 = dataEdge0.fileJs.fold {
          MJsFileInfo(
            blob = m.blob,
            blobUrl = blobUrlOpt
          )
        } { fileJs0 =>
          // Ссылка изменилась на blob, но нельзя трогать delta: quill не поддерживает blob-ссылки.
          fileJs0
            .withBlob( m.blob )
            .withBlobUrl( blobUrlOpt )
        }
        val dataEdge1 = dataEdge0
          .withFileJs( Some(fileJs2) )
        (dataEdge1, blobUrl)
      }

      // Залить в состояние обновлённый эдж.
      dataEdgeOpt2.fold {
        LOG.warn( WarnMsgs.SOURCE_FILE_NOT_FOUND, msg = m.blob )
        noChange
      } { case (dataEdge2, blobUrl) =>
        val dataEdgesMap1 = dataEdgesMap0.updated(dataEdge2.id, dataEdge2)
        val dataEdgesMap2 = quillDeltaJsUtil.purgeUnusedEdges(v0.jdArgs.template, dataEdgesMap1)
        val v2 = v0
          .withJdArgs(
            v0.jdArgs.withRenderArgs(
              v0.jdArgs.renderArgs.withEdges(
                dataEdgesMap2
              )
            )
          )

        // Запустить эффект хэширования и дальнейшей закачки файла на сервер.
        val hashFx = Effect.action( FileHashStart(dataEdge2.id, blobUrl) )
        updated(v2, hashFx)
      }


    // Поступила команда на проведение чистки карты эджей.
    case PurgeUnusedEdges =>
      val v0 = value
      val edges0 = v0.jdArgs.renderArgs.edges
      val edges2 = quillDeltaJsUtil.purgeUnusedEdges( v0.jdArgs.template, edges0 )
      if ( edges0.size == edges2.size ) {
        noChange
      } else {
        //println(s"edges count changed: ${edges0.size} => ${edges2.size}")
        val v2 = v0.withJdArgs(
          v0.jdArgs.withRenderArgs(
            v0.jdArgs.renderArgs.withEdges(
              edges2
            )
          )
        )
        updated(v2)
      }


    // Клик по кнопкам управления размером текущего блока
    case m: BlockSizeBtnClick =>
      val v0 = value
      val strip0 = v0.jdArgs.selectedTag.get
      val bm0 = strip0.props1.bm.get
      val bm2 = m.model match {
        case bhs @ BlockHeights =>
          val sz0 = bm0.h
          val szOpt2 = m.direction match {
            case MHands.Left  => bhs.previousOf( sz0 )
            case MHands.Right => bhs.nextOf( sz0 )
          }
          val sz2 = szOpt2.get
          bm0.withHeight( sz2 )

        case bws @ BlockWidths =>
          val sz0 = bm0.w
          val szOpt2 = m.direction match {
            case MHands.Left  => bws.previousOf( sz0 )
            case MHands.Right => bws.nextOf( sz0 )
          }
          val sz2 = szOpt2.get
          bm0.withWidth( sz2 )
      }

      val strip2 = strip0.withProps1(
        strip0.props1.withBm(
          Some( bm2 )
        )
      )

      val template2 = v0.jdArgs
        .template
        .deepUpdateOne( strip0, strip2 :: Nil )
        .head

      // Обновить и дерево, и currentTag новым инстансом.
      val v2 = v0.withJdArgs(
        jdArgs = v0.jdArgs.copy(
          selectedTag = Some( strip2 ),
          template    = template2,
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(template2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
          )
        )
      )

      updated( v2 )


    // Клик по кнопке удаления или подтверждения удаления текущего стрипа
    case m: StripDelete =>
      val v0 = value
      val stripEdS0 = v0.stripEd.get

      if (!m.confirmed) {
        // Юзер нажал на обычную кнопку удаления стрипа.
        if (stripEdS0.confirmingDelete) {
          // Кнопка первого шага удаления уже была нажата: игнорим дубликат неактуального события
          noChange
        } else {
          val v2 = v0.withStripEd(
            Some( stripEdS0.withConfirmDelete( true ) )
          )
          updated(v2)
        }

      } else {
        // Второй шаг удаления, и юзер подтвердил удаление.
        val strip4del = v0.jdArgs
          .selectedTag
          .get

        val tpl2 = v0.jdArgs
          .template
          .deepUpdateOne(strip4del, Nil)
          .head

        if (tpl2.children.nonEmpty) {

          val jdCss2 = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
          )

          val v2 = v0.copy(
            jdArgs = v0.jdArgs.copy(
              template    = tpl2,
              jdCss       = jdCss2,
              selectedTag = None
            ),
            stripEd = None,
            // qdEdit: Вроде бы это сюда не относится вообще. Сбросим заодно и текстовый редактор:
            qdEdit  = None
          )

          updated( v2 )

        } else {
          // Нельзя удалять из карточки последний оставшийся блок.
          noChange
        }
      }


    // Юзер передумал удалять текущий блок.
    case StripDeleteCancel =>
      val v0 = value
      val stripEdS0 = v0.stripEd.get
      if (stripEdS0.confirmingDelete) {
        // Юзер отменяет удаление
        val v2 = v0.withStripEd(
          Some( stripEdS0.withConfirmDelete(false) )
        )
        updated(v2)
      } else {
        // Какой-то левый экшен пришёл. Возможно, просто дублирующийся.
        noChange
      }


    // Началось перетаскивание какого-то jd-тега из текущего документа.
    case m: JdTagDragStart =>
      val v0 = value
      val dnd0 = v0.jdArgs.dnd
      if (dnd0.jdt contains m.jdTag) {
        noChange
      } else {
        val v2 = v0.withJdArgs(
          v0.jdArgs.withDnd(
            dnd0.withJdt(
              jdt = Some( m.jdTag )
            )
          )
        )
        updated( v2 )
      }


    // dragend. Нередко, он не наступает вообще. Т.е. код тут ненадёжен и срабатывает редко, почему-то.
    case _: JdTagDragEnd =>
      val v0 = value
      val dnd0 = v0.jdArgs.dnd
      if (dnd0.jdt.nonEmpty) {
        val v2 = v0.withJdArgs(
          v0.jdArgs.withDnd(
            dnd0.withJdt(
              jdt = None
            )
          )
        )
        updated( v2 )
      } else {
        noChange
      }


    // Юзер отпустил перетаскиваемый объект на какой-то стрип. Нужно запихать этот объект в дерево текущего стрипа.
    case m: JdDropContent =>
      val v0 = value
      val tpl0 = v0.jdArgs.template

      // Удалить текущий тег из старого места, если возможно. Готовим через batch, чтобы избежать двойной пересборки дерева.
      val delOldJdBatchOpt = for (currTag <- v0.jdArgs.dnd.jdt) yield {
        IDocTag.Batches.delete(currTag)
      }

      // Получить на руки инстанс сброшенного тега
      val dndJdt = m.foreignTag
        .orElse( v0.jdArgs.dnd.jdt )
        .get

      val clXy0 = m.clXy
      // TODO Если strip изменился, то надо изменить Y на высоту пройденных стрипов от исходного стрипа (включительно) до конечного стрипа (исключительно).
      // Найти исходный strip в исходном шаблоне
      val fromStripOpt = tpl0
        .deepOfTypeIter( MJdTagNames.STRIP )
        .find { s =>
          s.children contains dndJdt
        }

      // Смотрим, изменился ли strip...
      val clXy2 = fromStripOpt.fold(clXy0) { fromStrip =>
        if (fromStrip ==* m.strip) {
          // Перемещение в рамках одного стрипа
          clXy0
        } else {
          // Перемещение между разными strip'ами. Надо пофиксить координату Y, иначе добавляемый элемент отрендерится где-то за экраном.
          val strips = tpl0
            .deepOfTypeIter( MJdTagNames.STRIP )
            .toSeq
          // TODO Отработать ситуацию, когда хотя бы один из index'ов == -1
          val fromStripIndex = strips.indexOf(fromStrip)
          val toStripIndex = strips.indexOf(m.strip)
          val (topStrip, bottomStrip, yModSign) = if (fromStripIndex <= toStripIndex) {
            (fromStrip, m.strip, -1)
          } else {
            (m.strip, fromStrip, +1)
          }
          // Собрать все стрипы от [текущего до целевого), просуммировать высоту блоков, вычесть из Y
          val iter = tpl0
            .deepOfTypeIter( MJdTagNames.STRIP )
            .dropWhile(_ !=* topStrip)
            .takeWhile(_ !=* bottomStrip)
            .flatMap(_.props1.bm)
          if (iter.nonEmpty) {
            val yDiff = iter
              .map { _.h.value }
              .sum
            val y2 = clXy0.y + yModSign * yDiff
            //println(s"mod Y: ${clXy0.y} by $yDiff => $y2")
            clXy0.withY( y2 )
          } else {
            // Странно: нет пройденных стрипов, хотя они должны бы быть
            LOG.warn( msg = s"$clXy0 [$fromStrip => ${m.strip})" )
            clXy0
          }
        }
      }
      // Рассчитать пиксельную высоту-разницу от исходного fromStrip до текущего m.strip


      // Выставить новые координаты тегу
      val apJdt2 = dndJdt.withProps1(
        dndJdt.props1
          .withTopLeft( Some(clXy2) )
      )

      // Добавить перетащенный тег в текущий стрип.
      val batches0 = List[JdBatch_t](
        m.strip -> { strip1s: Seq[IDocTag] =>
          val childrenTail = Seq(apJdt2)
          for (s <- strip1s) yield {
            s.withChildren( s.children ++ childrenTail )
          }
        }
      )

      val batches2 = Lists.prependOpt(delOldJdBatchOpt)( batches0 )

      val tpl2 = IDocTag.batchUpdateOne2(tpl0, batches2)
        .head

      // Пересобрать данные для рендера.
      val v2 = v0.withJdArgs(
        v0.jdArgs.copy(
          template    = tpl2,
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
          ),
          dnd         = MJdDndS.empty,
          selectedTag = Some(apJdt2)
        )
      )

      updated(v2)


    // Реакцие на завершение перетаскивания целого стрипа.
    case m: JdDropStrip =>
      val v0 = value
      val droppedStrip = v0.jdArgs.dnd.jdt.get
      val targetStrip = m.targetStrip
      if (droppedStrip == targetStrip) {
        noChange
      } else {
        val strips2 = v0.jdArgs.template
          .deepOfTypeIter( MJdTagNames.STRIP )
          .filter(_ !=* droppedStrip)
          .flatMap { s =>
            if (s ==* targetStrip) {
              if (m.isUpper) {
                // Поместить dropped strip сверху
                droppedStrip :: s :: Nil
              } else {
                // Поместить перемещаемый стрип снизу
                s :: droppedStrip :: Nil
              }
            } else {
              // Это не тот стрип, на который шло перемещение.
              s :: Nil
            }
          }
          .toList
        // Залить обновлённый список стрипов в исходный документ
        val tpl2 = v0.jdArgs.template
          .withChildren(strips2)
        val v2 = v0.withJdArgs(
          v0.jdArgs
            .withTemplate(tpl2)
            .withDnd()
        )
        updated(v2)
      }


    // Появилась новая гистограмма в карте гисторамм. Нужно поправить эджи, у которых фон соответствует связанной картинке.
    case m: HandleNewHistogramInstalled =>
      val v0 = value
      val edgeUids4mod = v0.jdArgs.renderArgs
        .edges
        .valuesIterator
        .flatMap { e =>
          for {
            fileSrv <- e.jdEdge.fileSrv
            if m.nodeId ==* fileSrv.nodeId
          } yield {
            e.id
          }
        }
        .toSet

      if (edgeUids4mod.isEmpty) {
        // Если эджа для указанной гистограммы не найдено, то это палево какое-то.
        LOG.warn( WarnMsgs.SOURCE_FILE_NOT_FOUND, msg = m )
        noChange

      } else {
        // Найти гистограмму в карте
        v0.colorsState
          .histograms
          .get(m.nodeId)
          .filter( _.sorted.nonEmpty )
          .fold {
            // Should never happen: не найдена гистограмма, указанная в событии.
            LOG.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
            noChange
          } { mhist =>
            // Надо пробежаться по template, и всем элеметам, которые изменились, выставить обновлённые значения.
            val topColorMcd = mhist.sorted
              .maxBy { mcd =>
                mcd.freqPc
                  .getOrElse(-1)
              }
            val topColorMcdOpt = Some(topColorMcd)

            // К сожалению, теги дерева сейчас дублируются в поле .selecetedTag. Нужно там обновлять инстанс тега одновременно с деревом с помощью var:
            // TODO selJdt Ужаснейший быдлокод тут. Надо поле .selectedTag сделать трейсом до узла, а не инстансом узла. И перевести дерево документа на scalaz Tree.
            var selJdt2 = v0.jdArgs.selectedTag
            var modifiedElOpt: Option[IJdElement] = None
            //println("selJdt0 =  " + selJdt2)
            val tpl2 = v0.jdArgs
              .template
              .deepElMap { (el0, el1) =>
                val bgImgEdgeId = el1.bgImgEdgeId
                //println( bgImgEdgeId, edgeUids4mod )
                if ( bgImgEdgeId.map(_.edgeUid).exists(edgeUids4mod.contains) ) {
                  //println("replace bgColor with " + topColorMcd + " on " + el1)
                  // Требуется замена bgColor на обновлённый вариант.
                  val el2 = el1.setBgColor( topColorMcdOpt )
                  modifiedElOpt = Some(el2)
                  if ( selJdt2.contains(el0) )
                    selJdt2 = Some( el2.asInstanceOf[IDocTag] )
                  el2
                } else {
                  el1
                }
              }
              .asInstanceOf[IDocTag]

            // Сохранить новые темплейт в состояние.
            var jdArgs2 = v0.jdArgs
              .withTemplate(tpl2)
              .withJdCss(
                jdCssFactory.mkJdCss(
                  MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
                )
              )

            // Если selected-тег тоже изменился, то его тоже обновить.
            val selJdtNeedUpdate = selJdt2 !===* v0.jdArgs.selectedTag
            //println( selJdtNeedUpdate )
            if (selJdtNeedUpdate)
              jdArgs2 = jdArgs2.withSelectedTag( selJdt2 )
            var v2 = v0.withJdArgs( jdArgs2 )

            // Надо заставить перерендерить quill, если он изменился и открыт сейчас:
            for {
              qdEdit0 <- v0.qdEdit
              modifiedEl <- modifiedElOpt
              qdTag <- tpl2.findTagsByChildQdEl(modifiedEl)
            } {
              // TODO selJdt Тут всё работает благодаря сайд-эффектам перефокусировки при рендере quill при обновлении состояния. selJdt2 содержит неправильный инстанс сейчас.
              // А sleJdt при этом содержит неактуальное значение
              //println( qdEdit0 )
              v2 = v0.withQdEdit(
                Some(qdEdit0.copy(
                  initDelta = quillDeltaJsUtil.qdTag2delta( qdTag, v2.jdArgs.renderArgs.edges ),
                  realDelta = None
                ))
              )
            }

            updated( v2 )
          }
      }


    // Реакция на клик по кнопке создания нового элемента.
    case AddBtnClick =>
      val v0 = value
      if (v0.addS.nonEmpty) {
        noChange
      } else {
        val v2 = v0.withAddS(
          Some( MAddS.default )
        )
        updated(v2)
      }


    // Реакция на клик по кнопке создания "контента", который у нас является синонимом QdTag.
    case AddContentClick =>
      val v0 = value
      val intoStrip0 = v0.jdArgs.selectedTag.fold[IDocTag] {
        v0.jdArgs.template
          .deepOfTypeIter( MJdTagNames.STRIP )
          .next()
      } { jdt =>
        if (jdt.jdTagName ==* MJdTagNames.STRIP) {
          // Если выбран strip, то его и вернуть
          jdt
        } else {
          // Если выбран какой-то не-strip элемент, то найти его strip
          v0.jdArgs.template
            .deepOfTypeIter( MJdTagNames.STRIP )
            .find { s =>
              s contains jdt
            }
            .get
        }
      }

      val rnd = new Random()
      val bm0 = intoStrip0.props1.bm.get //OrElse( BlockMeta.DEFAULT )
      val coordsRnd = MCoords2di(
        x = rnd.nextInt( bm0.w.value/3 ) + 10,
        y = rnd.nextInt( (bm0.h.value * 0.75).toInt ) + (bm0.h.value * 0.12).toInt
      )

      val textL10ed = Messages( MsgCodes.`Example.text` )

      val textPred = MPredicates.JdContent.Text
      val edgesMap0 = v0.jdArgs.renderArgs.edges
      val (edgesMap2, edgeUid) = edgesMap0
        .valuesIterator
        .find { e =>
          e.jdEdge.predicate ==* textPred &&
            e.jdEdge.text.contains( textL10ed )
        }
        .fold [(Map[EdgeUid_t, MEdgeDataJs], Int)] {
          // Нет примера текста в эджах: добавить его туда.
          val nextEdgeUid = EdgesUtil.nextEdgeUidFromMap( edgesMap0 )
          val e = MEdgeDataJs(
            jdEdge = MJdEditEdge(
              predicate = textPred,
              id        = nextEdgeUid,
              text      = Some(textL10ed)
            )
          )
          val edgesMap1 = edgesMap0 + (nextEdgeUid -> e)
          (edgesMap1, nextEdgeUid)
        } { exampleTextEdge =>
          (edgesMap0, exampleTextEdge.id)
        }

      val qdt = IDocTag.edgeQd( edgeUid, coordsRnd )

      val intoStrip2 = intoStrip0.withChildren {
        intoStrip0.children ++ Seq( qdt )
      }

      val tpl2 = v0.jdArgs.template
        .deepUpdateChild(intoStrip0, intoStrip2 :: Nil)
        .head

      val v2 = v0.copy(
        jdArgs = v0.jdArgs.copy(
          template    = tpl2,
          renderArgs  = v0.jdArgs.renderArgs
            .withEdges( edgesMap2 ),
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, edgesMap2)
          ),
          selectedTag = Some(qdt)
        ),
        qdEdit = Some {
          val qdelta = quillDeltaJsUtil.qdTag2delta(
            qd    = qdt,
            edges = edgesMap2
          )
          MQdEditS(
            initDelta = qdelta
          )
        },
        stripEd = None,
        addS = None
      )
      updated(v2)


    // Клик по кнопке добавления нового стрипа.
    case AddStripClick =>
      val v0 = value
      val beforeStripOpt = v0.jdArgs
        // Попробовать текущеий выделенный strip
        .selectedTag
        .filterByType( MJdTagNames.STRIP )
        .orElse {
          // добавить в конец списка стрипов, если есть хотя бы один стрип.
          v0.jdArgs.template
            .deepOfTypeIter( MJdTagNames.STRIP )
            .toStream
            .lastOption
        }

      val iter0 = v0.jdArgs.template
        .deepOfTypeIter( MJdTagNames.STRIP )

      val newStrip = IDocTag.strip(
        bm = BlockMeta.DEFAULT,
        bgColor = Some(MColorData("ffffff"))
      )()

      val iter2 = beforeStripOpt.fold {
        iter0 ++ Seq(newStrip)
      } { beforeStrip =>
        v0.jdArgs.template
          .deepOfTypeIter( MJdTagNames.STRIP )
          .flatMap { s =>
            if (s == beforeStrip) {
              s :: newStrip :: Nil
            } else {
              s :: Nil
            }
          }
      }

      val tpl2 = v0.jdArgs.template.withChildren( iter2.toSeq )

      val v2 = v0
        .withAddS(None)
        .withJdArgs(
          v0.jdArgs.copy(
            template    = tpl2,
            selectedTag = Some(newStrip),
            jdCss       = jdCssFactory.mkJdCss(
              MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
            )
          )
        )
      updated(v2)


    // Отмена добавления чего-либо.
    case AddCancelClick =>
      val v0 = value
      if (v0.addS.isEmpty) {
        noChange
      } else {
        val v2 = v0.withAddS( None )
        updated( v2 )
      }


    // Выставить новый масштаб для рендера карточи.
    case m: SetScale =>
      val v0 = value
      val conf2 = v0.jdArgs.conf
        .withSzMult( m.szMult )
      val v2 = v0
        .withJdArgs(
          v0.jdArgs
            .withConf( conf2 )
            .withJdCss(
              jdCssFactory.mkJdCss(
                MJdCssArgs.singleCssArgs(v0.jdArgs.template, conf2, v0.jdArgs.renderArgs.edges)
              )
            )
        )
      updated(v2)

  }

}

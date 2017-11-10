package io.suggest.ad.edit.c

import diode._
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.edit.{MAddS, MQdEditS}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.color.MColorData
import io.suggest.common.MHands
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.html.HtmlConstants
import io.suggest.file.MJsFileInfo
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.render.m._
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags.JdTag.Implicits._
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd._
import io.suggest.model.n2.edge.{EdgeUid_t, EdgesUtil, MPredicates}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.Base64JsUtil
import io.suggest.primo.SetVal
import io.suggest.quill.m.TextChanged
import io.suggest.quill.u.QuillDeltaJsUtil
import io.suggest.react.ReactDiodeUtil
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.ueq.QuillUnivEqUtil._
import japgolly.univeq._
import org.scalajs.dom.raw.URL
import io.suggest.scalaz.ZTreeUtil._
import japgolly.scalajs.react.vdom.TagMod

import scala.util.Random
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 14:34
  * Description: Контроллер событий редактирования документа.
  */
class DocEditAh[M](
                    modelRW           : ModelRW[M, MDocS],
                    jdCssFactory      : JdCssFactory,
                    lkAdEditCss       : LkAdEditCss,
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
                       tpl: Tree[JdTag],
                       edges: Map[EdgeUid_t, MEdgeDataJs]): (Option[Effect], Map[EdgeUid_t, MEdgeDataJs]) = {
    // 10. Очистить эджи от неиспользуемых.
    val edges2 = JdTag.purgeUnusedEdges( tpl, edges )

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

      if ( v0.qdEdit.exists(_.initDelta ==* m.fullDelta) ) {
        // Бывают ложные срабатывания. Например, прямо при инициализации редактора. Но не факт конечно, что они тут подавляются.
        noChange

      } else {
        // Текст действительно изменился. Пересобрать json-document.
        //println( JSON.stringify(m.fullDelta) )
        val currTag0 = v0.jdArgs.selectedTag.get
        val selJdtPath = v0.jdArgs.selPath.get
        // Спроецировать карту сборных эджей в jd-эджи
        val edgesData0 = v0.jdArgs.renderArgs.edges
        val (qdTag2, edgesData2) = quillDeltaJsUtil.delta2qdTag(m.fullDelta, currTag0, edgesData0)

        // Собрать новый json-document
        val tpl2 = v0.jdArgs
          .template
          .pathToNode( selJdtPath )
          .get
          .setTree( qdTag2 )
          .toTree

        // Пост-процессить новые эджи, т.к. там может быть мусор или эджи, требующие фоновой обработи.
        val (fxOpt, edgesData3) = _ppEdges( edgesData0, tpl2, edgesData2 )

        val jdArgs2 = v0.jdArgs.copy(
          template    = tpl2,
          renderArgs  = v0.jdArgs.renderArgs
            .withEdges( edgesData3 ),
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, edgesData3)
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
    case m: JdTagSelect =>
      val v0 = value
      if ( v0.jdArgs.selectedTag.containsLabel(m.jdTag) ) {
        // Бывают повторные щелчки по уже выбранным элементам, это нормально.
        noChange

      } else {
        // Юзер выбрал какой-то новый элемент. Залить новый тег в seleted:
        val newSelJdtTreeLoc = v0.jdArgs.template
          .loc
          .findByLabel( m.jdTag )
          .get
        val newSelJdt = newSelJdtTreeLoc.toNodePath
        val v1 = v0.withJdArgs(
          v0.jdArgs
            .withSelPath( Some(newSelJdt) )
        )

        // Если это QdTag, то отработать состояние quill-delta:
        val v2 = m.jdTag.name match {
          // Это qd-тег, значит нужно собрать и залить текущую дельту текста в состояние.
          case MJdTagNames.QD_CONTENT =>
            // Нужно получить текущее qd-под-дерево (для сборки дельты)
            val delta2 = quillDeltaJsUtil.qdTag2delta(
              qd    = newSelJdtTreeLoc.tree,
              edges = v1.jdArgs.renderArgs.edges
            )
            v1
              .withQdEdit(
                Some(
                  MQdEditS(
                    initDelta  = delta2
                  )
                )
              )
              .withSlideBlocks(
                v1.slideBlocks
                  .withExpanded( Some(SlideBlockKeys.CONTENT) )
              )
          // Очистить состояние от дельты.
          case _ =>
            v1.withOutQdEdit
        }

        // Если это strip, то активировать состояние strip-редактора.
        val v3 = m.jdTag.name match {
          // Переключение на новый стрип. Инициализировать состояние stripEd:
          case MJdTagNames.STRIP =>
            v2.withStripEd(
                Some(MStripEdS(
                  isLastStrip = {
                    val hasManyStrips = v2.jdArgs.template
                      .deepOfTypeIter( MJdTagNames.STRIP )
                      // Оптимизация: НЕ проходим весь strip-итератор, а считаем только первые два стрипа.
                      .slice(0, 2)
                      .size > 1
                    !hasManyStrips
                  }
                ))
              )
              .withSlideBlocks(
                v2.slideBlocks
                  .withExpanded( Some(SlideBlockKeys.BLOCK_BG) )
              )
          // Это не strip, обнулить состояние stripEd, если оно существует:
          case _ =>
            v2.withOutStripEd
        }

        // Может быть, был какой-то qd-tag и весь текст теперь в нём удалён? Удалить, если старый тег, если осталась дельта
        val v4 = v0.jdArgs.selectedTag.fold(v3) { jdtTree =>
          val jdt = jdtTree.rootLabel
          val dataEdges0 = v0.jdArgs.renderArgs.edges
          if (
            jdt.name ==* MJdTagNames.QD_CONTENT &&
            QdJsUtil.isEmpty(jdtTree, dataEdges0) &&
            v3.jdArgs.template.contains(jdt)
          ) {
            val tpl1 = v3.jdArgs.template
            val tpl2 = tpl1
              .loc
              .findByLabel(jdt)
              .flatMap(_.delete)
              .map(_.toTree)
              .getOrElse {
                LOG.warn( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = jdt )
                tpl1
              }
            // Очистить эджи от лишнего контента
            val dataEdges2 = JdTag.purgeUnusedEdges(tpl2, dataEdges0)
            v3
              .withJdArgs(
                v3.jdArgs.copy(
                  template    = tpl2,
                  renderArgs  = v3.jdArgs.renderArgs
                    .withEdges( dataEdges2 ),
                  jdCss       = jdCssFactory.mkJdCss(
                    MJdCssArgs.singleCssArgs(tpl2, v3.jdArgs.conf, dataEdges2)
                  ),
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
        val dataEdgesMap2 = JdTag.purgeUnusedEdges(v0.jdArgs.template, dataEdgesMap1)
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
      val edges2 = JdTag.purgeUnusedEdges( v0.jdArgs.template, edges0 )
      if ( edges0.size ==* edges2.size ) {
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

      val stripTreeLoc0 = v0.jdArgs.selectedTagLoc.get
      val strip0 = stripTreeLoc0.getLabel

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

      val template2 = stripTreeLoc0
        .setLabel(strip2)
        .toTree

      // Обновить и дерево, и currentTag новым инстансом.
      val v2 = v0.withJdArgs(
        jdArgs = v0.jdArgs
          .withTemplate( template2 )
          .withJdCss(
            jdCssFactory.mkJdCss(
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
        val strip4delLoc = v0.jdArgs
          .selectedTagLoc
          .get

        val tpl0 = v0.jdArgs.template
        val tpl2 = strip4delLoc
          .delete
          .fold(tpl0) { _.toTree }

        if (tpl2.subForest.nonEmpty) {
          val jdCss2 = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
          )

          val v2 = v0.copy(
            jdArgs = v0.jdArgs.copy(
              template    = tpl2,
              jdCss       = jdCss2,
              selPath     = None
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
      val dnd0Jdt = v0.jdArgs.draggingTagLoc
      if (dnd0Jdt.toLabelOpt contains m.jdTag) {
        noChange
      } else {
        val v2 = v0.withJdArgs(
          v0.jdArgs.withDnd(
            dnd0.withJdt(
              jdt = v0.jdArgs.template.nodeToPath(m.jdTag)
            )
          )
        )
        // Если запускается перетаскивание тега, который не является текущим, то надо "выбрать" таскаемый тег.
        if ( v0.jdArgs.selectedTagLoc.toLabelOpt contains m.jdTag ) {
          // Текущий тег перетаскивается, всё ок.
          updated( v2 )
        } else {
          // Активировать текущий тег
          val fx = Effect.action( JdTagSelect(m.jdTag) )
          updated( v2, fx )
        }

      }


    // dragend. Нередко, он не наступает вообще. Т.е. код тут ненадёжен и срабатывает редко, почему-то.
    case _: JdTagDragEnd =>
      val v0 = value
      val dnd0 = v0.jdArgs.dnd
      dnd0.jdt.fold(noChange) { _ =>
        val v2 = v0.withJdArgs(
          v0.jdArgs.withDnd(
            dnd0.withJdt(
              jdt = None
            )
          )
        )
        updated( v2 )
      }


    // Юзер отпустил перетаскиваемый объект на какой-то стрип. Нужно запихать этот объект в дерево нового стрипа.
    case m: JdDropContent =>
      val v0 = value
      val tpl0 = v0.jdArgs.template

      // Найти tree loc текущего тега наиболее оптимальным путём. С некоторой вероятностью это -- selected-тег:
      val dndJdtLoc = {
        // Получить на руки инстанс сброшенного тега.
        // Прячемся от общего scope, чтобы работать с элементом только через его tree loc.
        val dndJdt = m.foreignTag
          .orElse( v0.jdArgs.draggingTagLoc.toLabelOpt )
          .get

        v0.jdArgs.selectedTagLoc
          .filter { loc =>
            // Убедиться, что текущий selected-тег содержит dndJdt:
            dndJdt ==* loc.getLabel
          }
          .orElse {
            // Это не selected-тег. Возможны перетаскивание без выделения тега: просто взял да потащил.
            // Это нормально. Перебираем всё дерево:
            tpl0
              .loc
              .findByLabel( dndJdt )
          }
          .get
      }

      // Извлекаем тег из старого местоположения:
      val dndJdtTree0 = dndJdtLoc.tree
      val dndJdtLocNoSrc = dndJdtLoc.delete.get
      // Фокус на соседнем или родительском узле.

      // Дополнительно обработать Y-координату.
      val clXy0 = m.clXy
      // Найти исходный strip в исходном шаблоне. Оптимально: подняться по parents от текущего таскаемого элемента.
      val clXy2 = dndJdtLocNoSrc
        .findUpByType( MJdTagNames.STRIP )
        .fold(clXy0) { fromStripLoc =>
          val fromStrip = fromStripLoc.getLabel
          // Изменился стрип или сброшено на тот же?
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

      // Выставить новые координаты тегу
      val dndJdtTree2 = Tree.Node(
        root = {
          val dndJdt0 = dndJdtTree0.rootLabel
          dndJdt0
            .withProps1(
              dndJdt0.props1
                .withTopLeft( Some(clXy2) )
            )
        },
        forest = dndJdtTree0.subForest
      )

      // Добавить перетащенный тег в целевой стрип:
      val dndJdtLoc3 = dndJdtLocNoSrc
        // Найти целевой стрип:
        .root
        .findByLabel( m.strip )
        .get
        // Добавляем как последний дочерний элемент текущего стрипа. TODO Opt Может как начальный добавлять? Это быстрее будет, хоть и менее логично.
        .insertDownLast( dndJdtTree2 )

      val tpl2 = dndJdtLoc3.toTree

      // Пересобрать данные для рендера.
      val v2 = v0.withJdArgs(
        v0.jdArgs.copy(
          template    = tpl2,
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
          ),
          dnd         = MJdDndS.empty,
          selPath     = tpl2.nodeToPath( dndJdtTree2.rootLabel )
        )
      )

      updated(v2)


    // Реакция на завершение перетаскивания целого стрипа.
    case m: JdDropStrip =>
      val v0 = value
      val droppedStripLabel = v0.jdArgs.draggingTagLoc.get.getLabel
      val targetStripLabel = m.targetStrip
      if (droppedStripLabel ==* targetStripLabel) {
        noChange
      } else {
        val tpl0 = v0.jdArgs.template
        val droppedStripLoc = tpl0
          .loc
          .findByLabel( droppedStripLabel )
          .get
        val droppedStripTree = droppedStripLoc.tree

        val targetStripLoc = droppedStripLoc
          .delete
          .get
          .root
          .findByLabel( targetStripLabel )
          .get

        val droppedStripLoc2 = if (m.isUpper) {
          targetStripLoc.insertLeft( droppedStripTree )
        } else {
          targetStripLoc.insertRight( droppedStripTree )
        }

        val tpl2 = droppedStripLoc2.toTree
        // Залить обновлённый список стрипов в исходный документ
        val v2 = v0.withJdArgs(
          v0.jdArgs.copy(
            template  = tpl2,
            dnd       = MJdDndS.empty,
            selPath   = tpl2.nodeToPath( droppedStripLabel )
          )
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

            val tpl2 = v0.jdArgs.template
              .map { el1 =>
                el1
                  .edgeUids
                  .map(_.edgeUid)
                  .find(edgeUids4mod.contains)
                  .fold(el1) { _ =>
                    el1.qdProps.fold {
                      el1.withProps1(
                        el1.props1
                          .withBgColor( topColorMcdOpt )
                      )
                    } { qdProps0 =>
                      el1.withQdProps( Some(
                        qdProps0.withAttrsText( Some(
                          qdProps0.attrsText
                            .getOrElse( MQdAttrsText.empty )
                            .withBackground(
                              topColorMcdOpt.map {SetVal.apply }
                            )
                        ))
                      ))
                    }
                  }
              }

            // Сохранить новые темплейт в состояние.
            val jdArgs2 = v0.jdArgs
              .withTemplate(tpl2)
              .withJdCss(
                jdCssFactory.mkJdCss(
                  MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
                )
              )

            var v2 = v0.withJdArgs( jdArgs2 )

            // Надо заставить перерендерить quill, если он изменился и открыт сейчас:
            for {
              qdEdit0     <- v0.qdEdit
              qdTag2      <- jdArgs2.selectedTag
              if qdTag2.rootLabel.name ==* MJdTagNames.QD_CONTENT
              // Перерендеривать quill только если изменение гистограммы коснулось эджа внутри текущего qd-тега:
              qdTag0      <- v0.jdArgs.selectedTag
              if qdTag0 !=* qdTag2
            } {
              v2 = v0.withQdEdit(
                Some(qdEdit0.copy(
                  initDelta = quillDeltaJsUtil.qdTag2delta( qdTag2, v2.jdArgs.renderArgs.edges ),
                  realDelta = None
                ))
              )
            }

            updated( v2 )
          }
      }


    // Замена состояния галочки широкого рендера текущего стрипа новым значением
    case m: StripStretchAcross =>
      val v0 = value
      val tpl2 = v0.jdArgs
        .selectedTagLoc
        .get
        .modifyLabel { strip0 =>
          assert(strip0.name ==* MJdTagNames.STRIP)
          val bm0 = strip0.props1.bm.get
          strip0.withProps1(
            strip0.props1.withBm(
              Some( bm0.withWide( m.isWide ) )
            )
          )
        }
        .toTree
      val v2 = v0.withJdArgs(
        v0.jdArgs
          .withTemplate( tpl2 )
          .withJdCss(
            jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges) )
          )
      )
      updated( v2 )


    // Изменение галочки управления main-флагом текущего блока.
    case m: MainStripChange =>
      val v0 = value
      v0.jdArgs.selectedTagLoc
        .filter(_.getLabel.name ==* MJdTagNames.STRIP)
        .fold(noChange) { currStripLoc =>
          val tpl2 = currStripLoc
            .modifyLabel { currStrip =>
              currStrip.withProps1(
                currStrip.props1.withIsMain(
                  isMain = OptionUtil.maybeTrue( m.isMain )
                )
              )
            }
            .toTree
          val v2 = v0.withJdArgs(
            v0.jdArgs
              .withTemplate(tpl2)
              .withJdCss(
                jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges) )
              )
          )
          updated(v2)
        }


    // Сигнал переключения
    case m: ShowMainStrips =>
      val v0 = value
      val nonMainStripsCss2 = OptionUtil.maybe(m.showing) {
        lkAdEditCss.JdAddons.muffledStrip.htmlClass
      }
      if (v0.jdArgs.renderArgs.nonMainStripsCss ==* nonMainStripsCss2) {
        noChange
      } else {
        val v2 = v0.withJdArgs(
          v0.jdArgs.withRenderArgs(
            v0.jdArgs.renderArgs
              .withNonMainStripsCss( nonMainStripsCss2 )
          )
        )
        updated(v2)
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
      val stripName = MJdTagNames.STRIP
      val intoStripLoc = v0.jdArgs.selectedTagLoc
        .fold {
          // Сейчас нет выделенных тегов. Найти первый попавшийся strip
          v0.jdArgs.template
            .loc
            .findByType( stripName )
        } { selLoc =>
          // Если выбран какой-то не-strip элемент, то найти его strip. Если выделен strip, то вернуть его.
          selLoc.findUpByType( stripName )
        }
        .get

      val rnd = new Random()
      val bm0 = intoStripLoc.getLabel.props1.bm.get //OrElse( BlockMeta.DEFAULT )
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

      val qdtTree = Tree.Node(
        root = JdTag.qd(coordsRnd),
        forest = Stream(
          Tree.Leaf(
            JdTag.edgeQdOp( edgeUid )
          )
        )
      )

      val qdtLoc = intoStripLoc.insertDownLast( qdtTree )

      val tpl2 = qdtLoc.toTree

      val v2 = v0.copy(
        jdArgs = v0.jdArgs.copy(
          template    = tpl2,
          renderArgs  = v0.jdArgs.renderArgs
            .withEdges( edgesMap2 ),
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, edgesMap2)
          ),
          selPath     = tpl2.nodeToPath( qdtTree.rootLabel )
        ),
        qdEdit = Some {
          val qdelta = quillDeltaJsUtil.qdTag2delta(
            qd    = qdtTree,
            edges = edgesMap2
          )
          MQdEditS(
            initDelta = qdelta
          )
        },
        stripEd = None,
        addS = None,
        slideBlocks = v0.slideBlocks
          .withExpanded( Some(SlideBlockKeys.CONTENT) )
      )
      updated(v2)


    // Клик по кнопке добавления нового стрипа.
    case AddStripClick =>
      val v0 = value

      val newStripTree = Tree.Leaf(
        JdTag.strip(
          bm      = BlockMeta.DEFAULT,
          bgColor = Some(MColorData("ffffff"))
        )
      )

      // Найти предшествующий стрип и воткнуть справа от него. Либо в конце подураня корня шаблона, если текущего стрипа нет.
      val newStripLoc = v0.jdArgs.selectedTagLoc
        .flatMap {
          _.findUpByType( MJdTagNames.STRIP )
        }
        .fold {
          // Нет текущего стрипа. Впихнуть просто в шаблон
          v0.jdArgs.template
            .loc
            .insertDownLast( newStripTree )
        } { currStripLoc =>
          currStripLoc.insertRight( newStripTree )
        }

      val tpl2 = newStripLoc.toTree

      val v2 = v0
        .withAddS(None)
        .withJdArgs(
          v0.jdArgs.copy(
            template    = tpl2,
            selPath     = tpl2.nodeToPath( newStripTree.rootLabel ),
            jdCss       = jdCssFactory.mkJdCss(
              MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf, v0.jdArgs.renderArgs.edges)
            )
          )
        )
        .withSlideBlocks(
          v0.slideBlocks
            .withExpanded( Some(SlideBlockKeys.BLOCK) )
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

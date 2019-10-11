package io.suggest.ad.edit.c

import diode._
import diode.data.Pot
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.{MDocS, MEditorsS, MJdDocEditS, MQdEditS, MSlideBlocks, MStripEdS, SlideBlockKeys}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.color.MColorData
import io.suggest.common.MHands
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.html.HtmlConstants
import io.suggest.file.MJsFileInfo
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.i18n.MsgCodes
import io.suggest.jd.edit.m.{ResizeContent, JdChangeLayer, JdDropContent, JdDropToDocument, JdTagDragEnd, JdTagDragStart, JdTagSelect, QdEmbedResize}
import io.suggest.jd.{JdConst, MJdConf, MJdDoc, MJdEdge}
import io.suggest.jd.render.m._
import io.suggest.jd.render.u.JdUtil
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd._
import io.suggest.lk.m.color.MColorsState
import io.suggest.lk.m.{FileHashStart, HandleNewHistogramInstalled, PurgeUnusedEdges}
import io.suggest.model.n2.edge.{EdgeUid_t, EdgesUtil, MPredicates}
import io.suggest.msg.{ErrorMsgs, Messages, WarnMsgs}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.Base64JsUtil
import io.suggest.primo.SetVal
import io.suggest.quill.m.TextChanged
import io.suggest.quill.u.QuillDeltaJsUtil
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ueq.QuillUnivEqUtil._
import japgolly.univeq._
import org.scalajs.dom.raw.URL
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.ueq.UnivEqUtil._
import monocle.Traversal

import scala.util.Random
import scalaz.{Tree, TreeLoc}
import scalaz.std.option._

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 14:34
  * Description: Контроллер событий редактирования документа.
  */
class DocEditAh[M](
                    modelRW           : ModelRW[M, MDocS],
                    lkAdEditCss       : LkAdEditCss,
                    quillDeltaJsUtil  : QuillDeltaJsUtil
                  )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  import DocEditAh._

  private def _qdUpdateWidth(qdSubTreeLoc0: TreeLoc[JdTag], edgeUid: EdgeUid_t, width: Int,
                             heightPxOpt: Option[Int] = None, needUpdateF: Option[Int => Boolean] = None): TreeLoc[JdTag] = {
    qdSubTreeLoc0
      .root
      // Найти qd-op-тег, содержащего текущую новую картинку:
      .find(
        _.getLabel.qdProps.exists(
          _.edgeInfo.exists(
            _.edgeUid ==* edgeUid)))
      // Требует ли модицификации текущая картинка?
      .filter { imgOpLoc =>
        val widthPxOpt = for {
          qd          <- imgOpLoc.getLabel.qdProps
          attrsEmbed  <- qd.attrsEmbed
          widthSU     <- attrsEmbed.width
          width       <- widthSU.toOption
        } yield {
          width
        }
        widthPxOpt.isEmpty || (needUpdateF.isEmpty || widthPxOpt.exists(needUpdateF.get))
      }
      .fold(qdSubTreeLoc0) { imgOpLoc0 =>
        imgOpLoc0.modifyLabel {
          JdTag.qdProps
            .composeTraversal( Traversal.fromTraverse[Option, MQdOp] )
            .composeLens( MQdOp.attrsEmbed )
            .modify { attrsEmbedOpt0 =>
              val widthSuOpt  = Some( SetVal(width) )
              val heightSuOpt = heightPxOpt.map(SetVal.apply)
              // обычно attrs embed пуст для новой картинки/видео. Но quill может сам изменить размер сразу.
              val ae2 = attrsEmbedOpt0.fold {
                MQdAttrsEmbed( width = widthSuOpt, height = heightSuOpt )
              } { attrsEmbed =>
                attrsEmbed.withWidthHeight(
                  width  = widthSuOpt,
                  height = heightSuOpt
                )
              }
              Some(ae2)
            }
        }
      }
  }


  implicit class MDocSOptOpsExt( val treeLocOpt: Option[TreeLoc[JdTag]] ) {

    def shadowUpdated(v0: MDocS)(f: Option[MJdShadow] => Option[MJdShadow]): ActionResult[M] = {
      treeLocOpt.fold(noChange) { treeLoc =>
        val tpl2 = treeLoc
          .modifyLabel {
            JdTag.props1
              .composeLens( MJdtProps1.textShadow )
              .modify(f)
          }
          .toTree
        val jdDoc2 = MJdDoc.template.set( tpl2 )( v0.jdDoc.jdArgs.data.doc )

        val v2 = MDocS.jdDoc
          .composeLens(MJdDocEditS.jdArgs)
          .modify(
            MJdArgs.jdRuntime
              .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) ) andThen
            MJdArgs.data
              .composeLens( MJdDataJs.doc )
              .set(jdDoc2)
          )( v0 )

        updated(v2)
      }
    }

  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Набор текста в wysiwyg-редакторе.
    case m: TextChanged =>
      val v0 = value

      if ( v0.editors.qdEdit.exists(_.initDelta ==* m.fullDelta) ) {
        // Бывают ложные срабатывания. Например, прямо при инициализации редактора. Но не факт конечно, что они тут подавляются.
        noChange

      } else if (v0.jdDoc.jdArgs.selJdt.treeLocOpt.isEmpty) {
        LOG.warn( WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = m.getClass.getName )
        noChange

      } else {
        // Код обновления qd-тега в шаблоне:
        def __updateTpl(qdSubTree2: Tree[JdTag]): Tree[JdTag] = {
          v0.jdDoc
            .jdArgs
            .selJdt.treeLocOpt
            .get
            .setTree( qdSubTree2 )
            .toTree
        }

        // Текст действительно изменился. Пересобрать json-document.
        //println( JSON.stringify(m.fullDelta) )
        val qdSubTree0 = v0.jdDoc.jdArgs.selJdt.treeOpt.get
        // Спроецировать карту сборных эджей в jd-эджи
        val edgesData0 = v0.jdDoc.jdArgs.data.edges
        //println( "textChanged:\n" + JSON.stringify(m.fullDelta) )
        val (qdSubTree2, edgesData2) = quillDeltaJsUtil.delta2qdTag(m.fullDelta, qdSubTree0, edgesData0)

        // Собрать новый json-document
        val tpl1 = __updateTpl( qdSubTree2 )

        // 10. Очистить эджи от неиспользуемых.
        val edgesData3 = JdTag.purgeUnusedEdges( tpl1, edgesData2 )

        // 20. Необходимо организовать блобификацию файлов эджей, заданных через dataURL.
        val dataPrefix = HtmlConstants.Proto.DATA_

        // 30. Найти новые эджи картинок, которые надо загружать на сервер.
        val dataEdgesForUpload = (
          for {
            edgeData <- edgesData3.valuesIterator
            jde = edgeData.jdEdge
            // Три варианта:
            // - Просто эдж, который надо молча завернуть в EdgeData. Текст, например.
            // - Эдж, сейчас который проходит асинхронную процедуру приведения к блобу. Он уже есть в исходной карте эджей со ссылкой в виде base64.
            // - Эдж, который с base64-URL появился в новой карте, но отсутсвует в старой. Нужно запустить его блоббирование.
            dataUrl <- jde.url
            if (dataUrl startsWith dataPrefix) &&
              // Это dataURL. Тут два варианта: юзер загрузил новую картинку только что, либо загружена ранее.
              // Смотрим в old-эджи, есть ли там текущий эдж с этой картинкой.
              !(edgesData0 contains jde.id)
          } yield {
            (dataUrl, edgeData)
          }
        )
          .toSeq

        // Собрать эффект запуска аплоада на сервер для всех найденных картинок.
        val uploadFxOpt = dataEdgesForUpload
          .iterator
          .map [Effect] { case (dataUrl, _) =>
            // Это новая картинка. Организовать перегонку в blob.
            Effect {
              val fut = for {
                blob <- Base64JsUtil.b64Url2Blob(dataUrl)
              } yield {
                B64toBlobDone(dataUrl, blob)
              }
              for (ex <- fut.failed)
                LOG.error(ErrorMsgs.BASE64_TO_BLOB_FAILED, ex = ex)
              fut
            }
          }
          .mergeEffects

        val maxEmbedWidth = BlockWidths.NARROW.value
        val qdSubTree3 = dataEdgesForUpload
          .foldLeft(qdSubTree2.loc) { case (qdLoc, (_, edgeData)) =>
            // Новая картинка. Найти и уменьшить её ширину в шаблоне.
            val edgeUid = edgeData.jdEdge.id
            _qdUpdateWidth(
              qdLoc,
              edgeUid = edgeUid,
              width = maxEmbedWidth,
              heightPxOpt = None,
              needUpdateF = Some { widthPx =>
                widthPx > maxEmbedWidth || widthPx <= 0
              }
            )
          }
          .toTree

        val jdDoc2 = MJdDoc.template.set( __updateTpl(qdSubTree3) )(v0.jdDoc.jdArgs.data.doc)

        // Залить все данные в новое состояние.
        val v2 = (
          MDocS.jdDoc.modify(
            MJdDocEditS.jdArgs.modify(
              MJdArgs.data.modify(
                MJdDataJs.doc.set( jdDoc2 ) andThen
                MJdDataJs.edges.set( edgesData3 )
              ) andThen
              MJdArgs.jdRuntime.set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
            )
          ) andThen
          MDocS.editors
            .composeLens(MEditorsS.qdEdit)
            .modify { qdEditOpt0 =>
              for (qdEdit <- qdEditOpt0) yield {
                if (dataEdgesForUpload.isEmpty) {
                  // Перерендер не требуется, тихо сохранить текущую дельту в состояние.
                  MQdEditS.realDelta.set( Some(m.fullDelta) )(qdEdit)
                } else {
                  // Если был новый embed, то надо перерендерить редактор новой дельтой, т.к. наверняка изменились размеры чего-либо.
                  qdEdit.withInitRealDelta(
                    initDelta = quillDeltaJsUtil.qdTag2delta(qdSubTree3, edgesData3)
                  )
                }
              }
            }
        )(v0)

        // Вернуть итоговую карту эджей и объединённый эффект.
        ah.updatedMaybeEffect(v2, uploadFxOpt)
      }


    // Изменения состояния ротации текущего jd-тега.
    case m: RotateSet =>
      val v0 = value

      (for {
        selJdtLoc0 <- v0.jdDoc.jdArgs.selJdt.treeLocOpt
        jdt0 = selJdtLoc0.getLabel
        if (jdt0.props1.rotateDeg !=* m.degrees) &&
          // Убедится, что значение не выходит за допустымые пределы поворота:
          m.degrees.fold(true) { deg =>
            Math.abs(deg) <= JdConst.ROTATE_MAX_ABS
          }
      } yield {
        val jdt2 = JdTag.props1
          .composeLens( MJdtProps1.rotateDeg )
          .set( m.degrees )( jdt0 )
        val tpl2 = selJdtLoc0
          .setLabel(jdt2)
          .toTree
        val jdDoc2 = MJdDoc.template.set(tpl2)( v0.jdDoc.jdArgs.data.doc )
        var jdRuntime2 = mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)

        // Для qd-blockless надо пробросить Pot.pending() и новый ключ для тега с прежним значением внутрь рантайма.
        if ( isQdBlockless(selJdtLoc0) )
          jdRuntime2 = repairQdBl(v0.jdDoc.jdArgs.jdRuntime.data.qdBlockLess, jdt0, jdt2, jdRuntime2, reMeasure = true)

        val v2 = MDocS.jdDoc.modify(
          MJdDocEditS.jdArgs.modify(
            MJdArgs.data
              .composeLens( MJdDataJs.doc )
              .set( jdDoc2 ) andThen
            MJdArgs.jdRuntime
              .set( jdRuntime2 )
          )
        )( v0 )

        updated( v2 )
      })
       .getOrElse( noChange )


    // Клик по элементу карточки.
    case m: JdTagSelect =>
      val v0 = value
      val oldSelectedTag = v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .map(_.getLabel)
      if ( oldSelectedTag contains m.jdTag ) {
        // Бывают повторные щелчки по уже выбранным элементам, это нормально.
        noChange

      } else {
        val oldTagName = oldSelectedTag.map(_.name)

        // Юзер выбрал какой-то новый элемент. Залить новый тег в seleted:
        val newSelJdtTreeLoc = v0.jdDoc.jdArgs.data.doc.template
          .loc
          .findByLabel( m.jdTag )
          .get
        val newSelJdt = newSelJdtTreeLoc.toNodePath

        var v2 = MDocS.jdDoc
          .composeLens( MJdDocEditS.jdArgs )
          .composeLens( MJdArgs.renderArgs )
          .composeLens( MJdRenderArgs.selPath )
          .set( Some(newSelJdt) )(v0)

        // Если это QdTag, то отработать состояние quill-delta:
        v2 = if (m.jdTag.name ==* MJdTagNames.QD_CONTENT) {
          // Это qd-тег, значит нужно собрать и залить текущую дельту текста в состояние.
          // Нужно получить текущее qd-под-дерево (для сборки дельты)
          val delta2 = quillDeltaJsUtil.qdTag2delta(
            qd    = newSelJdtTreeLoc.tree,
            edges = v2.jdDoc.jdArgs.data.edges
          )
          //println( "selJdt\n" + JSON.stringify(delta2) )
            MDocS.editors.modify(
              MEditorsS.qdEdit
                .set( Some(
                  MQdEditS( initDelta = delta2 )
                )) andThen
              MEditorsS.slideBlocks
                .composeLens( MSlideBlocks.expanded )
                .set( Some(SlideBlockKeys.CONTENT) )
          )(v2)

        } else {
          // Очистить состояние от дельты.
          MDocS.editors
            .modify(_.withOutQdEdit)(v2)
        }

        // Если это strip, то активировать состояние strip-редактора.
        m.jdTag.name match {
          // Переключение на новый стрип. Инициализировать состояние stripEd:
          case n @ MJdTagNames.STRIP =>
            val s2 = MStripEdS(
              isLastStrip = {
                val hasManyStrips = v2.jdDoc.jdArgs.data.doc.template
                  .deepOfTypeIter( n )
                  // Оптимизация: НЕ проходим весь strip-итератор, а считаем только первые два стрипа.
                  .slice(0, 2)
                  .size > 1
                !hasManyStrips
              }
            )
            v2 = MDocS.editors
              .composeLens(MEditorsS.stripEd)
              .set( Some(s2) )(v2)

            // Если тип текущего тега изменился, то сбросить текущий slide-блок.
            if ( !oldTagName.contains(n) ) {
              v2 = MDocS.editors
                .composeLens( MEditorsS.slideBlocks )
                .composeLens( MSlideBlocks.expanded )
                .set( Some(SlideBlockKeys.BLOCK_BG) )(v2)
            }

          // Это не strip, обнулить состояние stripEd, если оно существует:
          case _ =>
            v2 = MDocS.editors
              .modify(_.withOutStripEd)(v2)
        }

        // Может быть, был какой-то qd-tag и весь текст теперь в нём удалён? Удалить, если старый тег, если осталась дельта
        for {
          jdtTree <- v0.jdDoc.jdArgs.selJdt.treeOpt
          jdt = jdtTree.rootLabel
          dataEdges0 = v0.jdDoc.jdArgs.data.edges
          if (jdt.name ==* MJdTagNames.QD_CONTENT) &&
            QdJsUtil.isEmpty(jdtTree, dataEdges0) &&
            v2.jdDoc.jdArgs.data.doc.template.contains(jdt)
        } {
          val tpl1 = v2.jdDoc.jdArgs.data.doc.template
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
          val jdDoc2 = (MJdDoc.template set tpl2)( v2.jdDoc.jdArgs.data.doc )
          v2 = MDocS.jdDoc.modify(
            MJdDocEditS.jdArgs.modify(
              MJdArgs.data.modify(
                MJdDataJs.doc.set( jdDoc2 ) andThen
                MJdDataJs.edges.set( dataEdges2 )
              ) andThen
              MJdArgs.jdRuntime.set( mkJdRuntime(jdDoc2, v2.jdDoc.jdArgs) )
            )
          )(v2)
        }

        // Если состояние dnd непустое, то значит была ошибка перетакивания, и надо принудительно сбросить dnd-состояние.
        // Если этого не сделать, то рамочка вокруг текущего тега не будет рендерится.
        val fxOpt = OptionUtil.maybe(v2.jdDoc.jdArgs.renderArgs.dnd.nonEmpty)( JdTagDragEnd.toEffectPure )

        // Обновить список color-preset'ов.
        val bgColorsAppend = for {
          // Закинуть цвет фона нового тега в самое начало списка презетов. Затем - окончательный фон предыдущего тега.
          jdt <- m.jdTag :: v0.jdDoc.jdArgs.selJdt.treeLocOpt.map(_.getLabel).toList
          bgColor <- jdt.props1.bgColor
          if !v2.editors.colorsState.colorPresets.contains(bgColor)
        } yield {
          bgColor
        }
        if (bgColorsAppend.nonEmpty) {
          val presets2 = bgColorsAppend.foldLeft(v2.editors.colorsState.colorPresets) { MColorsState.prependPresets }
          // то закинуть его цвет фона в color-презеты.
          v2 = MDocS.editors
            .composeLens( MEditorsS.colorsState )
            .composeLens( MColorsState.colorPresets )
            .set( presets2 )(v2)
        }

        ah.updatedMaybeEffect( v2, fxOpt )
      }


    // Завершена фоновая конвертация base64-URL в Blob.
    case m: B64toBlobDone =>
      val v0 = value
      val dataEdgesMap0 = v0.jdDoc.jdArgs.data.edges

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
            blob    = m.blob,
            blobUrl = blobUrlOpt
          )
        } {
          // Ссылка изменилась на blob, но нельзя трогать delta: quill не поддерживает blob-ссылки.
          MJsFileInfo.blob.set( m.blob ) andThen
          MJsFileInfo.blobUrl.set( blobUrlOpt )
        }

        val dataEdge1 = MEdgeDataJs.fileJs
          .set( Some(fileJs2) )(dataEdge0)

        (dataEdge1, blobUrl)
      }

      // Залить в состояние обновлённый эдж.
      dataEdgeOpt2.fold {
        LOG.warn( WarnMsgs.SOURCE_FILE_NOT_FOUND, msg = m.blob )
        noChange
      } { case (dataEdge2, blobUrl) =>
        val dataEdgesMap1 = dataEdgesMap0.updated(dataEdge2.id, dataEdge2)
        val dataEdgesMap2 = JdTag.purgeUnusedEdges(v0.jdDoc.jdArgs.data.doc.template, dataEdgesMap1)
        val v2 = MDocS.jdDoc
          .composeLens( MJdDocEditS.jdArgs )
          .composeLens( MJdArgs.data )
          .composeLens( MJdDataJs.edges )
          .set( dataEdgesMap2 )(v0)

        // Запустить эффект хэширования и дальнейшей закачки файла на сервер.
        val hashFx = FileHashStart(dataEdge2.id, blobUrl).toEffectPure
        updated(v2, hashFx)
      }


    // Поступила команда на проведение чистки карты эджей.
    case PurgeUnusedEdges =>
      val v0 = value
      val edges0 = v0.jdDoc.jdArgs.data.edges
      val edges2 = JdTag.purgeUnusedEdges( v0.jdDoc.jdArgs.data.doc.template, edges0 )
      if ( edges0.size ==* edges2.size ) {
        noChange
      } else {
        val v2 = MDocS.jdDoc
          .composeLens( MJdDocEditS.jdArgs )
          .composeLens( MJdArgs.data )
          .composeLens( MJdDataJs.edges )
          .set( edges2 )(v0)
        updated(v2)
      }


    // Принудительный ребилд плитки из JdAh.
    case GridRebuild =>
      val v0 = value
      val v2 = MDocS.jdDoc
        .composeLens( MJdDocEditS.gridBuild )
        .set( GridBuilderUtilJs.buildGridFromJdArgs(v0.jdDoc.jdArgs) )(v0)

      updated(v2)


    // Клик по кнопкам управления размером текущего блока
    case m: BlockSizeBtnClick =>
      val v0 = value

      val stripTreeLoc0 = v0.jdDoc.jdArgs.selJdt.treeLocOpt.get
      val strip0 = stripTreeLoc0.getLabel

      val jdt_p1_bm_LENS = JdTag.props1
        .composeLens( MJdtProps1.bm )

      val bmOpt0 = jdt_p1_bm_LENS.get( strip0 )
      bmOpt0
        // Сконвертить в функцию обновления, если значение требует изменения:
        .flatMap { bm0 =>
          m.model match {
            case bhs @ BlockHeights =>
              val sz0 = bm0.h
              val szOpt2 = m.direction match {
                case MHands.Left  => bhs.previousOf( sz0 )
                case MHands.Right => bhs.nextOf( sz0 )
              }
              szOpt2.map( BlockMeta.h.set )

            case bws @ BlockWidths =>
              val sz0 = bm0.w
              val szOpt2 = m.direction match {
                case MHands.Left  => bws.previousOf( sz0 )
                case MHands.Right => bws.nextOf( sz0 )
              }
              szOpt2.map(BlockMeta.w.set)
          }
        }
        .fold(noChange) { bmUpdateF =>
          val strip2 = jdt_p1_bm_LENS
            .composeTraversal( Traversal.fromTraverse[Option, BlockMeta] )
            .modify( bmUpdateF )(strip0)

          val template2 = stripTreeLoc0
            .setLabel(strip2)
            .toTree
          val jdDoc2 = (MJdDoc.template set template2)( v0.jdDoc.jdArgs.data.doc )
          val jdArgs2 = (
            MJdArgs.data
              .composeLens( MJdDataJs.doc )
              .set( jdDoc2 ) andThen
            MJdArgs.jdRuntime
              .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
          )(v0.jdDoc.jdArgs)

          // Обновить и дерево, и currentTag новым инстансом.
          val v2 = MDocS.jdDoc.modify(
            (MJdDocEditS.jdArgs set jdArgs2) andThen
            (MJdDocEditS.gridBuild set GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2))
          )(v0)

          updated( v2 )
        }


    // Клик по кнопке удаления или подтверждения удаления текущего стрипа
    case m: StripDelete =>
      val v0 = value
      val stripEdS0 = v0.editors.stripEd.get

      if (!m.confirmed) {
        // Юзер нажал на обычную кнопку удаления стрипа.
        if (stripEdS0.confirmingDelete) {
          // Кнопка первого шага удаления уже была нажата: игнорим дубликат неактуального события
          noChange
        } else {
          val v2 = MDocS.editors
            .composeLens( MEditorsS.stripEd )
            .composeTraversal( Traversal.fromTraverse[Option, MStripEdS] )
            .composeLens( MStripEdS.confirmingDelete )
            .set(true)(v0)
          updated(v2)
        }

      } else {
        // Второй шаг удаления, и юзер подтвердил удаление.
        val strip4delLoc = v0.jdDoc.jdArgs
          .selJdt.treeLocOpt
          .get

        val tpl0 = v0.jdDoc.jdArgs.data.doc.template
        val tpl2 = strip4delLoc
          .delete
          .fold(tpl0)( _.toTree )
        val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

        if (tpl2.subForest.nonEmpty) {
          val jdArgs0 = v0.jdDoc.jdArgs
          val jdArgs2 = jdArgs0.copy(
            data        = (MJdDataJs.doc set jdDoc2)( jdArgs0.data ),
            jdRuntime   = mkJdRuntime(jdDoc2, jdArgs0),
            renderArgs  = (MJdRenderArgs.selPath set None)( jdArgs0.renderArgs ),
          )
          val v2 = (
            MDocS.jdDoc.modify(
              MJdDocEditS.jdArgs
                .set( jdArgs2 ) andThen
              MJdDocEditS.gridBuild
                .set( GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2) )
            ) andThen
            MDocS.editors.modify(
              (MEditorsS.stripEd set None) andThen
              // qdEdit: Вроде бы это сюда не относится вообще. Сбросим заодно и текстовый редактор:
              (MEditorsS.qdEdit set None)
            )
          )(v0)

          updated( v2 )

        } else {
          // Нельзя удалять из карточки последний оставшийся блок.
          noChange
        }
      }


    // Юзер передумал удалять текущий блок.
    case StripDeleteCancel =>
      val v0 = value
      val stripEdS0 = v0.editors.stripEd.get
      if (stripEdS0.confirmingDelete) {
        // Юзер отменяет удаление
        val s2 = MStripEdS.confirmingDelete
          .set(false)(stripEdS0)
        val v2 = MDocS.editors
          .composeLens(MEditorsS.stripEd)
          .set( Some(s2) )(v0)
        updated(v2)
      } else {
        // Какой-то левый экшен пришёл. Возможно, просто дублирующийся.
        noChange
      }


    // Началось перетаскивание какого-то jd-тега из текущего документа.
    case m: JdTagDragStart =>
      val v0 = value
      val dnd0Jdt = v0.jdDoc.jdArgs.draggingTagLoc
      if (dnd0Jdt.toLabelOpt contains m.jdTag) {
        noChange
      } else {
        val v2 = _jdArgs_renderArgs_dnd_LENS
          .composeLens( MJdDndS.jdt )
          .set( v0.jdDoc.jdArgs.data.doc.template.nodeToPath(m.jdTag) )(v0)

        // Если запускается перетаскивание тега, который не является текущим, то надо "выбрать" таскаемый тег.
        if ( v0.jdDoc.jdArgs.selJdt.treeLocOpt.toLabelOpt contains m.jdTag ) {
          // Текущий тег перетаскивается, всё ок.
          updated( v2 )
        } else {
          // Активировать текущий тег
          val fx = JdTagSelect(m.jdTag).toEffectPure
          updated( v2, fx )
        }
      }


    // dragend. Нередко, он не наступает вообще. Т.е. код тут ненадёжен и срабатывает редко, почему-то.
    case JdTagDragEnd =>
      val v0 = value
      val l = _jdArgs_renderArgs_dnd_LENS
      l.get(v0).jdt.fold(noChange) { _ =>
        val v2 = l.set( MJdDndS.empty )(v0)
        updated( v2 )
      }


    // Юзер отпустил перетаскиваемый объект на какой-то стрип. Нужно запихать этот объект в дерево нового стрипа.
    case m: JdDropContent =>
      val v0 = value
      val tpl0 = v0.jdDoc.jdArgs.data.doc.template

      // Найти tree loc текущего тега наиболее оптимальным путём. С некоторой вероятностью это -- selected-тег:
      val dndJdtLoc0 = {
        // Получить на руки инстанс сброшенного тега.
        // Прячемся от общего scope, чтобы работать с элементом только через его tree loc.
        val dndJdt = m.foreignTag
          .orElse( v0.jdDoc.jdArgs.draggingTagLoc.toLabelOpt )
          .get

        v0.jdDoc.jdArgs.selJdt.treeLocOpt
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

      // Найти исходный strip в исходном шаблоне:
      val strip0Opt = dndJdtLoc0
        .findUpByType( MJdTagNames.STRIP )

      val isSameStrip = strip0Opt.fold(false)(_.getLabel ==* m.strip)

      // Дополнительно обработать Y-координату.
      val clXy0 = m.clXy

      // Если strip изменился, то надо пересчитать координаты относительно нового стрипа:
      val clXy2 = if (isSameStrip || strip0Opt.isEmpty) {
        clXy0
      } else {
        val fromStrip = strip0Opt.get.getLabel
        // Перемещение между разными strip'ами. Надо пофиксить координату Y, иначе добавляемый элемент отрендерится где-то за экраном.
        val strips = tpl0
          .deepOfTypeIter( MJdTagNames.STRIP )
          .toSeq
        // TODO Отработать ситуацию, когда хотя бы один из index'ов == -1
        val fromStripIndex = strips.indexOf(fromStrip)
        val toStripIndex = strips.indexOf(m.strip)

        val (topStrip, bottomStrip, yModSign) =
          if (fromStripIndex <= toStripIndex) (fromStrip, m.strip, -1)
          else (m.strip, fromStrip, +1)

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
          MCoords2di.y.set(y2)( clXy0 )
        } else {
          // Странно: нет пройденных стрипов, хотя они должны бы быть
          LOG.warn( msg = s"$clXy0 [$fromStrip => ${m.strip})" )
          clXy0
        }
      }

      val jdtLabel2 = {
        val dndJdt0 = dndJdtLoc0.getLabel
        JdTag.props1
          .composeLens( MJdtProps1.topLeft )
          .set( Some(clXy2) )(dndJdt0)
      }

      val loc2 = if (isSameStrip) {
        // strip не изменился. Надо обновить узел не меняя местоположения в дереве.
        dndJdtLoc0.setLabel( jdtLabel2 )

      } else {
        // Изменился стрип. Удалить из старого места, закинуть в хвост новому блоку/стрипу. Выставить новые координаты тегу
        val dndJdtTree2 = Tree.Node(
          root   = jdtLabel2,
          forest = dndJdtLoc0.tree.subForest
        )
        // Извлекаем тег из старого местоположения:
        dndJdtLoc0.delete.get
          // Фокус на соседнем или родительском узле.
          .root
          // Найти целевой стрип:
          .findByLabel( m.strip )
          .get
          // Добавляем как последний дочерний элемент текущего стрипа. Пусть будет поверх всех.
          .insertDownLast( dndJdtTree2 )
      }

      val tpl2 = loc2.toTree
      val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

      // Пересобрать данные для рендера.
      val v2 = MDocS.jdDoc
        .composeLens(MJdDocEditS.jdArgs)
        .modify { jdArgs0 =>
          jdArgs0.copy(
            data        = MJdDataJs.doc.set(jdDoc2)( jdArgs0.data ),
            jdRuntime   = mkJdRuntime(jdDoc2, jdArgs0),
            renderArgs  = (
              MJdRenderArgs.selPath
                .set( tpl2.nodeToPath( loc2.getLabel ) ) andThen
              MJdRenderArgs.dnd
                .set( MJdDndS.empty )
            )(jdArgs0.renderArgs)
          )
        }(v0)

      updated(v2)


    // Сброшен целый блок куда-то на документ.
    case m: JdDropToDocument =>
      val v0 = value
      val droppedBlockLoc = v0.jdDoc.jdArgs.draggingTagLoc.get
      val droppedBlockLabel = droppedBlockLoc.getLabel

      // Надо вычислить позицию координаты сброса относительно текущей плитки блоков.
      val paddingPx = v0.jdDoc.jdArgs.conf.blockPadding.value

      val allBlocks = v0.jdDoc.jdArgs
        .data.doc.template
        .subForest

      // Сброс может быть на блок (на верхнюю или нижнюю половины блока) или в промежутки между блоков.
      val droppedNear = allBlocks
        .zip( v0.jdDoc.gridBuild.coords )
        .map { case (jdt, topLeft) =>
          MJdtWithXy(
            jdt = jdt,
            topLeft = topLeft,
            isDownerTl =
              (m.docXy.x >= topLeft.x - paddingPx) &&
              (m.docXy.y >= topLeft.y - paddingPx)
          )
        }
        // Отсеять элементы сверху.
        .dropWhile { jdtXy =>
          // Оставлять, если ниже верхнего левого угла с padding, но выше нижнего правого угла с padding.
          val isKeep = jdtXy.isDownerTl && {
            val jdtWh = jdtXy.jdt.rootLabel.props1.bm.get
            (m.docXy.x <= jdtXy.topLeft.x + jdtWh.width  + paddingPx) &&
            (m.docXy.y <= jdtXy.topLeft.y + jdtWh.height + paddingPx)
          }
          !isKeep
        }
        // Отсеять элементы снизу: т.е. оставить только элементы, которые ощутимо ниже нижней границе блока (кроме смежного/пересекающегося c точкой блока).
        .takeWhile( _.isDownerTl )

      // droppedNear содержит один или два элемента (хотя не исключены и иные ситуации).
      if (droppedNear.exists(_.jdt.rootLabel ==* droppedBlockLabel)) {
        // Перетаскивание блока на самого себя или в щель вокруг себя - игнор.
        // Убрать dnd-состояние.
        val dnd_LENS = _jdArgs_renderArgs_dnd_LENS
        if (dnd_LENS.get(v0).jdt.nonEmpty) {
          val v2 = (dnd_LENS set MJdDndS.empty)(v0)
          updated(v2)
        } else {
          // should not happen
          noChange
        }

      } else {
        // Разобраться, как именно нужно обновить jd-дерево.
        val (nearJdtTree, isUpper) = droppedNear match {
          // Если один блок - то сброс произошёл прямо в конкретный блок, и нужно по высоте оценить: выше или ниже.
          case Stream(jdtXy) =>
            val jdtWh = jdtXy.jdt.rootLabel.props1.bm.get
            val innerY = m.docXy.y - jdtXy.topLeft.y
            val isUp = innerY < jdtWh.height / 2
            (jdtXy.jdt, isUp)

          // Если два блока, то сброс был в щель между двумя блоками.
          case Stream(_ /*before*/, after) =>
            (after.jdt, true)

          // Неопределённая ситуация. Переносим блок или в начало или в конец документа.
          case other =>
            LOG.warn( WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = (m, other.mkString(HtmlConstants.PIPE)) )
            val coord0 = v0.jdDoc.gridBuild.coords.head
            if (m.docXy.y > coord0.y) {
              // Положительная координата перетаскивания по вертикали: просто переносим таскаемый блок в конец документа.
              (allBlocks.last, false)
            } else {
              // Поместить таскаемый блок ПЕРЕД самым первым блоком документа.
              (allBlocks.head, true)
            }
        }
        val nearJdt = nearJdtTree.rootLabel

        // Выполнить запланированные действия в дереве документа.
        val targetStripLoc = droppedBlockLoc
          .delete
          .get
          .root
          .findByLabel( nearJdt )
          .get

        // Убрать значение topLeft, если задано. Это для qd-blockless, когда контент был вынесен за пределы блока.
        val jdt_p1_topLeft_LENS = JdTag.props1 composeLens MJdtProps1.topLeft
        val needModifyJdt = jdt_p1_topLeft_LENS.get(droppedBlockLabel).nonEmpty
        val droppedBlockLoc1 = if (needModifyJdt) {
          droppedBlockLoc
            // Обрубаем loc сверху, чтобы modifyLabel() гарантировано не затрагивал ничего кроме текущего уровня, который стал верхним.
            .tree
            .loc
            .modifyLabel( jdt_p1_topLeft_LENS set None )
        } else {
          droppedBlockLoc
        }
        val droppedBlockTree = droppedBlockLoc1.tree

        val droppedBlockLoc2 = if (isUpper) {
          targetStripLoc.insertLeft( droppedBlockTree )
        } else {
          targetStripLoc.insertRight( droppedBlockTree )
        }

        // Залить обновлённый список стрипов в исходный документ
        val tpl2 = droppedBlockLoc2.toTree
        val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

        val jdArgs2 = (
          MJdArgs.data
            .composeLens( MJdDataJs.doc )
            .set(jdDoc2) andThen
          MJdArgs.renderArgs.modify(
            MJdRenderArgs.selPath
              .set( tpl2 nodeToPath droppedBlockTree.rootLabel ) andThen
            MJdRenderArgs.dnd
              .set( MJdDndS.empty )
          ) andThen
          MJdArgs.jdRuntime
            .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
        )(v0.jdDoc.jdArgs)

        // Сборка основной модификации состояния jdArgs в связи с перемещением:
        var v2 = MDocS.jdDoc.modify(
          (MJdDocEditS.jdArgs set jdArgs2) andThen
          // Если перемещение strip'а или сброс qd-контента на документ, то надо пересчитать плитку:
          (MJdDocEditS.gridBuild set GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2) )
        )(v0)
        updated(v2)
      }


    // Появилась новая гистограмма в карте гисторамм. Нужно поправить эджи, у которых фон соответствует связанной картинке.
    case m: HandleNewHistogramInstalled =>
      val v0 = value
      val edgeUids4mod = v0.jdDoc.jdArgs
        .data.edges
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
        v0.editors
          .colorsState
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

            lazy val p1_bgColor_LENS = JdTag.props1
              .composeLens( MJdtProps1.bgColor )

            val tpl2 = v0.jdDoc.jdArgs.data.doc.template
              .map { el1 =>
                el1
                  .edgeUids
                  .map(_.edgeUid)
                  .find(edgeUids4mod.contains)
                  .fold(el1) { _ =>
                    el1.qdProps.fold {
                      p1_bgColor_LENS
                        .set( topColorMcdOpt )(el1)
                    } { qdProps0 =>
                      JdTag.qdProps.set( Some(
                        MQdOp.attrsText.set( Some(
                          qdProps0.attrsText
                            .getOrElse( MQdAttrsText.empty )
                            .withBackground(
                              topColorMcdOpt.map {SetVal.apply }
                            )
                        ))(qdProps0)
                      ))(el1)
                    }
                  }
              }
            val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

            // Сохранить новые темплейт в состояние.
            var v2 = MDocS.jdDoc
              .composeLens( MJdDocEditS.jdArgs )
              .modify(
                MJdArgs.data
                  .composeLens( MJdDataJs.doc )
                  .set(jdDoc2) andThen
                MJdArgs.jdRuntime
                  .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
              )(v0)

            // Надо заставить перерендерить quill, если он изменился и открыт сейчас:
            for {
              qdEdit0     <- v0.editors.qdEdit
              qdTag2      <- v2.jdDoc.jdArgs.selJdt.treeOpt
              if qdTag2.rootLabel.name ==* MJdTagNames.QD_CONTENT
              // Перерендеривать quill только если изменение гистограммы коснулось эджа внутри текущего qd-тега:
              qdTag0      <- v0.jdDoc.jdArgs.selJdt.treeOpt
              if qdTag0 !=* qdTag2
            } {
              v2 = MDocS.editors
                .composeLens(MEditorsS.qdEdit)
                .set( Some(
                  qdEdit0.withInitRealDelta(
                    initDelta = quillDeltaJsUtil.qdTag2delta( qdTag2, v2.jdDoc.jdArgs.data.edges )
                  )
                ))(v0)
            }

            updated( v2 )
          }
      }


    // Ручной ресайз контента (по ширине).
    case m: ResizeContent =>
      val v0 = value
      val loc0 = v0.jdDoc.jdArgs
        .selJdt.treeLocOpt
        .get
      val jdt0 = loc0.getLabel
      val jdt_p1_width_LENS = JdTag.props1
        .composeLens( MJdtProps1.widthPx )
      if (jdt_p1_width_LENS.get(jdt0) contains[Int] m.widthPx) {
        // Ширина изменилась в исходное значение.
        noChange

      } else {
        require( jdt0.name ==* MJdTagNames.QD_CONTENT )
        // Сохранить новую ширину в состояние текущего тега:
        val jdt2 = jdt_p1_width_LENS
          .set( Some(m.widthPx) )( jdt0 )
        val loc2 = loc0.setLabel( jdt2 )
        val tpl2 = loc2.toTree
        val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )
        var jdRuntime2 = mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
        if (!parentIsStrip(loc2))
          jdRuntime2 = repairQdBl(v0.jdDoc.jdArgs.jdRuntime.data.qdBlockLess, jdt0, jdt2, jdRuntime2, reMeasure = true)

        val v2 = MDocS.jdDoc
          .composeLens(MJdDocEditS.jdArgs)
          .modify(
            MJdArgs.data
              .composeLens( MJdDataJs.doc )
              .set(jdDoc2) andThen
            MJdArgs.jdRuntime
              .set( jdRuntime2 )
          )(v0)

        updated(v2)
      }


    // Реакция на сигнал ресайза у embed'а.
    case m: QdEmbedResize =>
      val v0 = value
      v0.jdDoc.jdArgs
        .selJdt.treeOpt
        .filter { jdt => jdt.rootLabel.name ==* MJdTagNames.QD_CONTENT }
        .map { qdSubTree =>
          _qdUpdateWidth(qdSubTree.loc, m.edgeUid, width = m.widthPx, heightPxOpt = m.heightPx)
        }
        .fold {
          LOG.log( WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT )
          noChange
        } { qdSubTreeLoc2 =>
          val qdSubTree2 = qdSubTreeLoc2.toTree
          val tpl2 = v0.jdDoc.jdArgs.selJdt.treeLocOpt
            .get
            .setTree(qdSubTree2)
            .toTree

          val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )
          val v2 = (
            MDocS.jdDoc
              .composeLens(MJdDocEditS.jdArgs)
              .modify(
                MJdArgs.data
                  .composeLens( MJdDataJs.doc )
                  .set( jdDoc2 ) andThen
                MJdArgs.jdRuntime
                  .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
              ) andThen
            MDocS.editors
              .composeLens(MEditorsS.qdEdit)
              .modify { qdEditOpt0 =>
                for (qdEdit0 <- qdEditOpt0) yield {
                  qdEdit0.withInitRealDelta(
                    initDelta = quillDeltaJsUtil.qdTag2delta( qdSubTreeLoc2.root.tree, v0.jdDoc.jdArgs.data.edges )
                  )
                }
              }
          )(v0)

          updated( v2 )
        }


    // Изменение слоя для выделенного контента.
    case m: JdChangeLayer =>
      val v0 = value
      // Текущий тег в дереве - держим на руках:
      val loc0 = v0.jdDoc.jdArgs.selJdt.treeLocOpt.get

      val loc2OrNull: TreeLoc[JdTag] = if (!m.bounded) {
        // Если !m.bounded, то поменять тег местами с соседними тегами.
        // Если шаг влево и есть теги слева...
        if (!m.up && loc0.lefts.nonEmpty) {
          // Меняем местами первый левый тег с текущим
          val leftLoc = loc0.left.get
          leftLoc
            .setTree( loc0.tree )
            .right.get.setTree( leftLoc.tree )
        } else if (m.up && loc0.rights.nonEmpty) {
          val rightLoc = loc0.right.get
          rightLoc
            .setTree( loc0.tree )
            .left.get.setTree( rightLoc.tree )
        } else {
          null
        }

      } else if (loc0.lefts.nonEmpty || loc0.rights.nonEmpty) {
        // bounded: Можно и нужно двигать до края, значит надо удалить текущий элемент из treeLoc и добавить в начало/конец через parent.
        val parentLoc1 = loc0
          .delete.get
          .parent.get
        if (!m.up) parentLoc1.insertDownFirst( loc0.tree )
        else parentLoc1.insertDownLast( loc0.tree )

      } else {
        // Нет соседних элементов для движения по слоям.
        null
      }

      Option(loc2OrNull).fold(noChange) { loc2 =>
        val tpl2 = loc2.toTree

        val v2 = MDocS.jdDoc
          .composeLens(MJdDocEditS.jdArgs)
          .modify(
            MJdArgs.data
              .composeLens( MJdDataJs.doc )
              .composeLens( MJdDoc.template )
              .set(tpl2) andThen
            // Надо пересчитать path до перемещённого тега.
            MJdArgs.renderArgs
              .composeLens( MJdRenderArgs.selPath )
              .set( tpl2.nodeToPath( loc0.getLabel ) )
            // css можно не обновлять, т.к. там просто поменяется порядок стилей без видимых изменений.
          )(v0)

        updated(v2)
      }


    // Замена состояния галочки широкого рендера текущего стрипа новым значением
    case m: BlockExpand =>
      val v0 = value

      val jdt_p1_bm_expandOpt_LENS = JdTag.props1
        .composeLens( MJdtProps1.bm )
        .composeTraversal( Traversal.fromTraverse[Option, BlockMeta] )
        .composeLens( BlockMeta.expandMode )
      val jdtLoc0 = v0.jdDoc.jdArgs
        .selJdt.treeLocOpt
        .get

      val jdt0 = jdtLoc0.getLabel
      require(jdt0.name ==* MJdTagNames.STRIP)

      if ( jdt_p1_bm_expandOpt_LENS.exist(_ ==* m.expandMode)(jdt0) ) {
        noChange

      } else {
        val tpl2 = jdtLoc0
          .modifyLabel { strip0 =>
            (jdt_p1_bm_expandOpt_LENS set m.expandMode)(strip0)
          }
          .toTree
        val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

        val jdArgs2 = (
          MJdArgs.data
            .composeLens(MJdDataJs.doc)
            .set( jdDoc2 ) andThen
          MJdArgs.jdRuntime
            .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
        )(v0.jdDoc.jdArgs)

        val v2 = MDocS.jdDoc.modify(
          MJdDocEditS.jdArgs.set( jdArgs2 ) andThen
          MJdDocEditS.gridBuild.set(
            GridBuilderUtilJs.buildGridFromJdArgs( jdArgs2 )
          )
        )(v0)

        updated( v2 )
      }


    // Изменение галочки управления main-флагом текущего блока.
    case m: MainStripChange =>
      val v0 = value

      // Фунция для ленивого обновления стрипа новым значением.
      val p1_isMain_LENS = JdTag.props1
        .composeLens( MJdtProps1.isMain )

      def __updateLocLabel(label: JdTag, newValue: Option[Boolean]): JdTag = {
        if ( p1_isMain_LENS.get(label) !=* newValue) {
          p1_isMain_LENS
            .set( newValue )( label )
        } else {
          label
        }
      }

      v0.jdDoc.jdArgs.selJdt.treeLocOpt
        .filter(_.getLabel.name ==* MJdTagNames.STRIP)
        .fold(noChange) { currStripLoc =>
          // Вычислить новые значения для этого и всех соседних элементов.
          val currTagNewMainValue = OptionUtil.maybeTrue( m.isMain )
          // Надо обновить все соседние инстансы новым анти-значением:
          val otherStripsNewMainValue = Option.empty[Boolean]

          val currStrip = currStripLoc.getLabel

          val tpl0 = v0.jdDoc.jdArgs.data.doc.template

          // Собрать корневой элемент с обновлённым стрипами:
          val tpl2 = Tree.Node(
            root = tpl0.rootLabel,
            forest = for (stripTree <- v0.jdDoc.jdArgs.data.doc.template.subForest) yield {
              val lbl = stripTree.rootLabel
              require( lbl.name ==* MJdTagNames.STRIP )
              val label2 = __updateLocLabel(
                label     = lbl,
                newValue  =
                  if (lbl ===* currStrip) currTagNewMainValue
                  else otherStripsNewMainValue
              )
              // Собрать узел стрипа:
              Tree.Node(
                root    = label2,
                forest  = stripTree.subForest
              )
            }
          )
          val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

          val v2 = MDocS.jdDoc
            .composeLens(MJdDocEditS.jdArgs)
            .modify(
              MJdArgs.data
                .composeLens( MJdDataJs.doc )
                .set( jdDoc2 ) andThen
              MJdArgs.jdRuntime
                .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
            )( v0 )

          updated(v2)
        }


    // Сигнал переключения
    case m: ShowMainStrips =>
      val v0 = value
      val hideNonMainStrips2 = m.showing
      if (v0.jdDoc.jdArgs.renderArgs.hideNonMainStrips ==* hideNonMainStrips2) {
        noChange
      } else {
        val v2 = MDocS.jdDoc
          .composeLens( MJdDocEditS.jdArgs )
          .composeLens( MJdArgs.renderArgs )
          .composeLens( MJdRenderArgs.hideNonMainStrips )
          .set( hideNonMainStrips2 )(v0)

        updated(v2)
      }


    // Реакция на клик по кнопке создания "контента", который у нас является синонимом QdTag.
    case AddContentClick =>
      val v0 = value
      val stripName = MJdTagNames.STRIP
      val intoStripLoc = v0.jdDoc.jdArgs.selJdt.treeLocOpt
        .fold {
          // Сейчас нет выделенных тегов. Найти первый попавшийся strip
          v0.jdDoc.jdArgs.data.doc.template
            .loc
            .findByType( stripName )
        } { selLoc =>
          // Если выбран какой-то не-strip элемент, то найти его strip. Если выделен strip, то вернуть его.
          selLoc.findUpByType( stripName )
        }
        .get


      val textL10ed = Messages( MsgCodes.`Example.text` )

      val textPred = MPredicates.JdContent.Text
      val edgesMap0 = v0.jdDoc.jdArgs.data.edges
      val (edgesMap2, edgeUid) = edgesMap0
        .valuesIterator
        .find { e =>
          e.jdEdge.predicate ==* textPred &&
          (e.jdEdge.text contains textL10ed)
        }
        .fold [(Map[EdgeUid_t, MEdgeDataJs], Int)] {
          // Нет примера текста в эджах: добавить его туда.
          val nextEdgeUid = EdgesUtil.nextEdgeUidFromMap( edgesMap0 )
          val e = MEdgeDataJs(
            jdEdge = MJdEdge(
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
        root = {
          val bm0 = intoStripLoc.getLabel.props1.bm.get
          val rnd = new Random()
          val coordsRnd = MCoords2di(
            x = rnd.nextInt( bm0.w.value/3 ) + 10,
            y = rnd.nextInt( (bm0.h.value * 0.75).toInt ) + (bm0.h.value * 0.12).toInt
          )
          JdTag.qd(
            topLeft = coordsRnd,
          )
        },
        forest = Stream(
          Tree.Leaf(
            JdTag.edgeQdOp( edgeUid )
          )
        )
      )

      val qdtLoc = intoStripLoc.insertDownLast( qdtTree )

      val tpl2 = qdtLoc.toTree
      val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

      val v2 = (
        MDocS.jdDoc
          .composeLens( MJdDocEditS.jdArgs )
          .modify (
            MJdArgs.data.modify(
              MJdDataJs.doc.set( jdDoc2 ) andThen
              MJdDataJs.edges.set( edgesMap2 )
            ) andThen
            MJdArgs.jdRuntime
              .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) ) andThen
            MJdArgs.renderArgs
              .composeLens( MJdRenderArgs.selPath )
              .set( tpl2.nodeToPath( qdtTree.rootLabel ) )
          ) andThen
        MDocS.editors.modify(
          MEditorsS.qdEdit.set( Some {
            MQdEditS(
              initDelta = quillDeltaJsUtil.qdTag2delta(
                qd    = qdtTree,
                edges = edgesMap2
              )
            )
          }) andThen
          MEditorsS.stripEd
            .set( None ) andThen
          MEditorsS.slideBlocks
            .composeLens( MSlideBlocks.expanded )
            .set( Some(SlideBlockKeys.CONTENT) )
        )
      )(v0)

      updated(v2)


    // Клик по кнопке добавления нового стрипа.
    case AddStripClick =>
      val v0 = value

      val currStripLocOpt = v0.jdDoc.jdArgs.selJdt.treeLocOpt
        .flatMap {
          _.findUpByType( MJdTagNames.STRIP )
        }

      // Взять цвет фона с текущего стрипа.
      val bgColorSome0 = currStripLocOpt
        .map(_.getLabel)
        .orElse {
          // Попытаться взять цвет из главного блока или любого первого попавшегося блока с цветом фона.
          v0.jdDoc.jdArgs.data.doc.template
            .getMainBlock
            .map(_.rootLabel)
        }
        .toIterator
        // Поискать цвет фона среди всех стрипов.
        .++ {
          v0.jdDoc.jdArgs.data.doc.template
            .deepOfTypeIter( MJdTagNames.STRIP )
        }
        .flatMap( _.props1.bgColor )
        .toStream
        // Взять первый цвет
        .headOption
        .orElse {
          // Если нет цвета, то использовать белый цвет.
          Some(MColorData.Examples.WHITE)
        }

      // Собрать начальный блок:
      val newStripTree = Tree.Leaf(
        JdTag.strip(
          bm      = BlockMeta.DEFAULT,
          bgColor = bgColorSome0
        )
      )

      // Найти предшествующий стрип и воткнуть справа от него. Либо в конце подураня корня шаблона, если текущего стрипа нет.
      val newStripLoc = currStripLocOpt.fold {
        // Нет текущего стрипа. Впихнуть просто в шаблон
        v0.jdDoc.jdArgs.data.doc.template
          .loc
          .insertDownLast( newStripTree )
      } { currStripLoc =>
        currStripLoc.insertRight( newStripTree )
      }

      val tpl2 = newStripLoc.toTree
      val jdDoc2 = MJdDoc.template.set(tpl2)( v0.jdDoc.jdArgs.data.doc )
      val jdArgs2 = (
        MJdArgs.data
          .composeLens( MJdDataJs.doc )
          .set(jdDoc2) andThen
          MJdArgs.renderArgs
            .composeLens( MJdRenderArgs.selPath )
            .set( tpl2.nodeToPath( newStripTree.rootLabel ) ) andThen
          MJdArgs.jdRuntime
            .set( mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs) )
      )(v0.jdDoc.jdArgs)

      val v2 = (
        MDocS.jdDoc.modify(
          MJdDocEditS.jdArgs
            .set( jdArgs2 ) andThen
          MJdDocEditS.gridBuild
            .set( GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2) )
        ) andThen
        MDocS.editors
          .composeLens( MEditorsS.slideBlocks )
          .composeLens( MSlideBlocks.expanded )
          .set( Some(SlideBlockKeys.BLOCK) )
      )(v0)

      updated(v2)


    // Выставить новый масштаб для рендера карточи.
    case m: SetScale =>
      val v0 = value

      val conf2 = MJdConf.szMult
        .set(m.szMult)( v0.jdDoc.jdArgs.conf )

      val jdArgs0 = v0.jdDoc.jdArgs
      val jdArgs2 = (
        MJdArgs.conf
          .set( conf2 ) andThen
        MJdArgs.jdRuntime
          .set( mkJdRuntime(jdArgs0.data.doc, conf2, jdArgs0.jdRuntime) )
      )(jdArgs0)

      val v2 = MDocS.jdDoc.modify(
        (MJdDocEditS.jdArgs set jdArgs2) andThen
        (MJdDocEditS.gridBuild set GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2))
      )(v0)

      updated(v2)


    // Включение-выключение тени для текста/контента
    case m: SetTextShadowEnabled =>
      val v0 = value
      v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .filter { treeLoc =>
          val jdt = treeLoc.getLabel
          (jdt.name ==* MJdTagNames.QD_CONTENT) &&
          (jdt.props1.textShadow.isDefined !=* m.enabled)
        }
        .shadowUpdated(v0) { _ =>
          OptionUtil.maybe(m.enabled) {
            MJdShadow(
              hOffset = 4,
              vOffset = 4,
              color   = None,
              blur    = Some(1)
            )
          }
        }

    // Выставление горизонтального параметра тени.
    case m: SetHorizOffTextShadow =>
      val v0 = value
      v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .shadowUpdated(v0) { shadOpt0 =>
          for (shad0 <- shadOpt0) yield {
            shad0.withHOffset(
              m.offset
            )
          }
        }

    case m: SetVertOffTextShadow =>
      val v0 = value
      v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .shadowUpdated(v0) { shadOpt0 =>
          for (shad0 <- shadOpt0) yield {
            shad0.withVOffset( m.offset )
          }
        }

    case m: SetBlurTextShadow =>
      val v0 = value
      v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .shadowUpdated(v0) { shadOpt0 =>
          for (shad0 <- shadOpt0) yield {
            shad0.withBlur(
              OptionUtil.maybe(m.blur > 0)(m.blur)
            )
          }
        }

  }


  private def _jdArgs_renderArgs_dnd_LENS = {
    MDocS.jdDoc
      .composeLens( MJdDocEditS.jdArgs )
      .composeLens( MJdArgs.renderArgs )
      .composeLens( MJdRenderArgs.dnd )
  }

}

object DocEditAh {

  def mkJdRuntime(jdDoc: MJdDoc, jdArgs: MJdArgs): MJdRuntime =
    mkJdRuntime(jdDoc, jdArgs.conf, jdArgs.jdRuntime)
  def mkJdRuntime(jdDoc: MJdDoc, jdConf: MJdConf, jdRuntime0: MJdRuntime): MJdRuntime = {
    JdUtil
      .mkRuntime( jdConf )
      .docs( jdDoc )
      .prev( jdRuntime0 )
      .make
  }


  def parentIsStrip(selJdtLoc0: TreeLoc[JdTag]): Boolean = {
    selJdtLoc0
      .parent
      .exists(_.getLabel.name ==* MJdTagNames.STRIP)
  }

  def isQdBlockless(selJdtLoc0: TreeLoc[JdTag]): Boolean = {
    (selJdtLoc0.getLabel.name ==* MJdTagNames.QD_CONTENT) &&
    !parentIsStrip( selJdtLoc0 )
  }

  def repairQdBl(qdBlMap0  : HashMap[JdTag, Pot[MQdBlSize]],
                 jdt0      : JdTag,
                 jdt2      : JdTag,
                 jdRuntime : MJdRuntime,
                 reMeasure : Boolean = false,
                ): MJdRuntime = {
    // Извлечь старое состояние qd-blockless из старого рантайма:
    val qdBlPot0 = qdBlMap0
      .getOrElse(jdt0, Pot.empty)
    // Выставить pending, чтобы принудительно вызвать measure()-функцию в шаблоне-компоненте.
    val qdBlPot2 =
      if (reMeasure && !qdBlPot0.isPending) qdBlPot0.pending()
      else qdBlPot0
    MJdRuntime.data
      .composeLens(MJdRuntimeData.qdBlockLess)
      .modify(_ + (jdt2 -> qdBlPot2))(jdRuntime)
  }

}


/** Вспомогательная внутренняя модель при обработке [[io.suggest.jd.edit.m.JdDropToDocument]].
  *
  * @param jdt Jd-тег
  * @param topLeft Верхней левый угол тега в координатах плитки.
  * @param isDownerTl Находится ли верхний левый угол с паддингом выше точки клика?
  */
private case class MJdtWithXy(
                               jdt          : Tree[JdTag],
                               topLeft      : MCoords2di,
                               isDownerTl   : Boolean,
                             )

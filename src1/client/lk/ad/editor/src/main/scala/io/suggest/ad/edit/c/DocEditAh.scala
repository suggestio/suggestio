package io.suggest.ad.edit.c

import diode._
import diode.data.Pot
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.{MDocS, MEditorsS, MJdDocEditS, MQdEditS, MSlideBlocks, MStripEdS, SlideBlockKeys}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.html.HtmlConstants
import io.suggest.file.MJsFileInfo
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.i18n.MsgCodes
import io.suggest.jd.edit.m.{JdChangeLayer, JdDropToBlock, JdDropToDocument, JdTagDragEnd, JdTagDragStart, JdTagSelect, QdEmbedResize, SetContentWidth}
import io.suggest.jd.{JdConst, MJdConf, MJdDoc, MJdEdge, MJdTagId}
import io.suggest.jd.render.m._
import io.suggest.jd.render.u.JdUtil
import io.suggest.jd.tags._
import io.suggest.jd.tags.qd._
import io.suggest.lk.m.color.MColorsState
import io.suggest.lk.m.{FileHashStart, HandleNewHistogramInstalled, PurgeUnusedEdges}
import io.suggest.n2.edge.{EdgesUtil, MEdgeDataJs, MEdgeDoc, MPredicates}
import io.suggest.msg.{ErrorMsgs, Messages}
import io.suggest.pick.{BlobJsUtil, ContentTypeCheck, MimeConst}
import io.suggest.primo.SetVal
import io.suggest.quill.m.{EmbedFile, TextChanged}
import io.suggest.quill.u.QuillDeltaJsUtil
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ueq.QuillUnivEqUtil._
import io.suggest.common.BooleanUtil.Implicits._
import io.suggest.common.geom.d2.ISize2di
import io.suggest.img.MImgFormats
import io.suggest.n2.media.MFileMeta
import io.suggest.proto.http.HttpConst
import io.suggest.scalaz.NodePath_t
import japgolly.univeq._
import org.scalajs.dom.raw.URL
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.text.StringUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.Traversal

import scala.util.Random
import scalaz.{EphemeralStream, Tree, TreeLoc}
import scalaz.std.option._

import scala.annotation.tailrec
import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 14:34
  * Description: Контроллер событий редактирования документа.
  */
class DocEditAh[M](
                    modelRW           : ModelRW[M, MDocS],
                    quillDeltaJsUtil  : QuillDeltaJsUtil
                  )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  import DocEditAh._

  private def _qdUpdateWidth(imgOpLoc0: TreeLoc[JdTag], width: Int, heightPxOpt: Option[Int] = None): TreeLoc[JdTag] = {
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


  implicit class MDocSOptOpsExt( val treeLocOpt: Option[TreeLoc[JdTag]] ) {

    def shadowUpdated(v0: MDocS)(f: Option[MJdShadow] => Option[MJdShadow]): ActionResult[M] = {
      treeLocOpt.fold(noChange) { treeLoc0 =>
        val treeLoc2 = treeLoc0
          .modifyLabel {
            JdTag.props1
              .composeLens( MJdtProps1.textShadow )
              .modify(f)
          }
        val tpl2 = treeLoc2.toTree
        val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

        val jdRuntime2 = mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
          .result

        val v2 = MDocS.jdDoc
          .composeLens(MJdDocEditS.jdArgs)
          .modify(
            MJdArgs.jdRuntime
              .set( jdRuntime2 ) andThen
            MJdArgs.data
              .composeLens( MJdDataJs.doc )
              .set(jdDoc2)
          )( v0 )

        updated(v2)
      }
    }

  }

  private def _shadowUpdating(f: Option[MJdShadow] => Option[MJdShadow]): ActionResult[M] = {
    val v0 = value
    v0.jdDoc.jdArgs.selJdt
      .treeLocOpt
      .shadowUpdated(v0)(f)
  }


  private def _updateQdContentProps(selJdt2: JdTag, v0: MDocS): ActionResult[M] = {
    val jdArgs0 = v0.jdDoc.jdArgs
    val selJdtLoc0 = jdArgs0.selJdt.treeLocOpt.get

    val tpl2 = selJdtLoc0
      .setLabel(selJdt2)
      .toTree
    val jdDoc2 = (MJdDoc.template set tpl2)( jdArgs0.data.doc )

    // Выставить Pot.pending в qdBlockLess для текущего тега, а потом только пересобирать новый рантайм.
    val jdRuntime1 = if (isQdBlockless(selJdtLoc0)) {
      DocEditAh.qdBlockLess2Pending( jdArgs0 )
    } else {
      jdArgs0.jdRuntime
    }

    val v2 = MDocS.jdDoc.modify(
      MJdDocEditS.jdArgs.modify(
        MJdArgs.data
          .composeLens( MJdDataJs.doc )
          .set( jdDoc2 ) andThen
        MJdArgs.jdRuntime.set(
          mkJdRuntime2(jdDoc2, jdArgs0.conf, jdRuntime1)
            .result
        )
      )
    )( v0 )

    updated( v2 )
  }


  /** Общий код добавления контента как вне блока, так и внутри блока.
    *
    * @param qdContentTag Добавляемый тег.
    * @param insertToLocF Функция вставки в необходимый tree loc.
    * @return ActionResult.
    */
  private def _addContent(v0: MDocS, qdContentTag: JdTag)
                         (insertToLocF: Tree[JdTag] => TreeLoc[JdTag]): ActionResult[M] = {

    val textL10ed = Messages( MsgCodes.`Example.text` )

    val textPred = MPredicates.JdContent.Text
    val edgesMap0 = v0.jdDoc.jdArgs.data.edges

    val (edgesMap2, edgeUid) = (for {
      e <- edgesMap0.valuesIterator
      if (e.jdEdge.predicate ==* textPred) &&
         (e.jdEdge.edgeDoc.text contains textL10ed)
      edgeUid <- e.jdEdge.edgeDoc.id
    } yield {
      (edgesMap0, edgeUid)
    })
      .nextOption()
      .getOrElse {
        // Нет примера текста в эджах: добавить его туда.
        val nextEdgeUid = EdgesUtil.nextEdgeUidFromMap( edgesMap0 )
        val e = MEdgeDataJs(
          jdEdge = MJdEdge(
            predicate = textPred,
            edgeDoc = MEdgeDoc(
              id        = Some( nextEdgeUid ),
              text      = Some( textL10ed ),
            )
          )
        )
        val edgesMap1 = edgesMap0 + (nextEdgeUid -> e)
        (edgesMap1, nextEdgeUid)
      }

    val qdtTree = Tree.Node[JdTag](
      root = qdContentTag,
      forest = {
        Tree.Leaf(
          JdTag.edgeQdOp( edgeUid )
        ) ##::
          EphemeralStream[Tree[JdTag]]
      }
    )

    val loc2 = insertToLocF( qdtTree )
    val tpl2 = loc2.toTree
    val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

    val v2 = (
      MDocS.jdDoc.modify {
        val jdArgs2 = (
          MJdArgs.data.modify(
            MJdDataJs.doc.set( jdDoc2 ) andThen
            MJdDataJs.edges.set( edgesMap2 )
          ) andThen
          MJdArgs.jdRuntime.set(
            mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
              .result
          ) andThen
          MJdArgs.renderArgs
            .composeLens( MJdRenderArgs.selPath )
            .set( Some(loc2.toNodePath) )
        )(v0.jdDoc.jdArgs)

        var modF = MJdDocEditS.jdArgs.set(jdArgs2)

        // Это qd-blockless? Надо пересчитать плитку.
        if (qdContentTag.props1.topLeft.isEmpty) {
          modF = modF andThen MJdDocEditS.gridBuild
            .set( GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2) )
        }

        modF
      } andThen
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
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Набор текста в wysiwyg-редакторе.
    case m: TextChanged =>
      val v0 = value

      if ( v0.editors.qdEdit.exists(_.initDelta ==* m.fullDelta) ) {
        // Бывают ложные срабатывания. Например, прямо при инициализации редактора. Но не факт конечно, что они тут подавляются.
        noChange

      } else if (v0.jdDoc.jdArgs.selJdt.treeLocOpt.isEmpty) {
        logger.warn( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = m.getClass.getName )
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
        val edgesData3 = JdTag
          .purgeUnusedEdges( tpl1, edgesData2 )
          .toMap

        // 20. Необходимо организовать блобификацию файлов эджей, заданных через dataURL.
        val dataPrefix = HtmlConstants.Proto.DATA_
        //val blobPrefix = HtmlConstants.Proto.BLOB_

        // 30. Найти новые эджи картинок, которые надо загружать на сервер.
        val dataEdgesForUpload = (
          for {
            edgeData <- edgesData3.valuesIterator
            jde = edgeData.jdEdge
            edgeUid <- jde.edgeDoc.id
            // Три варианта:
            // - Просто эдж, который надо молча завернуть в EdgeData. Текст, например.
            // - Эдж, сейчас который проходит асинхронную процедуру приведения к блобу. Он уже есть в исходной карте эджей со ссылкой в виде base64.
            // - Эдж, который с base64-URL появился в новой карте, но отсутсвует в старой. Нужно запустить его блоббирование.
            dataUrl <- jde.url
            if {
              (
                //(dataUrl startsWith blobPrefix) ||    // Quill.Formats.Image режет ссылки, не являющиеся http | https | data:base64
                (dataUrl startsWith dataPrefix)
              ) && {
                // Это dataURL. Тут два варианта: юзер загрузил новую картинку только что, либо загружена ранее.
                // Смотрим в old-эджи, есть ли там текущий эдж с этой картинкой.
                !(edgesData0 contains edgeUid)
              }
            }
          } yield {
            (dataUrl, edgeData)
          }
        )
          .toSeq

        // Собрать эффект запуска аплоада на сервер для всех найденных картинок.
        val toB64FxOpt = dataEdgesForUpload
          .iterator
          .map [Effect] { case (dataUrl, _ /*dataEdgeJs*/) =>
            // Это новая картинка. Организовать перегонку в blob.
            Effect {
              val blobReadyFut = (for {
                qdEdit0 <- v0.editors.qdEdit
                domFile <- qdEdit0.newFiles.get( dataUrl )
              } yield {
                val r = B64toBlobDone(dataUrl, domFile)
                Future.successful( r )
              })
                .getOrElse {
                  logger.warn( ErrorMsgs.BLOB_EXPECTED, msg = StringUtil.strLimitLen(dataUrl, 16) )
                  for {
                    blob <- BlobJsUtil.b64Url2Blob( dataUrl )
                  } yield {
                    B64toBlobDone(dataUrl, blob)
                  }
                }

              for (ex <- blobReadyFut.failed)
                logger.error(ErrorMsgs.BASE64_TO_BLOB_FAILED, ex = ex)
              blobReadyFut
            }
          }
          .mergeEffects

        val maxEmbedWidth = BlockWidths.NARROW.value
        val qdSubTree3 = dataEdgesForUpload
          .foldLeft(qdSubTree2.loc) { case (qdLoc, (_, edgeData)) =>
            // Новая картинка. Найти и уменьшить её ширину в шаблоне.
            (for {
              edgeUid <- edgeData.jdEdge.edgeDoc.id
              imgOpLoc <- qdLoc.findByEdgeUid( edgeUid )
              widthPxOpt = _loc2width(imgOpLoc)
              if widthPxOpt.fold(true) { widthPx =>
                widthPx > maxEmbedWidth || widthPx <= 0
              }
            } yield {
              _qdUpdateWidth(
                imgOpLoc,
                width = maxEmbedWidth,
                heightPxOpt = None,
              )
            })
              .getOrElse( qdLoc )
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
              MJdArgs.jdRuntime.set(
                mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
                  .result
              )
            )
          ) andThen
          MDocS.editors
            .composeLens(MEditorsS.qdEdit)
            .modify { qdEditOpt0 =>
              for (qdEdit <- qdEditOpt0) yield {
                val qdEdit1 = if (dataEdgesForUpload.isEmpty) {
                  // Перерендер не требуется, тихо сохранить текущую дельту в состояние.
                  (MQdEditS.realDelta set Some(m.fullDelta))(qdEdit)
                } else {
                  // Если был новый embed, то надо перерендерить редактор новой дельтой, т.к. наверняка изменились размеры чего-либо.
                  qdEdit.withInitRealDelta(
                    initDelta = quillDeltaJsUtil.qdTag2delta(qdSubTree3, edgesData3)
                  )
                }

                // Используем пошаговое вычитание из исходной карты, т.к. тут обычно Map1 или EmptyMap.
                val newFiles2 = dataEdgesForUpload
                  .iterator
                  .map(_._1)
                  .foldLeft( qdEdit.newFiles )( _ - _ )
                if (newFiles2 !===* qdEdit1.newFiles) {
                  (MQdEditS.newFiles set newFiles2 )(qdEdit1)
                } else {
                  qdEdit1
                }
              }
            }
        )(v0)

        // Вернуть итоговую карту эджей и объединённый эффект.
        ah.updatedMaybeEffect(v2, toB64FxOpt)
      }


    // Редактирование заголовка.
    case m: TitleEdit =>
      val v0 = value

      val ad_title_LENS = MDocS.jdDoc
        .composeLens( MJdDocEditS.jdArgs )
        .composeLens( MJdArgs.data )
        .composeLens( MJdDataJs.title )

      val titleOpt = Option.when(m.title !=* "")(m.title)

      if (ad_title_LENS.get(v0) ==* titleOpt) {
        noChange

      } else {
        val v2 = (ad_title_LENS set titleOpt)(v0)
        updated(v2)
      }


    // Изменение высоты строки (межстрочки).
    case m: LineHeightSet =>
      val v0 = value
      val jdArgs0 = v0.jdDoc.jdArgs

      (for {
        selJdtLoc0 <- jdArgs0.selJdt.treeLocOpt
        jdt0 = selJdtLoc0.getLabel

        if (jdt0.props1.lineHeight !=* m.lineHeight) && (
          // Убедится, что значение не выходит за допустымые пределы поворота:
          m.lineHeight
            .fold(true)( MJdtProps1.LineHeight.isValid )
        )

      } yield {
        val jdt2 = JdTag.props1
          .composeLens( MJdtProps1.lineHeight )
          .set( m.lineHeight )( jdt0 )

        _updateQdContentProps(jdt2, v0)
      })
        .getOrElse(noChange)


    // Изменения состояния ротации текущего jd-тега.
    case m: RotateSet =>
      val v0 = value

      (for {
        selJdtLoc0 <- v0.jdDoc.jdArgs.selJdt.treeLocOpt
        jdt0 = selJdtLoc0.getLabel

        if (jdt0.props1.rotateDeg !=* m.degrees) && (
          // Убедится, что значение не выходит за допустымые пределы поворота:
          m.degrees.fold(true) { deg =>
            Math.abs(deg) <= JdConst.ROTATE_MAX_ABS
          }
        )

      } yield {
        val jdt2 = JdTag.props1
          .composeLens( MJdtProps1.rotateDeg )
          .set( m.degrees )( jdt0 )

        _updateQdContentProps(jdt2, v0)
      })
       .getOrElse( noChange )


    // В quill в текст вставлен новый файл, но пока не обработан в TextChanged. Сохранить в состоянии.
    case m: EmbedFile =>
      val v0 = value

      (for {
        qdEdit0 <- v0.editors.qdEdit
        if !(qdEdit0.newFiles contains m.b64Url)
      } yield {
        val v2 = MDocS.editors
          .composeLens( MEditorsS.qdEdit )
          .composeTraversal( Traversal.fromTraverse[Option, MQdEditS] )
          .composeLens( MQdEditS.newFiles )
          .modify(_ + (m.b64Url -> m.file))(v0)
        updatedSilent(v2)
      })
        .getOrElse( noChange )


    // Клик по элементу карточки.
    case m: JdTagSelect =>
      val v0 = value
      val oldSelectedTag = v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .map(_.getLabel)

      if ( oldSelectedTag contains m.jdTag ) {
        if (!m.silent && v0.jdDoc.jdArgs.renderArgs.dnd.nonEmpty) {
          // Была какая-то ошибка во время перетаскивания. Надо сбросить dnd-состояние.
          // Если не сбросить, то тег останется невыделенным.
          val fx = JdTagDragEnd.toEffectPure
          effectOnly( fx )
        } else {
          // Бывают повторные щелчки по уже выбранным элементам, это нормально.
          noChange
        }

      } else {
        val oldTagName = oldSelectedTag.map(_.name)

        // Юзер выбрал какой-то новый элемент. Залить новый тег в seleted:
        val nodePath = m.jdId.selPathRev.reverse: NodePath_t
        val (newSelJdt, newSelJdtTreeLoc) = v0.jdDoc.jdArgs.data.doc.template
          .loc
          .pathToNode( nodePath )
          .map( nodePath -> _ )
          .orElse {
            // fallback на медленный поиск в дереве перебором тегов:
            logger.warn(ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = (m, nodePath) )
            v0.jdDoc.jdArgs.data.doc.template
              .loc
              .findByLabel( m.jdTag )
              .map { loc =>
                loc.toNodePath -> loc
              }
          }
          .get

        var v2 = MDocS.jdDoc
          .composeLens( MJdDocEditS.jdArgs )
          .composeLens( MJdArgs.renderArgs )
          .composeLens( MJdRenderArgs.selPath )
          .set( Some(newSelJdt) )(v0)

        // Если это QdTag, то отработать состояние quill-delta:
        v2 = (if (m.jdTag.name ==* MJdTagNames.QD_CONTENT) {
          // Это qd-тег, значит нужно собрать и залить текущую дельту текста в состояние.
          // Нужно получить текущее qd-под-дерево (для сборки дельты)
          val delta2 = quillDeltaJsUtil.qdTag2delta(
            qd    = newSelJdtTreeLoc.tree,
            edges = v2.jdDoc.jdArgs.data.edges
          )

          MDocS.editors.modify(
            MEditorsS.qdEdit
              .set( Some(
                MQdEditS( initDelta = delta2 )
              )) andThen
            MEditorsS.slideBlocks
              .composeLens( MSlideBlocks.expanded )
              .set( Some(SlideBlockKeys.CONTENT) )
          )

        } else {
          // Очистить состояние от дельты.
          MDocS.editors
            .modify(_.withOutQdEdit)
        })(v2)

        // Если это strip, то активировать состояние strip-редактора.
        m.jdTag.name match {
          // Переключение на новый стрип. Инициализировать состояние stripEd:
          case n @ MJdTagNames.STRIP =>
            val s2 = MStripEdS(
              isLastStrip = EphemeralStream
                .toIterable(
                  v2.jdDoc.jdArgs.data.doc.template
                    .subForest
                )
                .sizeIs <= 1
            )
            v2 = MDocS.editors
              .composeLens(MEditorsS.stripEd)
              .set( Some(s2) )(v2)

            // Если тип текущего тега изменился, то сбросить текущий slide-блок.
            if ( !(oldTagName contains n) ) {
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
             (v2.jdDoc.jdArgs.data.doc.template contains jdt)
          // v0, т.к. надо удалять исходный тег:
          selPath0 <- v0.jdDoc.jdArgs.renderArgs.selPath
          oldSelectedJdt <- newSelJdtTreeLoc
            .root
            .pathToNode( selPath0 )
        } {
          val tpl2 = oldSelectedJdt
            .delete
            .fold {
              logger.warn( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = jdt )
              v2.jdDoc.jdArgs.data.doc.template
            }(_.toTree)

          // Очистить эджи от лишнего контента
          val jdDoc2 = (MJdDoc.template set tpl2)( v2.jdDoc.jdArgs.data.doc )
          val jdArgs2 = (
            MJdArgs.data.modify(
              MJdDataJs.doc.set( jdDoc2 ) andThen
              MJdDataJs.edges.set(
                JdTag
                  .purgeUnusedEdges(tpl2, dataEdges0)
                  .toMap
              )
            ) andThen
            MJdArgs.jdRuntime.set(
              mkJdRuntime(jdDoc2, v2.jdDoc.jdArgs).result
            )
          )(v2.jdDoc.jdArgs)

          v2 = MDocS.jdDoc.modify {
            var jdDocModF = MJdDocEditS.jdArgs.set( jdArgs2 )

            if ( isQdBlockless( oldSelectedJdt ) )
              jdDocModF = jdDocModF andThen MJdDocEditS.gridBuild.set( GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2) )

            jdDocModF
          }(v2)
        }

        // Если состояние dnd непустое, то значит была ошибка перетакивания, и надо принудительно сбросить dnd-состояние.
        // Если этого не сделать, то рамочка вокруг текущего тега не будет рендерится.
        val fxOpt = OptionUtil.maybe(!m.silent && v2.jdDoc.jdArgs.renderArgs.dnd.nonEmpty)( JdTagDragEnd.toEffectPure )

        // Обновить список color-preset'ов.
        val bgColorsAppend = (for {
          // Закинуть цвет фона нового тега в самое начало списка презетов. Затем - окончательный фон предыдущего тега.
          jdt <- m.jdTag #:: v0.jdDoc.jdArgs.selJdt
            .treeLocOpt
            .map(_.getLabel)
            .to( LazyList )
          bgColor <- jdt.props1.bgColor
          if !(v2.editors.colorsState.colorPresets contains bgColor)
        } yield {
          bgColor
        })

        // Разобраться с цветами: сброс возможного color picker'а, закидон полезного цвета в палитру.
        var colorStateUpdAccF = List.empty[MColorsState => MColorsState]

        // Если есть обновления по цветам или отображается color picker...
        if (bgColorsAppend.nonEmpty) {
          // закинуть его цвет фона в color-презеты.
          colorStateUpdAccF ::= MColorsState.colorPresets.modify {
            bgColorsAppend.foldLeft(_) { MColorsState.prependPresets }
          }
        }

        if (v2.editors.colorsState.picker.nonEmpty)
          colorStateUpdAccF ::= MColorsState.picker.set( None )

        if (colorStateUpdAccF.nonEmpty) {
          v2 = MDocS.editors
            .composeLens( MEditorsS.colorsState )
            .modify( colorStateUpdAccF.reduce(_ andThen _) )(v2)
        }

        // Всё готово, можно вернуть результаты:
        ah.updatedMaybeEffect( v2, fxOpt )
      }


    // Завершена фоновая конвертация base64-URL в Blob.
    case m: B64toBlobDone =>
      val v0 = value
      val dataEdgesMap0 = v0.jdDoc.jdArgs.data.edges

      val dataEdgeOpt0 = dataEdgesMap0
        .valuesIterator
        .find { e =>
          e.jdEdge.imgSrcOpt contains m.b64Url
        }

      // Вычислить обновлённый эдж, если есть старый эдж для данной картинки.
      val dataEdgeOpt2 = (for {
        // Поиска по исходной URL, потому что карта эджей могла изменится за время фоновой задачи.
        dataEdge0 <- dataEdgeOpt0
        edgeUid <- dataEdge0.jdEdge.edgeDoc.id

        // Убедится, что у нас тут картинка.
        imgContentTypeOpt = {
          val ctRaw = m.blob.`type`
          val r = MimeConst.readContentType(ctRaw, ContentTypeCheck.OnlyImages)
          if (r.isEmpty)
            logger.warn( ErrorMsgs.CONTENT_TYPE_UNEXPECTED, msg = (ctRaw, MImgFormats.values.iterator.flatMap(_.allMimes).mkString(", ") ) )
          r
        }
        if imgContentTypeOpt.nonEmpty

      } yield {
        // Найден исходный эдж. Залить в него инфу по блобу, выкинув оттуда dataURL:
        val blobUrl = if (m.b64Url startsWith HttpConst.Proto.BLOB_)
          m.b64Url
        else
          URL.createObjectURL( m.blob )

        val blobUrlOpt = Option( blobUrl )

        // Нельзя забыть Base64 dataURL, потому что quill их не понимает, заменяя их на "//:0".
        // Сохранить инфу по блобу.
        val fileJs2 = dataEdge0.fileJs.fold {
          MJsFileInfo(
            blob    = m.blob,
            blobUrl = blobUrlOpt,
            fileMeta = MFileMeta(
              mime  = imgContentTypeOpt,
              sizeB = Some( m.blob.size.toLong ),
            ),
          )
        } {
          // Ссылка изменилась на blob, но нельзя трогать delta: quill не поддерживает blob-ссылки.
          MJsFileInfo.blob.set( m.blob ) andThen
          MJsFileInfo.blobUrl.set( blobUrlOpt )
        }

        val dataEdge1 = (MEdgeDataJs.fileJs set Some(fileJs2))( dataEdge0 )
        val hashFx = FileHashStart( edgeUid, blobUrl ).toEffectPure
        (dataEdge1, hashFx, edgeUid)
      })

      val v2Opt = dataEdgeOpt2
        .map { case (dataEdge2, _, edgeUid) =>
          dataEdgesMap0.updated( edgeUid, dataEdge2 )
        }
        .orElse {
          for {
            dataEdge0 <- dataEdgeOpt0
            edgeUid <- dataEdge0.jdEdge.edgeDoc.id
          } yield {
            // Авто-удаление только что некорректного файла/блоба. TODO Надо вычистить и шаблон следом.
            dataEdgesMap0.removed( edgeUid )
          }
        }
        .map { dataEdgesMap1 =>
          val dataEdges2 = JdTag
            .purgeUnusedEdges(v0.jdDoc.jdArgs.data.doc.template, dataEdgesMap1)
            .toMap

          MDocS.jdDoc
            .composeLens( MJdDocEditS.jdArgs )
            .composeLens( MJdArgs.data )
            .composeLens( MJdDataJs.edges )
            .set( dataEdges2 )(v0)
        }

      val fxOpt = dataEdgeOpt2.map(_._2)
      ah.optionalResult(v2Opt, fxOpt)


    // Поступила команда на проведение чистки карты эджей.
    case PurgeUnusedEdges =>
      val v0 = value
      val edges0 = v0.jdDoc.jdArgs.data.edges
      val edges2 = JdTag
        .purgeUnusedEdges( v0.jdDoc.jdArgs.data.doc.template, edges0 )
        .toMap

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
    case _: GridRebuild =>
      val v0 = value
      val v2 = MDocS.jdDoc
        .composeLens( MJdDocEditS.gridBuild )
        .set( GridBuilderUtilJs.buildGridFromJdArgs(v0.jdDoc.jdArgs) )(v0)

      updated(v2)


    // Клик по кнопкам управления размером текущего блока
    case m: BlockSizeBtnClick =>
      val v0 = value

      val stripTreeLoc0 = v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .get
      val blk0 = stripTreeLoc0.getLabel

      val (sz3, lens) = m.model match {
        case bhs @ BlockHeights =>
          val sz2 = bhs.withValue( m.value )
          sz2 -> MJdtProps1.heightPx
        case bws @ BlockWidths =>
          val sz2 = bws.withValue( m.value )
          sz2 -> MJdtProps1.widthPx
      }
      val bmUpdateF = lens.set( Some(sz3.value) )

      val blk2 = JdTag.props1
        .modify(bmUpdateF)(blk0)

      val jdDoc2 = MJdDoc.template.set(
        stripTreeLoc0
          .setLabel(blk2)
          .toTree
      )( v0.jdDoc.jdArgs.data.doc )

      val jdArgs2 = (
        MJdArgs.data
          .composeLens( MJdDataJs.doc )
          .set( jdDoc2 ) andThen
        MJdArgs.jdRuntime.set(
          mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs).result
        )
      )(v0.jdDoc.jdArgs)

      // Обновить и дерево, и currentTag новым инстансом.
      val v2 = MDocS.jdDoc.modify(
        (MJdDocEditS.jdArgs set jdArgs2) andThen
        (MJdDocEditS.gridBuild set GridBuilderUtilJs.buildGridFromJdArgs(jdArgs2))
      )(v0)

      updated( v2 )


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

        val tpl2 = strip4delLoc
          .delete
          .fold( v0.jdDoc.jdArgs.data.doc.template )( _.toTree )
        val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

        if (!tpl2.subForest.isEmpty) {
          val jdArgs0 = v0.jdDoc.jdArgs
          val jdArgs2 = jdArgs0.copy(
            data        = (MJdDataJs.doc set jdDoc2)( jdArgs0.data ),
            jdRuntime   = mkJdRuntime(jdDoc2, jdArgs0).result,
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
        val v2 = MDocS.editors
          .composeLens(MEditorsS.stripEd)
          .set {
            val s2 = MStripEdS.confirmingDelete
              .set(false)(stripEdS0)
            Some(s2)
          }(v0)
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
        val nodePath = m.jdId.selPathRev.reverse
        val v2 = _jdArgs_renderArgs_dnd_LENS
          .composeLens( MJdDndS.jdt )
          //.set( v0.jdDoc.jdArgs.data.doc.template.nodeToPath(m.jdTag) )(v0)
          .set( Some(nodePath) )(v0)

        // Если запускается перетаскивание тега, который не является текущим, то надо "выбрать" таскаемый тег.
        if ( v0.jdDoc.jdArgs.selJdt.treeLocOpt.toLabelOpt contains m.jdTag ) {
          // Текущий тег перетаскивается, всё ок.
          updated( v2 )
        } else {
          // Активировать текущий тег
          val fx = JdTagSelect(m.jdTag, m.jdId, silent = true).toEffectPure
          updated( v2, fx )
        }
      }


    // dragend. Нередко, он не наступает вообще. Т.е. код тут ненадёжен и срабатывает редко, почему-то.
    case JdTagDragEnd =>
      val v0 = value
      val l = _jdArgs_renderArgs_dnd_LENS
      l.get(v0)
        .jdt
        .fold(noChange) { _ =>
          val v2 = l.set( MJdDndS.empty )(v0)
          updated( v2 )
        }


    // Юзер отпустил перетаскиваемый объект на какой-то стрип. Нужно запихать этот объект в дерево нового стрипа.
    case m: JdDropToBlock =>
      val v0 = value
      val jdDoc0 = v0.jdDoc.jdArgs.data.doc

      // Получить на руки инстанс сброшенного тега.
      // Прячемся от общего scope, чтобы работать с элементом только через его tree loc.
      val dndJdt = m.foreignTag
        .orElse( v0.jdDoc.jdArgs.draggingTagLoc.toLabelOpt )
        .get

      // Найти tree loc текущего тега наиболее оптимальным путём. С некоторой вероятностью это -- selected-тег:
      val dndJdtLoc0 = v0.jdDoc.jdArgs.selJdt
        .treeLocOpt
        .filter { loc =>
          // Убедиться, что текущий selected-тег содержит dndJdt:
          dndJdt ==* loc.getLabel
        }
        .orElse {
          // Это не selected-тег. Возможны перетаскивание без выделения тега: просто взял да потащил.
          // Это нормально. Перебираем всё дерево:
          jdDoc0.template
            .loc
            .findByLabel( dndJdt )
        }
        .get

      // Подъём вверх до sub-doc jd-тега: блока, qd-blockless или чего-то ещё.
      @tailrec def __goToSubDocTag(currLoc: TreeLoc[JdTag]): TreeLoc[JdTag] = {
        currLoc.parent match {
          case Some(parentLoc) =>
            val jdt = parentLoc.getLabel
            if (jdt.name ==* MJdTagNames.DOCUMENT) {
              currLoc
            } else {
              __goToSubDocTag( parentLoc )
            }
          case None =>
            currLoc
        }
      }

      // Найти исходный strip в исходном шаблоне:
      val subDocJdTree = __goToSubDocTag( dndJdtLoc0 )
      val fromBlock = subDocJdTree.getLabel
      val isSameBlock = fromBlock ==* m.targetBlock

      // Дополнительно обработать Y-координату.
      val clXy0 = m.clXy

      // Если strip изменился, то надо пересчитать координаты относительно нового стрипа:
      val clXy2 = if (isSameBlock) {
        clXy0

      } else {
        // Перемещение между разными strip'ами. Надо пофиксить координату Y, иначе добавляемый элемент отрендерится где-то за экраном.
        val tplIndexed0 = JdUtil.mkTreeIndexed( jdDoc0 )
        val blockAndQdBlsId = EphemeralStream.toIterable(
          tplIndexed0
            .subForest
            .map( _.rootLabel )
        )
          .to( LazyList )

        val blockAndQdBls = blockAndQdBlsId.map(_._2)

        val fromStripIndex  = blockAndQdBls indexOf[JdTag] fromBlock
        val toStripIndex    = blockAndQdBls indexOf[JdTag] m.targetBlock

        val (topStrip, bottomStrip, yModSign) =
          if (fromStripIndex <= toStripIndex) (fromBlock, m.targetBlock, -1)
          else (m.targetBlock, fromBlock, +1)

        // Собрать все стрипы от [текущего до целевого), просуммировать высоту блоков, вычесть из Y
        val subDocJdtsHeights = (for {
          jdtWithId <- blockAndQdBlsId
            .iterator
            .dropWhile { m => m._2 !===* topStrip }
            .takeWhile { m => m._2 !===* bottomStrip }

          // Узнать пиксельный размер тега.
          sz <- DocEditAh.getJdtWh(jdtWithId, v0)
        } yield {
          sz.height
        })
          .to( LazyList )

        if (subDocJdtsHeights.nonEmpty) {
          val yDiff = subDocJdtsHeights.sum
          val y2 = clXy0.y + yModSign * yDiff
          //println(s"mod Y: ${clXy0.y} by $yDiff => $y2")
          MCoords2di.y.set(y2)( clXy0 )
        } else {
          // Странно: нет пройденных стрипов, хотя они должны бы быть
          logger.warn( msg = s"$clXy0 [$fromBlock => ${m.targetBlock})" )
          clXy0
        }
      }

      val jdtLabel2 = {
        val dndJdt0 = dndJdtLoc0.getLabel
        JdTag.props1
          .composeLens( MJdtProps1.topLeft )
          .set( Some(clXy2) )(dndJdt0)
      }

      val loc2 = if (isSameBlock) {
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
          .findByLabel( m.targetBlock )
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
            jdRuntime   = mkJdRuntime(jdDoc2, jdArgs0).result,
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

      val tplIndexed0 = JdUtil.mkTreeIndexed( v0.jdDoc.jdArgs.data.doc )
      val allBlocks = EphemeralStream.toIterable( tplIndexed0.subForest )

      // Сброс может быть на блок (на верхнюю или нижнюю половины блока) или в промежутки между блоков.
      val droppedNear = (for {
        // По идее, в редакторе все блоки и плитки в том числе - идут в прямом порядке.
        // Но если что-то не так, то будет логгироваться.
        (jdIdTree, gbRes) <- allBlocks
          .iterator
          .zip( v0.jdDoc.gridBuild.coords )

        jdtWithId = jdIdTree.rootLabel
        jdtWh <- DocEditAh.getJdtWh( jdtWithId, v0 )
      } yield {
        // Пока просто логгируем ошибку
        if (gbRes.gbBlock.jdId !=* jdtWithId._1)
          logger.error( ErrorMsgs.JD_TREE_UNEXPECTED_ID, msg = (gbRes.gbBlock.jdId, jdtWithId._1) )

        MJdtWithXy(
          jdt = jdIdTree,
          topLeft = gbRes.topLeft,
          wh = jdtWh,
          isDownerTl =
            (m.docXy.x >= gbRes.topLeft.x - paddingPx) &&
            (m.docXy.y >= gbRes.topLeft.y - paddingPx)
        )
      })
        // Отсеять элементы сверху.
        .dropWhile { jdtXy =>
          // Оставлять, если ниже верхнего левого угла с padding, но выше нижнего правого угла с padding.
          val isKeep = jdtXy.isDownerTl && {
            (m.docXy.x <= jdtXy.topLeft.x + jdtXy.wh.width  + paddingPx) &&
            (m.docXy.y <= jdtXy.topLeft.y + jdtXy.wh.height + paddingPx)
          }
          !isKeep
        }
        // Отсеять элементы снизу: т.е. оставить только элементы, которые ощутимо ниже нижней границе блока (кроме смежного/пересекающегося c точкой блока).
        .takeWhile( _.isDownerTl )
        .to( LazyList )

      // droppedNear содержит один или два элемента (хотя не исключены и иные ситуации).
      if (droppedNear.exists(_.jdt.rootLabel._2 ==* droppedBlockLabel)) {
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
          case LazyList(jdtXy) =>
            val innerY = m.docXy.y - jdtXy.topLeft.y
            val isUp = innerY < jdtXy.wh.height / 2
            (jdtXy.jdt, isUp)

          // Если два блока, то сброс был в щель между двумя блоками.
          case LazyList(_ /*before*/, after) =>
            (after.jdt, true)

          // Неопределённая ситуация. Переносим блок или в начало или в конец документа.
          case other =>
            logger.warn( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = (m, other.mkString(HtmlConstants.PIPE)) )
            val gbRes0 = v0.jdDoc.gridBuild.coords.head
            if (m.docXy.y > gbRes0.topLeft.y) {
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
          .findByLabel( nearJdt._2 )
          .get

        // Убрать значение topLeft, если задано. Это для qd-blockless, когда контент был вынесен за пределы блока.
        val jdt_p1_topLeft_LENS = JdTag.props1
          .composeLens( MJdtProps1.topLeft )
        val needModifyJdt = jdt_p1_topLeft_LENS
          .get(droppedBlockLabel)
          .nonEmpty
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
          MJdArgs.jdRuntime.set {
            mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
              .result
          }
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

      val edgeUids4mod = (for {
        e <- v0.jdDoc.jdArgs.data.edges.valuesIterator
        fileSrv <- e.jdEdge.fileSrv
        if m.nodeId ==* fileSrv.nodeId
        edgeUid <- e.jdEdge.edgeDoc.id
      } yield {
        edgeUid
      })
        .toSet

      if (edgeUids4mod.isEmpty) {
        // Если эджа для указанной гистограммы не найдено, то это палево какое-то.
        logger.warn( ErrorMsgs.SOURCE_FILE_NOT_FOUND, msg = m )
        noChange

      } else {
        // Найти гистограмму в карте
        (for {
          mhist <- v0.editors
            .colorsState
            .histograms
            .get(m.nodeId)
          if mhist.colors.nonEmpty
        } yield {
          // Надо пробежаться по template, и всем элеметам, которые изменились, выставить обновлённые значения.
          val topColorMcd = mhist
            .colors
            .maxBy { mcd =>
              mcd.freqPc
                .getOrElse(-1)
            }
          val topColorMcdOpt = Some(topColorMcd)

          lazy val p1_bgColor_LENS = JdTag.props1
            .composeLens( MJdtProps1.bgColor )

          val tpl2 = for {
            el1 <- v0.jdDoc.jdArgs.data.doc.template
          } yield {
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
                      MQdAttrsText.background.set(
                        topColorMcdOpt.map( SetVal.apply )
                      )(
                        qdProps0.attrsText
                          .getOrElse( MQdAttrsText.empty )
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
              MJdArgs.jdRuntime.set(
                mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
                  .result
              )
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
        })
          .getOrElse {
            // Should never happen: не найдена гистограмма, указанная в событии.
            logger.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
            noChange
          }
      }


    // Ручной ресайз контента по ширине.
    case m: SetContentWidth =>
      val v0 = value
      val loc0 = v0.jdDoc.jdArgs
        .selJdt.treeLocOpt
        .get
      val jdt0 = loc0.getLabel

      // Прогнать по min/max-ограничениям, т.к. юзер таскать можно куда угодно.
      val widthPxOpt2 = for (widthPx0 <- m.widthPx) yield {
        Math.max(
          JdConst.ContentWidth.MIN_PX,
          Math.min(JdConst.ContentWidth.MAX_PX, widthPx0)
        )
      }

      val jdt_p1_width_LENS = JdTag.props1
        .composeLens( MJdtProps1.widthPx )

      if (jdt_p1_width_LENS.get(jdt0) ==* widthPxOpt2) {
        // Ширина изменилась в исходное значение.
        noChange
      } else {
        require( jdt0.name ==* MJdTagNames.QD_CONTENT )
        // Сохранить новую ширину в состояние текущего тега:
        val jdt2 = (jdt_p1_width_LENS set widthPxOpt2)( jdt0 )
        _updateQdContentProps(jdt2, v0)
      }


    // Реакция на сигнал ресайза у embed'а.
    case m: QdEmbedResize =>
      val v0 = value
      (for {
        qdSubTree <- v0.jdDoc.jdArgs.selJdt.treeOpt
        if (qdSubTree.rootLabel.name ==* MJdTagNames.QD_CONTENT)
        qdLoc0 = qdSubTree.loc
        embedLoc <- qdLoc0.findByEdgeUid( m.edgeUid )
      } yield {
        _qdUpdateWidth(embedLoc, width = m.widthPx, heightPxOpt = m.heightPx)
      })
        .fold {
          logger.log( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT )
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
                MJdArgs.jdRuntime.set(
                  mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
                    .result
                )
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

      // TODO Для qdBlockLess надо промигрировать данные qdBlockLess (смена jdId) и
      val isQdBl = DocEditAh.isQdBlockless(loc0)
      val isUp = m.up
        .invertIf( isQdBl )

      val loc2OrNull: TreeLoc[JdTag] = if (!m.bounded) {
        // Если !m.bounded, то поменять тег местами с соседними тегами.
        // Если шаг влево и есть теги слева...
        if (!isUp && !loc0.lefts.isEmpty) {
          // Меняем местами первый левый тег с текущим
          val leftLoc = loc0.left.get
          leftLoc
            .setTree( loc0.tree )
            .right.get.setTree( leftLoc.tree )
        } else if (isUp && !loc0.rights.isEmpty) {
          val rightLoc = loc0.right.get
          rightLoc
            .setTree( loc0.tree )
            .left.get.setTree( rightLoc.tree )
        } else {
          null
        }

      } else if (!loc0.lefts.isEmpty || !loc0.rights.isEmpty) {
        // bounded: Можно и нужно двигать до края, значит надо удалить текущий элемент из treeLoc и добавить в начало/конец через parent.
        val parentLoc1 = loc0
          .delete.get
          .parent.get
        if (!isUp) parentLoc1.insertDownFirst( loc0.tree )
        else parentLoc1.insertDownLast( loc0.tree )

      } else {
        // Нет соседних элементов для движения по слоям.
        null
      }

      Option(loc2OrNull).fold(noChange) { loc2 =>
        val tpl2 = loc2.toTree
        val jdArgs0 = v0.jdDoc.jdArgs
        val jdDoc2 = MJdDoc.template
          .set(tpl2)( jdArgs0.data.doc )

        var jdArgsModF = (
          MJdArgs.data
            .composeLens( MJdDataJs.doc )
            .set(jdDoc2) andThen
          // Надо пересчитать path до перемещённого тега.
          MJdArgs.renderArgs
            .composeLens( MJdRenderArgs.selPath )
            .set( tpl2.nodeToPath( loc0.getLabel ) )
        )

        if (isQdBl) {
          jdArgsModF = jdArgsModF andThen MJdArgs.jdRuntime
            .set( mkJdRuntime(jdDoc2, jdArgs0).result )
        }
        // else - css можно не обновлять, т.к. там просто поменяется порядок стилей без видимых изменений.

        val v2 = MDocS.jdDoc
          .composeLens(MJdDocEditS.jdArgs)
          .modify( jdArgsModF )(v0)

        updated(v2)
      }


    // Замена состояния галочки широкого рендера текущего стрипа новым значением
    case m: BlockExpand =>
      val v0 = value

      val jdt_p1_bm_expandOpt_LENS = JdTag.props1
        .composeLens( MJdtProps1.expandMode )
      val jdtLoc0 = v0.jdDoc.jdArgs
        .selJdt.treeLocOpt
        .get

      val jdt0 = jdtLoc0.getLabel
      require(jdt0.name ==* MJdTagNames.STRIP)

      if ( jdt_p1_bm_expandOpt_LENS.exist(_ ==* m.expandMode)(jdt0) ) {
        noChange

      } else {
        val jdtLoc2 = jdtLoc0
          .modifyLabel( jdt_p1_bm_expandOpt_LENS set m.expandMode )
        val tpl2 = jdtLoc2.toTree
        val jdDoc2 = (MJdDoc.template set tpl2)( v0.jdDoc.jdArgs.data.doc )

        val jdArgs2 = (
          MJdArgs.data
            .composeLens(MJdDataJs.doc)
            .set( jdDoc2 ) andThen
          MJdArgs.jdRuntime.set(
            mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
              .result
          )
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
    case m: MainBlockSet =>
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
            forest = for {
              stripTree <- tpl0.subForest
            } yield {
              val lbl = stripTree.rootLabel
              // Тут может быть и qd-blockless.
              //require( lbl.name ==* MJdTagNames.STRIP, lbl.name.toString )
              val label2 = __updateLocLabel(
                label     = lbl,
                newValue  =
                  if ((lbl ===* currStrip) && (lbl.name ==* MJdTagNames.STRIP))
                    currTagNewMainValue
                  else
                    otherStripsNewMainValue
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
              MJdArgs.jdRuntime.set(
                mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
                  .result
              )
            )( v0 )

          updated(v2)
        }


    // Сигнал переключения
    case m: ShowMainStrips =>
      val v0 = value
      val hideNonMainStrips2 = m.showing

      val jd_doc_args_render_hideNonMain_LENS = MDocS.jdDoc
        .composeLens( MJdDocEditS.jdArgs )
        .composeLens( MJdArgs.renderArgs )
        .composeLens( MJdRenderArgs.hideNonMainStrips )

      if (jd_doc_args_render_hideNonMain_LENS.get(v0) ==* hideNonMainStrips2) {
        noChange

      } else {
        val v2 = jd_doc_args_render_hideNonMain_LENS
          .set( hideNonMainStrips2 )(v0)

        updated(v2)
      }


    // Добавить внеблоковый контент.
    case AddBlockLessContentClick =>
      val v0 = value

      _addContent( v0, JdTag.qd() )(
        // Определить блок или qd-bl, после которого надо добавить новый qd-bl.
        // Если None - первый элемент дерева
        (for {
          currLoc <- v0.jdDoc.jdArgs.selJdt.treeLocOpt
          rootLoc = currLoc.root
          rootLabel = rootLoc.getLabel
          parentLoc <- currLoc.findUp(_.parent.exists(_.getLabel ==* rootLabel))
        } yield {
          parentLoc.insertRight _
        })
          .getOrElse( v0.jdDoc.jdArgs.data.doc.template.loc.insertDownFirst _ )
      )


    // Реакция на клик по кнопке создания "контента", который у нас является синонимом QdTag.
    case AddContentClick =>
      val v0 = value
      val jdtName = MJdTagNames.STRIP
      val intoStripLoc = v0.jdDoc.jdArgs.selJdt.treeLocOpt
        .fold {
          // Сейчас нет выделенных тегов. Найти первый попавшийся strip
          v0.jdDoc.jdArgs.data.doc.template
            .loc
            .findByType( jdtName )
        } { selLoc =>
          // Если выбран какой-то не-strip элемент, то найти его strip. Если выделен strip, то вернуть его.
          selLoc.findUpByType( jdtName )
        }
        .get

      val qdContentTag = {
        val p1 = intoStripLoc.getLabel.props1
        val rnd = new Random()
        val coordsRnd = MCoords2di(
          x = {
            val w0 = p1.widthPx getOrElse BlockWidths.min.value
            10 + rnd.nextInt( w0 / 3 )
          },
          y = {
            val h0 = p1.heightPx getOrElse BlockHeights.min.value
            rnd.nextInt( (h0 * 0.75).toInt ) + (h0 * 0.12).toInt
          }
        )
        JdTag.qd(
          topLeft = coordsRnd,
        )
      }

      _addContent(v0, qdContentTag)( intoStripLoc.insertDownLast )


    // Клик по кнопке добавления нового стрипа.
    case AddBlockClick =>
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
            .map(_._1.rootLabel)
        }
        .iterator
        // Поискать цвет фона среди всех стрипов.
        .++ {
          EphemeralStream.toIterable(
            v0.jdDoc.jdArgs.data.doc.template
              .deepOfType( MJdTagNames.STRIP )
          )
        }
        .flatMap( _.props1.bgColor )
        // Взять первый цвет
        .nextOption()
        .orElse {
          // Если нет цвета, то использовать белый цвет.
          Some(MColorData.Examples.WHITE)
        }

      // Собрать начальный блок:
      val newStripTree = Tree.Leaf(
        JdTag.block(
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
        MJdArgs.jdRuntime.set(
          mkJdRuntime(jdDoc2, v0.jdDoc.jdArgs)
            .result
        )
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
        MJdArgs.jdRuntime.set(
          mkJdRuntime2(jdArgs0.data.doc, conf2, jdArgs0.jdRuntime)
            .result
        )
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
              blur    = Some(32)
            )
          }
        }

    // Выставление горизонтального параметра тени.
    case m: SetHorizOffTextShadow =>
      _shadowUpdating {
        Traversal
          .fromTraverse[Option, MJdShadow]
          .composeLens( MJdShadow.hOffset )
          .set( m.offset )
      }

    case m: SetVertOffTextShadow =>
      _shadowUpdating {
        Traversal
          .fromTraverse[Option, MJdShadow]
          .composeLens( MJdShadow.vOffset )
          .set( m.offset )
      }

    case m: SetBlurTextShadow =>
      _shadowUpdating {
        Traversal
          .fromTraverse[Option, MJdShadow]
          .composeLens( MJdShadow.blur )
          .set(
            OptionUtil.maybe(m.blur > 0)(m.blur)
          )
      }

  }

}


object DocEditAh {

  private def _loc2width(imgOpLoc: TreeLoc[JdTag]) = {
    for {
      qd          <- imgOpLoc.getLabel.qdProps
      attrsEmbed  <- qd.attrsEmbed
      widthSU     <- attrsEmbed.width
      width       <- widthSU.toOption
    } yield {
      width
    }
  }


  private def _jdArgs_renderArgs_dnd_LENS = {
    MDocS.jdDoc
      .composeLens( MJdDocEditS.jdArgs )
      .composeLens( MJdArgs.renderArgs )
      .composeLens( MJdRenderArgs.dnd )
  }


  def mkJdRuntime(jdDoc: MJdDoc, jdArgs: MJdArgs) =
    mkJdRuntime2(jdDoc, jdArgs.conf, jdArgs.jdRuntime)
  def mkJdRuntime2(jdDoc: MJdDoc, jdConf: MJdConf, jdRuntime0: MJdRuntime) = {
    JdUtil
      .mkRuntime( jdConf )
      .docs( jdDoc )
      .prev( jdRuntime0 )
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


  /** Для текущего выбранного qd-content-тега выставить qdBlockLess в pending. */
  def qdBlockLess2Pending(jdArgs: MJdArgs): MJdRuntime = {
    import jdArgs.{jdRuntime => jdRuntime0}

    val jdRuntime_data_qdBlockLess_LENS = MJdRuntime.data
      .composeLens( MJdRuntimeData.qdBlockLess )
    val qdBlMap0 = jdRuntime_data_qdBlockLess_LENS.get( jdRuntime0 )
    val selJdId = MJdTagId.selPathRev.set( jdArgs.renderArgs.selPath.get )( jdArgs.data.doc.tagId )
    val qdBlOptPot0 = qdBlMap0.get( selJdId )
    qdBlOptPot0
      .filter(_.isPending)
      .fold {
        // Выставить pending для текущего qd-blockless-тега, чтобы произошло повторное измерение размера.
        val pot2 = qdBlOptPot0
          .getOrElse(Pot.empty)
          .pending()
        jdRuntime_data_qdBlockLess_LENS.set( qdBlMap0 + (selJdId -> pot2) )(jdRuntime0)
      }(_ => jdRuntime0)
  }


  def getJdtWh(jdtWithId: (MJdTagId, JdTag), v0: MDocS): Option[ISize2di] = {
    val jdt = jdtWithId._2
    jdt.name match {
      case MJdTagNames.STRIP =>
        jdt.props1.wh
      case MJdTagNames.QD_CONTENT =>
        for {
          qdBlPot <- v0.jdDoc.jdArgs.jdRuntime.data.qdBlockLess.get( jdtWithId._1 )
          qdBl    <- qdBlPot.toOption
        } yield {
          qdBl.bounds
        }
      case _ =>
        None
    }
  }

}


/** Вспомогательная внутренняя модель при обработке [[io.suggest.jd.edit.m.JdDropToDocument]].
  *
  * @param jdt Jd-тег
  * @param topLeft Верхней левый угол тега в координатах плитки.
  * @param isDownerTl Находится ли верхний левый угол с паддингом выше точки клика?
  */
private case class MJdtWithXy(
                               jdt          : Tree[(MJdTagId,JdTag)],
                               topLeft      : MCoords2di,
                               wh           : ISize2di,
                               isDownerTl   : Boolean,
                             )

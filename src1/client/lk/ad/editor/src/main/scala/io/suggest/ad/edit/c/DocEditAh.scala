package io.suggest.ad.edit.c

import diode._
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.edit.MQdEditS
import io.suggest.ad.edit.m.layout.MSlideBlocks
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.color.MColorData
import io.suggest.common.MHands
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.file.MJsFileInfo
import io.suggest.i18n.MsgCodes
import io.suggest.jd.{JdConst, MJdConf, MJdEdge}
import io.suggest.jd.render.m._
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

        val v2 = MDocS.jdArgs.modify(
          MJdArgs.jdRuntime.set(
            _mkJdRuntime(tpl2, v0)
          ) andThen
          MJdArgs.template.set( tpl2 )
        )( v0 )

        updated(v2)
      }
    }

  }


  private def _mkJdRuntime(tpl: Tree[JdTag], jdConf: MJdConf): MJdRuntime =
    MJdRuntime.make(
      tpls   = tpl :: Nil,
      jdConf = jdConf,
    )
  private def _mkJdRuntime(tpl: Tree[JdTag], jdArgs: MJdArgs): MJdRuntime =
    _mkJdRuntime(tpl, jdArgs.conf)
  private def _mkJdRuntime(tpl: Tree[JdTag], mdoc: MDocS): MJdRuntime =
    _mkJdRuntime(tpl, mdoc.jdArgs)


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Набор текста в wysiwyg-редакторе.
    case m: TextChanged =>
      val v0 = value

      if ( v0.qdEdit.exists(_.initDelta ==* m.fullDelta) ) {
        // Бывают ложные срабатывания. Например, прямо при инициализации редактора. Но не факт конечно, что они тут подавляются.
        noChange

      } else if (v0.jdArgs.selJdt.treeLocOpt.isEmpty) {
        LOG.warn( WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = m.getClass.getName )
        noChange

      } else {
        // Код обновления qd-тега в шаблоне:
        def __updateTpl(qdSubTree2: Tree[JdTag]): Tree[JdTag] = {
          v0.jdArgs
            .selJdt.treeLocOpt
            .get
            .setTree( qdSubTree2 )
            .toTree
        }

        // Текст действительно изменился. Пересобрать json-document.
        //println( JSON.stringify(m.fullDelta) )
        val qdSubTree0 = v0.jdArgs.selJdt.treeOpt.get
        // Спроецировать карту сборных эджей в jd-эджи
        val edgesData0 = v0.jdArgs.edges
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

        val tpl2 = __updateTpl( qdSubTree3 )

        // Вернуть итоговую карту эджей и объединённый эффект.
        val jdArgs2 = v0.jdArgs.copy(
          template    = tpl2,
          edges       = edgesData3,
          jdRuntime   = _mkJdRuntime(tpl2, v0),
        )

        // Залить все данные в новое состояние.
        val v2 = (
          MDocS.jdArgs.set( jdArgs2 ) andThen
          MDocS.qdEdit.modify { qdEditOpt0 =>
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

        // Объединить все эффекты, если они есть.
        ah.updatedMaybeEffect(v2, uploadFxOpt)
      }


    // Изменения состояния ротации текущего jd-тега.
    case m: RotateSet =>
      val v0 = value

      (for {
        selJdtLoc0 <- v0.jdArgs.selJdt.treeLocOpt
        jdt0 = selJdtLoc0.getLabel
        if (jdt0.props1.rotateDeg !=* m.degrees) &&
          // Убедится, что значение не выходит за допустымые пределы поворота:
          m.degrees.fold(true)(deg => Math.abs(deg) <= JdConst.ROTATE_MAX_ABS)
      } yield {
        val tpl2 = selJdtLoc0
          .modifyLabel { jdt00 =>
            JdTag.props1
              .composeLens( MJdtProps1.rotateDeg )
              .set( m.degrees )( jdt00 )
          }
          .toTree

        val v2 = MDocS.jdArgs.modify(
          MJdArgs.template.set( tpl2 ) andThen
          MJdArgs.jdRuntime.set(
            _mkJdRuntime(tpl2, v0)
          )
        )( v0 )

        updated( v2 )
      })
       .getOrElse( noChange )


    // Сообщение о завершении измерения высоты внеблокового qd-контента.
    case m: QdBoundsMeasured =>
      // Нужно найти тег в MJdRuntime и решить, что же делать дальше.
      val v0 = value

      val boundsSz2 = MSize2di(
        width  = m.bounds.width.toInt,
        height = m.bounds.height.toInt,
      )

      val doc_jd_runtime_qdBlockLess_LENS = MDocS.jdArgs
        .composeLens( MJdArgs.jdRuntime )
        .composeLens( MJdRuntime.qdBlockLess )

      val qdBl0 = doc_jd_runtime_qdBlockLess_LENS.get( v0 )

      // Найти все безблоковые qd-теги в текущем документе:
      lazy val blockLessQds = v0.jdArgs.template
        .subForest
        .map(_.rootLabel)
        .filter(_.name ==* MJdTagNames.QD_CONTENT)

      // Т.к. react-measure склонна присылать bounds дважды (согласно докам), то сначала смотрим уже записанные данные.
      val boundsOpt0 = qdBl0.get( m.jdTag )
      if (boundsOpt0 contains[MSize2di] boundsSz2) {
        // Повторный сигнал размера, и этот размер не изменился. Игнорируем.
        noChange

      } else if (
        // Убедившись, что тег существует в текущем документе, пересохранить полученные размеры.
        boundsOpt0.nonEmpty ||
        (blockLessQds contains m.jdTag)
      ) {
        val qdBl2 = qdBl0 + (m.jdTag -> boundsSz2)
        val v2 = doc_jd_runtime_qdBlockLess_LENS.set( qdBl2 )(v0)

        // Нужно понять, остались ли ещё внеблоковые qd-теги, от которых ожидаются размеры.
        val hasMoreBlQdsAwaiting = blockLessQds
          .iterator
          .filter( _ !=* m.jdTag )
          .exists { jdt =>
            // Остались ли ещё внеблоковые qd-контент-теги, от которых надо дождаться размеров?
            // Остались, если есть тег (кроме текущего), которые отсутствует в jdRuntime.qdBlockLess
            (jdt !=* m.jdTag) &&
            !(qdBl0 contains jdt)
          }

        if (hasMoreBlQdsAwaiting)
          // Пере-рендер плитки не требуется, т.к. в очереди есть ещё qd-bounds-экшены, помимо этого.
          updatedSilent( v2 )
        else
          // Больше не надо дожидаться экшенов от других qd-тегов, запускаем новый рендер плитки:
          updated( v2 )

      } else {
        // Сигнал от неизвестного тега. Вероятно, был пере-рендер, а сообщение с bounds просто застряло в очереди на обработку с прошлого рендера.
        noChange
      }


    // Клик по элементу карточки.
    case m: JdTagSelect =>
      val v0 = value
      val oldSelectedTag = v0.jdArgs.selJdt.treeLocOpt.map(_.getLabel)
      if ( oldSelectedTag contains m.jdTag ) {
        // Бывают повторные щелчки по уже выбранным элементам, это нормально.
        noChange

      } else {
        val oldTagName = oldSelectedTag.map(_.name)

        // Юзер выбрал какой-то новый элемент. Залить новый тег в seleted:
        val newSelJdtTreeLoc = v0.jdArgs.template
          .loc
          .findByLabel( m.jdTag )
          .get
        val newSelJdt = newSelJdtTreeLoc.toNodePath

        var v2 = MDocS.jdArgs
          .composeLens( MJdArgs.renderArgs )
          .composeLens( MJdRenderArgs.selPath )
          .set( Some(newSelJdt) )(v0)

        // Если это QdTag, то отработать состояние quill-delta:
        v2 = if (m.jdTag.name ==* MJdTagNames.QD_CONTENT) {
          // Это qd-тег, значит нужно собрать и залить текущую дельту текста в состояние.
          // Нужно получить текущее qd-под-дерево (для сборки дельты)
          val delta2 = quillDeltaJsUtil.qdTag2delta(
            qd    = newSelJdtTreeLoc.tree,
            edges = v2.jdArgs.edges
          )
          //println( "selJdt\n" + JSON.stringify(delta2) )
          (
            MDocS.qdEdit.set(
              Some(
                MQdEditS(
                  initDelta = delta2
                )
              )
            ) andThen
              MDocS.slideBlocks
                .composeLens( MSlideBlocks.expanded )
                .set( Some(SlideBlockKeys.CONTENT) )
          )(v2)

        } else {
          // Очистить состояние от дельты.
          v2.withOutQdEdit
        }

        // Если это strip, то активировать состояние strip-редактора.
        m.jdTag.name match {
          // Переключение на новый стрип. Инициализировать состояние stripEd:
          case n @ MJdTagNames.STRIP =>
            val s2 = MStripEdS(
              isLastStrip = {
                val hasManyStrips = v2.jdArgs.template
                  .deepOfTypeIter( n )
                  // Оптимизация: НЕ проходим весь strip-итератор, а считаем только первые два стрипа.
                  .slice(0, 2)
                  .size > 1
                !hasManyStrips
              }
            )
            v2 = MDocS.stripEd.set( Some(s2) )(v2)

            // Если тип текущего тега изменился, то сбросить текущий slide-блок.
            if ( !oldTagName.contains(n) ) {
              v2 = MDocS.slideBlocks
                .composeLens( MSlideBlocks.expanded )
                .set( Some(SlideBlockKeys.BLOCK_BG) )(v2)
            }

          // Это не strip, обнулить состояние stripEd, если оно существует:
          case _ =>
            v2 = v2.withOutStripEd
        }

        // Может быть, был какой-то qd-tag и весь текст теперь в нём удалён? Удалить, если старый тег, если осталась дельта
        for {
          jdtTree <- v0.jdArgs.selJdt.treeOpt
          jdt = jdtTree.rootLabel
          dataEdges0 = v0.jdArgs.edges
          if (jdt.name ==* MJdTagNames.QD_CONTENT) &&
            QdJsUtil.isEmpty(jdtTree, dataEdges0) &&
            v2.jdArgs.template.contains(jdt)
        } {
          val tpl1 = v2.jdArgs.template
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
          v2 = MDocS.jdArgs.set(
            v2.jdArgs.copy(
              template    = tpl2,
              edges       = dataEdges2,
              jdRuntime   = _mkJdRuntime(tpl2, v2)
            )
          )(v2)
        }

        // Обновить список color-preset'ов.
        val bgColorsAppend = for {
          // Закинуть цвет фона нового тега в самое начало списка презетов. Затем - окончательный фон предыдущего тега.
          jdt <- m.jdTag :: v0.jdArgs.selJdt.treeLocOpt.map(_.getLabel).toList
          bgColor <- jdt.props1.bgColor
          if !v2.colorsState.colorPresets.contains(bgColor)
        } yield {
          bgColor
        }
        if (bgColorsAppend.nonEmpty) {
          val presets2 = bgColorsAppend.foldLeft(v2.colorsState.colorPresets) { MColorsState.prependPresets }
          // то закинуть его цвет фона в color-презеты.
          v2 = MDocS.colorsState
            .composeLens( MColorsState.colorPresets )
            .set( presets2 )(v2)
        }

        updated( v2 )
      }


    // Завершена фоновая конвертация base64-URL в Blob.
    case m: B64toBlobDone =>
      val v0 = value
      val dataEdgesMap0 = v0.jdArgs.edges

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
        val dataEdgesMap2 = JdTag.purgeUnusedEdges(v0.jdArgs.template, dataEdgesMap1)
        val v2 = MDocS.jdArgs
          .composeLens( MJdArgs.edges )
          .set( dataEdgesMap2 )(v0)

        // Запустить эффект хэширования и дальнейшей закачки файла на сервер.
        val hashFx = FileHashStart(dataEdge2.id, blobUrl).toEffectPure
        updated(v2, hashFx)
      }


    // Поступила команда на проведение чистки карты эджей.
    case PurgeUnusedEdges =>
      val v0 = value
      val edges0 = v0.jdArgs.edges
      val edges2 = JdTag.purgeUnusedEdges( v0.jdArgs.template, edges0 )
      if ( edges0.size ==* edges2.size ) {
        noChange
      } else {
        val v2 = MDocS.jdArgs
          .composeLens( MJdArgs.edges )
          .set( edges2 )(v0)
        updated(v2)
      }


    // Клик по кнопкам управления размером текущего блока
    case m: BlockSizeBtnClick =>
      val v0 = value

      val stripTreeLoc0 = v0.jdArgs.selJdt.treeLocOpt.get
      val strip0 = stripTreeLoc0.getLabel

      val strip2 = JdTag.props1
        .composeLens( MJdtProps1.bm )
        .modify { bmOpt0 =>
          val bm0 = bmOpt0.get
          val bm2 = (m.model match {
            case bhs @ BlockHeights =>
              val sz0 = bm0.h
              val szOpt2 = m.direction match {
                case MHands.Left  => bhs.previousOf( sz0 )
                case MHands.Right => bhs.nextOf( sz0 )
              }
              val sz2 = szOpt2.get
              BlockMeta.h.set( sz2 )

            case bws @ BlockWidths =>
              val sz0 = bm0.w
              val szOpt2 = m.direction match {
                case MHands.Left  => bws.previousOf( sz0 )
                case MHands.Right => bws.nextOf( sz0 )
              }
              val sz2 = szOpt2.get
              BlockMeta.w.set( sz2 )
          })(bm0)
          Some(bm2)
        }(strip0)

      val template2 = stripTreeLoc0
        .setLabel(strip2)
        .toTree

      // Обновить и дерево, и currentTag новым инстансом.
      val v2 = MDocS.jdArgs.modify(
        MJdArgs.template.set( template2 ) andThen
        MJdArgs.jdRuntime.set( _mkJdRuntime(template2, v0) )
      )(v0)

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
          val v2 = MDocS.stripEd
            .composeTraversal( Traversal.fromTraverse[Option, MStripEdS] )
            .composeLens( MStripEdS.confirmingDelete )
            .set(true)(v0)
          updated(v2)
        }

      } else {
        // Второй шаг удаления, и юзер подтвердил удаление.
        val strip4delLoc = v0.jdArgs
          .selJdt.treeLocOpt
          .get

        val tpl0 = v0.jdArgs.template
        val tpl2 = strip4delLoc
          .delete
          .fold(tpl0) { _.toTree }

        if (tpl2.subForest.nonEmpty) {
          val v2 = v0.copy(
            jdArgs = v0.jdArgs.copy(
              template    = tpl2,
              jdRuntime   = _mkJdRuntime(tpl2, v0),
              renderArgs  = MJdRenderArgs.selPath.set(None)( v0.jdArgs.renderArgs ),
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
        val s2 = MStripEdS.confirmingDelete.set(false)(stripEdS0)
        val v2 = MDocS.stripEd.set( Some(s2) )(v0)
        updated(v2)
      } else {
        // Какой-то левый экшен пришёл. Возможно, просто дублирующийся.
        noChange
      }


    // Началось перетаскивание какого-то jd-тега из текущего документа.
    case m: JdTagDragStart =>
      val v0 = value
      val dnd0Jdt = v0.jdArgs.draggingTagLoc
      if (dnd0Jdt.toLabelOpt contains m.jdTag) {
        noChange
      } else {
        val v2 = _jdArgs_renderArgs_dnd_jdt_LENS
          .set( v0.jdArgs.template.nodeToPath(m.jdTag) )(v0)

        // Если запускается перетаскивание тега, который не является текущим, то надо "выбрать" таскаемый тег.
        if ( v0.jdArgs.selJdt.treeLocOpt.toLabelOpt contains m.jdTag ) {
          // Текущий тег перетаскивается, всё ок.
          updated( v2 )
        } else {
          // Активировать текущий тег
          val fx = JdTagSelect(m.jdTag).toEffectPure
          updated( v2, fx )
        }
      }


    // dragend. Нередко, он не наступает вообще. Т.е. код тут ненадёжен и срабатывает редко, почему-то.
    case _: JdTagDragEnd =>
      val v0 = value
      v0.jdArgs.renderArgs.dnd.jdt.fold(noChange) { _ =>
        val v2 = _jdArgs_renderArgs_dnd_jdt_LENS.set( None )(v0)
        updated( v2 )
      }


    // Юзер отпустил перетаскиваемый объект на какой-то стрип. Нужно запихать этот объект в дерево нового стрипа.
    case m: JdDropContent =>
      val v0 = value
      val tpl0 = v0.jdArgs.template

      // Найти tree loc текущего тега наиболее оптимальным путём. С некоторой вероятностью это -- selected-тег:
      val dndJdtLoc0 = {
        // Получить на руки инстанс сброшенного тега.
        // Прячемся от общего scope, чтобы работать с элементом только через его tree loc.
        val dndJdt = m.foreignTag
          .orElse( v0.jdArgs.draggingTagLoc.toLabelOpt )
          .get

        v0.jdArgs.selJdt.treeLocOpt
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

      // Пересобрать данные для рендера.
      val v2 = MDocS.jdArgs.modify { jdArgs0 =>
        jdArgs0.copy(
          template    = tpl2,
          jdRuntime   = _mkJdRuntime(tpl2, jdArgs0),
          renderArgs  = (
            MJdRenderArgs.selPath.set( tpl2.nodeToPath( loc2.getLabel ) ) andThen
            MJdRenderArgs.dnd.set( MJdDndS.empty )
          )(jdArgs0.renderArgs)
        )
      }(v0)

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
        val v2 = MDocS.jdArgs.modify(
          MJdArgs.template.set( tpl2 ) andThen
          MJdArgs.renderArgs.modify(
            _.copy(
              selPath = tpl2.nodeToPath( droppedStripLabel ),
              dnd     = MJdDndS.empty,
            )
          )
        )(v0)

        updated(v2)
      }


    // Появилась новая гистограмма в карте гисторамм. Нужно поправить эджи, у которых фон соответствует связанной картинке.
    case m: HandleNewHistogramInstalled =>
      val v0 = value
      val edgeUids4mod = v0.jdArgs
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

            lazy val p1_bgColor_LENS = JdTag.props1
              .composeLens( MJdtProps1.bgColor )

            val tpl2 = v0.jdArgs.template
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

            // Сохранить новые темплейт в состояние.
            val jdArgs2 = (
              MJdArgs.template.set(tpl2) andThen
              MJdArgs.jdRuntime.set( _mkJdRuntime(tpl2, v0.jdArgs) )
            )( v0.jdArgs )

            var v2 = MDocS.jdArgs.set( jdArgs2 )(v0)

            // Надо заставить перерендерить quill, если он изменился и открыт сейчас:
            for {
              qdEdit0     <- v0.qdEdit
              qdTag2      <- jdArgs2.selJdt.treeOpt
              if qdTag2.rootLabel.name ==* MJdTagNames.QD_CONTENT
              // Перерендеривать quill только если изменение гистограммы коснулось эджа внутри текущего qd-тега:
              qdTag0      <- v0.jdArgs.selJdt.treeOpt
              if qdTag0 !=* qdTag2
            } {
              v2 = MDocS.qdEdit.set( Some(
                qdEdit0.withInitRealDelta(
                  initDelta = quillDeltaJsUtil.qdTag2delta( qdTag2, v2.jdArgs.edges )
                )
              ))(v0)
            }

            updated( v2 )
          }
      }


    // Ручной ресайз контента (по ширине).
    case m: CurrContentResize =>
      val v0 = value
      val tpl2 = v0.jdArgs
        .selJdt.treeLocOpt
        .get
        .modifyLabel { jdTag0 =>
          require( jdTag0.name ==* MJdTagNames.QD_CONTENT )
          // Сохранить новую ширину в состояние текущего тега:
          JdTag.props1
            .composeLens( MJdtProps1.widthPx )
            .set( Some(m.widthPx) )( jdTag0 )
        }
        .toTree

      val v2 = MDocS.jdArgs.modify(
        MJdArgs.template.set( tpl2 ) andThen
        MJdArgs.jdRuntime.set( _mkJdRuntime(tpl2, v0) )
      )(v0)

      updated(v2)


    // Реакция на сигнал ресайза у embed'а.
    case m: QdEmbedResize =>
      val v0 = value
      v0.jdArgs
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
          val tpl2 = v0.jdArgs.selJdt.treeLocOpt
            .get
            .setTree(qdSubTree2)
            .toTree

          val v2 = (
            MDocS.jdArgs.modify(
              MJdArgs.template.set(tpl2) andThen
              MJdArgs.jdRuntime.set( _mkJdRuntime(tpl2, v0.jdArgs.conf) )
            ) andThen
            MDocS.qdEdit.modify { qdEditOpt0 =>
              for (qdEdit0 <- qdEditOpt0) yield {
                qdEdit0.withInitRealDelta(
                  initDelta = quillDeltaJsUtil.qdTag2delta( qdSubTreeLoc2.root.tree, v0.jdArgs.edges )
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
      val loc0 = v0.jdArgs.selJdt.treeLocOpt.get

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

        val v2 = MDocS.jdArgs.modify(
          MJdArgs.template.set(tpl2) andThen
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
      val jdtLoc0 = v0.jdArgs
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

        val v2 = MDocS.jdArgs.modify(
          MJdArgs.template.set( tpl2 ) andThen
          MJdArgs.jdRuntime.set( _mkJdRuntime(tpl2, v0) )
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

      v0.jdArgs.selJdt.treeLocOpt
        .filter(_.getLabel.name ==* MJdTagNames.STRIP)
        .fold(noChange) { currStripLoc =>
          // Вычислить новые значения для этого и всех соседних элементов.
          val currTagNewMainValue = OptionUtil.maybeTrue( m.isMain )
          // Надо обновить все соседние инстансы новым анти-значением:
          val otherStripsNewMainValue = Option.empty[Boolean]

          val currStrip = currStripLoc.getLabel

          val tpl0 = v0.jdArgs.template

          // Собрать корневой элемент с обновлённым стрипами:
          val tpl2 = Tree.Node(
            root = tpl0.rootLabel,
            forest = for (stripTree <- v0.jdArgs.template.subForest) yield {
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

          val v2 = MDocS.jdArgs.modify(
            MJdArgs.template.set(tpl2) andThen
            MJdArgs.jdRuntime.set( _mkJdRuntime(tpl2, v0) )
          )( v0 )

          updated(v2)
        }


    // Сигнал переключения
    case m: ShowMainStrips =>
      val v0 = value
      val hideNonMainStrips2 = m.showing
      if (v0.jdArgs.renderArgs.hideNonMainStrips ==* hideNonMainStrips2) {
        noChange
      } else {
        val v2 = MDocS.jdArgs
          .composeLens( MJdArgs.renderArgs )
          .composeLens( MJdRenderArgs.hideNonMainStrips )
          .set( hideNonMainStrips2 )(v0)

        updated(v2)
      }


    // Реакция на клик по кнопке создания "контента", который у нас является синонимом QdTag.
    case AddContentClick =>
      val v0 = value
      val stripName = MJdTagNames.STRIP
      val intoStripLoc = v0.jdArgs.selJdt.treeLocOpt
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


      val textL10ed = Messages( MsgCodes.`Example.text` )

      val textPred = MPredicates.JdContent.Text
      val edgesMap0 = v0.jdArgs.edges
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

      val v2 = v0.copy(
        jdArgs = v0.jdArgs.copy(
          template    = tpl2,
          edges       = edgesMap2,
          jdRuntime   = _mkJdRuntime(tpl2, v0),
          renderArgs  = MJdRenderArgs.selPath.set(
            tpl2.nodeToPath( qdtTree.rootLabel )
          )(v0.jdArgs.renderArgs),
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
        slideBlocks = MSlideBlocks.expanded.set( Some(SlideBlockKeys.CONTENT) )(v0.slideBlocks),
      )
      updated(v2)


    // Клик по кнопке добавления нового стрипа.
    case AddStripClick =>
      val v0 = value

      val currStripLocOpt = v0.jdArgs.selJdt.treeLocOpt
        .flatMap {
          _.findUpByType( MJdTagNames.STRIP )
        }

      // Взять цвет фона с текущего стрипа.
      val bgColorSome0 = currStripLocOpt
        .map(_.getLabel)
        .orElse {
          // Попытаться взять цвет из главного блока или любого первого попавшегося блока с цветом фона.
          v0.jdArgs.template
            .getMainBlock
            .map(_.rootLabel)
        }
        .toIterator
        // Поискать цвет фона среди всех стрипов.
        .++ {
          v0.jdArgs.template.deepOfTypeIter( MJdTagNames.STRIP )
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
        v0.jdArgs.template
          .loc
          .insertDownLast( newStripTree )
      } { currStripLoc =>
        currStripLoc.insertRight( newStripTree )
      }

      val tpl2 = newStripLoc.toTree

      val v2 = (
        MDocS.jdArgs.modify { jdArgs0 =>
          jdArgs0.copy(
            template    = tpl2,
            renderArgs  = MJdRenderArgs.selPath
              .set( tpl2.nodeToPath( newStripTree.rootLabel ) )(jdArgs0.renderArgs),
            jdRuntime   = _mkJdRuntime(tpl2, jdArgs0),
          )
        } andThen
        MDocS.slideBlocks
          .composeLens( MSlideBlocks.expanded )
          .set( Some(SlideBlockKeys.BLOCK) )
      )(v0)

      updated(v2)


    // Выставить новый масштаб для рендера карточи.
    case m: SetScale =>
      val v0 = value
      val conf2 = MJdConf.szMult
        .set(m.szMult)( v0.jdArgs.conf )

      val v2 = MDocS.jdArgs.modify(
        MJdArgs.conf.set( conf2 ) andThen
        MJdArgs.jdRuntime.set( _mkJdRuntime(v0.jdArgs.template, conf2) )
      )(v0)

      updated(v2)


    // Включение-выключение тени для текста/контента
    case m: SetTextShadowEnabled =>
      val v0 = value
      v0.jdArgs.selJdt
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
      v0.jdArgs.selJdt
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
      v0.jdArgs.selJdt
        .treeLocOpt
        .shadowUpdated(v0) { shadOpt0 =>
          for (shad0 <- shadOpt0) yield {
            shad0.withVOffset( m.offset )
          }
        }

    case m: SetBlurTextShadow =>
      val v0 = value
      v0.jdArgs.selJdt
        .treeLocOpt
        .shadowUpdated(v0) { shadOpt0 =>
          for (shad0 <- shadOpt0) yield {
            shad0.withBlur(
              OptionUtil.maybe(m.blur > 0)(m.blur)
            )
          }
        }

  }


  private def _jdArgs_renderArgs_dnd_jdt_LENS = {
    MDocS.jdArgs
      .composeLens( MJdArgs.renderArgs )
      .composeLens( MJdRenderArgs.dnd )
      .composeLens( MJdDndS.jdt )
  }

}

package io.suggest.ad.edit.c

import com.github.dominictobias.react.image.crop.PercentCrop
import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.pic.MPictureAh
import io.suggest.ad.edit.m.pop.MPictureCropPopup
import io.suggest.file.MJsFileInfo
import io.suggest.i18n.{MMessage, MsgCodes}
import io.suggest.img.MImgEdgeWithOps
import io.suggest.img.crop.MCrop
import io.suggest.jd.{MJdEditEdge, MJdEdgeId}
import io.suggest.jd.render.m.SetImgWh
import io.suggest.lk.m.MErrorPopupS
import io.suggest.model.n2.edge.{EdgesUtil, MPredicates}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.MimeConst
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import org.scalajs.dom.raw.URL
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 19:03
  * Description: Контроллер управления картинками.
  */
class PictureAh[M]( modelRW: ModelRW[M, MPictureAh] )
  extends ActionHandler(modelRW)
  with Log
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Выставлен файл в input'е заливки картинки.
    // 1. Отрендерить его на экране (т.е. сохранить в состоянии в виде блоба).
    // 2. Запустить фоновую закачку файла на сервер.
    case m: PictureFileChanged =>
      val v0 = value
      val selJdt0 = v0.selectedTag.get

      // Посмотреть, что пришло в сообщении...
      if (m.files.isEmpty) {
        // Файл удалён, т.е. список файлов изменился в []. Удалить bgImg и сопутствующий edgeUid.
        selJdt0.props1.bgImg.fold( noChange ) { bgImgOld =>
          val edgeUidOld = bgImgOld.imgEdge.edgeUid
          val edges2 = v0.edges.filterKeys { _ !=* edgeUidOld }
          val selJdt2 = selJdt0.withProps1(
            selJdt0.props1.withBgImg( None )
          )
          val v2 = v0.copy(
            edges       = edges2,
            selectedTag = Some( selJdt2 )
          )
          updated( v2 )
        }

      } else {

        val errMsg = MMessage( MsgCodes.`File.is.not.a.picture` )

        // Выставлен новый файл. Надо записать его в состояние.
        val v9 = m.files
          .find { fileNew =>
            MimeConst.Image.isImageForAd(fileNew.`type`)
          }
          .fold [MPictureAh] {
            // Не найдено картинок среди новых файлов.
            v0.withErrorPopup(
              Some(MErrorPopupS(
                messages = errMsg :: Nil
              ))
            )

          } { fileNew =>
            // Найти в состоянии текущий файл, если он там есть.
            val bgImgOldOpt = selJdt0.props1.bgImg
            val edgeUidOldOpt = bgImgOldOpt.map(_.imgEdge.edgeUid)
            val dataEdgeOldOpt = edgeUidOldOpt.flatMap( v0.edges.get )

            // Вычислить новый edgeUid.
            val edgeUid2 = edgeUidOldOpt.getOrElse {
              EdgesUtil.nextEdgeUidFromMap( v0.edges )
            }

            val blobUrlNew = URL.createObjectURL( fileNew )

            // Записать текущий файл в состояние.
            val dataEdge2 = MEdgeDataJs(
              jdEdge = MJdEditEdge(
                predicate   = MPredicates.Bg,
                id          = edgeUid2,
                url         = Some( blobUrlNew )
              ),
              fileJs = Some(MJsFileInfo(
                blob      = fileNew,
                blobUrl   = Option( blobUrlNew ),
                fileName  = Option( fileNew.name )
              ))
              // TODO Запустить upload XHR, и выставить 0% upload progress
            )
            val selJdt2 = selJdt0.withProps1(
              selJdt0.props1.withBgImg( Some(
                MImgEdgeWithOps(
                  imgEdge = MJdEdgeId( edgeUid2 ),
                )
              ))
            )

            // Собрать обновлённое состояние.
            val v1 = v0.copy(
              edges       = v0.edges.updated(edgeUid2, dataEdge2),
              selectedTag = Some(selJdt2)
            )

            // Если есть старый файл...
            for (dataEdgeOld <- dataEdgeOldOpt; fileJsOld <- dataEdgeOld.fileJs) {
              // Закрыть старый blobURL в фоне, после пере-рендера.
              for (blobUrlOld <- fileJsOld.blobUrl) {
                Future {
                  URL.revokeObjectURL( blobUrlOld )
                }
              }

              // Старый файл надо закрыть. В фоне, чтобы избежать теоретически-возможных
              Future {
                fileJsOld.blob.close()
              }

              // Прервать upload файла на сервер, есть возможно.
              for (upXhrOld <- fileJsOld.uploadXhr) {
                try {
                  upXhrOld.abort()
                } catch { case ex: Throwable =>
                  LOG.warn(ErrorMsgs.XHR_UNEXPECTED_RESP, ex, dataEdgeOld)
                }
              }
            }

            v1
          }

        updated(v9)
      }


    // Загрузилась картинка, и стали известны некоторые параметры этой самой картинки.
    case m: SetImgWh =>
      // Сохранить в состояние ширину и длину.
      val v0 = value
      val e0 = v0.edges(m.edgeUid)
      val f0 = e0.fileJs.get
      val e2 = e0.withFileJs(
        Some(f0.withWhPx(
          Some(m.wh)
        ))
      )
      val v2 = v0.withEdges(
        v0.edges.updated(m.edgeUid, e2)
      )

      // TODO Следует уменьшить в размерах большую картинку, к которой относилось это сообщение внутри qd.
      /*
      val upV2 = updated(v2)
      v2.selectedTag.fold(upV2) { selJdt =>
        val qdOps = for (qdOp <- selJdt.props1.qdOps) yield {
          var isChanged = false
          if (qdOp.edgeInfo.exists(_.edgeUid ==* m.edgeUid) && qdOp.attrsEmbed.fold(true)(_.isEmpty) ) {
            // Эта qd op нуждается в выставлении wh
            isChanged = true
            // TODO Пока заборшено из-за трудностей определения корректных размеров картинки на основе каких-то других параметров.
            ???
          } else {
            // Эта quill-delta op не нуждается в апдейте.
            qdOp
          }
        }
      }
      */

      // Вернуть итог.
      updated(v2)


    // Клик по кнопке открытия попапа для кропа.
    case CropOpen =>
      val v0 = value
      val selJdt = v0.selectedTag.get
      val bgImg = selJdt.props1.bgImg.get
      val edge = v0.edges( bgImg.imgEdge.edgeUid )
      val bm = selJdt.props1.bm.get

      val aspectRatio = bm.width.toDouble / bm.height.toDouble

      // Попытаемся восстановить %crop на основе кропа из состояния:
      val existingPcCrop = for {
        crop    <- bgImg.crop
        fileJs  <- edge.fileJs
        origWh  <- fileJs.whPx
      } yield {
        val c100 = 100d
        val heightPc = crop.height / origWh.height.toDouble * c100
        val xPc = crop.offX / origWh.width.toDouble * c100
        val yPc = crop.offY / origWh.height.toDouble * c100
        (heightPc, xPc, yPc)
      }

      println( existingPcCrop )

      val (heightPc, xPc, yPc) = existingPcCrop.getOrElse {
        // Нет кропа - в дефолт
        (100d, 0d, 0d)
      }

      val cropPc = new PercentCrop {
        override val aspect = aspectRatio
        override val height = heightPc
        override val x      = xPc
        override val y      = yPc
      }

      val v2 = v0.withCropPopup( Some(
        MPictureCropPopup(
          imgEdgeUid  = edge.id,
          percentCrop = cropPc,
        )
      ))
      updated( v2 )


    // Выставлен новый кроп для картинки.
    case m: CropChanged =>
      val v0 = value
      val cropPopup0 = v0.cropPopup.get
      val cropPopup2 = cropPopup0.copy(
        percentCrop = m.percentCrop,
        pixelCrop   = Some( m.pixelCrop )
      )
      val v2 = v0.withCropPopup( Some(cropPopup2) )
      updated(v2)


    // Сохранение выбранного кропа для картинки.
    case CropSave =>
      val v0 = value
      val cropPopup0 = v0.cropPopup.get
      val selJdt0 = v0.selectedTag.get
      val iEdgeUid = cropPopup0.imgEdgeUid
      val iEdge = v0.edges( iEdgeUid )

      val pcCrop = cropPopup0.percentCrop
      val origWhOpt = iEdge.fileJs.flatMap(_.whPx)

      // Вычисляем MCrop в пикселях. TODO Переехать полностью на % crop вместо пикселей?
      val pixelCrop = cropPopup0.pixelCrop.fold {
        val origWh = origWhOpt.get
        // Попытаться перемножить percent crop и image wh
        MCrop(
          width  = _imult2(origWh.width,  pcCrop.width),
          height = _imult2(origWh.height, pcCrop.height),
          offX   = _imult3(origWh.width,  pcCrop.x, 0),
          offY   = _imult3(origWh.height, pcCrop.y, 0)
        )
      } { pxCrop =>
        MCrop(
          width  = pxCrop.width.getOrElse( origWhOpt.get.width ),
          height = pxCrop.height.getOrElse( origWhOpt.get.height ),
          offX   = _orZero( pxCrop.x ),
          offY   = _orZero( pxCrop.y )
        )
      }

      // Сохранить в текущий тег параметры кропа.
      val bgImg2 = for (bgImg0 <- selJdt0.props1.bgImg) yield {
        bgImg0
          .withCrop( Some( pixelCrop ) )
      }
      // TODO Добавить поддержку выставления кропа в qdTag. Но тут нужно сам quill ещё промодифицировать.

      // Сохранить новый кроп в состояние.
      val v2 = v0
        .withCropPopup( None )
        .withSelectedTag(Some(
          selJdt0.withProps1(
            selJdt0.props1.withBgImg(
              bgImg2
            )
          )
        ))
      updated( v2 )


    // Отмена кропа
    case CropCancel =>
      val v0 = value
      v0.cropPopup.fold(noChange) { _ =>
        val v2 = v0.withCropPopup( None )
        updated(v2)
      }

  }


  // int mult с процентами.
  private def _imult3(a: Int, b: UndefOr[Double], dflt: Int)  : Int = b.fold(dflt)(x => (a * x / 100).toInt)
  private def _imult2(a: Int, b: UndefOr[Double])             : Int = _imult3(a, b, a)
  private def _orZero(a: js.UndefOr[Int])                     : Int = a.getOrElse(0)

}

package io.suggest.ad.edit.c

import com.github.dominictobias.react.image.crop.PercentCrop
import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m.{CropCancel, CropChanged, CropOpen, PictureFileChanged}
import io.suggest.ad.edit.m.edit.pic.MPictureAh
import io.suggest.ad.edit.m.pop.MPictureCropPopup
import io.suggest.file.MJsFileInfo
import io.suggest.i18n.{MMessage, MsgCodes}
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.qd.MQdEdgeInfo
import io.suggest.lk.m.MErrorPopupS
import io.suggest.model.n2.edge.{EdgeUid_t, EdgesUtil, MPredicates}
import io.suggest.pick.MimeConst
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import org.scalajs.dom.raw.URL
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.concurrent.Future

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
          val edgeUidOld = bgImgOld.edgeUid
          val deleteKeyF = { k: EdgeUid_t => k !=* edgeUidOld }
          val edges2 = v0.edges.filterKeys( deleteKeyF )
          val files2 = v0.files.filterKeys( deleteKeyF )
          val selJdt2 = selJdt0.withProps1(
            selJdt0.props1.withBgImg( None )
          )
          val v2 = v0.copy(
            files       = files2,
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
            val edgeUidOldOpt = bgImgOldOpt.map(_.edgeUid)
            val fileOldOpt = edgeUidOldOpt.flatMap( v0.files.get )

            // Вычислить новый edgeUid.
            val edgeUid2 = edgeUidOldOpt.getOrElse {
              EdgesUtil.nextEdgeUidFromMap( v0.edges )
            }

            val blobUrl = URL.createObjectURL( fileNew )

            // Записать текущий файл в состояние.
            val fileInfo2 = MJsFileInfo(
              file    = Option( fileNew ),
              blobUrl = Option( blobUrl )
            )
            val edge2 = MJdEditEdge(
              predicate   = MPredicates.Bg,
              id          = edgeUid2,
              url         = Some( blobUrl )
              // TODO Запустить upload XHR, и выставить 0% upload progress
            )
            val edgeInfo2 = MQdEdgeInfo( edgeUid2 )
            val selJdt2 = selJdt0.withProps1(
              selJdt0.props1.withBgImg( Some(edgeInfo2) )
            )

            // Собрать обновлённое состояние.
            val v1 = v0.copy(
              files       = v0.files + (edgeUid2 -> fileInfo2),
              edges       = v0.edges + (edgeUid2 -> edge2),
              selectedTag = Some(selJdt2)
            )

            // Если есть старый файл...
            for (fileOld <- fileOldOpt) {
              // Закрыть старый blobURL в фоне, после пере-рендера.
              for (blobUrl <- fileOld.blobUrl) {
                Future {
                  URL.revokeObjectURL( blobUrl )
                }
              }

              // Старый файл надо закрыть. В фоне, чтобы избежать теоретически-возможных
              for (_ <- fileOld.blobUrl; file <- fileOld.file) {
                Future {
                  file.close()
                }
              }

              // Прервать upload файла на сервер, есть возможно.
              for (upXhr <- fileOld.uploadXhr) {
                try {
                  upXhr.abort()
                } catch { case ex: Throwable =>
                  LOG.warn(ErrorMsgs.XHR_UNEXPECTED_RESP, ex, fileOld)
                }
              }
            }

            v1
          }

        updated(v9)
      }


    // Клик по кнопке открытия попапа для кропа.
    case CropOpen =>
      val v0 = value
      val selJdt = v0.selectedTag.get
      val bgImg = selJdt.props1.bgImg.get
      val edge = v0.edges( bgImg.edgeUid )
      val imgSrc = edge.url.getOrElse( edge.text.get )
      val bm = selJdt.props1.bm.get

      val percentCrop = new PercentCrop {
        override val aspect = bm.width.toDouble / bm.height.toDouble
        override val height = 100
        override val x = 0
        override val y = 0
      }

      val v2 = v0.withCropPopup( Some(
        MPictureCropPopup(
          imgSrc      = imgSrc,
          percentCrop = percentCrop,
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


    // Отмена кропа
    case CropCancel =>
      val v0 = value
      v0.cropPopup.fold(noChange) { _ =>
        val v2 = v0.withCropPopup( None )
        updated(v2)
      }

  }

}

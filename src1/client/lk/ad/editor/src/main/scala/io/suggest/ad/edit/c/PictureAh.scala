package io.suggest.ad.edit.c

import com.github.dominictobias.react.image.crop.PercentCrop
import diode._
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.pic.MPictureAh
import io.suggest.ad.edit.m.pop.MPictureCropPopup
import io.suggest.ad.edit.srv.IAdEditSrvApi
import io.suggest.common.geom.d2.ISize2di
import io.suggest.crypto.asm.HashWwTask
import io.suggest.crypto.hash.{HashesHex, MHashes}
import io.suggest.file.MJsFileInfo
import io.suggest.file.up.{MFile4UpProps, MFileUploadS, MUploadUrlData}
import io.suggest.i18n.{MMessage, MsgCodes}
import io.suggest.img.MImgEdgeWithOps
import io.suggest.img.crop.MCrop
import io.suggest.jd.{MJdEdgeId, MJdEditEdge}
import io.suggest.jd.render.m.SetImgWh
import io.suggest.js.UploadConstants
import io.suggest.lk.m.MErrorPopupS
import io.suggest.model.n2.edge.{EdgeUid_t, EdgesUtil, MPredicates}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.pick.MimeConst
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import org.scalajs.dom.raw.URL
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.up.IUploadApi
import io.suggest.ww._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 19:03
  * Description: Контроллер управления картинками.
  */
class PictureAh[M](
                    api         : IAdEditSrvApi,
                    uploadApi   : IUploadApi,
                    modelRW     : ModelRW[M, MPictureAh]
                  )
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
        // Файл удалён, т.е. список файлов изменился в []. Удалить bgImg и сопутствующий edgeUid, если файл более никому не нужен.
        selJdt0.props1
          .bgImg
          .fold( noChange ) { _ =>
            // Не чистим эджи, пусть другие контроллеры проконтроллируют карту эджей на предмет ненужных эджей.
            // Это нужно, чтобы избежать удаления файла, который используется в каком-то другом теге.
            val selJdt2 = selJdt0.withProps1(
              selJdt0.props1.withBgImg( None )
            )
            val v2 = v0.withSelectedTag(
              selectedTag = Some( selJdt2 )
            )
            // Отправить в очередь задачу по зачистке карты эджей:
            val fx = Effect.action(PurgeUnusedEdges)
            updated( v2, fx )
          }

      } else {

        val errMsg = MMessage( MsgCodes.`File.is.not.a.picture` )

        // Выставлен новый файл. Надо записать его в состояние.
        val (v9, fxOpt9) = m.files
          .find { fileNew =>
            MimeConst.Image.isImageForAd(fileNew.`type`)
          }
          .fold [(MPictureAh, Option[Effect])] {
            // Не найдено картинок среди новых файлов.
            val v1 = v0.withErrorPopup(
              Some(MErrorPopupS(
                messages = errMsg :: Nil
              ))
            )
            v1 -> None

          } { fileNew =>
            // Попробовать найти в состоянии файл с такими же характеристиками.
            // TODO Искать не только в эджах, но и среди известных файлов с сервера.
            val (dataEdge9, edges9, fxOpt) = v0.edges
              .valuesIterator
              .flatMap { e =>
                for {
                  fileJs <- e.fileJs
                  if fileJs.blob.size ==* fileNew.size &&
                    // TODO Нужен хэш вместо имени, надо бы через asm-crypto.js SHA1 это реализовать.
                    fileJs.fileName.contains( fileNew.name )
                } yield {
                  e
                }
              }
              .toStream
              .headOption
              .fold [(MEdgeDataJs, Map[EdgeUid_t, MEdgeDataJs], Option[Effect])] {
                // Новый файл выбран юзером, который пока неизвестен.
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
                )

                // Если есть старый файл, то нужно позаботиться о его удалении...
                Future {
                  for {
                    dataEdgeOld   <- dataEdgeOldOpt
                    fileJsOld     <- dataEdgeOld.fileJs
                  } {
                    // Закрыть старый blobURL в фоне, после пере-рендера.
                    for (blobUrlOld <- fileJsOld.blobUrl) {
                      Future {
                        URL.revokeObjectURL(blobUrlOld)
                      }
                    }

                    // Старый файл надо закрыть. В фоне, чтобы избежать теоретически-возможных
                    Future {
                      fileJsOld.blob.close()
                    }

                    // Прервать upload файла на сервер, есть возможно.
                    for {
                      fUpload     <- fileJsOld.upload
                      upXhrOld    <- fUpload.xhr
                    } {
                      try {
                        upXhrOld.abort()
                      } catch {
                        case ex: Throwable =>
                          LOG.warn(ErrorMsgs.XHR_UNEXPECTED_RESP, ex, dataEdgeOld)
                      }
                    }
                  }
                } .failed
                  .foreach { ex =>
                    LOG.error( ErrorMsgs.FILE_CLEANUP_FAIL, ex, msg = dataEdgeOldOpt )
                  }

                // Прохешировать файл в фоне.
                val hashFx = (MHashes.Sha1 :: MHashes.Sha256 :: Nil)
                  .map { mhash =>
                    Effect {
                      // Отправить в веб-воркер описание задачи по хэшированию кода.
                      WwMgr
                        .runTask( HashWwTask(mhash, fileNew) )
                        // Обернуть результат работы в понятный экшен:
                        .transform { tryRes =>
                          Success( FileHashRes(edgeUid2, blobUrlNew, mhash, tryRes) )
                        }
                    }
                  }
                  .reduce[Effect](_ + _)

                val edges2 = v0.edges.updated(edgeUid2, dataEdge2)
                (dataEdge2, edges2, Some(hashFx))

              } { edge =>
                // Внезапно, этот файл уже известен.
                //println("dup: " + fileNew.name + " | " + fileNew.size + " bytes")
                (edge, v0.edges, None)
              }

            val selJdt2 = selJdt0.withProps1(
              selJdt0.props1.withBgImg( Some(
                MImgEdgeWithOps(
                  imgEdge = MJdEdgeId( dataEdge9.id ),
                )
              ))
            )

            // Собрать обновлённое состояние.
            val v1 = v0.copy(
              edges       = edges9,
              selectedTag = Some(selJdt2)
            )
            v1 -> fxOpt
          }

        fxOpt9
          .fold(updated(v9))(updated(v9, _))
      }


    // Сигнал окончания рассчёта sha1-хэша файла. Найти эдж файла и вписать туда.
    case m: FileHashRes =>
      val v0 = value
      m.hex.fold [ActionResult[M]] (
        { ex =>
          // Отрендерить попап с ошибкой.
          val v2 = v0.withErrorPopup(
            Some( MErrorPopupS(
              // TODO Передать имя файла $0-параметром сообщения
              messages  = MMessage(MsgCodes.`Cannot.checksum.file`) ::
                // Не стирать предыдущие ошибки, если есть:
                v0.errorPopup.fold(List.empty[MMessage])(_.messages),
              exception = Option(ex)
            ))
          )
          updated(v2)
        },
        { hashHex =>
          // Сохранить рассчётный хэш в состояние.
          _findEdgeByIdOrBlobUrl(v0.edges, m.edgeUid, m.blobUrl)
            .fold {
              // Нет такого файла. Вероятно, пока считался хэш, юзер уже выбрал какой-то другой файл.
              LOG.log( WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = m )
              noChange
            } { edge0 =>
              // Дедубликация кода обновления текущего эджа:
              def __v2F(fileJs9: MJsFileInfo): MPictureAh = {
                val edge2 = edge0.withFileJs( Some( fileJs9 ) )
                v0.withEdges(
                  v0.edges.updated( edge0.id, edge2 )
                )
              }

              val fileJs0 = edge0.fileJs.get
              val hashesHex2: HashesHex = fileJs0.hashesHex + (m.hash -> hashHex)

              val fileJs1 = fileJs0
                .withHashesHex( hashesHex2 )

              // Попытаться провалидировать хеши так же, как это сделает сервер.
              // Это поможет определить достаточность собранной карты хешей для запуска аплоада.
              HashesHex
                .hashesHexV(hashesHex2, UploadConstants.CleverUp.PICTURE_FILE_HASHES)
                .fold(
                  // Хешей пока недостаточно, ждать ещё хэшей...
                  {_ =>
                    val edge2 = edge0.withFileJs( Some( fileJs1 ) )
                    val v2 = v0.withEdges(
                      v0.edges.updated( edge0.id, edge2 )
                    )
                    updated( __v2F(fileJs1) )
                  },
                  // Собрано достаточно хешей для аплоада. Запустить процедуру аплоада на сервер:
                  {_ =>
                    val fx =
                      Effect {
                        val upProps = MFile4UpProps(
                          sizeB     = fileJs0.blob.size.toLong,
                          hashesHex = hashesHex2,
                          mimeType  = fileJs0.blob.`type`
                        )
                        val edgeUid = edge0.id
                        val blobUrl = fileJs0.blobUrl.get
                        api
                          .prepareUpload( upProps )
                          .transform { tryRes =>
                            Success( PrepUploadResp(tryRes, edgeUid, blobUrl) )
                          }
                      }

                    val fileJs2 = fileJs1.withUpload {
                      val upState0 = fileJs0.upload.getOrElse( MFileUploadS.empty )
                      Some( upState0
                        .withPrepareReq(
                          upState0.prepareReq.pending()
                        )
                      )
                    }

                    val v2 = __v2F(fileJs2)
                    updated(v2, fx)
                  }
                )

            }
        }
      )


    // Сигнал о завершении запроса подготовки к аплоаду файла.
    case m: PrepUploadResp =>
      val v0 = value
      _findEdgeByIdOrBlobUrl(v0.edges, m.edgeUid_t, m.blobUrl).fold {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      } { edge0 =>
        m.tryRes.fold(
          // Ошибка выполнения запроса к серверу. Залить её в состояние для текущего файла.
          {ex =>
            val fileJsOpt2 = for (fileJs0 <- edge0.fileJs) yield {
              fileJs0.withUpload(
                for (upload0 <- fileJs0.upload) yield {
                  upload0
                    .withXhr(None)
                    .withPrepareReq( upload0.prepareReq.fail(ex) )
                }
              )
            }
            val edge2 = edge0.withFileJs( fileJsOpt2 )
            //
            val errPopup0 = v0.errorPopup.getOrElse( MErrorPopupS() )
            val v2 = v0
              .withEdges( v0.edges + (edge0.id -> edge2) )
              // Распахнуть попап с ошибкой закачки файла:
              .withErrorPopup( Some(
                errPopup0.withException( Some(ex) )
              ))
            updated(v2)
          },
          // Сервер вернул читабельный ответ. Разобраться что там в ответе:
          {resp =>
            if (resp.upUrls.nonEmpty) {
              edge0.fileJs.fold {
                LOG.error( ErrorMsgs.FILE_MISSING_EXPECTED, msg = edge0 )
                noChange
              } { fileJs =>
                // Есть ссылка для заливки файла. Перейти к процессу заливания.
                def __tryUpload(upData: MUploadUrlData, rest: List[MUploadUrlData]): Future[_] = {
                  val upRespFut = uploadApi.doFileUpload(upData, fileJs)
                  // Одновременно наладить связь с хостом через websocket для опознания цвета картинки:
                  ???
                }

                ???
              }

            } else if (resp.fileExist.nonEmpty) {
              ???
            } else {
              ???
            }
          }
        )

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

      val bmWhRatio = ISize2di.whRatio(bm)

      val origWhOpt = edge.fileJs
        .flatMap(_.whPx)

      val c100 = 100d

      // Попытаемся восстановить %crop на основе кропа из состояния:
      val existingPcCrop = for {
        crop    <- bgImg.crop
        origWh  <- origWhOpt
      } yield {
        val heightPc = crop.height / origWh.height.toDouble * c100
        //val widthPc = crop.width / origWh.width.toDouble * c100
        val xPc = crop.offX / origWh.width.toDouble * c100
        val yPc = crop.offY / origWh.height.toDouble * c100
        (/*widthPc,*/ heightPc, xPc, yPc)
      }

      // TODO Надо осилить выставление начального кропа. Тут серьезная проблема в этом быдлокоде
      /*
      val (widthPc, heightPc, xPc, yPc) = existingPcCrop.getOrElse {
        val (w9, h9) = origWhOpt
          .filter { origWh =>
            val orient = MOrientations2d.forSize2d( origWh )
            orient ==* MOrientations2d.Vertical
          }
          .fold {
            // Горизонтальная/квадратная ориентация исходной картинки. Или ориентация неизвестна вообще.
            val h = c100
            val w = h / bmWhRatio
            (w, h)
          } { origWh =>
            // Вертикальная ориентация.
            val w = c100
            val h = w * bmWhRatio
            println(origWh, bmWhRatio, w, h)
            (w, h)
          }
        println(w9, h9)
        (w9, h9, 0d, 0d)
      }
      */

      val (heightPc, xPc, yPc) = existingPcCrop.getOrElse {
        (100d, 0d, 0d)
      }

      val cropPc = new PercentCrop {
        override val aspect = bmWhRatio
        override val height = heightPc
        //override val width  = widthPc
        override val x      = xPc
        override val y      = yPc
      }

      val v2 = v0.withCropPopup( Some(
        MPictureCropPopup(
          origCrop    = bgImg.crop,
          imgEdgeUid  = edge.id,
          percentCrop = cropPc,
        )
      ))

      //val v2 = _updateSelectedTag(v1)
      updated( v2 )


    // Выставлен новый кроп для картинки.
    case m: CropChanged =>
      val v0 = value
      // TODO Надо фильтровать ошибочные кропы.

      val cropPopup0 = v0.cropPopup.get
      val cropPopup2 = cropPopup0.copy(
        percentCrop = m.percentCrop,
        pixelCrop   = Some( m.pixelCrop )
      )

      val v1 = v0.withCropPopup( Some(cropPopup2) )

      // Надо рендерить crop и в самой карточке.
      val v2 = _updateSelectedTag( v1 )
      updated(v2)


    // Сохранение выбранного кропа для картинки.
    case CropSave =>
      val v0 = value
      val cropPopup0 = v0.cropPopup.get
      val selJdt0 = v0.selectedTag.get
      val iEdgeUid = cropPopup0.imgEdgeUid
      val iEdge = v0.edges( iEdgeUid )

      val origWh = iEdge.fileJs.flatMap(_.whPx).get

      // Вычисляем MCrop в пикселях.
      val pixelCrop = _cropPopupS2mcrop(cropPopup0, origWh)

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
      v0.cropPopup.fold(noChange) { cropPopup =>
        val v1 = v0.withCropPopup( None )
        // Восстановить настройки кропа.
        val v2 = if (
          v1.selectedTag.exists(
            _.props1.bgImg.exists(
              _.crop ===* cropPopup.origCrop))
        ) {
          // Исходный кроп и текущий эквивалентны. Поэтому пропускаем всё как есть.
          v1
        } else {
          // Восстановить исходный кроп sel-тега в состоянии.
          v1.withSelectedTag(
            v1.selectedTag.map { selJdt0 =>
              selJdt0.withProps1(
                selJdt0.props1.withBgImg(
                  selJdt0.props1.bgImg.map { bgImg =>
                    bgImg.withCrop( cropPopup.origCrop )
                  }
                )
              )
            }
          )
        }
        updated(v2)
      }

  }


  // int mult с процентами.
  private def _imult3(a: Int, b: UndefOr[Double], dflt: Int)  : Int = b.fold(dflt)(x => (a * x / 100).toInt)
  private def _imult2(a: Int, b: UndefOr[Double])             : Int = _imult3(a, b, a)
  private def _orZero(a: js.UndefOr[Int])                     : Int = a.getOrElse(0)


  /** Вычисляем MCrop в пикселях на основе данных состояния и wh изображения. */
  private def _cropPopupS2mcrop(cropPopup: MPictureCropPopup, origWh: ISize2di): MCrop = {
    cropPopup.pixelCrop.fold {
      val pcCrop = cropPopup.percentCrop
      // Попытаться перемножить percent crop и image wh
      MCrop(
        width  = _imult2(origWh.width,  pcCrop.width),
        height = _imult2(origWh.height, pcCrop.height),
        offX   = _imult3(origWh.width,  pcCrop.x, 0),
        offY   = _imult3(origWh.height, pcCrop.y, 0)
      )
    } { pxCrop =>
      MCrop(
        width  = pxCrop.width.getOrElse( origWh.width ),
        height = pxCrop.height.getOrElse( origWh.height ),
        offX   = _orZero( pxCrop.x ),
        offY   = _orZero( pxCrop.y )
      )
    }
  }


  /** Когда надо рендерить кроп на экране в карточке, то использовать этот код. */
  private def _updateSelectedTag(v0: MPictureAh): MPictureAh = {
    val selJdt2 = for {
      cropPopup <- v0.cropPopup
      e       <- v0.edges.get( cropPopup.imgEdgeUid )
      fileJs  <- e.fileJs
      origWh  <- fileJs.whPx
      mcrop2  = _cropPopupS2mcrop(cropPopup, origWh)
      selJdt0 <- v0.selectedTag
      bgImg   <- selJdt0.props1.bgImg
      // Не обновлять ничего, если ничего не изменилось.
      if !bgImg.crop.contains( mcrop2 )
    } yield {
      selJdt0.withProps1(
        selJdt0.props1.withBgImg(Some(
          bgImg
            .withCrop( Some(mcrop2) )
        ))
      )
    }

    selJdt2.fold(v0) { _ =>
      v0.withSelectedTag( selJdt2 )
    }
  }



  def _findEdgeByIdOrBlobUrl(edges: Map[EdgeUid_t, MEdgeDataJs], edgeUid: EdgeUid_t, blobUrl: String): Option[MEdgeDataJs] = {
    val blobUrlFilterF = { e: MEdgeDataJs =>
      e.fileJs.exists(_.blobUrl.contains( blobUrl ))
    }
    edges
      .get(edgeUid)
      .filter(blobUrlFilterF)
      .orElse {
        // Нет эджа с таким id и url, возможна карта эджей изменилась с тех пор.
        edges
          .valuesIterator
          .find(blobUrlFilterF)
      }
  }

}

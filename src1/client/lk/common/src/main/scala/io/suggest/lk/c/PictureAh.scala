package io.suggest.lk.c

import com.github.dominictobias.react.image.crop.PercentCrop
import diode._
import io.suggest.color.{MHistogram, MHistogramWs}
import io.suggest.common.geom.d2.ISize2di
import io.suggest.crypto.asm.HashWwTask
import io.suggest.crypto.hash.{HashesHex, MHashes}
import io.suggest.file.up.{MFile4UpProps, MFileUploadS}
import io.suggest.file.{MJsFileInfo, MSrvFileInfo}
import io.suggest.i18n.{MMessage, MsgCodes}
import io.suggest.img.crop.MCrop
import io.suggest.img.MImgFmts
import io.suggest.jd.{MJdEdge, MJdEdgeId}
import io.suggest.js.UploadConstants
import io.suggest.lk.m._
import io.suggest.lk.m.frk.MFormResourceKey
import io.suggest.lk.m.img.{MPictureAh, MPictureCropPopup}
import io.suggest.model.n2.edge.{EdgeUid_t, EdgesUtil, MPredicates}
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.proto.http.model.Route
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.ueq.UnivEqUtil._
import io.suggest.up.IUploadApi
import io.suggest.ws.MWsMsgTypes
import io.suggest.ws.pool.m.{MWsConnTg, WsChannelMsg, WsEnsureConn}
import io.suggest.ww._
import japgolly.univeq._
import org.scalajs.dom.raw.URL

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
class PictureAh[V, M](
                       prepareUploadRoute  : MFormResourceKey => Route,
                       uploadApi           : IUploadApi,
                       modelRW             : ModelRW[M, MPictureAh[V]]
                     )(implicit picViewContAdp: IPictureViewAdp[V])
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  private type ResPair_t = (MPictureAh[V], Effect)

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Выставлен файл в input'е заливки картинки.
    // 1. Отрендерить его на экране (т.е. сохранить в состоянии в виде блоба).
    // 2. Запустить фоновую закачку файла на сервер.
    case m: PictureFileChanged =>
      val v0 = value

      // Посмотреть, что пришло в сообщении...
      if (m.files.isEmpty) {
        // Файл удалён, т.е. список файлов изменился в []. Удалить bgImg и сопутствующий edgeUid, если файл более никому не нужен.
        picViewContAdp
          .get(v0.view, m.resKey)
          .fold( noChange ) { _ =>
            // Не чистим эджи, пусть другие контроллеры проконтроллируют карту эджей на предмет ненужных эджей.
            // Это нужно, чтобы избежать удаления файла, который используется в каком-то другом теге.
            var v2 = v0.withView(
              view = picViewContAdp.updated(v0.view, m.resKey)(None)
            )

            // Закрыть cropPopup, если открыт. В CropPopup бывает кнопка "удалить".
            for (_ <- v2.cropPopup)
              v2 = v2.withCropPopup(None)

            // Отправить в очередь задачу по зачистке карты эджей:
            val fx = PurgeUnusedEdges.toEffectPure
            updated( v2, fx )
          }

      } else {
        // Выставлен новый файл. Надо записать его в состояние.
        val (v9, fxOpt9) = m.files
          .find { fileNew =>
            MImgFmts.withMime( fileNew.`type`.toLowerCase )
              .nonEmpty
          }
          .fold [(MPictureAh[V], Option[Effect])] {
            val errMsg = MMessage( MsgCodes.`File.is.not.a.picture` )
            // Не найдено картинок среди новых файлов.
            val v1 = v0.withErrorPopup(
              Some(MErrorPopupS(
                messages = errMsg :: Nil
              ))
            )
            v1 -> None

          } { fileNew =>
            // Попробовать найти в состоянии файл с такими же характеристиками.
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
                val bgImgOldOpt = picViewContAdp.get(v0.view, m.resKey)
                val edgeUidOldOpt = bgImgOldOpt.map(_.edgeUid)
                val dataEdgeOldOpt = edgeUidOldOpt.flatMap( v0.edges.get )

                // Вычислить новый edgeUid.
                val edgeUid2 = edgeUidOldOpt.getOrElse {
                  EdgesUtil.nextEdgeUidFromMap( v0.edges )
                }

                val blobUrlNew = URL.createObjectURL( fileNew )

                // Записать текущий файл в состояние.
                val dataEdge2 = MEdgeDataJs(
                  jdEdge = MJdEdge(
                    predicate   = MPredicates.JdContent.Image,
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

                // Запустить хеширование файла в фоне:
                val hashFx = FileHashStart(edgeUid2, blobUrlNew).toEffectPure
                val edges2 = v0.edges.updated(edgeUid2, dataEdge2)
                (dataEdge2, edges2, Some(hashFx))

              } { edge =>
                // Внезапно, этот файл уже известен.
                //println("dup: " + fileNew.name + " | " + fileNew.size + " bytes")
                (edge, v0.edges, None)
              }

            val imgEdgeIdSome2 = Some(
              MJdEdgeId(
                edgeUid = dataEdge9.id
              )
            )

            // Собрать обновлённое состояние.
            val v1 = v0.copy(
              edges       = edges9,
              view        = picViewContAdp.updated(v0.view, m.resKey)(imgEdgeIdSome2)
            )
            v1 -> fxOpt
          }

        fxOpt9
          .fold(updated(v9))(updated(v9, _))
      }


    // Сигнал к запуску хеширования файлов.
    case m: FileHashStart =>
      val v0 = value
      _findEdgeByIdOrBlobUrl(v0.edges, m.edgeUid, m.blobUrl).fold {
        LOG.warn(WarnMsgs.SOURCE_FILE_NOT_FOUND, msg = m)
        noChange
      } { eData =>
        eData.fileJs.fold {
          LOG.warn(WarnMsgs.SOURCE_FILE_NOT_FOUND, msg = (eData, m))
          noChange
        } { fileJs0 =>
          // Есть js-файл на руках. Огранизовать хеширование:
          val hashFx = (MHashes.Sha1 :: MHashes.Sha256 :: Nil)
            .map { mhash =>
              Effect {
                // Отправить в веб-воркер описание задачи по хэшированию кода.
                WwMgr
                  .runTask( HashWwTask(mhash, fileJs0.blob) )
                  // Обернуть результат работы в понятный экшен:
                  .transform { tryRes =>
                    Success( FileHashRes(m.edgeUid, m.blobUrl, mhash, tryRes) )
                  }
              }
            }
            .reduce[Effect](_ + _)

          // TODO Может быть надо Pot[] для каждого хеша организовать? Чтобы вычисление хэшей отражалось в состоянии.
          effectOnly( hashFx )
        }
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
              def __v2F(fileJs9: MJsFileInfo): MPictureAh[V] = {
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
                    updated( __v2F(fileJs1) )
                  },
                  // Собрано достаточно хешей для аплоада. Запустить процедуру аплоада на сервер:
                  {_ =>
                    val fx = Effect {
                      val upProps = MFile4UpProps(
                        sizeB     = fileJs0.blob.size.toLong,
                        hashesHex = hashesHex2,
                        mimeType  = fileJs0.blob.`type`
                      )
                      val edgeUid = edge0.id
                      val blobUrl = fileJs0.blobUrl.get
                      val reqRoute = prepareUploadRoute(
                        MFormResourceKey(
                          edgeUid = Some( edgeUid ),
                          nodePath = None
                        )
                      )
                      uploadApi
                        .prepareUpload( reqRoute, upProps )
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
      _findEdgeByIdOrBlobUrl(v0.edges, m.edgeUid, m.blobUrl).fold {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange

      } { edge0 =>
        // Ожидаемый ответ. Разобраться, что там прислал сервер.
        m.tryRes.fold(
          // Ошибка выполнения запроса к серверу. Залить её в состояние для текущего файла.
          {ex =>
            val fileJsOpt2 = _fileJsWithUpload(edge0.fileJs) { upload0 =>
              upload0
                .withXhr(None)
                .withPrepareReq( upload0.prepareReq.fail(ex) )
            }
            val edge2 = edge0.withFileJs( fileJsOpt2 )
            val errPopup0 = v0.errorPopup.getOrElse( MErrorPopupS.empty )
            val v2 = v0
              .withEdges( v0.edges + (edge0.id -> edge2) )
              // Распахнуть попап с ошибкой закачки файла:
              .withErrorPopup( Some(
                MErrorPopupS.exception.set( Some(ex) )(errPopup0)
              ))
            updated(v2)
          },

          // Сервер вернул читабельный ответ. Разобраться что там в ответе:
          {resp =>
            resp.upUrls
              .headOption
              .map { firstUpUrl =>
                edge0.fileJs.fold {
                  // Внезапно нет файла, который заказчиваются. Такое может быть только по воле юзера.
                  LOG.warn( ErrorMsgs.EXPECTED_FILE_MISSING, msg = edge0 )
                  noChange

                } { fileJs =>
                  // Есть ссылка для заливки файла. Залить.
                  val uploadFx = Effect {
                    val upRespFut = uploadApi.doFileUpload(firstUpUrl, fileJs)
                    // TODO Пописаться на события xhr.upload.onprogress, чтобы мониторить ход заливки.
                    // Завернуть ответ сервера в итоговый Action:
                    upRespFut.transform { tryRes =>
                      Success( UploadRes(tryRes, m.edgeUid, m.blobUrl, firstUpUrl) )
                    }
                  }

                  // Залить изменения в состояние:
                  val fileJsOpt2 = _fileJsWithUpload(edge0.fileJs) { upload0 =>
                    upload0.copy(
                      xhr         = None,
                      prepareReq  = upload0.prepareReq.ready(resp),
                      uploadReq   = upload0.uploadReq.pending()
                    )
                  }
                  val edge2 = edge0.withFileJs( fileJsOpt2 )
                  val v2 = v0
                    .withEdges( v0.edges + (edge0.id -> edge2) )
                  updated(v2, uploadFx)
                }
              }
              // Нет ссылок для аплоада. Проверить fileExists-поле:
              .orElse {
                for (fe <- resp.fileExist) yield {
                  // Файл уже залит на сервер. Это нормально. Залить данные по файлу в состояние:
                  val edge2 = edge0.copy(
                    jdEdge = _srvFileIntoJdEdge( fe, edge0.jdEdge ),
                    fileJs = _fileJsWithUpload(edge0.fileJs) { upload0 =>
                      upload0
                        .withXhr(None)
                        .withPrepareReq(
                          upload0.prepareReq.ready(resp)
                        )
                    }
                  )
                  val v1 = v0.withEdges(
                    v0.edges
                      .updated(edge2.id, edge2)
                  )
                  // Если пришла гистограмма, то залить её в состояние.
                  _maybeWithHistogram(fe, v1)
                }
              }
              // Есть проблемы с принятием такого файла: его нельзя отправить на сервер.
              // Возможно, великоват или MIME не поддерживается. Сообщить юзеру, чтобы подобное больше не предлагал.
              .orElse {
                for (_ <- resp.errors.headOption) yield {
                  val v2 = v0.copy(
                    // Удалить эдж текущего файла.
                    edges      = v0.edges - edge0.id,
                    view       = picViewContAdp.forgetEdge(v0.view, m.edgeUid),
                    // Вывести попап с ошибками, присланными сервером:
                    errorPopup = _errorPopupWithMessages( v0.errorPopup, resp.errors )
                  )
                  updated(v2)
                }
              }
              // Некорректный ответ сервера или некорректный код разбора в этом контроллере.
              .getOrElse {
                LOG.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = resp )
                noChange
              }
          }
        )
      }


    // Выполнен аплоад на сервер. Пришёл результат выполнения запроса.
    case m: UploadRes =>
      val v0 = value
      _findEdgeByIdOrBlobUrl(v0.edges, m.edgeUid, m.blobUrl).fold {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange

      } { edge0 =>
        m.tryRes.fold(
          // Ошибка выполнения upload'а.
          {ex =>
            val edge2 = edge0.withFileJs(
              _fileJsWithUpload(edge0.fileJs) { upload0 =>
                upload0.copy(
                  xhr       = None,
                  uploadReq = upload0.uploadReq.fail(ex),
                  progress  = None
                )
              }
            )
            val v2 = v0.withEdges(
              v0.edges.updated(edge2.id, edge2)
            )
            updated( v2 )
          },
          // Сервер ответил что-то внятное. Осталось понять, что именно:
          {resp =>
            resp.fileExist
              .map { fileExist =>
                // Файл успешно залит на сервер. Сервер присылает только базовые данные по загруженному файлу, надо не забывать это.
                // Сохранить это в состояние:
                val edge2 = edge0.copy(
                  jdEdge = _srvFileIntoJdEdge(fileExist, edge0.jdEdge),
                  fileJs = _fileJsWithUpload(edge0.fileJs) { upload0 =>
                    upload0.copy(
                      xhr       = None,
                      uploadReq = upload0.uploadReq.ready(resp),
                      progress  = None
                    )
                  }
                )
                val v2 = v0.withEdges(
                  v0.edges.updated(edge2.id, edge2)
                )

                // В ответе может быть гистограмма. Это важно проаналализировать и вынести решение:
                fileExist.colors.fold {
                  // Сервер не прислал гистограмму. Она придёт по websocket'у.
                  // В фоне: запустить открытие websocket'а для связи с сервером по поводу гистограммы.
                  uploadApi.conf.ctxIdOpt.fold {
                    // Нет ctxId в аплоаде - не будет веб-сокета с палитрой.
                    updated(v2)
                  } { _ =>
                    // Есть ctxId - нужен веб-сокет
                    val wsMsg = WsEnsureConn(
                      target = MWsConnTg(
                        host = m.hostUrl.host
                      ),
                      closeAfterSec = Some(120)
                    )
                    val wsEnsureFx = wsMsg.toEffectPure
                    updated(v2, wsEnsureFx)
                  }

                } { histogram =>
                  // Гистограмма уже есть в комплекте с ответом сервера. Внести гистограмму в карту и запустить дальнейший процессинг дерева документа:
                  val (v3, fx3) = _withHistogram( fileExist.nodeId, histogram, v2 )
                  updated(v3, fx3)
                }
              }
              // Возможно, что-то пошло на сервере не так. Нужно отрендерить .errors:
              .orElse {
                for (_ <- resp.errors.headOption) yield {
                  val v2 = v0
                    .withErrorPopup(
                      _errorPopupWithMessages( v0.errorPopup, resp.errors )
                    )
                  updated(v2)
                }
              }
              .getOrElse {
                LOG.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = m)
                noChange
              }
          }
        )
      }


    // Сообщение из WebSocket с гистограммой цветов к какой-то картинке.
    case m: WsChannelMsg if m.msg.typ ==* MWsMsgTypes.ColorsHistogram =>
      try {
        val histWs = m.msg.payload.as[MHistogramWs]
        //println("wsChannel => hist: " + histWs + s" \n hists: ${v0.histograms} => ${v2.histograms}")
        _resPair2res(
          _withHistogram(histWs.nodeId, histWs.hist, value)
        )

      } catch {
        case ex: Throwable =>
          LOG.error( ErrorMsgs.JSON_PARSE_ERROR, ex, m )
          noChange
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
    case m: CropOpen =>
      val v0 = value
      val bgImg = picViewContAdp
        .get(v0.view, m.resKey)
        .get
      val edge = v0.edges( bgImg.edgeUid )

      val bmWhRatio = ISize2di.whRatio( m.cropContSz )

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
          percentCrop = cropPc
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
      val v2 = _updateSelectedTag( v1, m.resKey )
      updated(v2)


    // Сохранение выбранного кропа для картинки.
    case m: CropSave =>
      val v0 = value
      val cropPopup0 = v0.cropPopup.get
      val iEdgeUid = cropPopup0.imgEdgeUid
      val iEdge = v0.edges( iEdgeUid )

      val origWh = iEdge.origWh.get

      // Вычисляем MCrop в пикселях.
      val pixelCrop = _cropPopupS2mcrop(cropPopup0, origWh)

      val bgImg0 = picViewContAdp.get(v0.view, m.resKey).get

      // Сохранить в текущий тег параметры кропа.

      val view2 = picViewContAdp.updated(v0.view, m.resKey) {
        Some {
          MJdEdgeId.crop.set( Some(pixelCrop) )(bgImg0)
        }
      }

      // TODO Добавить поддержку выставления кропа в qdTag. Но тут нужно сам quill ещё промодифицировать.

      // Сохранить новый кроп в состояние.
      val v2 = v0
        .withCropPopup( None )
        .withView( view2 )
      updated( v2 )


    // Отмена кропа
    case m: CropCancel =>
      val v0 = value
      v0.cropPopup.fold(noChange) { cropPopup =>
        val v1 = v0.withCropPopup( None )
        // Восстановить настройки кропа.
        val v2 = if (
          picViewContAdp.get(v1.view, m.resKey)
            .exists( _.crop ===* cropPopup.origCrop )
        ) {
          // Исходный кроп и текущий эквивалентны. Поэтому пропускаем всё как есть.
          v1
        } else {
          // Восстановить исходный кроп sel-тега в состоянии.
          v1.withView(
            picViewContAdp.updateWith(v1.view, m.resKey) { imgEdgeIdOpt =>
              imgEdgeIdOpt.map( MJdEdgeId.crop set cropPopup.origCrop )
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
  private def _updateSelectedTag(v0: MPictureAh[V], resKey: MFormResourceKey): MPictureAh[V] = {
    val imgEdgeId2 = for {
      cropPopup <- v0.cropPopup
      e       <- v0.edges.get( cropPopup.imgEdgeUid )
      origWh  <- e.origWh
      mcrop2  = _cropPopupS2mcrop(cropPopup, origWh)
      bgImg   <- picViewContAdp.get(v0.view, resKey)
      // Не обновлять ничего, если ничего не изменилось.
      if !bgImg.crop.contains( mcrop2 )
    } yield {
      MJdEdgeId.crop.set( Some(mcrop2) )( bgImg )
    }

    imgEdgeId2.fold(v0) { _ =>
      v0.withView(
        picViewContAdp.updated(v0.view, resKey)(imgEdgeId2)
      )
    }
  }



  private def _findEdgeByIdOrBlobUrl(edges: Map[EdgeUid_t, MEdgeDataJs], edgeUid: EdgeUid_t, blobUrl: String): Option[MEdgeDataJs] = {
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


  private def _errorPopupWithMessages(errorPopupOpt0: Option[MErrorPopupS], messages: TraversableOnce[MMessage]): Some[MErrorPopupS] = {
    val ep0 = errorPopupOpt0.getOrElse( MErrorPopupS.empty )
    val ep2 = MErrorPopupS.messages.modify(_ ++ messages)(ep0)
    Some(ep2)
  }


  private def _fileJsWithUpload(fileJsOpt0: Option[MJsFileInfo])(f: MFileUploadS => MFileUploadS): Option[MJsFileInfo] = {
    for (fileJs0 <- fileJsOpt0) yield {
      fileJs0.withUpload(
        fileJs0.upload
          .map(f)
      )
    }
  }


  // Объединяем старый и новый набор данных по файлу на сервере.
  private def _srvFileIntoJdEdge(fileInfo: MSrvFileInfo, jdEdge0: MJdEdge): MJdEdge = {
    val srvFileInfo0 = jdEdge0
      .fileSrv
      .getOrElse(MSrvFileInfo.empty)
    val srvFileInfo2 = srvFileInfo0.updateFrom( fileInfo )
    jdEdge0
      .withFileSrv( Some(srvFileInfo2) )
  }


  private def _maybeWithHistogram(fe: MSrvFileInfo, v0: MPictureAh[V]): ActionResult[M] = {
    fe.colors
      .filter(_.nonEmpty)
      .fold(updated(v0)) { hist =>
        _resPair2res(
          _withHistogram(fe.nodeId, hist, v0)
        )
      }
  }

  private def _withHistogram(nodeId: String, colors: MHistogram, v0: MPictureAh[V]): ResPair_t = {
    val v2 = v0.withHistograms(
      v0.histograms
        .updated(nodeId, colors)
    )
    val fx = HandleNewHistogramInstalled(nodeId).toEffectPure
    (v2, fx)
  }
  private def _resPair2res(withHistRes: ResPair_t): ActionResult[M] = {
    updated(withHistRes._1, withHistRes._2)
  }

}

package io.suggest.lk.c

import com.github.dominictobias.react.image.crop.PercentCrop
import diode._
import diode.data.Pot
import io.suggest.color.{MHistogram, MHistogramWs}
import io.suggest.common.geom.d2.ISize2di
import io.suggest.crypto.asm.HashWwTask
import io.suggest.crypto.hash.HashesHex
import io.suggest.file.MJsFileInfo
import io.suggest.form.MFormResourceKey
import io.suggest.i18n.{MMessage, MsgCodes}
import io.suggest.img.crop.MCrop
import io.suggest.jd.{MJdEdge, MJdEdgeId}
import io.suggest.lk.m._
import io.suggest.lk.m.img.{MPictureCropPopup, MUploadAh}
import io.suggest.lk.u.LkImgUtilJs
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge._
import io.suggest.n2.media.{MFileMeta, MFileMetaHash, MFileMetaHashFlags}
import io.suggest.pick.{ContentTypeCheck, MimeConst}
import io.suggest.routes.PlayRoute
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.ueq.UnivEqUtil._
import io.suggest.up.{IUploadApi, MFileUploadS, UploadConstants}
import io.suggest.ws.MWsMsgTypes
import io.suggest.ws.pool.m.{MWsConnTg, WsChannelMsg, WsEnsureConn}
import io.suggest.ww._
import japgolly.univeq._
import monocle.Traversal
import org.scalajs.dom.raw.URL
import scalaz.std.option._

import scala.concurrent.Future
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 19:03
  * Description: Контроллер управления картинками.
 *
  * @param ctxIdOptRO      Доступ к CtxId.
  */
final class UploadAh[V, M](
                            prepareUploadRoute  : MFormResourceKey => PlayRoute,
                            uploadApi           : IUploadApi,
                            contentTypeCheck    : ContentTypeCheck,
                            ctxIdOptRO          : ModelRO[Option[String]],
                            modelRW             : ModelRW[M, MUploadAh[V]],
                            dispatcher          : Dispatcher,
                          )(implicit picViewContAdp: IJdEdgeIdViewAdp[V])
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  private type ResPair_t = (MUploadAh[V], Effect)

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: UploadProgress =>
      val v0 = value
      UploadAh
        ._findEdgeByIdOrBlobUrl(v0.edges, m.src)
        .fold( noChange ) { edgeDataJs0 =>
          val progressOpt2 = Some( m.info )
          val lens = MEdgeDataJs.fileJs
            .andThen( Traversal.fromTraverse[Option, MJsFileInfo] )
            .andThen( MJsFileInfo.upload )
            .andThen( MFileUploadS.progress )

          if (lens.getAll(edgeDataJs0) contains progressOpt2) {
            noChange
          } else {
            val edgeDataJs2 = (lens replace progressOpt2)(edgeDataJs0)
            val v2 = v0
              .withEdges( v0.edges + (m.src.edgeUid -> edgeDataJs2) )
            updated( v2 )
          }
        }


    // Выставлен файл в input'е заливки картинки.
    // 1. Отрендерить его на экране (т.е. сохранить в состоянии в виде блоба).
    // 2. Запустить фоновую закачку файла на сервер.
    case m: UploadFile =>
      val v0 = value

      // Посмотреть, что пришло в сообщении...a
      m.files.headOption.fold {
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

      } { _ /*firstFile*/ =>
        // Выставлен новый файл. Надо записать его в состояние.
        val (v9, fxOpt9) = m.files
          .iterator
          .map { fileNew =>
            val contentType = MimeConst
              .readContentType( fileNew.`type`, contentTypeCheck )
              // Для all - возвращать app/octet-stream, для картинок - None тут, чтобы empty-ветвь уходило всё:
              .orElse( contentTypeCheck.default )
            contentType -> fileNew
          }
          .nextOption()
          .fold [(MUploadAh[V], Option[Effect])] {
            val errMsg = MMessage( MsgCodes.`File.is.not.a.picture` )
            // Не найдено картинок среди новых файлов.
            val v1 = v0.withErrorPopup(
              Some(MErrorPopupS(
                messages = errMsg :: Nil
              ))
            )
            v1 -> None

          } { case (contentType, fileNew) =>
            val fileSize2 = fileNew.size
            // Попробовать найти в состоянии файл с такими же характеристиками.
            val (_ /*dataEdge9*/, edges9, fxOpt, edgeUid9) = (for {
              e <- v0.edges.valuesIterator
              fileJs <- e.fileJs
              if (fileJs.blob.size ==* fileSize2) &&
                 // TODO Нужен хэш вместо имени, надо бы через asm-crypto.js SHA1 это реализовать.
                 (fileJs.fileName contains[String] fileNew.name)
              edgeUid <- e.jdEdge.edgeDoc.id
            } yield {
              // Внезапно, этот файл уже известен.
              //println("dup: " + fileNew.name + " | " + fileNew.size + " bytes")
              (e, v0.edges, None, edgeUid)
            })
              .nextOption()
              .getOrElse {
                // Новый файл выбран юзером, который пока неизвестен.
                // Найти в состоянии текущий файл, если он там есть.
                val edgeUidOldOpt = for {
                  // Хотя m.resKey.edgeUid может содержать id, всё равно прогоняем через adp, т.к. uid может и отсутсовать при наличии эджа.
                  jdEdgeIdOld <- picViewContAdp.get(v0.view, m.resKey)
                } yield {
                  jdEdgeIdOld.edgeUid
                }
                val dataEdgeOldOpt = edgeUidOldOpt.flatMap( v0.edges.get )

                // Если есть старый файл, то нужно позаботиться о его удалении...
                val blobPurgeFx = Effect.action {
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
                      upReqHolderOld    <- fileJsOld.upload.resultHolder
                      ex                <- upReqHolderOld.abort().failed
                    }
                      logger.warn(ErrorMsgs.XHR_UNEXPECTED_RESP, ex, dataEdgeOld)
                  }

                  DoNothing
                }

                // Вычислить новый edgeUid.
                val edgeUid2 = edgeUidOldOpt.getOrElse {
                  EdgesUtil.nextEdgeUidFromMap( v0.edges )
                }

                val blobUrlNew = URL.createObjectURL( fileNew )

                // Записать текущий файл в состояние.
                val dataEdge2 = MEdgeDataJs(
                  jdEdge = MJdEdge(
                    predicate   = MPredicates.JdContent.Image,
                    edgeDoc = MEdgeDoc(
                      id = Some( edgeUid2 ),
                    ),
                    url         = Some( blobUrlNew )
                  ),
                  fileJs = Some(MJsFileInfo(
                    blob      = fileNew,
                    blobUrl   = Option( blobUrlNew ),
                    fileName  = Option( fileNew.name ),
                    // Заливка ещё не началась, а только предподготовка, но можно ведь отрендерить крутилку...
                    upload    = MFileUploadS(
                      prepareReq = Pot.empty.pending(),
                    ),
                    fileMeta = MFileMeta(
                      mime  = contentType,
                      sizeB = Some( fileSize2.toLong ),
                    ),
                  ))
                )

                // Запустить хеширование файла в фоне:
                val hashFx = FileHashStart(edgeUid2, blobUrlNew).toEffectPure
                val edges2 = v0.edges + (edgeUid2 -> dataEdge2)
                (dataEdge2, edges2, Some(hashFx + blobPurgeFx), edgeUid2)
              }

            val imgEdgeIdSome2 = Some(
              MJdEdgeId(
                edgeUid = edgeUid9,
              )
            )

            // Собрать обновлённое состояние.
            val v1 = v0.copy(
              edges       = edges9,
              view        = picViewContAdp.updated(v0.view, m.resKey)(imgEdgeIdSome2)
            )
            v1 -> fxOpt
          }

        ah.updatedMaybeEffect(v9, fxOpt9)
      }


    // Сигнал к запуску хеширования файлов.
    case m: FileHashStart =>
      val v0 = value

      (for {
        eData <- {
          val edOpt = UploadAh._findEdgeByIdOrBlobUrl(v0.edges, m)
          if (edOpt.isEmpty)
            logger.warn(ErrorMsgs.SOURCE_FILE_NOT_FOUND, msg = m)
          edOpt
        }

        fileJs0 <- {
          val fjs = eData.fileJs
          if (fjs.isEmpty)
            logger.warn(ErrorMsgs.SOURCE_FILE_NOT_FOUND, msg = (eData, m))
          fjs
        }
      } yield {
        // Есть js-файл на руках. Огранизовать хеширование:
        val hashFx = (for {
          mhash <- UploadConstants.CleverUp
            .UPLOAD_FILE_HASHES
            .iterator
        } yield {
          Effect {
            // Отправить в веб-воркер описание задачи по хэшированию кода.
            WwMgr
              .runTask( HashWwTask(mhash, fileJs0.blob) )
              // Обернуть результат работы в понятный экшен:
              .transform { tryRes =>
                Success( FileHashRes(m, mhash, tryRes) )
              }
          }
        })
          .mergeEffects
          .get

        // TODO Может быть надо Pot[] для каждого хеша организовать? Чтобы вычисление хэшей отражалось в состоянии.
        effectOnly( hashFx )
      })
        .getOrElse( noChange )


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
          UploadAh._findEdgeByIdOrBlobUrl(v0.edges, m.src)
            .fold {
              // Нет такого файла. Вероятно, пока считался хэш, юзер уже выбрал какой-то другой файл.
              logger.log( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = m )
              noChange
            } { edge0 =>
              val edgeUid = edge0.jdEdge.edgeDoc.id.get

              // Дедубликация кода обновления текущего эджа:
              def __v2F(fileJs9: MJsFileInfo): MUploadAh[V] = {
                val edge2 = (MEdgeDataJs.fileJs replace Some(fileJs9))(edge0)
                v0.withEdges(
                  v0.edges.updated( edgeUid, edge2 )
                )
              }

              val fileJs1 = MJsFileInfo.fileMeta
                .andThen( MFileMeta.hashesHex )
                .modify { hhs0 =>
                  MFileMetaHash(
                    hType     = m.hash,
                    hexValue  = hashHex,
                    flags     = MFileMetaHashFlags.ORIGINAL_FLAGS,
                  ) +: hhs0
                }( edge0.fileJs.get )

              // Пересборка m.src
              val src2 = FileHashStart(
                edgeUid = edgeUid,
                blobUrl = fileJs1.blobUrl.get,
              )

              // Попытаться провалидировать хеши так же, как это сделает сервер.
              // Это поможет определить достаточность собранной карты хешей для запуска аплоада.
              HashesHex
                .hashesHexV(
                  MFileMetaHash.toHashesHex( fileJs1.fileMeta.hashesHex ),
                  UploadConstants.CleverUp.UPLOAD_FILE_HASHES
                )
                .fold(
                  // Хешей пока недостаточно, ждать ещё хэшей...
                  {_ =>
                    updated( __v2F(fileJs1) )
                  },
                  // Собрано достаточно хешей для аплоада. Запустить процедуру аплоада на сервер:
                  {_ =>
                    val fx = Effect {
                      val reqRoute = prepareUploadRoute(
                        MFormResourceKey(
                          edgeUid  = Some( edgeUid ),
                          nodePath = None
                        )
                      )

                      uploadApi
                        .prepareUpload( reqRoute, fileJs1.fileMeta )
                        .transform { tryRes =>
                          Success( PrepUploadResp(tryRes, src2) )
                        }
                    }

                    val prepareReq_LENS = MJsFileInfo.upload
                      .andThen( MFileUploadS.prepareReq )
                    val fileJs2 = if (prepareReq_LENS.get(fileJs1).isPending) {
                      fileJs1
                    } else {
                      prepareReq_LENS
                        .modify( _.pending() )( fileJs1 )
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

      UploadAh._findEdgeByIdOrBlobUrl(v0.edges, m.src).fold {
        logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange

      } { edge0 =>
        val edgeUid = edge0.jdEdge.edgeDoc.id.get

        // Ожидаемый ответ. Разобраться, что там прислал сервер.
        m.tryRes.fold(
          // Ошибка выполнения запроса к серверу. Залить её в состояние для текущего файла.
          {ex =>
            val fileJsOpt2 = UploadAh._fileJsWithUpload(edge0.fileJs) {
              (MFileUploadS.reqHolder replace None) andThen
              MFileUploadS.prepareReq.modify( _.fail(ex) )
            }
            val edge2 = (MEdgeDataJs.fileJs replace fileJsOpt2)(edge0)
            val errPopup0 = v0.errorPopup getOrElse MErrorPopupS.empty
            val v2 = v0
              .withEdges( v0.edges + (edgeUid -> edge2) )
              // Распахнуть попап с ошибкой закачки файла:
              .withErrorPopup( Some(
                (MErrorPopupS.exception replace Some(ex))(errPopup0)
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
                  logger.warn( ErrorMsgs.EXPECTED_FILE_MISSING, msg = edge0 )
                  noChange

                } { fileJs =>
                  // Есть ссылка для заливки файла. Залить.
                  val uploadStartFx = Effect.action {
                    UploadReqStarted(
                      respHolder  = uploadApi.doFileUpload(
                        upData      = firstUpUrl,
                        file        = fileJs,
                        ctxIdOpt    = ctxIdOptRO.value,
                        onProgress  = Some { info =>
                          dispatcher( UploadProgress(info, m.src) )
                        },
                      ),
                      src         = m.src,
                      hostUrl     = firstUpUrl,
                    )
                  }

                  // Залить изменения в состояние:
                  val fileJsOpt2 = UploadAh._fileJsWithUpload(edge0.fileJs) { upload0 =>
                    upload0.copy(
                      resultHolder   = None,
                      prepareReq  = upload0.prepareReq.ready(resp),
                      uploadReq   = upload0.uploadReq.pending()
                    )
                  }
                  val edge2 = MEdgeDataJs.fileJs
                    .replace(fileJsOpt2)(edge0)
                  var v2 = v0
                    .withEdges( v0.edges + (edgeUid -> edge2) )

                  if (resp.extra !=* v0.uploadExtra)
                    v2 = v2.withUploadExtra( resp.extra )

                  updated(v2, uploadStartFx)
                }
              }
              // Нет ссылок для аплоада. Проверить fileExists-поле:
              .orElse {
                for (fe <- resp.fileExist) yield {
                  // Файл уже залит на сервер. Это нормально. Залить данные по файлу в состояние:
                  val edge2 = edge0.copy(
                    jdEdge = UploadAh._srvFileIntoJdEdge( fe, edge0.jdEdge ),
                    fileJs = UploadAh._fileJsWithUpload(edge0.fileJs) {
                      MFileUploadS.reqHolder.replace( None ) andThen
                      MFileUploadS.prepareReq.modify( _.ready(resp) )
                    }
                  )
                  var v2 = v0.withEdges(
                    v0.edges
                      .updated( edgeUid, edge2 )
                  )
                  if (resp.extra !=* v0.uploadExtra)
                    v2 = v2.withUploadExtra( resp.extra )
                  // Если пришла гистограмма, то залить её в состояние.
                  _maybeWithHistogram(fe, v2)
                }
              }
              // Есть проблемы с принятием такого файла: его нельзя отправить на сервер.
              // Возможно, великоват или MIME не поддерживается. Сообщить юзеру, чтобы подобное больше не предлагал.
              .orElse {
                for (_ <- resp.errors.headOption) yield {
                  val v2 = v0.copy(
                    // Удалить эдж текущего файла.
                    edges      = v0.edges - edgeUid,
                    view       = picViewContAdp.forgetEdge(v0.view, edgeUid),
                    // Вывести попап с ошибками, присланными сервером:
                    errorPopup = UploadAh._errorPopupWithMessages( v0.errorPopup, resp.errors )
                  )
                  updated(v2)
                }
              }
              // Некорректный ответ сервера или некорректный код разбора в этом контроллере.
              .getOrElse {
                logger.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = resp )
                noChange
              }
          }
        )
      }


    // Запущен реквест аплоада на сервер: отработать состояние реквеста.
    case m: UploadReqStarted =>
      val v0 = value

      (for {
        edge0 <- UploadAh._findEdgeByIdOrBlobUrl(v0.edges, m.src)
        if edge0.fileJs.nonEmpty
      } yield {
        // Эффект окончания загрузки.
        // Сайд-эффект запуска запроса уже произошёл ранее, а здесь просто эффект подписки на событие завершения запроса:
        val uploadFinishFx = Effect {
          // Завернуть ответ сервера в итоговый Action:
          m.respHolder
            .resultFut
            .transform { tryRes =>
              Success( UploadRes(tryRes, m.src, m.hostUrl) )
            }
        }

        // Сохранить httpReq.httpRespHolder в состояние для возможности взаимодействия с закачкой:
        val fileJsOpt2 = UploadAh._fileJsWithUpload(edge0.fileJs)(
          MFileUploadS.reqHolder replace Some( m.respHolder )
        )

        val edge2 = (MEdgeDataJs.fileJs replace fileJsOpt2)(edge0)
        val edgeUid = edge0.jdEdge.edgeDoc.id.get
        val v2 = v0
          .withEdges( v0.edges + (edgeUid -> edge2) )
        updatedSilent(v2, uploadFinishFx)

      })
        .getOrElse {
          logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
          noChange
        }


    // Выполнен аплоад на сервер. Пришёл результат выполнения запроса.
    case m: UploadRes =>
      val v0 = value
      UploadAh._findEdgeByIdOrBlobUrl(v0.edges, m.src).fold {
        logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange

      } { edge0 =>
        val edgeUid = edge0.jdEdge.edgeDoc.id.get

        m.tryRes.fold(
          // Ошибка выполнения upload'а.
          {ex =>
            val edge2 = MEdgeDataJs.fileJs.replace(
              UploadAh._fileJsWithUpload(edge0.fileJs) { upload0 =>
                upload0.copy(
                  resultHolder = None,
                  uploadReq = upload0.uploadReq.fail(ex),
                  progress  = None
                )
              }
            )( edge0 )

            var v2 = v0.withEdges(
              v0.edges.updated( edgeUid, edge2)
            )
            updated( v2 )
          },
          // Сервер ответил что-то внятное. Осталось понять, что именно:
          {resp =>
            (for {
              fileExistEdge <- resp.fileExist
              fileNodeId <- fileExistEdge.nodeId
              fileSrv <- fileExistEdge.fileSrv
            } yield {
              // Файл успешно залит на сервер. Сервер присылает только базовые данные по загруженному файлу, надо не забывать это.
              // Сохранить это в состояние:
              val edge2 = edge0.copy(
                jdEdge = UploadAh._srvFileIntoJdEdge( fileExistEdge, edge0.jdEdge ),
                fileJs = UploadAh._fileJsWithUpload(edge0.fileJs) { upload0 =>
                  upload0.copy(
                    resultHolder = None,
                    // TODO reqHolder/progress: если было Some(), то отписаться от onprogress эффектом.
                    uploadReq = upload0.uploadReq.ready(resp),
                    progress  = None,
                  )
                }
              )
              var v2 = v0.withEdges(
                v0.edges.updated( edgeUid, edge2 )
              )
              if (resp.extra !=* v0.uploadExtra)
                v2 = v2.withUploadExtra( resp.extra )

              // В ответе может быть гистограмма. Это важно проаналализировать и вынести решение:
              fileSrv.pictureMeta.histogram.fold {
                // Сервер не прислал гистограмму. Она придёт по websocket'у.
                // В фоне: запустить открытие websocket'а для связи с сервером по поводу гистограммы.
                if (ctxIdOptRO.value.isEmpty) {
                  // Нет ctxId в аплоаде - не будет веб-сокета с палитрой.
                  updated(v2)
                } else {
                  // Есть ctxId - нужен веб-сокет
                  val wsEnsureFx = Effect.action {
                    WsEnsureConn(
                      target = MWsConnTg(
                        host = m.hostUrl.host
                      ),
                      closeAfterSec = Some(120)
                    )
                  }
                  updated(v2, wsEnsureFx)
                }

              } { histogram =>
                // Гистограмма уже есть в комплекте с ответом сервера. Внести гистограмму в карту и запустить дальнейший процессинг дерева документа:
                val (v3, fx3) = _withHistogram( fileNodeId, histogram, v2 )
                updated(v3, fx3)
              }
            })
              // Возможно, что-то пошло на сервере не так. Нужно отрендерить .errors:
              .orElse {
                for (_ <- resp.errors.headOption) yield {
                  val v2 = v0
                    .withErrorPopup(
                      UploadAh._errorPopupWithMessages( v0.errorPopup, resp.errors )
                    )
                  updated(v2)
                }
              }
              .getOrElse {
                logger.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = m)
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
          logger.error( ErrorMsgs.JSON_PARSE_ERROR, ex, m )
          noChange
      }


    // Загрузилась картинка, и стали известны некоторые параметры этой самой картинки.
    case m: SetImgWh =>
      // Сохранить в состояние ширину и длину.
      val v0 = value
      val e0 = v0.edges(m.edgeUid)

      val e2 = MEdgeDataJs.fileJs
        .andThen( Traversal.fromTraverse[Option, MJsFileInfo] )
        .andThen( MJsFileInfo.whPx )
        .replace( Some(m.wh) )(e0)

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
      val (heightPc, xPc, yPc) = (for {
        crop    <- bgImg.crop
        origWh  <- origWhOpt
      } yield {
        val heightPc = crop.height / origWh.height.toDouble * c100
        //val widthPc = crop.width / origWh.width.toDouble * c100
        val xPc = crop.offX / origWh.width.toDouble * c100
        val yPc = crop.offY / origWh.height.toDouble * c100
        (/*widthPc,*/ heightPc, xPc, yPc)
      })
        .getOrElse {
          (100d, 0d, 0d)
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
          imgEdgeUid  = bgImg.edgeUid,
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

      val pixelCrop = LkImgUtilJs.cropPopupS2mcrop(cropPopup0, origWh)

      val bgImg0 = picViewContAdp.get(v0.view, m.resKey).get

      // Сохранить в текущий тег параметры кропа.

      val view2 = picViewContAdp.updated(v0.view, m.resKey) {
        Some {
          (MJdEdgeId.crop replace Some(pixelCrop))(bgImg0)
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
              imgEdgeIdOpt.map( MJdEdgeId.crop replace cropPopup.origCrop )
            }
          )
        }
        updated(v2)
      }

  }


  /** Когда надо рендерить кроп на экране в карточке, то использовать этот код. */
  private def _updateSelectedTag(v0: MUploadAh[V], resKey: MFormResourceKey): MUploadAh[V] = {
    val imgEdgeId2 = for {
      cropPopup <- v0.cropPopup
      e       <- v0.edges.get( cropPopup.imgEdgeUid )
      origWh  <- e.origWh
      mcrop2  = LkImgUtilJs.cropPopupS2mcrop(cropPopup, origWh)
      bgImg   <- picViewContAdp.get(v0.view, resKey)
      // Не обновлять ничего, если ничего не изменилось.
      if !(bgImg.crop contains[MCrop] mcrop2)
    } yield {
      (MJdEdgeId.crop replace Some(mcrop2))( bgImg )
    }

    imgEdgeId2.fold(v0) { _ =>
      v0.withView(
        picViewContAdp.updated(v0.view, resKey)(imgEdgeId2)
      )
    }
  }


  private def _maybeWithHistogram(fe: MJdEdge, v0: MUploadAh[V]): ActionResult[M] = {
    (for {
      nodeId <- fe.nodeId
      fileSrv <- fe.fileSrv
      hist2 <- fileSrv.pictureMeta.histogram
      if hist2.colors.nonEmpty
    } yield {
      _resPair2res(
        _withHistogram( nodeId, hist2, v0 )
      )
    })
      .getOrElse( updated(v0) )
  }

  private def _withHistogram(nodeId: String, colors: MHistogram, v0: MUploadAh[V]): ResPair_t = {
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


object UploadAh {

  private def _findEdgeByIdOrBlobUrl(edges: Map[EdgeUid_t, MEdgeDataJs], src: FileHashStart): Option[MEdgeDataJs] = {
    val blobUrlFilterF = { e: MEdgeDataJs =>
      e.fileJs
        .exists(_.blobUrl contains[String] src.blobUrl)
    }
    edges
      .get(src.edgeUid)
      .filter(blobUrlFilterF)
      .orElse {
        // Нет эджа с таким id и url, возможна карта эджей изменилась с тех пор.
        edges
          .valuesIterator
          .find(blobUrlFilterF)
      }
  }


  private def _errorPopupWithMessages(errorPopupOpt0: Option[MErrorPopupS], messages: IterableOnce[MMessage]): Some[MErrorPopupS] = {
    val ep0 = errorPopupOpt0.getOrElse( MErrorPopupS.empty )
    val ep2 = MErrorPopupS.messages.modify(_ ++ messages)(ep0)
    Some(ep2)
  }


  private def _fileJsWithUpload(fileJsOpt0: Option[MJsFileInfo])(f: MFileUploadS => MFileUploadS): Option[MJsFileInfo] = {
    fileJsOpt0.map(
      MJsFileInfo.upload
        .modify(f)
    )
  }


  // Объединяем старый и новый набор данных по файлу на сервере.
  private def _srvFileIntoJdEdge(fileNew: MJdEdge, jdEdge0: MJdEdge): MJdEdge = {
    if (
      (fileNew.nodeId ==* jdEdge0.nodeId) &&
      (jdEdge0.fileSrv ==* fileNew.fileSrv)
    ) {
      jdEdge0

    } else {
      (
        (MJdEdge.nodeId replace fileNew.nodeId) andThen
        MJdEdge.fileSrv.modify { fileSrvOpt0 =>
          fileSrvOpt0.fold( fileNew.fileSrv ) { fileSrv0 =>
            Some(fileSrv0 updateFrom fileNew.fileSrv.get)
          }
        }
      )( jdEdge0 )
    }
  }

}

package io.suggest.n2.edge.edit

import diode.ModelRO
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.file.MSrvFileInfo
import io.suggest.jd.{MJdEdge, MJdEdgeId}
import io.suggest.lk.c.UploadAh
import io.suggest.lk.m.MErrorPopupS
import io.suggest.lk.m.img.MUploadAh
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.{EdgeUid_t, MEdge, MEdgeDataJs, MEdgeDoc, MPredicates}
import io.suggest.n2.edge.edit.c.{EdgeEditAh, ErrorDiaAh, FileExistAh}
import io.suggest.n2.edge.edit.m.{MEdgeEditRoot, MEdgeEditS, PredicateSet}
import io.suggest.n2.edge.edit.u.{EdgeEditApiHttp, IEdgeEditApi}
import io.suggest.n2.media.MEdgeMedia
import io.suggest.n2.media.storage.{MStorageInfo, MStorageInfoData, MStorages}
import io.suggest.pick.ContentTypeCheck
import io.suggest.routes.routes
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.{CircuitUtil, DoNothingActionProcessor, OptFastEq, StateInp}
import io.suggest.up.{IUploadApi, MFileUploadS, UploadApiHttp}
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import org.scalajs.dom.window

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:50
  * Description: Circuit для формы заливки файла.
  */
class EdgeEditCircuit
  extends CircuitLog[MEdgeEditRoot]
  with ReactConnector[MEdgeEditRoot]
{

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.FORM_ERROR

  override protected def initialModel: MEdgeEditRoot = {
    (for {
      stateInp    <- StateInp.find()
      stateValue  <- stateInp.value
      state0      <- Json
        .parse( stateValue )
        .asOpt[MEdgeEditFormInit]
    } yield {
      val edge1 =  state0.edge getOrElse MEdge( MPredicates.values.head )
      MEdgeEditRoot(
        edge = edge1,
        conf = state0.edgeId,
        edit = MEdgeEditS(
          nodeIds  = edge1.nodeIds.toList,
        ),
      )
    })
      .get
  }


  private val edgeEditApi: IEdgeEditApi = new EdgeEditApiHttp
  private val uploadApi: IUploadApi = new UploadApiHttp

  private val rootRW = zoomRW(identity)( (_, v2) => v2 )( MEdgeEditRoot.EdgeEditRootFastEq )
  private val editRW = CircuitUtil.mkLensRootZoomRW( this, MEdgeEditRoot.edit )( MEdgeEditS.MEdgeEditFastEq )

  private val confRO = CircuitUtil.mkLensRootZoomRO( this, MEdgeEditRoot.conf )( MNodeEdgeIdQs.MNodeEdgeIdQsFeq )

  private val ctxIdOptRO = zoom(_ => Option.empty[String])


  private val mUploadRW = {
    val jdEdgeId = MJdEdgeId( edgeUid = 0 )
    val edgeDoc = MEdgeDoc(
      id = Some( jdEdgeId.edgeUid ),
    )
    val jdEdgeIdSome = Some( jdEdgeId )

    zoomRW [MUploadAh[Option[MJdEdgeId]]] { mroot =>
      val jdEdge = MEdgeDataJs(
        jdEdge = MJdEdge(
          predicate = mroot.edge.predicate,
          edgeDoc   = edgeDoc,
          fileSrv   = for {
            edgeMedia <- mroot.edge.media
            nodeId    <- mroot.edge.nodeIds.headOption
          } yield {
            MSrvFileInfo(
              nodeId      = nodeId,
              fileMeta    = edgeMedia.file,
              pictureMeta = edgeMedia.picture,
            )
          },
        ),
        fileJs = mroot.edit.fileJs,
      )
      MUploadAh(
        edges       = Map.empty[EdgeUid_t, MEdgeDataJs] + (jdEdgeId.edgeUid -> jdEdge),
        view        = jdEdgeIdSome,
        errorPopup  = mroot.edit.errorDia,
        cropPopup   = None,
        histograms  = Map.empty,
      )

    } { (mroot, v2) =>
      // Обновление состояния после upload-действия.
      v2.edges
        .get( jdEdgeId.edgeUid )
        .fold {
          // Нет данных файла. Удалить файл из состояния.
          (
            MEdgeEditRoot.edit
              .modify(
                MEdgeEditS.errorDia.set( v2.errorPopup ) andThen
                MEdgeEditS.upload.set( MFileUploadS.empty )
              ) andThen
            MEdgeEditRoot.edge
              .composeLens( MEdge.media )
              .set( None )
          )(mroot)

        } { edgeDataJs2 =>
          // Есть/выставлен файл. Залить данные в эдж.
          val rootEditUpdateF = MEdgeEditRoot.edit.modify(
            MEdgeEditS.fileJs
              .set( edgeDataJs2.fileJs ) andThen
            MEdgeEditS.errorDia
              .set( v2.errorPopup ) andThen
            MEdgeEditS.upload
              .set( edgeDataJs2.fileJs.fold(MFileUploadS.empty)(_.upload) )
          )

          val nodeIdOpt2 = edgeDataJs2.jdEdge.fileSrv.map(_.nodeId)

          (nodeIdOpt2.fold {
            // Какая-то ошибка, должно быть.
            rootEditUpdateF
          } { nodeId =>
            if (nodeId ==* mroot.conf.nodeId) {
              // После реальной заливки эдж текущего обновляется на сервере. Нужно отразить эти изменения визуально в редакторе.
              (for {
                uploadExtraStr <- v2.uploadExtra
                uploadExtra <- Json
                  .parse( uploadExtraStr )
                  .asOpt[MEdgeWithId]
              } yield {
                (
                  rootEditUpdateF andThen
                  MEdgeEditRoot.edge.set( uploadExtra.edge ) andThen
                  MEdgeEditRoot.conf.set( uploadExtra.edgeId ) andThen
                  MEdgeEditRoot.edit
                    .composeLens( MEdgeEditS.nodeIds )
                    .set( uploadExtra.edge.nodeIds.toSeq )
                )
              })
                .getOrElse {
                  LOG.warn("Invalid/missing upload.extra: " + v2.uploadExtra)
                  // Не ясно, нужна ли эта ветвь.
                  (
                    rootEditUpdateF andThen
                    MEdgeEditRoot.edge.modify(
                      MEdge.predicate
                        .set( edgeDataJs2.jdEdge.predicate ) andThen
                      MEdge.media.modify { edgeMediaOpt0 =>
                        for (srvInfo <- edgeDataJs2.jdEdge.fileSrv) yield {
                          val storInfo = srvInfo.storage getOrElse {
                            LOG.warn("SrvInfo.storage is empty")
                            MStorageInfo( MStorages.SeaWeedFs, MStorageInfoData(meta = "") )
                          }
                          edgeMediaOpt0.fold {
                            MEdgeMedia(
                              storage = storInfo,
                              file    = srvInfo.fileMeta,
                              picture = srvInfo.pictureMeta,
                            )
                          } { edgeMedia0 =>
                            edgeMedia0.copy(
                              storage = storInfo,
                              file    = srvInfo.fileMeta,
                              picture = srvInfo.pictureMeta,
                            )
                          }
                        }
                      }
                    )
                  )
                }

            } else {
              // Если id узла не соответствует текущему, значит этот файл уже загружался ранее и живёт в другом узле.
              // Нужно просто сослаться на этот узел в списке узлов эджа.
              (
                rootEditUpdateF andThen
                MEdgeEditRoot.edit
                  .composeLens( MEdgeEditS.fileExistNodeId )
                  .set( nodeIdOpt2 )
              )
            }
          })(mroot)
        }
    }
  }


  private val edgeEditAh = new EdgeEditAh(
    edgeEditApi = edgeEditApi,
    modelRW     = rootRW,
  )

  private val errorDiaAh = new ErrorDiaAh(
    modelRW = CircuitUtil.mkLensZoomRW( editRW, MEdgeEditS.errorDia )( OptFastEq.Wrapped(MErrorPopupS.MErrorPopupSFastEq) ),
  )

  private val uploadAh = new UploadAh(
    prepareUploadRoute = { _ =>
      routes.controllers.SysNodeEdges.prepareUploadFile( _confQs() )
    },
    uploadApi = uploadApi,
    contentTypeCheck = ContentTypeCheck.AllowAll,
    ctxIdOptRO = ctxIdOptRO,
    modelRW = mUploadRW,
  )

  private val fileExistAh = new FileExistAh(
    modelRW = editRW,
  )


  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      edgeEditAh,
      uploadAh,
      fileExistAh,
      errorDiaAh,
    )
  }


  addProcessor( DoNothingActionProcessor[MEdgeEditRoot] )

  private def _confQs(edgeIdRO: ModelRO[MNodeEdgeIdQs] = confRO) = {
    PlayJsonSjsUtil.toNativeJsonObj(
      Json.toJsObject( edgeIdRO.value )
    )
  }

  // Подписаться на изменение конфига: если изменился конфиг, то document.location должен быть обновлён.
  if ( Try(window.history.length).isSuccess ) {
    subscribe( confRO ) { confProxy =>
      val url = routes.controllers.SysNodeEdges
        .editEdge( _confQs(confProxy) )
        .absoluteURL( true )
      window.history.replaceState( js.Dynamic.literal(), "", url )
    }
  }

  // Если загружен файл, то надо выставить предикат в File
  subscribe( editRW.zoom(_.upload.currentReq) ) { currUploadPotProxy =>
    val mroot = rootRW.value
    if (
      (!(mroot.edge.predicate eqOrHasParent MPredicates.File)) &&
      (currUploadPotProxy.value !=* Pot.empty)
    ) {
      dispatch( PredicateSet( MPredicates.File ) )
    }
  }

}

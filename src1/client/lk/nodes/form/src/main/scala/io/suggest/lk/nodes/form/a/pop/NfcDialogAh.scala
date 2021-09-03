package io.suggest.lk.nodes.form.a.pop

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRO, ModelRW}
import io.suggest.dev.MOsFamilies
import io.suggest.lk.nodes.MLknConf
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.lk.nodes.form.m.{MNfcDiaS, MNfcOperation, MNfcOperations, NfcDialog, NfcWrite, NodesDiConf}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.nfc.{INfcApi, NfcPendingState, NfcScanProps}
import io.suggest.proto.http.client.HttpClient
import io.suggest.routes.routes
import io.suggest.sc.ScConstants
import io.suggest.spa.DiodeUtil.Implicits.{ActionHandlerExt, PotOpsExt}
import io.suggest.spa.{DoNothing, SioPages}
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.univeq._
import play.api.libs.json.Json

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.JavaScriptException
import scala.util.{Success, Try}

/** Controller for dialog of NFC writing. */
final class NfcDialogAh[M](
                            diConfig       : NodesDiConf,
                            modelRW        : ModelRW[M, Option[MNfcDiaS]],
                            currNodeIdRO   : ModelRO[Option[String]],
                            confRO         : ModelRO[MLknConf],
                            dispatcher     : Dispatcher,
                          )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** NFC Operation starter. Make things happen depending on NfcOperation passed. */
  private def _doNfcOperation(nfcApi: INfcApi): PartialFunction[MNfcOperation, NfcPendingState] = {
    // Write showcase url and app id into NFC tag.
    case MNfcOperations.WriteShowcase =>
      val conf = confRO.value

      // Prepare NDEF message records before writing:
      val writeNdefMessage = {
        // URI Record:
        nfcApi.uriRecord(
          uri = {
            val state = SioPages.Sc3(
              nodeId      = currNodeIdRO.value orElse conf.onNodeId,
              focusedAdId = diConfig.contextAdId(),
            )
            val jsRouteArgsObj = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(state) )
            val absUrl = routes.controllers.sc.ScSite
              .geoSite(
                scJsState = jsRouteArgsObj,
              )
              .absoluteURL( secure = true )
            val absUrl2 = ScConstants.ScJsState.fixJsRouterUrl( absUrl )
            HttpClient.mkAbsUrl( absUrl2 )
          }
        ) ::
        // AAR for app open/install on android:
        nfcApi.androidApplicationRecord( ScConstants.App.APP_ID ) ::
        Nil
      }

      nfcApi.write( writeNdefMessage )

    // Making NFC tag immutable:
    case MNfcOperations.MakeReadOnly =>
      // TODO Check current message, if NFC tag currently contains any valid data.
      nfcApi.makeReadOnly()

  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Open/close dialog.
    case m: NfcDialog =>
      val v0Opt = value

      if (m.isOpen ==* v0Opt.isDefined) {
        logger.log( ErrorMsgs.ALREADY_DONE, msg = (m, v0Opt) )
        noChange

      } else {
        val v2Opt = Option.when( m.isOpen )( MNfcDiaS() )

        val fxOpt = for {
          v0        <- v0Opt
          cancelF   <- v0.cancelF
        } yield {
          Effect.action {
            for (ex <- Try( cancelF() ).failed)
              logger.error( ErrorMsgs.NFC_API_ERROR, ex, m )

            DoNothing
          }
        }

        ah.updatedMaybeEffect( v2Opt, fxOpt )
      }


    // NFC start writing.
    case m: NfcWrite =>
      (for {
        v0 <- value
      } yield {
        if (m.op.isEmpty || m.state.isUnavailable) {
          // Cancel writing by user.
          v0.cancelF.fold {
            // Not cancellable. Just clean-up current state.
            val v2 = (MNfcDiaS.writing set Pot.empty)(v0)
            updated( Some(v2) )
          } { cancelF =>
            val fx = Effect.action {
              for (ex <- Try( cancelF() ).failed)
                logger.error( ErrorMsgs.NFC_API_ERROR, ex, m )
              m
            }
            val v2 = (
              (MNfcDiaS.writing set m.state.pending()) andThen
              (MNfcDiaS.cancelF set None)
            )(v0)
            updated( Some(v2), fx )
          }

        } else if (m.state.isPending) {
          var modF = (MNfcDiaS.writing set m.state)

          // Save/update cancelling function:
          for (res <- m.state)
            modF = modF andThen (MNfcDiaS.cancelF set res.cancel)

          val v2 = modF(v0)
          updated( Some(v2) )

        } else if (m.state.isReady) {
          // Successfully done writing.
          val v2 = (
            (MNfcDiaS.writing set m.state) andThen
            (MNfcDiaS.cancelF set None)
          )(v0)
          updated( Some(v2) )

        } else if (m.state.isFailed) {
          // Failed to write data into NFC-tag.
          logger.error( ErrorMsgs.NFC_API_ERROR, msg = m )
          val v2 = (
            (MNfcDiaS.writing set m.state) andThen
            (MNfcDiaS.cancelF set None)
          )(v0)
          updated( Some(v2) )

        } else {
          // state==Pot.empty. Start writing.
          val state2 = m.state.pending()

          // Start waiting for NFC-tag. Returns cancelF into state.
          val nfcScanWriteTagFx = Effect {
            val nfcApi = diConfig.nfcApi.get
            val writingFinishedP = Promise[Any]()

            // Update cancelF instance in controller's state:
            def __dispatchPendingState(pendingState: NfcPendingState) = {
              val action = NfcWrite.state.modify(
                _.ready( pendingState )
                  .pending()
              )(m)
              dispatcher.dispatch( action )
            }

            // Execute write operation, dispatch cancelF into controller's state.
            def __doWriteOperation(): NfcPendingState = {
              val opPendingState = _doNfcOperation( nfcApi )( m.op.get )
              writingFinishedP completeWith opPendingState.result
              __dispatchPendingState( opPendingState )
              opPendingState
            }

            val appOsFamily = diConfig.appOsFamily
            val fut: Future[NfcPendingState] = appOsFamily match {
              // iOS: writing simplified to minify native API usage.
              // https://github.com/chariotsolutions/phonegap-nfc#ios---simple
              case Some( MOsFamilies.Apple_iOS ) =>
                val writePendingState = __doWriteOperation()

                writePendingState
                  .result
                  // Return normal pending state container:
                  .map(_ => writePendingState)

              // android | web-browser.
              case Some( MOsFamilies.Android ) | None =>
                // "On Android, write() must be called inside an event handler" - https://github.com/chariotsolutions/phonegap-nfc#android
                lazy val writePendingState = __doWriteOperation()

                val scanPendingState = nfcApi.scan( NfcScanProps(
                  onMessage = { _ =>
                    writePendingState
                  },
                  onError = Some { nfcError =>
                    // Notify about scanning errors, but continue for scanning:
                    val ex = new JavaScriptException( nfcError )
                    val action = NfcWrite.state.modify(
                      _.fail( ex )
                        .pending()
                    )(m)
                    dispatcher.dispatch( action )
                  },
                  // For iOS: https://github.com/chariotsolutions/phonegap-nfc#ios---read-and-write
                  // Not needed here, really.
                  keepSessionOpen = Some(true),
                ))

                // Respond to controller with initial cancelF() function instance:
                __dispatchPendingState( scanPendingState )

                for {
                  _ <- scanPendingState.result
                  _ <- writingFinishedP.future
                } yield {
                  writePendingState
                }
            }

            // Compose overall effect over all futures/promises:
            fut
              .transform { tryRes =>
                // keepSessionOpen was defined above, so call clearSessionOpen() here to finalize all stuff
                // for iOS needs. https://github.com/chariotsolutions/phonegap-nfc#nfcinvalidatesession
                // > [NFCNDEFReaderSession beginSessionWithConfig:]:365  error:Error Domain=NFCError Code=202 "Session invalidated unexpectedly"
                // Possibly error inactual, nothing should be cleared, because NFC reading session was not started for iOS,
                // but for writing NFC session should be started and closed automatically.
                Try {
                  nfcApi
                    .clearSessionOpen()
                    .recover { case ex =>
                      logger.error( ErrorMsgs.NFC_API_ERROR, ex, (appOsFamily, tryRes) )
                    }
                }

                val action = NfcWrite.state.modify( _ withTry tryRes )(m)
                Success( action )
              }
          }

          val v2 = (MNfcDiaS.writing set state2)(v0)
          updated( Some(v2), nfcScanWriteTagFx )
        }
      })
        .getOrElse {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        }

  }


}

package io.suggest.ad.edit.c

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.ad.edit.m.{EventActionAdAddRemove, EventAskMoreAds, EventNodeIdSet, EventOnOff, MAdEditFormConf}
import io.suggest.ad.edit.m.edit.{MDocS, MEditorsS, MEventsEdit, MJdDocEditS}
import io.suggest.ads.{LkAdsFormConst, MLkAdsOneAdResp}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.jd.{MJdDoc, MJdEdge, MJdEdgeId}
import io.suggest.jd.render.m.{MJdArgs, MJdDataJs}
import io.suggest.jd.render.u.JdUtil
import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.event.{MJdActionTypes, MJdtAction, MJdtEventActions, MJdtEventInfo, MJdtEvents}
import io.suggest.lk.api.ILkAdsApi
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.{EdgeUid_t, EdgesUtil, MEdgeDataJs, MEdgeDoc, MPredicates}
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import scalaz.Tree

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.2021 17:57
  * Description: Контроллер редактора событий.
  */
final class EventsEditorAh[M](
                               modelRW  : ModelRW[M, MDocS],
                               api      : => ILkAdsApi,
                               confRO   : ModelRO[MAdEditFormConf],
                             )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал переключения события.
    case m: EventOnOff =>
      val v0 = value

      (for {
        loc0 <- v0.jdDoc.jdArgs.selJdt.treeLocOpt
        jdt0 = loc0.getLabel
        if jdt0.name.isEventsAllowed

        _predicateByTypeF = (ea: MJdtEventActions) => ea.event.eventType ==* m.eventType
        haveEventListener = jdt0.events.events.exists(_predicateByTypeF)

        // Нечего менять?
        if {
          val r = haveEventListener !=* m.checked
          if (!r) logger.log( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, jdt0, haveEventListener, m.checked) )
          r
        }

      } yield {
        var fxAcc = List.empty[Effect]

        // Выставить/удалить реакцию на событие из тега:
        val jdDataJsModF = if (m.checked) {
          // При возможном добавлении нового эджа, тут храним его uid:
          val nextEdgeUid = EdgesUtil.nextEdgeUidFromMap( v0.jdDoc.jdArgs.data.edges )
          val action = MJdtAction(
            action   = MJdActionTypes.InsertAds,
            jdEdgeIds = MJdEdgeId( nextEdgeUid ) :: Nil,
          )

          val jdTree2 = loc0
            .modifyLabel(
              _jdt_events_events_LENS.modify { events0 =>
                MJdtEventActions(
                  event   = MJdtEventInfo( m.eventType ),
                  // Тут сразу добавляется единственный экшен.
                  actions = action :: Nil,
                ) +: events0
              }
            )
            .toTree

          val jdEdge = _emptyAdEdge( nextEdgeUid )

          var vModF =  _mdoc_jdDoc_jdArgs_data.modify(
            (_doc_tpl_LENS set jdTree2) andThen
            MJdDataJs.edges.modify(_ + (nextEdgeUid -> jdEdge))
          )

          // Если нужно загружать карточки, то запустить эффект подгрузки:
          val evEdit0 = v0.editors.events
          if (
            action.action.isAdsChoose &&
            evEdit0.hasMoreAds &&
            !evEdit0.adsAvail.isPending
          ) {
            vModF = vModF andThen _doc_editors_events_adsAvail_LENS.modify( _.pending() )
            fxAcc ::= _askAllAdsFx( evEdit0 )
          }

          vModF

        } else {
          // Удалить поддержку указанного типа событий.
          val jdTree2 = loc0
            .modifyLabel(
              _jdt_events_events_LENS.modify { events0 =>
                events0.filterNot( _predicateByTypeF )
              }
            )
            .toTree

          _mdoc_jdDoc_jdArgs_data.modify(
            (_doc_tpl_LENS set jdTree2) andThen
            MJdDataJs.edges.modify( JdTag.purgeUnusedEdges( jdTree2, _ ) )
          )
        }

        val v2 = jdDataJsModF(v0)
        ah.updatedMaybeEffect( v2, fxAcc.mergeEffects )
      })
        .getOrElse( noChange )


    // Выставление id карточки.
    case m: EventNodeIdSet =>
      val v0 = value

      (for {
        mJdEdgeUid <- m.eventPtr.jdEdgeId
        loc0 <- v0.jdDoc.jdArgs.selJdt.treeLocOpt
        jdt0 = loc0.getLabel
        eventActions0 <- jdt0.events.events.find(_.event ===* m.eventPtr.eventInfo)
        action0 <- eventActions0.actions.find(_ ===* m.eventPtr.action)
        if action0.jdEdgeIds contains[MJdEdgeId] mJdEdgeUid
        jdEdge0 <- v0.jdDoc.jdArgs.data.edges.get( mJdEdgeUid.edgeUid )
        if jdEdge0.jdEdge.nodeId.fold(true)( _ !=* m.nodeId )
      } yield {
        // Обновить новый nodeId, залив его в эдж:
        val jdEdge2 = MEdgeDataJs.jdEdge
          .composeLens( MJdEdge.nodeId )
          .set( Option(m.nodeId) )(jdEdge0)

        val v2 = _mdoc_jdDoc_jdArgs_data
          .composeLens( MJdDataJs.edges )
          .modify(_ + (mJdEdgeUid.edgeUid -> jdEdge2))(v0)

        updated( v2 )
      })
        .getOrElse {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        }


    // Ход обработки запросов
    case m: EventAskMoreAds =>
      val v0 = value

      m.resp
        .exceptionOption
        .map { ex =>
          val v2 = _doc_editors_events_adsAvail_LENS.modify(_.fail(ex))(v0)
          updated(v2)
        }
        .orElse {
          for (respSeq <- m.resp.toOption) yield {
            // Положительный ответ: докинуть карточек в состояние.
            val adsAppend = for (oneAdResp <- respSeq) yield {
              MJdDataJs.fromJdData( oneAdResp.jdAdData )
            }
            val eventsEd0 = v0.editors.events

            val adsAvail0 = eventsEd0.adsAvail
            val adsAvailV2 = adsAvail0.fold(adsAppend)(_ ++ adsAppend)

            var modF = (MEventsEdit.adsAvail set adsAvail0.ready(adsAvailV2))

            // Обновить значение флага hasMoreAds:
            if (
              (adsAppend.lengthIs < LkAdsFormConst.GET_ADS_COUNT_PER_REQUEST) &&
              v0.editors.events.hasMoreAds
            ) {
              modF = modF andThen (MEventsEdit.hasMoreAds set false)
            }

            val jdRuntime = {
              var jdRuntimePrep = JdUtil
                .prepareJdRuntime( MEventsEdit.ADS_JD_CONF )
                .docs(
                  adsAvailV2
                    .iterator
                    .map(_.doc)
                    .to( LazyList )
                )
                .cssRelBlocks
                .cssNameGen( _ + "-ev" )

              for (prevJdRuntime <- eventsEd0.jdRuntime)
                jdRuntimePrep = jdRuntimePrep.prev( prevJdRuntime )

              jdRuntimePrep.make
            }

            modF = modF andThen (MEventsEdit.jdRuntime set Some(jdRuntime))

            val v2 = MDocS.editors
              .composeLens( MEditorsS.events )
              .modify( modF )(v0)

            updated(v2)
          }
        }
        .orElse {
          val eventsEd0 = v0.editors.events
          Option.when(
            eventsEd0.hasMoreAds &&
            (m.resp ==* Pot.empty)
          ) {
            val v2 = _doc_editors_events_adsAvail_LENS.modify(_.pending())( v0 )
            val fx = _askAllAdsFx( eventsEd0 )
            updatedSilent( v2, fx )
          }
        }
        .getOrElse {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        }


    // Работа с кнопками добавления/удаления карточки в списке карточек одного действия.
    case m: EventActionAdAddRemove =>
      val v0 = value

      // Найти указанную карточку.
      (for {
        loc0 <- v0.jdDoc.jdArgs.selJdt.treeLocOpt
      } yield {

        // Обновить дерево.
        def __mkTreeUpdated(modActionF: MJdtAction => MJdtAction): Tree[JdTag] = {
          loc0
            .modifyLabel(
              JdTag.events
                .composeLens( MJdtEvents.events )
                .modify { jdtEvents0 =>
                  for (evData <- jdtEvents0) yield {
                    if (evData.event ===* m.eventPtr.eventInfo) {
                      // Найти действие внутри текущего события:
                      MJdtEventActions.actions.modify { jdtActions0 =>
                        for (actionData <- jdtActions0) yield {
                          if (m.eventPtr.action ===* actionData) {
                            // Обновить данное действие с карточкой:
                            modActionF( actionData )
                          } else {
                            // Не этот экшен, другой...
                            actionData
                          }
                        }
                      }(evData)
                    } else {
                      // Не это событие, текущее событие пропускаем.
                      evData
                    }
                  }
                }
            )
            .toTree
        }

        val v2 = _mdoc_jdDoc_jdArgs_data.modify(
          if (m.isAdd) {
            val nextEdgeUid = EdgesUtil.nextEdgeUidFromMap( v0.jdDoc.jdArgs.data.edges )
            val jdEdge = _emptyAdEdge( nextEdgeUid )

            val tree2 = __mkTreeUpdated {
              MJdtAction.edgeUids
                .modify( _ :+ MJdEdgeId(nextEdgeUid) )
            }

            (_doc_tpl_LENS set tree2) andThen
            MJdDataJs.edges.modify(_ + (nextEdgeUid -> jdEdge))

          } else {
            val tree2 = __mkTreeUpdated {
              MJdtAction.edgeUids.modify { jdEdgeIds0 =>
                // Удалить указанную карточку:
                m.eventPtr.jdEdgeId.fold( List.empty[MJdEdgeId] ) { jdEdgeIdsToDel =>
                  jdEdgeIds0.filter(_ !===* jdEdgeIdsToDel)
                }
              }
            }
            (_doc_tpl_LENS set tree2) andThen
            MJdDataJs.edges.modify( JdTag.purgeUnusedEdges( tree2, _ ) )
          }
        )(v0)

        updated(v2)
      })
        .getOrElse {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        }

  }


  private def _doc_editors_events_adsAvail_LENS = {
    MDocS.editors
      .composeLens( MEditorsS.events )
      .composeLens( MEventsEdit.adsAvail )
  }

  private def _jdt_events_events_LENS = {
    JdTag.events
      .composeLens( MJdtEvents.events )
  }

  private def _mdoc_jdDoc_jdArgs_data = {
    MDocS.jdDoc
      .composeLens( MJdDocEditS.jdArgs )
      .composeLens( MJdArgs.data )
  }

  private def _doc_tpl_LENS = {
    MJdDataJs.doc
      .composeLens( MJdDoc.template )
  }


  private def _askAllAdsFx(eventsEd: MEventsEdit = value.editors.events,
                           pot0: Pot[Seq[MLkAdsOneAdResp]] = Pot.empty) = {
    Effect {
      val conf = confRO.value
      api
        .getAds(
          nodeKey = RcvrKey.fromNodeId( conf.producerId ),
          offset  = eventsEd.adsAvail.fold(0)(_.length),
        )
        .transform { tryRes =>
          Success( EventAskMoreAds( pot0.withTry(tryRes) ) )
        }
    }
  }


  private def _emptyAdEdge(nextEdgeUid: EdgeUid_t): MEdgeDataJs = {
    MEdgeDataJs(MJdEdge(
      predicate = MPredicates.JdContent.Ad,
      nodeId    = Some(""),
      edgeDoc   = MEdgeDoc(
        id = Some( nextEdgeUid ),
      ),
    ))
  }

}

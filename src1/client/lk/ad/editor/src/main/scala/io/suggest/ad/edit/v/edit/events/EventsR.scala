package io.suggest.ad.edit.v.edit.events

import com.materialui.{Mui, MuiCheckBox, MuiCheckBoxProps, MuiFormControlLabel, MuiFormControlLabelProps, MuiListItemIcon, MuiListItemText, MuiMenuItem, MuiMenuItemProps, MuiSelectClasses, MuiSelectProps, MuiTextField, MuiTextFieldProps, MuiTypoGraphy}
import diode.AnyAction.aType
import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.{EventNodeIdSet, EventOnOff}
import io.suggest.ad.edit.m.edit.{MDocS, MEventEditPtr, MEventsEdit}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.css.CssR
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdR}
import io.suggest.jd.tags.event.{MJdActionTypes, MJdtEventType, MJdtEventTypes, MJdtEvents}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.{EdgeUid_t, MEdgeDataJs}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits.VdomElOptionExt
import io.suggest.react.ReactDiodeUtil.Implicits.ModelProxyExt
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.2021 0:00
  * Description: Управление событиями редактора.
  */
final class EventsR(
                     jdR                    : JdR,
                     lkAdEditCss            : LkAdEditCss,
                     crCtxP                 : React.Context[MCommonReactCtx],
                   )
  extends Log
{

  /** Тип данных для необходимых компоненту данных по событиям. */
  protected case class EvData(
                               jdtEvents  : MJdtEvents,
                               edges      : Map[EdgeUid_t, MEdgeDataJs],
                               editor     : MEventsEdit,
                             )
  implicit val evDataFeq = FastEqUtil [EvData] { (a, b) =>
    (a.jdtEvents ===* b.jdtEvents) &&
    (a.editor ===* b.editor) &&
    (a.edges ==* b.edges)
  }

  type Props_t = MDocS
  type Props = ModelProxy[Props_t]

  case class State(
                    eventsEdgesC    : ReactConnectProxy[EvData],
                    jdCssOptC       : ReactConnectProxy[Option[JdCss]],
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Реакция на переключение перехвата события. */
    private def _onEventTypeCheckBoxChanged(eventType: MJdtEventType) = ReactCommonUtil.cbFun1ToJsCb {
      (e: ReactEventFromInput) =>
        val isChecked = e.target.checked
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, EventOnOff(eventType, isChecked) )
    }

    /** Реакция на редактирование nodeId. */
    private def _onChangeNodeId(eventPtr: MEventEditPtr) = ReactCommonUtil.cbFun1ToJsCb {
      (e: ReactEventFromInput) =>
        val nodeId = e.target.value
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, EventNodeIdSet(eventPtr, nodeId) )
    }


    def render(p: Props, s: State): VdomElement = {
      crCtxP.consume { crCtx =>
        lazy val _chooseMsg = crCtx.messages( MsgCodes.`Choose...` )
        lazy val _nonNativeSelectProps = new MuiSelectProps {
          override val native = false
          override val classes = new MuiSelectClasses {
            override val select = lkAdEditCss.inlineFlex.htmlClass
          }
        }

        // Элементы селекта для выбора типа экшена MJdActionType:
        lazy val jdActionTypesItems = MJdActionTypes
          .values
          .map[VdomElement] { actionType =>
            MuiMenuItem.component.withKey( actionType.value )(
              new MuiMenuItemProps {
                override val value = actionType.value
              }
            )(
              // Иконка:
              MuiListItemIcon()(
                (actionType match {
                  case MJdActionTypes.InsertAds => Mui.SvgIcons.AddBox
                })()(),
              ),
              MuiListItemText()(
                crCtx.messages( actionType.i18nCode ),
              ),
            )
          }

        // Тело редактора событий:
        <.div(

          // Отрендерить CSS для рендерящихся карточек:
          s.jdCssOptC { jdCssOptProxy =>
            jdCssOptProxy
              .value
              .whenDefinedEl { jdCss =>
                CssR.component( jdCss )
              }
          },

          // Непосредственная вёрстка редактора событий.
          s.eventsEdgesC { evDataProxy =>
            val evData = evDataProxy.value

            <.div(
              (for {
                eventType <- MJdtEventTypes.values
                eventsForType = evData.jdtEvents.events.filter(_.event.eventType ==* eventType)
              } yield {
                <.div(
                  ^.key := eventType.value,

                  MuiFormControlLabel.component {
                    val _eventOnOffCheckBox = MuiCheckBox(
                      new MuiCheckBoxProps {
                        override val checked = js.defined( eventsForType.nonEmpty )
                      }
                    )
                    val _label = MuiTypoGraphy()(
                      eventType.toString,
                    )

                    new MuiFormControlLabelProps {
                      override val label = _label.rawNode
                      override val control = _eventOnOffCheckBox.rawElement
                      override val onChange = _onEventTypeCheckBoxChanged( eventType )
                    }
                  },

                  (for {
                    (evActions, evI)   <- eventsForType.iterator.zipWithIndex
                  } yield {
                    <.div(
                      ^.key := evI,

                      (for {
                        (action, aI) <- evActions.actions.iterator.zipWithIndex
                      } yield {
                        <.div(
                          ^.key := aI,

                          MuiTextField(
                            new MuiTextFieldProps {
                              //override val disabled = true
                              override val value = action.action.value
                              override val select = true
                              override val SelectProps = _nonNativeSelectProps
                              override val label = crCtx.messages( MsgCodes.`Action` )
                            }
                          )(
                            jdActionTypesItems: _*
                          ),

                          (for {
                            (jdEdgeId, i) <- action.jdEdgeIds.zipWithIndex
                            evEditPtr = MEventEditPtr( evActions.event, action, Some(jdEdgeId) )
                            jdEdgeJs <- {
                              val r = evData.edges.get( jdEdgeId.edgeUid )
                              if (r.isEmpty) logger.warn( ErrorMsgs.EDGE_NOT_EXISTS, msg = jdEdgeId )
                              r
                            }
                            nodeId <- jdEdgeJs.jdEdge.nodeId
                          } yield {
                            MuiTextField.component.withKey( i )(
                              new MuiTextFieldProps {
                                // TODO value: Нужен эдж - найти в карте эджей, прочитать nodeId.
                                override val value        = nodeId
                                override val onChange     = _onChangeNodeId( evEditPtr )
                                override val label        = _chooseMsg.rawNode
                                override val select       = true
                                override val SelectProps  = _nonNativeSelectProps
                              }
                            )(
                              // Отрендерить карточки на выбор.
                              (for {
                                adsAvail      <- evData.editor.adsAvail.iterator
                                jdRuntime     <- evData.editor.jdRuntime.iterator
                                adJdDataJs    <- adsAvail.iterator
                                nodeId        <- adJdDataJs.doc.tagId.nodeId.iterator
                              } yield {
                                MuiMenuItem.component.withKey( nodeId )(
                                  new MuiMenuItemProps {
                                    override val value = nodeId
                                  }
                                )(
                                  // Отрендерить маленькую карточку. Где-то выше уже должен быть отрендерен JdCss.
                                  jdR {
                                    p.resetZoom {
                                      MJdArgs(
                                        data      = adJdDataJs,
                                        jdRuntime = jdRuntime,
                                        conf      = MEventsEdit.ADS_JD_CONF,
                                      )
                                    }
                                  }
                                )
                                  .vdomElement
                              })
                                .toSeq: _*
                            )
                          })
                            .toVdomArray,
                        )
                      })
                        .toVdomArray,

                    )
                  })
                    .toVdomArray,
                )
              })
                .toVdomArray,
            )
          },

        )
      }
    }
  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        eventsEdgesC = propsProxy.connect { props =>
          val jdtEvents = props.jdDoc.jdArgs.selJdt
            .treeLocOpt
            .fold( MJdtEvents.empty )( _.getLabel.events )

          val allEdgesMap = props.jdDoc.jdArgs.data.edges

          EvData(
            jdtEvents = jdtEvents,
            // Собрать только необходимые эджи. Без filterKeys, т.к. обычно тут полтора эджа максимум.
            edges = if (jdtEvents.events.isEmpty || allEdgesMap.isEmpty) {
              Map.empty
            } else {
              (for {
                event   <- jdtEvents.events.iterator
                action  <- event.actions
                jdEdgeId <- action.jdEdgeIds
                edge    <- allEdgesMap.get( jdEdgeId.edgeUid )
              } yield {
                (jdEdgeId.edgeUid, edge)
              })
                .toMap
            },
            editor = props.editors.events,
          )
        } ( evDataFeq ),

        jdCssOptC = propsProxy.connect { props =>
          props.editors.events.jdRuntime
            .map(_.jdCss)
        }( OptFastEq.Plain ),

      )
    }
    .renderBackend[Backend]
    .build

}

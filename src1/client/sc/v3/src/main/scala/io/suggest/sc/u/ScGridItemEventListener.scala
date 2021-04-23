package io.suggest.sc.u

import diode.react.ModelProxy
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.jd.render.v.JdEventListener
import io.suggest.jd.tags.event.{MJdActionTypes, MJdtEventActions}
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.grid.{GridAdKey_t, GridBlockClick, MGridItem}
import japgolly.scalajs.react.{Callback, ReactEvent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.2021 16:03
  * Description: Реализация event listener'а для карточек плитки.
  */
final class ScGridItemEventListener(
                                     propsProxy   : ModelProxy[_],
                                     gridItem     : MGridItem,
                                     gridPath     : List[GridAdKey_t],
                                     jdDataJs     : MJdDataJs,
                                   )
  extends JdEventListener
{

  override def handleEvent(eventAction: MJdtEventActions)(e: ReactEvent): Callback = {
    Callback.when( eventAction.actions.nonEmpty ) {

      // Собрать итератор, который будет дожидаться результатов
      val dispatchActionsIter = for {
        jdAction <- eventAction.actions.iterator

        // Добавить новую карточку в плитку
        circuitAction <- jdAction.action match {

          // Вставка карточек вместо текущей. Произвести фокусировку на указанные id.
          case MJdActionTypes.InsertAds =>
            val adIdsIter = for {
              jdEdgeId  <- jdAction.jdEdgeIds.iterator
              // TODO edge, nodeId: Нужно передавать всё одном GBC(adId=[...]), чтобы не делать кучи запросов.
              edge      <- jdDataJs.edges.get( jdEdgeId.edgeUid )
              nodeId    <- edge.jdEdge.nodeId
            } yield {
              nodeId
            }

            // Option: возможно тут надо Iterable или что-то такое.
            // В будущем, по мере реализации MJdActionTypes это будет видно.
            Option.when( adIdsIter.nonEmpty ) {
              Callback.lazily {
                val gbc = GridBlockClick(
                  gridPath  = Some( gridPath ),
                  gridKey   = Some( gridItem.gridKey ),
                  adIds     = adIdsIter.toList,
                )
                propsProxy.dispatchCB( gbc )
              }
            }

        }

      } yield {
        circuitAction
      }

      // Если есть хотя бы один экшен, то нужен e.stopPropagation()
      Callback.when( dispatchActionsIter.nonEmpty ) {
        ReactCommonUtil.stopPropagationCB(e) >>
        Callback.sequence( dispatchActionsIter )
      }
    }
  }

}

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
    (for {
      action <- eventAction.actions.iterator
      cb <- action.action match {
        // Добавить новую карточку в плитку
        case MJdActionTypes.InsertAds =>
          for {
            jdEdgeId <- action.jdEdgeIds.iterator
            edge    <- jdDataJs.edges.get( jdEdgeId.edgeUid ).iterator
            nodeId  <- edge.jdEdge.nodeId.iterator
          } yield {
            val gbc = GridBlockClick(
              gridPath  = Some( gridPath ),
              gridKey   = Some( gridItem.gridKey ),
              adId      = Some( nodeId ),
            )
            propsProxy.dispatchCB( gbc )
          }
      }
    } yield {
      cb
    })
      .reduceLeftOption( _ >> _ )
      .fold( Callback.empty )( ReactCommonUtil.stopPropagationCB(e) >> _ )
  }

}

package io.suggest.adn.edit.m

import io.suggest.jd.MJdEdgeId
import io.suggest.lk.c.IPictureViewAdp
import io.suggest.lk.m.frk.MFormResourceKey
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 22:10
  * Description: Доп.утиль для модели MAdnResView.
  */
object MAdnResViewUtil extends Log {

  implicit object ResViewAdp extends IPictureViewAdp[MAdnResView] {

    override def get(view: MAdnResView, resKey: MFormResourceKey): Option[MJdEdgeId] = {
      resKey.jdEdgeId
    }

    override def updated(view: MAdnResView, resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): MAdnResView = {
      // Найти в view присланный инстанс jdEdgeId и обновить.
      val jdEdgeId4Update = resKey.jdEdgeId.get
      def __cmpF( jdEdgeId: MJdEdgeId ): Boolean = jdEdgeId ===* jdEdgeId4Update

      if ( view.logo.exists(__cmpF) ) {
        view.withLogo( newValue )

      } else if ( view.wcFg.exists(__cmpF) ) {
        view.withWcFg( newValue )

      } else if ( view.galImgs.exists(__cmpF) ) {
        // Обновить картинку в галерее.
        view.withGalImgs(
          view
            .galImgs
            .flatMap { e =>
              if (__cmpF(e)) newValue
              else e :: Nil
            }
        )

      } else {
        LOG.warn( WarnMsgs.MISSING_UPDATED_EDGE, msg = (view, resKey, newValue) )
        view
      }
    }

    override def forgetEdge(view: MAdnResView, edgeUid: EdgeUid_t): MAdnResView = {
      def __jdEdgeF(jdEdge: MJdEdgeId): Boolean =
        jdEdge.edgeUid !=* edgeUid

      view.copy(
        logo = view.logo.filter(__jdEdgeF),
        wcFg = view.wcFg.filter(__jdEdgeF),
        galImgs =  view.galImgs
          .filter( __jdEdgeF )
      )
    }

  }

}

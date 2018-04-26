package io.suggest.adn.edit.m

import io.suggest.jd.MJdEdgeId
import io.suggest.lk.c.IPictureViewAdp
import io.suggest.lk.m.frk.{MFormResourceKey, MFrkTypes}
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.sjs.common.log.Log
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 22:10
  * Description: Доп.утиль для модели MAdnResView.
  */
object MAdnResViewUtil extends Log {

  implicit object ResViewAdp extends IPictureViewAdp[MAdnResView] {

    override def get(view: MAdnResView, resKey: MFormResourceKey): Option[MJdEdgeId] = {
      resKey.frkType.get match {
        case MFrkTypes.Logo =>
          view.logo
        case MFrkTypes.WcFg =>
          view.wcFg
        case MFrkTypes.GalImg =>
          resKey.jdEdgeId.flatMap { jdEdgeId =>
            view.galImgs.find { e =>
              e.edgeUid ==* jdEdgeId.edgeUid
            }
          }
      }
    }

    override def updated(view: MAdnResView, resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): MAdnResView = {
      // Найти в view присланный инстанс jdEdgeId и обновить.
      resKey.frkType.get match {
        // Апдейт галереи.
        case MFrkTypes.Logo =>
          view.withLogo( newValue )
        case MFrkTypes.WcFg =>
          view.withWcFg( newValue )
        case MFrkTypes.GalImg =>
          // Нужно найти по id эджа, если он задан.
          resKey.jdEdgeId.fold {
            // Нет id. Добавить в начало галеры
            newValue.fold {
              // Нет добавляемой картинки. Ошибочная ситуация какая-то.
              view
            } { addEdgeId =>
              view.withGalImgs(
                addEdgeId +: view.galImgs
              )
            }
          } { existingEdgeId =>
            view.withGalImgs(
              view.galImgs.flatMap { galImg =>
                if (galImg.edgeUid ==* existingEdgeId.edgeUid) {
                  newValue
                } else {
                  galImg :: Nil
                }
              }
            )
          }
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

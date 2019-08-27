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
          resKey.edgeUid.flatMap { edgeUid =>
            view.galImgs.find { e =>
              e.edgeUid ==* edgeUid
            }
          }
      }
    }


    /** Обновить указатель на картинку, вернув обновлённый V. */
    override def updated(view: MAdnResView, resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): MAdnResView =
      updateF(resKey)(newValue)(view)

    def updateF(resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): MAdnResView => MAdnResView = {
      // Найти в view присланный инстанс jdEdgeId и обновить.
      resKey.frkType.get match {
        // Апдейт галереи.
        case MFrkTypes.Logo =>
          MAdnResView.logo.set( newValue )
        case MFrkTypes.WcFg =>
          MAdnResView.wcFg.set( newValue )
        case MFrkTypes.GalImg =>
          // Нужно найти по id эджа, если он задан.
          resKey.edgeUid.fold {
            // Нет id. Добавить в начало галеры
            newValue.fold[MAdnResView => MAdnResView] {
              // Нет добавляемой картинки. Ошибочная ситуация какая-то.
              identity
            } { addEdgeId =>
              MAdnResView.galImgs.modify(addEdgeId +: _)
            }
          } { existingEdgeUid =>
            MAdnResView.galImgs.modify { galImgs0 =>
              galImgs0.flatMap { galImg =>
                if (galImg.edgeUid ==* existingEdgeUid) {
                  newValue
                } else {
                  galImg :: Nil
                }
              }
            }
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

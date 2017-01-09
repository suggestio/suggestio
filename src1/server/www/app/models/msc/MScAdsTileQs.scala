package models.msc

import io.suggest.ad.search.AdSearchConstants._
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sc.ScConstants.ReqArgs.VSN_FN
import models.im.DevScreen
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 18:05
  * Description: qs-модель аргументов запроса плитки карточек sc ads tile.
  * Включает в себя как параметры поиска карточек, так и tile-специфичные опции рендера: screen например.
  */
object MScAdsTileQs {

  implicit def qsb(implicit
                   scAdsSearchArgsB : QueryStringBindable[MScAdsSearchQs],
                   devScreenOptB    : QueryStringBindable[Option[DevScreen]],
                   apiVsnB          : QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[MScAdsTileQs] = {
    new QueryStringBindableImpl[MScAdsTileQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScAdsTileQs]] = {
        val k = key1F(key)
        for {
          searchArgsE      <- scAdsSearchArgsB.bind  (key,                 params)
          if searchArgsE.right.exists(_.nonEmpty)
          apiVsnE          <- apiVsnB.bind           (k(VSN_FN),           params)
          devScreenOptE    <- devScreenOptB.bind     (k(SCREEN_INFO_FN),   params)
        } yield {
          for {
            searchArgs     <- searchArgsE.right
            apiVsn         <- apiVsnE.right
            devScreenOpt   <- devScreenOptE.right
          } yield {
            MScAdsTileQs(
              search       = searchArgs,
              apiVsn       = apiVsn,
              screen       = devScreenOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MScAdsTileQs): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Seq(
            scAdsSearchArgsB.unbind   (key,               value.search),
            apiVsnB.unbind            (k(VSN_FN),         value.apiVsn),
            devScreenOptB.unbind      (k(SCREEN_INFO_FN), value.screen)
          )
        }
      }

    }
  }

}


/** Контейнер аргументов запроса плитки карточек. */
case class MScAdsTileQs(
  search    : MScAdsSearchQs,
  screen    : Option[DevScreen] = None,
  apiVsn    : MScApiVsn         = MScApiVsns.unknownVsn
)

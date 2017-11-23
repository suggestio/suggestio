package models.msc

import io.suggest.ad.search.AdSearchConstants._
import io.suggest.sc.ScConstants.ReqArgs.VSN_FN
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sc.focus.MLookupMode
import models.im.DevScreen
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 22:13
  * Description: Модель забинденных qs-аргументов вызова sc-рендера focused-выдачи.
  * Модель очень похожа на [[MScAdsTileQs]].
  */
object MScAdsFocQs {

  /** Поддержка биндинга инстансов модели из URL qs. */
  implicit def mScAdsFocQsQsb(implicit
                              scAdsSearchArgsB : QueryStringBindable[MScAdsSearchQs],
                              devScreenOptB    : QueryStringBindable[Option[DevScreen]],
                              apiVsnB          : QueryStringBindable[MScApiVsn],
                              boolB            : QueryStringBindable[Boolean],
                              strB             : QueryStringBindable[String],
                              lookupModeOptB   : QueryStringBindable[Option[MLookupMode]]
                             ): QueryStringBindable[MScAdsFocQs] = {
    new QueryStringBindableImpl[MScAdsFocQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScAdsFocQs]] = {
        for {
          searchE             <- scAdsSearchArgsB.bind(key, params)
          k = key1F(key)
          apiVsnE             <- apiVsnB.bind       (k(VSN_FN),               params)
          focJumpAllowedE     <- boolB.bind         (k(FOC_JUMP_ALLOWED_FN),  params)
          lookupModeE         <- lookupModeOptB.bind(k(AD_LOOKUP_MODE_FN),    params)
          lookupAdIdE         <- strB.bind          (k(AD_ID_LOOKUP_FN),      params)
          screenE             <- devScreenOptB.bind (k(SCREEN_INFO_FN),       params)
        } yield {
          for {
            search            <- searchE.right
            focJumpAllowed    <- focJumpAllowedE.right
            lookupMode        <- lookupModeE.right
            lookupAdId        <- lookupAdIdE.right
            screen            <- screenE.right
            apiVsn            <- apiVsnE.right
          } yield {
            MScAdsFocQs(
              search          = search,
              focJumpAllowed  = focJumpAllowed,
              lookupMode      = lookupMode,
              lookupAdId      = lookupAdId,
              screen          = screen,
              apiVsn          = apiVsn
            )
          }
        }
      }

      override def unbind(key: String, value: MScAdsFocQs): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Seq(
            scAdsSearchArgsB.unbind (key,                     value.search),
            boolB.unbind            (k(FOC_JUMP_ALLOWED_FN),  value.focJumpAllowed),
            lookupModeOptB.unbind   (k(AD_LOOKUP_MODE_FN),    value.lookupMode),
            strB.unbind             (k(AD_ID_LOOKUP_FN),      value.lookupAdId),
            devScreenOptB.unbind    (k(SCREEN_INFO_FN),       value.screen),
            apiVsnB.unbind          (k(VSN_FN),               value.apiVsn)
          )
        }
      }

    }
  }

}


/**
  * Контейнер qs-аргументов запроса focused-выдачи.
  *
  * @param search Аргументы поиска карточек.
  * @param focJumpAllowed Допускается ли возвращать перескок в другое место вместо focused-выдачи.
  * @param lookupMode Режим поиска последовательности карточек на основе опорного id карточки.
  * @param lookupAdId Контроллер должен узнать начальные параметры текущей focused-выдачи для карточки с указанным id.
  * @param screen Данные по экрану устройства.
  * @param apiVsn Версия SC API.
  */
case class MScAdsFocQs(
                        search          : MScAdsSearchQs,
                        focJumpAllowed  : Boolean,
                        lookupMode      : Option[MLookupMode],
                        lookupAdId      : String,
                        screen          : Option[DevScreen],
                        apiVsn          : MScApiVsn         = MScApiVsns.unknownVsn
)

package io.suggest.sys.mdr

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.06.14 16:39
 * Description: Модель для представления данных запроса поиска карточек в контексте s.io-пост-модерации.
 */
object MdrSearchArgsJvm {

  implicit def mdrSearchArgsQsb(implicit
                                strOptB   : QueryStringBindable[Option[String]],
                                intOptB   : QueryStringBindable[Option[Int]],
                                boolOptB  : QueryStringBindable[Option[Boolean]]
                               ): QueryStringBindable[MdrSearchArgs] = {
    new QueryStringBindableImpl[MdrSearchArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MdrSearchArgs]] = {
        val k1 = key1F(key)
        val F = MdrSearchArgs.Fields
        for {
          maybeOffsetOpt        <- intOptB.bind  (k1(F.OFFSET_FN),                 params)
          maybeProducerIdOpt    <- strOptB.bind  (k1(F.PRODUCER_ID_FN),            params)
          maybeFreeAdvIsAllowed <- boolOptB.bind (k1(F.FREE_ADV_IS_ALLOWED_FN),    params)
          maybeHideAdIdOpt      <- strOptB.bind  (k1(F.HIDE_AD_ID_FN),             params)
        } yield {
          for {
            offsetOpt           <- maybeOffsetOpt.right
            prodIdOpt           <- maybeProducerIdOpt.right
            freeAdvIsAllowed    <- maybeFreeAdvIsAllowed.right
            hideAdIdOpt         <- maybeHideAdIdOpt.right
          } yield {
            MdrSearchArgs(
              offsetOpt         = offsetOpt,
              producerId        = prodIdOpt,
              isAllowed         = freeAdvIsAllowed,
              hideAdIdOpt       = hideAdIdOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MdrSearchArgs): String = {
        val k1 = key1F(key)
        val F = MdrSearchArgs.Fields
        _mergeUnbinded1(
          strOptB.unbind (k1(F.PRODUCER_ID_FN),          value.producerId),
          intOptB.unbind (k1(F.OFFSET_FN),               value.offsetOpt),
          boolOptB.unbind(k1(F.FREE_ADV_IS_ALLOWED_FN),  value.isAllowed),
          strOptB.unbind (k1(F.HIDE_AD_ID_FN),           value.hideAdIdOpt)
        )
      }
    }
  }

}

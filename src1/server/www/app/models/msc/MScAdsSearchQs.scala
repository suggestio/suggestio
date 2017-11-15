package models.msc

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import io.suggest.ad.search.AdSearchConstants._
import io.suggest.common.empty.EmptyProduct
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MLocEnv

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:34
  * Description: Аргументы поиска карточек, компилируемые в MNodeSearch.
  */
object MScAdsSearchQs {

  /** Поддержка интеграции с URL query string через play router. */
  implicit def mScAdsSearchQsQsb(implicit
                                 esIdOptB   : QueryStringBindable[Option[MEsUuId]],
                                 longOptB   : QueryStringBindable[Option[Long]],
                                 intOptB    : QueryStringBindable[Option[Int]],
                                 locEnvB    : QueryStringBindable[MLocEnv]
                                ): QueryStringBindable[MScAdsSearchQs] = {
    new QueryStringBindableImpl[MScAdsSearchQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScAdsSearchQs]] = {
        val k = key1F(key)
        for {
          prodIdOptE        <- esIdOptB.bind  (k(PRODUCER_ID_FN),     params)
          rcvrIdOptE        <- esIdOptB.bind  (k(RECEIVER_ID_FN),     params)
          locEnvE           <- locEnvB.bind   (k(LOC_ENV_FN),         params)
          genOptE           <- longOptB.bind  (k(GENERATION_FN),      params)
          limitOptE         <- intOptB.bind   (k(LIMIT_FN),           params)
          offsetOptE        <- intOptB.bind   (k(OFFSET_FN),          params)
          tagNodeIdOptE     <- esIdOptB.bind  (k(TAG_NODE_ID_FN),     params)
        } yield {
          for {
            prodIdOpt       <- prodIdOptE.right
            rcvrIdOpt       <- rcvrIdOptE.right
            locEnv          <- locEnvE.right
            genOpt          <- genOptE.right
            limitOpt        <- limitOptE.right
            offsetOpt       <- offsetOptE.right
            tagNodeIdOpt    <- tagNodeIdOptE.right
          } yield {
            MScAdsSearchQs(
              prodIdOpt     = prodIdOpt,
              rcvrIdOpt     = rcvrIdOpt,
              locEnv        = locEnv,
              genOpt        = genOpt,
              limitOpt      = limitOpt,
              offsetOpt     = offsetOpt,
              tagNodeIdOpt  = tagNodeIdOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MScAdsSearchQs): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Seq(
            esIdOptB.unbind   (k(PRODUCER_ID_FN),     value.prodIdOpt),
            esIdOptB.unbind   (k(RECEIVER_ID_FN),     value.rcvrIdOpt),
            locEnvB.unbind    (k(LOC_ENV_FN),         value.locEnv),
            longOptB.unbind   (k(GENERATION_FN),      value.genOpt),
            intOptB.unbind    (k(LIMIT_FN),           value.limitOpt),
            intOptB.unbind    (k(OFFSET_FN),          value.offsetOpt),
            esIdOptB.unbind   (k(TAG_NODE_ID_FN),     value.tagNodeIdOpt)
          )
        }
      }
    }
  }

}


/** Контейнер qs-аргументов для поиска карточек.
  *
  * @param prodIdOpt Опциональный id продьюсера.
  * @param rcvrIdOpt Опциональный id ресивера.
  * @param tagNodeIdOpt Опциональный id текущего тега.
  * @param locEnv Возможные данные по геолокации, маячкам и прочему окружению.
  * @param genOpt seed для псевдо-рандомной сортировки.
  * @param limitOpt Лимит выдачи карточек.
  * @param offsetOpt Сдвиг выдачи карточек.
  */
case class MScAdsSearchQs(
                           prodIdOpt     : Option[MEsUuId]     = None,
                           rcvrIdOpt     : Option[MEsUuId]     = None,
                           tagNodeIdOpt  : Option[MEsUuId]     = None,
                           locEnv        : MLocEnv             = MLocEnv.empty,
                           genOpt        : Option[Long]        = None,
                           limitOpt      : Option[Int]         = None,
                           offsetOpt     : Option[Int]         = None
)
  extends EmptyProduct
{

  /** Есть ли какие-то полезные данные для поиска карточек?
    * Если false, значит поисковый запрос на базе данных из этого инстанса вернёт вообще все карточки. */
  def hasAnySearchCriterias: Boolean = {
    rcvrIdOpt.nonEmpty ||
      locEnv.nonEmpty ||
      prodIdOpt.nonEmpty ||
      tagNodeIdOpt.nonEmpty
  }

}
